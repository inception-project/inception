/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb.stmt.editor;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
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

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.app.Focusable;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.WriteProtectionBehavior;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxQualifierChangedEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxStatementChangedEvent;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.QualifierEditor;
import de.tudarmstadt.ukp.inception.ui.kb.value.ValueType;
import de.tudarmstadt.ukp.inception.ui.kb.value.ValueTypeSupportRegistry;
import de.tudarmstadt.ukp.inception.ui.kb.value.editor.ValueEditor;

public class StatementEditor extends Panel
{

    private static final long serialVersionUID = 7643837763550205L;
    private static final Logger LOG = LoggerFactory.getLogger(StatementEditor.class);

    private static final String CONTENT_MARKUP_ID = "content";

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean ValueTypeSupportRegistry valueTypeRegistry;
    
    private IModel<KnowledgeBase> kbModel;
    private IModel<KBStatement> statement;
    private IModel<KBProperty> property;
    private Component content;

    public StatementEditor(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<KBStatement> aStatement, IModel<KBProperty> aProperty) {
        super(aId, aStatement);

        setOutputMarkupId(true);
        
        kbModel = aKbModel;
        statement = aStatement;
        property = aProperty;

        // new statements start with edit mode right away
        boolean isNewStatement = statement.getObject().getOriginalStatements().isEmpty();
        if (isNewStatement) {
            EditMode editMode = new EditMode(CONTENT_MARKUP_ID, statement, true);

            // obtain AjaxRequestTarget and set the focus
            AjaxRequestTarget target = RequestCycle.get()
                    .find(AjaxRequestTarget.class);
            if (target != null) {
                target.focusComponent(editMode.getFocusComponent());
            }
            content = editMode;
        } else {
            content = new ViewMode(CONTENT_MARKUP_ID, statement);
        }
        add(content);
    }

    protected void actionEdit(AjaxRequestTarget aTarget) {
        // Edit mode works on a model of a shallow copy of the original statement. Any floating
        // changes to the statement are either persisted by saving or undone by canceling. In
        // conjunction with onchange AjaxFormComponentUpdatingBehaviours, this makes sure that
        // floating changes are persisted in the UI, meaning other statements can be added or
        // deleted while changes to this statement in the UI are not being reset.
        KBStatement shallowCopy = new KBStatement(statement.getObject());
        CompoundPropertyModel<KBStatement> shallowCopyModel = new CompoundPropertyModel<>(
                shallowCopy);

        EditMode editMode = new EditMode(CONTENT_MARKUP_ID, shallowCopyModel, false);
        content = content.replaceWith(editMode);
//        aTarget.focusComponent(editMode.getFocusComponent());
        aTarget.add(this);
    }

    private void actionAddQualifier(AjaxRequestTarget aTarget, KBStatement statement) {
        KBQualifier qualifierPorto = new KBQualifier(statement);
        statement.addQualifier(qualifierPorto);
        aTarget.add(this);
    }

    private void actionCancelExistingStatement(AjaxRequestTarget aTarget) {
        content = content.replaceWith(new ViewMode(CONTENT_MARKUP_ID, statement));
        aTarget.add(this);
    }

