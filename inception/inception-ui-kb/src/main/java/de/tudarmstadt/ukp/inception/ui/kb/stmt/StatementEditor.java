/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb.stmt;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Iterator;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.ui.core.Focusable;
import de.tudarmstadt.ukp.inception.ui.kb.WriteProtectionBehavior;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxQualifierChangedEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxStatementChangedEvent;

public class StatementEditor
    extends Panel
{

    private static final long serialVersionUID = 7643837763550205L;
    private static final Logger LOG = LoggerFactory.getLogger(StatementEditor.class);

    private static final String CONTENT_MARKUP_ID = "content";

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KnowledgeBase> kbModel;
    private IModel<KBStatement> statement;
    private Component content;

    public StatementEditor(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<KBStatement> aStatement)
    {
        super(aId, aStatement);

        setOutputMarkupId(true);

        kbModel = aKbModel;
        statement = aStatement;

        // new statements start with edit mode right away
        boolean isNewStatement = statement.getObject().getOriginalTriples().isEmpty();
        if (isNewStatement) {
            EditMode editMode = new EditMode(CONTENT_MARKUP_ID, statement, true);

            // obtain AjaxRequestTarget and set the focus
            RequestCycle.get().find(AjaxRequestTarget.class)
                    .ifPresent(target -> target.focusComponent(editMode.getFocusComponent()));

            content = editMode;
        }
        else {
            content = new ViewMode(CONTENT_MARKUP_ID, statement);
        }
        add(content);
    }

    protected void actionEdit(AjaxRequestTarget aTarget)
    {
        // Edit mode works on a model of a shallow copy of the original statement. Any floating
        // changes to the statement are either persisted by saving or undone by canceling. In
        // conjunction with onchange AjaxFormComponentUpdatingBehaviours, this makes sure that
        // floating changes are persisted in the UI, meaning other statements can be added or
        // deleted while changes to this statement in the UI are not being reset.
        KBStatement shallowCopy = new KBStatement(statement.getObject());
        IModel<KBStatement> shallowCopyModel = Model.of(shallowCopy);

        EditMode editMode = new EditMode(CONTENT_MARKUP_ID, shallowCopyModel, false);
        content = content.replaceWith(editMode);
        aTarget.focusComponent(editMode.getFocusComponent());
        aTarget.add(this);
    }

    private void actionAddQualifier(AjaxRequestTarget aTarget, KBStatement aStatement)
    {
        KBQualifier qualifierPorto = new KBQualifier(aStatement);
        aStatement.addQualifier(qualifierPorto);
        aTarget.add(this);
    }

    private void actionCancelExistingStatement(AjaxRequestTarget aTarget)
    {
        content = content.replaceWith(new ViewMode(CONTENT_MARKUP_ID, statement));
        aTarget.add(this);
    }

    private void actionCancelNewStatement(AjaxRequestTarget aTarget)
    {
        // send a delete event to trigger the deletion in the UI
        AjaxStatementChangedEvent deleteEvent = new AjaxStatementChangedEvent(aTarget,
                statement.getObject(), this, true);
        send(getPage(), Broadcast.BREADTH, deleteEvent);
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<KBStatement> aForm)
    {
        KBStatement modifiedStatement = aForm.getModelObject();
        try {
            String language = aForm.getModelObject().getLanguage() != null
                    ? aForm.getModelObject().getLanguage()
                    : kbModel.getObject().getDefaultLanguage();
            modifiedStatement.setLanguage(language);

            // persist the modified statement and replace the original, unchanged model
            kbService.upsertStatement(kbModel.getObject(), modifiedStatement);
            statement.setObject(modifiedStatement);
            // switch back to ViewMode and send notification to listeners
            actionCancelExistingStatement(aTarget);
            send(getPage(), Broadcast.BREADTH,
                    new AjaxStatementChangedEvent(aTarget, statement.getObject()));
        }
        catch (RepositoryException e) {
            error("Unable to update statement: " + e.getLocalizedMessage());
            LOG.error("Unable to update statement.", e);
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private void actionDelete(AjaxRequestTarget aTarget)
    {
        try {
            kbService.deleteStatement(kbModel.getObject(), statement.getObject());

            AjaxStatementChangedEvent deleteEvent = new AjaxStatementChangedEvent(aTarget,
                    statement.getObject(), this, true);
            send(getPage(), Broadcast.BREADTH, deleteEvent);
        }
        catch (RepositoryException e) {
            error("Unable to delete statement: " + e.getLocalizedMessage());
            LOG.error("Unable to delete statement.", e);
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private void actionMakeExplicit(AjaxRequestTarget aTarget)
    {
        try {
            // add the statement as-is to the knowledge base
            kbService.upsertStatement(kbModel.getObject(), statement.getObject());

            // to update the statement in the UI, one could either reload all statements of the
            // corresponding instance or (much easier) just set the inferred attribute of the
            // KBStatement to false, so that's what's done here
            statement.getObject().setInferred(false);
            aTarget.add(this);
            send(getPage(), Broadcast.BREADTH,
                    new AjaxStatementChangedEvent(aTarget, statement.getObject()));

        }
        catch (RepositoryException e) {
            error("Unable to make statement explicit " + e.getLocalizedMessage());
            LOG.error("Unable to make statement explicit.", e);
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    public class ViewMode
        extends Fragment
    {
        private static final long serialVersionUID = 2375450134740203778L;

        private WebMarkupContainer qualifierListWrapper;

        public ViewMode(String aId, IModel<KBStatement> aStatement)
        {
            super(aId, "viewMode", StatementEditor.this, aStatement);

            CompoundPropertyModel<KBStatement> compoundModel = new CompoundPropertyModel<>(
                    aStatement);

            add(new Label("value", compoundModel.bind("value")));
            add(new Label("language", compoundModel.bind("language"))
            {
                private static final long serialVersionUID = 3436068825093393740L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();

                    setVisible(isNotEmpty(aStatement.getObject().getLanguage()));
                }
            });

            LambdaAjaxLink editLink = new LambdaAjaxLink("edit", StatementEditor.this::actionEdit)
                    .onConfigure((_this) -> _this.setVisible(!statement.getObject().isInferred()));
            editLink.add(new WriteProtectionBehavior(kbModel));
            add(editLink);

            LambdaAjaxLink addQualifierLink = new LambdaAjaxLink("addQualifier",
                    t -> actionAddQualifier(t, aStatement.getObject())).onConfigure(
                            (_this) -> _this.setVisible(!statement.getObject().isInferred()
                                    && kbModel.getObject().getReification().supportsQualifier()));
            addQualifierLink.add(new Label("label", new ResourceModel("qualifier.add")));
            addQualifierLink.add(new WriteProtectionBehavior(kbModel));
            add(addQualifierLink);

            LambdaAjaxLink makeExplicitLink = new LambdaAjaxLink("makeExplicit",
                    StatementEditor.this::actionMakeExplicit).onConfigure(
                            (_this) -> _this.setVisible(statement.getObject().isInferred()));
            makeExplicitLink.add(new WriteProtectionBehavior(kbModel));
            add(makeExplicitLink);

            RefreshingView<KBQualifier> qualifierList = new RefreshingView<KBQualifier>(
                    "qualifierList")
            {
                private static final long serialVersionUID = -8342276415072873329L;

                @Override
                protected Iterator<IModel<KBQualifier>> getItemModels()
                {
                    return new ModelIteratorAdapter<KBQualifier>(
                            statement.getObject().getQualifiers())
                    {
                        @Override
                        protected IModel<KBQualifier> model(KBQualifier object)
                        {
                            return Model.of(object);
                        }
                    };
                }

                @Override
                protected void populateItem(Item<KBQualifier> aItem)
                {
                    QualifierEditor editor = new QualifierEditor("qualifier", kbModel,
                            aItem.getModel());
                    aItem.add(editor);
                    aItem.setOutputMarkupId(true);
                }
            };
            qualifierList.setItemReuseStrategy(new ReuseIfModelsEqualStrategy());

            qualifierListWrapper = new WebMarkupContainer("qualifierListWrapper");
            qualifierListWrapper.setOutputMarkupId(true);
            qualifierListWrapper.add(qualifierList);
            add(qualifierListWrapper);
        }

        @OnEvent
        public void actionQualifierChanged(AjaxQualifierChangedEvent event)
        {
            boolean isEventForThisStatement = event.getQualifier().getStatement()
                    .equals(statement.getObject());
            if (isEventForThisStatement) {
                if (event.isDeleted()) {
                    event.getQualifier().getStatement().getQualifiers()
                            .remove(event.getQualifier());
                }
                statement.setObject(event.getQualifier().getStatement());
                event.getTarget().add(qualifierListWrapper);
            }
        }

    }

    private class EditMode
        extends Fragment
        implements Focusable
    {
        private static final long serialVersionUID = 2489925553729209190L;

        private Component initialFocusComponent;

        /**
         * Creates a new fragment for editing a statement.<br>
         * The editor has two slightly different behaviors, depending on the value of
         * {@code isNewStatement}:
         * <ul>
         * <li>{@code !isNewStatement}: Save button commits changes, cancel button discards unsaved
         * changes, delete button removes the statement from the KB.</li>
         * <li>{@code isNewStatement}: Save button commits changes (creates a new statement in the
         * KB), cancel button removes the statement from the UI, delete button is not visible.</li>
         * </ul>
         * 
         * @param aId
         *            markup ID
         * @param aStatement
         *            statement model
         * @param isNewStatement
         *            whether the statement being edited is new, meaning it has no corresponding
         *            statement in the KB backend
         */
        public EditMode(String aId, IModel<KBStatement> aStatement, boolean isNewStatement)
        {
            super(aId, "editMode", StatementEditor.this, aStatement);

            Form<KBStatement> form = new Form<>("form", CompoundPropertyModel.of(aStatement));

            // text area for the statement value should receive focus
            Component valueTextArea = new TextArea<String>("value");
            valueTextArea.setOutputMarkupId(true);
            initialFocusComponent = valueTextArea;
            form.add(valueTextArea);

            // FIXME This field should only be visible if the selected datatype is
            // langString

            TextField<String> textField = new TextField<>("language");
            textField.setOutputMarkupId(true);
            form.add(textField);

            // FIXME Selection of the data type should only be possible if it is not
            // restricted to a single type in the property definition - take into account
            // inheritance?
            // form.add(new TextField<>("datatype"));

            // We do not allow the user to change the property

            // FIXME should offer different editors depending on the data type
            // in particular when the datatype is a concept type, then
            // it should be possible to select an instance of that concept using some
            // auto-completing dropdown box

            form.add(new LambdaAjaxButton<>("save", StatementEditor.this::actionSave));
            form.add(new LambdaAjaxLink("cancel", t -> {
                if (isNewStatement) {
                    StatementEditor.this.actionCancelNewStatement(t);
                }
                else {
                    StatementEditor.this.actionCancelExistingStatement(t);
                }
            }));
            form.add(new LambdaAjaxLink("delete", StatementEditor.this::actionDelete)
                    .setVisibilityAllowed(!isNewStatement));
            add(form);
        }

        @Override
        public Component getFocusComponent()
        {
            return initialFocusComponent;
        }
    }
}
