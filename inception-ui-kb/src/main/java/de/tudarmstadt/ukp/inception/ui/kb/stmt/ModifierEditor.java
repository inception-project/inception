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
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBModifier;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class ModifierEditor extends Panel
{
    private static final long serialVersionUID = -4152363403483032196L;

    private static final String CONTENT_MARKUP_ID = "content";

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KnowledgeBase> kbModel;
    private IModel<KBModifier> modifier;
    private Component content;

    public ModifierEditor(String id, IModel<KnowledgeBase> aKbModel,
        IModel<KBModifier> aModifier)
    {
        super(id, aModifier);
        setOutputMarkupId(true);

        kbModel = aKbModel;
        modifier = aModifier;

        boolean isNewModifier = true; //(modifier.getObject().getKbProperty()==null);
        if (isNewModifier) {
            EditMode editMode = new EditMode(CONTENT_MARKUP_ID, modifier, true);
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
         * @param aModifier
         *            modifier model
         * @param isNewModifier
         *            whether the modifier being edited is new, meaning it has no corresponding
         *            modifier in the KB backend
         */
        public EditMode(String aId, IModel<KBModifier> aModifier, boolean isNewModifier) {
            super(aId, "editMode", ModifierEditor.this, aModifier);

            IModel<KBModifier> compoundModel = CompoundPropertyModel.of(aModifier);

            Form<KBModifier> form = new Form<>("form", compoundModel);
            DropDownChoice<KBHandle> type = new DropDownChoice<>("kbProperty");
            type.setChoiceRenderer(new ChoiceRenderer<>("uiLabel"));
            type.setChoices(kbService.listProperties(kbModel.getObject(), false));
            type.setRequired(true);
            type.setOutputMarkupId(true);
            form.add(type);
            initialFocusComponent = type;

            form.add(new TextField<>("value"));

            form.add(new LambdaAjaxButton<>("create", this::actionTest));
            form.add(new LambdaAjaxButton<>("cancel", this::actionTest));

            add(form);
        }

        private void actionTest(AjaxRequestTarget ajaxRequestTarget, Form<KBModifier> aForm)
        {
            KBModifier modifier = aForm.getModelObject();
        }

        @Override
        public Component getFocusComponent() {
            return initialFocusComponent;
        }
    }

}