    private void actionCancelNewStatement(AjaxRequestTarget aTarget) {
        // send a delete event to trigger the deletion in the UI
        AjaxStatementChangedEvent deleteEvent = new AjaxStatementChangedEvent(aTarget,
                statement.getObject(), this, true);
        send(getPage(), Broadcast.BREADTH, deleteEvent);
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<KBStatement> aForm) {
        KBStatement modifiedStatement = aForm.getModelObject();
        try {
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

    private void actionDelete(AjaxRequestTarget aTarget) {
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

    private void actionMakeExplicit(AjaxRequestTarget aTarget) {
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

    private class ViewMode extends Fragment {
        private static final long serialVersionUID = 2375450134740203778L;

        private WebMarkupContainer qualifierListWrapper;

        public ViewMode(String aId, IModel<KBStatement> aStatement) {
            super(aId, "viewMode", StatementEditor.this, aStatement);

            CompoundPropertyModel<KBStatement> model = new CompoundPropertyModel<>(
                    aStatement);
            
            WebMarkupContainer presenter = valueTypeRegistry
                    .getValueSupport(aStatement.getObject(), property.getObject())
                    .createPresenter("value", model, property);
            add(presenter);
            
            LambdaAjaxLink editLink = new LambdaAjaxLink("edit", StatementEditor.this::actionEdit)
                    .onConfigure((_this) -> _this.setVisible(!statement.getObject().isInferred()));
            editLink.add(new WriteProtectionBehavior(kbModel));
            add(editLink);

            LambdaAjaxLink addQualifierLink = new LambdaAjaxLink("addQualifier",
                t -> actionAddQualifier(t, aStatement.getObject()))
                .onConfigure((_this) -> _this.setVisible(!statement.getObject().isInferred() &&
                    kbModel.getObject().getReification().supportsQualifier()));
            addQualifierLink.add(new Label("label", new ResourceModel("qualifier.add")));
            addQualifierLink.add(new WriteProtectionBehavior(kbModel));
            add(addQualifierLink);
            
            LambdaAjaxLink makeExplicitLink = new LambdaAjaxLink("makeExplicit",
                    StatementEditor.this::actionMakeExplicit).onConfigure(
                        (_this) -> _this.setVisible(statement.getObject().isInferred()));
            makeExplicitLink.add(new WriteProtectionBehavior(kbModel));
            add(makeExplicitLink);

            RefreshingView<KBQualifier> qualifierList = new RefreshingView<KBQualifier>("qualifierList")
            {
                private static final long serialVersionUID = -8342276415072873329L;

                @Override
                protected Iterator<IModel<KBQualifier>> getItemModels()
                {
                    return new ModelIteratorAdapter<KBQualifier>(
                        statement.getObject().getQualifiers())
                    {
                        @Override protected IModel<KBQualifier> model(KBQualifier object)
                        {
                            return LambdaModel.of(() -> object);
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
            boolean isEventForThisStatement = event.getQualifier().getKbStatement()
                .equals(statement.getObject());
            if (isEventForThisStatement) {
                if (event.isDeleted()) {
                    event.getQualifier().getKbStatement().getQualifiers()
                        .remove(event.getQualifier());
                }
                statement.setObject(event.getQualifier().getKbStatement());
                event.getTarget().add(qualifierListWrapper);
            }
        }

    }

    private class EditMode extends Fragment implements Focusable {
        private static final long serialVersionUID = 2489925553729209190L;

        private ValueEditor editor;
        
        private DropDownChoice<ValueType> valueType;

        /**
         * Creates a new fragement for editing a statement.<br>
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
        public EditMode(String aId, IModel<KBStatement> aStatement, boolean isNewStatement) {
            super(aId, "editMode", StatementEditor.this, aStatement);
            CompoundPropertyModel<KBStatement> model = CompoundPropertyModel.of(aStatement);
            Form<KBStatement> form = new Form<>("form", model);
            List<ValueType> valueTypes;
            String rangeValue = property.getObject().getRange();
            Optional<KBObject> rangeKBHandle = kbService.
                    readKBIdentifier(kbModel.getObject().getProject(), rangeValue);
            if (rangeKBHandle.isPresent() || rangeValue != null) {
                valueTypes = valueTypeRegistry.getRangeTypes(rangeValue, rangeKBHandle);
            }
            else {
                valueTypes = valueTypeRegistry.getAllTypes();
            }
            valueType = new DropDownChoice<>("valueType", valueTypes);
            valueType.setChoiceRenderer(new ChoiceRenderer<>("uiName"));
            valueType.setModel(Model.of(
                    valueTypeRegistry.getValueType(aStatement.getObject(), property.getObject())));
            
            // replace the editor when the choice is changed
            valueType.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
                
                ValueEditor newEditor = valueTypeRegistry
                        .getValueSupport(valueType.getModelObject())
                        .createEditor("value", model, property, kbModel);
                editor.setOutputMarkupId(true);
                editor = (ValueEditor) editor.replaceWith(newEditor);
                t.add(editor);
            }));
            
            form.add(valueType);
            
            // use the IRI to obtain the appropriate value editor
            editor = valueTypeRegistry.getValueSupport(aStatement.getObject(), property.getObject())
                    .createEditor("value", model, property, kbModel);
            editor.setOutputMarkupId(true);
            form.add(editor);
            
            form.add(new LambdaAjaxButton<>("save", StatementEditor.this::actionSave));
            form.add(new LambdaAjaxLink("cancel", t -> {
                if (isNewStatement) {
                    StatementEditor.this.actionCancelNewStatement(t);
                } else {
                    StatementEditor.this.actionCancelExistingStatement(t);
                }
            }));
            form.add(new LambdaAjaxLink("delete", StatementEditor.this::actionDelete)
                    .setVisibilityAllowed(!isNewStatement));
            add(form);
        }

        @Override
        public Component getFocusComponent() {
            return editor.getFocusComponent();
        }
    }
}
