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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
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

        boolean isNewQualifier = true; //(qualifier.getObject().getKbProperty()==null);
        if (isNewQualifier) {
            EditMode editMode = new EditMode(CONTENT_MARKUP_ID, qualifier, isNewQualifier);
            AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                target.focusComponent(editMode.getFocusComponent());
            }
            content = editMode;
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
         * {@code isNewModifier}:
         * <ul>
         * <li>{@code !isNewModifier}: Save button commits changes, cancel button discards unsaved
         * changes, delete button removes the qualifier from the statement.</li>
         * <li>{@code isNewModifier}: Save button commits changes (creates a new qualifier in the
         * statement), cancel button removes the qualifier from the UI, delete button is not visible
         * .</li>
         * </ul>
         *
         * @param aId
         *            markup ID
         * @param aQualifier
         *            qualifier model
         * @param isNewModifier
         *            whether the qualifier being edited is new, meaning it has no corresponding
         *            qualifier in the KB backend
         */
        public EditMode(String aId, IModel<KBQualifier> aQualifier, boolean isNewModifier)
        {
            super(aId, "editMode", QualifierEditor.this, aQualifier);

            IModel<KBQualifier> compoundModel = CompoundPropertyModel.of(aQualifier);

            Form<KBQualifier> form = new Form<>("form", compoundModel);
            DropDownChoice<KBHandle> type = new DropDownChoice<>("kbProperty");
            type.setChoiceRenderer(new ChoiceRenderer<>("uiLabel"));
            type.setChoices(kbService.listProperties(kbModel.getObject(), false));
            type.setRequired(true);
            type.setOutputMarkupId(true);
            form.add(type);
            initialFocusComponent = type;

            form.add(new TextField<>("value"));

            form.add(new LambdaAjaxButton<>("create", QualifierEditor.this::actionTest));
            form.add(new LambdaAjaxLink("cancel", QualifierEditor.this::actionCancelNewQualifier));
            form.add(new LambdaAjaxLink("delete", QualifierEditor.this::actionLinkTest)
                .setVisibilityAllowed(!isNewModifier));

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

        public ViewMode(String aId, IModel<KBQualifier> aModifier)
        {
            super(aId, "viewMode", QualifierEditor.this, aModifier);
            CompoundPropertyModel<KBQualifier> compoundModel = new CompoundPropertyModel<>(
                aModifier);
            add(new Label("property", aModifier.getObject().getKbProperty().getUiLabel()));
            add(new Label("value", compoundModel.bind("value")));

            LambdaAjaxLink editLink = new LambdaAjaxLink("edit", QualifierEditor
                .this::actionLinkTest);
            add(editLink);
        }
    }

    private void actionTest(AjaxRequestTarget ajaxRequestTarget, Form<KBQualifier> aForm)
    {
        KBQualifier qualifier = aForm.getModelObject();
    }

    private void actionLinkTest(AjaxRequestTarget ajaxRequestTarget)
    {

    }

    private void actionCancelNewQualifier(AjaxRequestTarget aTarget) {
        // send a delete event to trigger the deletion in the UI
        AjaxQualifierChangedEvent deleteEvent = new AjaxQualifierChangedEvent(aTarget,
            qualifier.getObject(), this, true);
        send(getPage(), Broadcast.BREADTH, deleteEvent);
    }

    private void actionCancelExistingQualifier(AjaxRequestTarget aTarget) {

    }

}
