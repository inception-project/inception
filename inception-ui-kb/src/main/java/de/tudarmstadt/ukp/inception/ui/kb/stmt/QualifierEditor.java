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
package de.tudarmstadt.ukp.inception.ui.kb.stmt;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.IRI;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.core.Focusable;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxQualifierChangedEvent;

public class QualifierEditor
    extends Panel
{
    private static final long serialVersionUID = -4152363403483032196L;

    private static final String CONTENT_MARKUP_ID = "content";

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KnowledgeBase> kbModel;
    private IModel<KBQualifier> qualifier;
    private Component content;

    public QualifierEditor(String id, IModel<KnowledgeBase> aKbModel,
        IModel<KBQualifier> aQualifier)
    {
        super(id, aQualifier);
        setOutputMarkupId(true);

        kbModel = aKbModel;
        qualifier = aQualifier;

        boolean isNewQualifier = qualifier.getObject().getProperty() == null;
        if (isNewQualifier) {
            EditMode editMode = new EditMode(CONTENT_MARKUP_ID, qualifier, isNewQualifier);
            RequestCycle.get()
                    .find(AjaxRequestTarget.class)
                    .ifPresent(target -> target.focusComponent(editMode.getFocusComponent()));
            content = editMode;
        }
        else {
            content = new ViewMode(CONTENT_MARKUP_ID, qualifier);
        }
        add(content);
    }

    private class EditMode extends Fragment
        implements Focusable {
        private static final long serialVersionUID = 2333017379066971404L;
        private Component initialFocusComponent;

        /**
         * Creates a new fragement for editing a qualifier.<br>
         * The editor has two slightly different behaviors, depending on the value of
         * {@code isNewQualifier}:
         * <ul>
         * <li>{@code !isNewQualifier}: Save button commits changes, cancel button discards unsaved
         * changes, delete button removes the qualifier from the statement.</li>
         * <li>{@code isNewQualifier}: Save button commits changes (creates a new qualifier in the
         * statement), cancel button removes the qualifier from the UI, delete button is not visible
         * .</li>
         * </ul>
         *
         * @param aId
         *            markup ID
         * @param aQualifier
         *            qualifier model
         * @param isNewQualifier
         *            whether the qualifier being edited is new, meaning it has no corresponding
         *            qualifier in the KB backend
         */
        public EditMode(String aId, IModel<KBQualifier> aQualifier, boolean isNewQualifier)
        {
            super(aId, "editMode", QualifierEditor.this, aQualifier);

            IModel<KBQualifier> compoundModel = CompoundPropertyModel.of(aQualifier);

            Form<KBQualifier> form = new Form<>("form", compoundModel);
            DropDownChoice<KBProperty> type = new BootstrapSelect<>("property");
            type.setChoiceRenderer(new ChoiceRenderer<>("uiLabel"));
            type.setChoices(kbService.listProperties(kbModel.getObject(), false));
            type.setRequired(true);
            type.setOutputMarkupId(true);
            form.add(type);
            initialFocusComponent = type;

            form.add(new TextField<>("language"));

            Component valueTextArea = new TextArea<String>("value");
            form.add(valueTextArea);

            form.add(new LambdaAjaxButton<>("create", QualifierEditor.this::actionSave));
            form.add(new LambdaAjaxLink("cancel", t -> {
                if (isNewQualifier) {
                    QualifierEditor.this.actionCancelNewQualifier(t);
                } else {
                    QualifierEditor.this.actionCancelExistingQualifier(t);
                }
            }));
            form.add(new LambdaAjaxLink("delete", QualifierEditor.this::actionDelete)
                .setVisibilityAllowed(!isNewQualifier));

            add(form);
        }



        @Override
        public Component getFocusComponent() {
            return initialFocusComponent;
        }
    }

    private class ViewMode
        extends Fragment
    {
        private static final long serialVersionUID = 6771056914040868827L;

        public ViewMode(String aId, IModel<KBQualifier> aQualifier)
        {
            super(aId, "viewMode", QualifierEditor.this, aQualifier);
            CompoundPropertyModel<KBQualifier> compoundModel = new CompoundPropertyModel<>(
                aQualifier);
            add(new Label("property", aQualifier.getObject().getProperty().getUiLabel()));
            add(new Label("language", compoundModel.bind("language")).add(
                    LambdaBehavior.onConfigure(_this -> 
                            _this.setVisible(isNotEmpty(aQualifier.getObject().getLanguage())))));
            add(new Label("value",
                    LoadableDetachableModel.of(() -> getLabel(compoundModel.getObject()))));

            LambdaAjaxLink editLink = new LambdaAjaxLink("edit", QualifierEditor.this::actionEdit);
            editLink.add(visibleWhen(() -> kbModel.map(kb -> !kb.isReadOnly())
                    .orElse(false).getObject()));
            add(editLink);
        }
    }

    private String getLabel(KBQualifier aKbQualifier)
    {
        if (aKbQualifier == null) {
            return null;
        }
        
        if (aKbQualifier != null && aKbQualifier.getValueLabel() != null) {
            return aKbQualifier.getValueLabel();
        }
        
        if (aKbQualifier.getValue() instanceof IRI) {
            return ((IRI) aKbQualifier.getValue()).getLocalName();
        }
        
        return String.valueOf(aKbQualifier.getValue());
    }
    
    private void actionDelete(AjaxRequestTarget aTarget) {
        kbService.deleteQualifier(kbModel.getObject(), qualifier.getObject());

        AjaxQualifierChangedEvent deleteEvent = new AjaxQualifierChangedEvent(aTarget, qualifier
            .getObject(), this, true);
        send(getPage(), Broadcast.BREADTH, deleteEvent);
    }

    private void actionEdit(AjaxRequestTarget aTarget) {
        KBQualifier shallowCopy = new KBQualifier(qualifier.getObject());
        IModel<KBQualifier> shallowCopyModel = Model.of(shallowCopy);

        EditMode editMode = new EditMode(CONTENT_MARKUP_ID, shallowCopyModel, false);
        content = content.replaceWith(editMode);
        aTarget.focusComponent(editMode.getFocusComponent());
        aTarget.add(this);
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<KBQualifier> aForm) {
        KBQualifier modifiedQualifier = aForm.getModelObject();
        kbService.upsertQualifier(kbModel.getObject(), modifiedQualifier);
        qualifier.setObject(modifiedQualifier);

        actionCancelExistingQualifier(aTarget);
    }

    private void actionCancelNewQualifier(AjaxRequestTarget aTarget) {
        // send a delete event to trigger the deletion in the UI
        AjaxQualifierChangedEvent deleteEvent = new AjaxQualifierChangedEvent(aTarget,
            qualifier.getObject(), this, true);
        send(getPage(), Broadcast.BREADTH, deleteEvent);
    }

    private void actionCancelExistingQualifier(AjaxRequestTarget aTarget) {
        content = content.replaceWith(new ViewMode(CONTENT_MARKUP_ID, qualifier));
        aTarget.add(this);
    }

}
