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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.IRI;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxStatementChangedEvent;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.Focusable;
import de.tudarmstadt.ukp.inception.ui.kb.util.WriteProtectionBehavior;

public class StatementEditor extends Panel {

    private static final long serialVersionUID = 7643837763550205L;

    private static final String CONTENT_MARKUP_ID = "content";

    private @SpringBean KnowledgeBaseService kbService;
    
    private IModel<DatatypeSupport> datatypeSupport;
    private IModel<KnowledgeBase> kbModel;
    private IModel<KBStatement> statement;
    private IModel<IRI> propertyIri;
    private Component content;

    public StatementEditor(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<KBStatement> aStatement, IModel<IRI> aPropertyIri) {
        super(aId, aStatement);

        setOutputMarkupId(true);
        
        // TODO avoid frequent reinstantiation - instance could be kept same across all stmt eds.
        datatypeSupport = Model.of(new MetaDatatypeSupport(aKbModel.getObject()));

        kbModel = aKbModel;
        statement = aStatement;
        propertyIri = aPropertyIri;

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

        // persist the modified statement and replace the original, unchanged model
        kbService.upsertStatement(kbModel.getObject(), modifiedStatement);
        statement.setObject(modifiedStatement);

        // switch back to ViewMode and send notification to listeners
        actionCancelExistingStatement(aTarget);
        send(getPage(), Broadcast.BREADTH,
                new AjaxStatementChangedEvent(aTarget, statement.getObject()));
    }

    private void actionDelete(AjaxRequestTarget aTarget) {
        kbService.deleteStatement(kbModel.getObject(), statement.getObject());

        AjaxStatementChangedEvent deleteEvent = new AjaxStatementChangedEvent(aTarget,
                statement.getObject(), this, true);
        send(getPage(), Broadcast.BREADTH, deleteEvent);
    }

    private void actionMakeExplicit(AjaxRequestTarget aTarget) {
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

    private class ViewMode extends Fragment {
        private static final long serialVersionUID = 2375450134740203778L;

        public ViewMode(String aId, IModel<KBStatement> aStatement) {
            super(aId, "viewMode", StatementEditor.this, aStatement);

            CompoundPropertyModel<KBStatement> model = new CompoundPropertyModel<>(
                    aStatement);

            WebMarkupContainer presenter = datatypeSupport.getObject()
                    .createPresenter(propertyIri.getObject(), "value", model.bind("value"));
            add(presenter);
            
            LambdaAjaxLink editLink = new LambdaAjaxLink("edit", StatementEditor.this::actionEdit)
                    .onConfigure((_this) -> _this.setVisible(!statement.getObject().isInferred()));
            editLink.add(new WriteProtectionBehavior(kbModel));
            add(editLink);
            
            LambdaAjaxLink makeExplicitLink = new LambdaAjaxLink("makeExplicit",
                    StatementEditor.this::actionMakeExplicit).onConfigure(
                        (_this) -> _this.setVisible(statement.getObject().isInferred()));
            makeExplicitLink.add(new WriteProtectionBehavior(kbModel));
            add(makeExplicitLink);
        }
    }

    private class EditMode extends Fragment implements Focusable {
        private static final long serialVersionUID = 2489925553729209190L;

        private ValueEditor<?> editor;

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
                       
            // use the IRI to obtain the appropriate value editor
            editor = datatypeSupport.getObject().createEditor(propertyIri.getObject(),
                    "value", model.bind("value"));
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
