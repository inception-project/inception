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
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.wicket.event.Broadcast.BUBBLE;
import static org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog.CONTENT_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.template.IJQueryTemplate;
import org.wicketstuff.kendo.ui.form.multiselect.lazy.MultiSelect;
import org.wicketstuff.kendo.ui.renderer.ChoiceRenderer;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.MultiValueConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.SuggestionStatePanel;
import de.tudarmstadt.ukp.inception.support.kendo.KendoStyleUtils;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxConceptSelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxInstanceSelectionEvent;

public class MultiValueConceptFeatureEditor
    extends ConceptFeatureEditor_ImplBase
{
    private static final long serialVersionUID = -8326017157405023711L;

    private static final String CID_VALUE = "value";

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean KnowledgeBaseService knowledgeBaseService;

    private FormComponent<Collection<KBHandle>> focusComponent;
    private BootstrapModalDialog dialog;

    private final AnnotationActionHandler handler;
    private final IModel<AnnotatorState> stateModel;
    private boolean featureUpdateBehaviorRequested = false;
    private boolean featureUpdateBehaviorAdded = false;

    public MultiValueConceptFeatureEditor(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel, IModel<AnnotatorState> aStateModel,
            AnnotationActionHandler aHandler)
    {
        super(aId, aItem, aModel);

        handler = aHandler;
        stateModel = aStateModel;

        focusComponent = createInput();
        add(focusComponent);

        dialog = new BootstrapModalDialog("dialog");
        dialog.trapFocus();
        queue(dialog);

        queue(new LambdaAjaxLink("openBrowseDialog", this::actionOpenBrowseDialog)
                .add(visibleWhen(this::isBrowsingAllowed)));

        add(new SuggestionStatePanel("suggestionInfo", aModel));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onConfigure()
    {
        super.onConfigure();

        // Workaround for https://github.com/sebfz1/wicket-jquery-ui/issues/352
        if ((isEnabledInHierarchy() && !(focusComponent instanceof MultiSelect))
                || !isEnabledInHierarchy() && (focusComponent instanceof MultiSelect)) {
            focusComponent = (FormComponent<Collection<KBHandle>>) focusComponent
                    .replaceWith(createInput());
        }

        if (featureUpdateBehaviorRequested && !featureUpdateBehaviorAdded) {
            super.addFeatureUpdateBehavior();
            featureUpdateBehaviorAdded = true;
        }
    }

    private FormComponent<Collection<KBHandle>> createInput()
    {
        featureUpdateBehaviorAdded = false;
        if (isEnabledInHierarchy()) {
            return createEditableInput();
        }
        else {
            return createReadOnlyInput();
        }
    }

    @Override
    public void addFeatureUpdateBehavior()
    {
        featureUpdateBehaviorRequested = true;
    }

    private FormComponent<Collection<KBHandle>> createEditableInput()
    {
        return new KBHandleMultiSelect(CID_VALUE, handler, stateModel);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private FormComponent<Collection<KBHandle>> createReadOnlyInput()
    {
        var input = new org.wicketstuff.kendo.ui.form.multiselect. //
                MultiSelect<KBHandle>(CID_VALUE)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onConfigure(JQueryBehavior aBehavior)
            {
                super.onConfigure(aBehavior);
                styleMultiSelect(aBehavior);
            }
        };
        input.setChoiceRenderer(
                new org.apache.wicket.markup.html.form.ChoiceRenderer<>("uiLabel", "identifier"));
        input.setChoices(() -> input.getModel().map(ArrayList::new).getObject());
        return (FormComponent) input;
    }

    private void styleMultiSelect(JQueryBehavior aBehavior)
    {
        // aBehavior.setOption("autoWidth", true);
        KendoStyleUtils.autoDropdownWidth(aBehavior);
        // aBehavior.setOption("height", 300);
        KendoStyleUtils.autoDropdownHeight(aBehavior);
        aBehavior.setOption("animation", false);
        aBehavior.setOption("delay", 250);
    }

    @Override
    public FormComponent<Collection<KBHandle>> getFocusComponent()
    {
        return focusComponent;
    }

    @Override
    protected MultiValueConceptFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature)
    {
        FeatureSupport<?> fs = featureSupportRegistry.findExtension(aAnnotationFeature)
                .orElseThrow();
        return (MultiValueConceptFeatureTraits) fs.readTraits(aAnnotationFeature);
    }

    private boolean isBrowsingAllowed()
    {
        var traits = featureSupportRegistry.readTraits(getModelObject().feature,
                MultiValueConceptFeatureTraits::new);

        // There is now KB selector in the browser yet, so we do not show it unless either the
        // feature is bound to a specific KB or there is only a single KB in the project.
        if (traits.getRepositoryId() == null && knowledgeBaseService
                .hasMoreThanOneEnabledKnowledgeBases(getModelObject().feature.getProject())) {
            return false;
        }

        // Properties are not supported in the browser
        if (traits.getAllowedValueType() == ConceptFeatureValueType.PROPERTY) {
            return false;
        }

        return true;
    }

    private void actionOpenBrowseDialog(AjaxRequestTarget aTarget)
    {
        var traits = featureSupportRegistry.readTraits(getModelObject().feature,
                MultiValueConceptFeatureTraits::new);

        var content = new BrowseKnowledgeBaseDialogContentPanel(CONTENT_ID,
                getModel().map(fs -> fs.getFeature().getProject()), Model.of(), Model.of(traits));
        dialog.open(content, aTarget);
    }

    @OnEvent
    public void onConceptSelectionEvent(AjaxConceptSelectionEvent aEvent)
    {
        selectItem(aEvent.getTarget(), aEvent.getSelection());
    }

    @OnEvent
    public void onInstanceSelectionEvent(AjaxInstanceSelectionEvent aEvent)
    {
        selectItem(aEvent.getTarget(), aEvent.getSelection());
    }

    private void selectItem(AjaxRequestTarget aTarget, KBObject aKBObject)
    {
        ((Collection<KBObject>) getModelObject().value).add(aKBObject);
        dialog.close(aTarget);
        aTarget.add(this);
        send(focusComponent, BUBBLE, new FeatureEditorValueChangedEvent(this, aTarget));
    }

    private final class KBHandleMultiSelect
        extends MultiSelect<KBHandle>
    {
        private final AnnotationActionHandler handler;
        private final IModel<AnnotatorState> stateModel;
        private static final long serialVersionUID = 7769511105678209462L;

        private KBHandleMultiSelect(String aId, AnnotationActionHandler aHandler,
                IModel<AnnotatorState> aStateModel)
        {
            super(aId, new ChoiceRenderer<>("uiLabel", "identifier"));
            handler = aHandler;
            stateModel = aStateModel;
        }

        @Override
        protected List<KBHandle> getChoices(String aInput)
        {
            if (aInput == null || aInput.length() == 0) {
                return new ArrayList<>(getModelObject());
            }

            var candidates = getCandidates(stateModel, handler, aInput);

            var selected = new ArrayList<>(getModelObject());
            selected.removeAll(candidates);

            var choices = new ArrayList<KBHandle>();
            choices.addAll(candidates);
            choices.addAll(selected);

            return choices;
        }

        @Override
        public void onConfigure(JQueryBehavior aBehavior)
        {
            super.onConfigure(aBehavior);

            styleMultiSelect(aBehavior);

            // These three settings should avoid a query when simply clicking into the multiselect
            // field, but they seem to have no effect
            // aBehavior.setOption("autoBind", false);
            // aBehavior.setOption("minLength", 1);
            // aBehavior.setOption("enforceMinLength", true);
        }

        @Override
        protected IJQueryTemplate newTemplate()
        {
            return new KBHandleTemplate();
        }
    }
}
