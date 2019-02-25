/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.renderer.TextRenderer;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingServiceImpl;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.DisabledKBWarning;

/**
 * Component for editing knowledge-base-related features on annotations.
 */
public class ConceptFeatureEditor extends FeatureEditor {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MID_FEATURE = "feature";
    private static final String MID_VALUE = "value";

    private static final long serialVersionUID = 7763348613632105600L;
    private static final Logger LOG = LoggerFactory.getLogger(ConceptFeatureEditor.class);

    private Component focusComponent;

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean ConceptLinkingServiceImpl clService;

    public ConceptFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel,
            IModel<AnnotatorState> aStateModel, AnnotationActionHandler aHandler) {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));
        add(new Label(MID_FEATURE, getModelObject().feature.getUiName()));
        add(focusComponent = createAutoCompleteTextField(aStateModel.getObject(), aHandler));
        add(createDisabledKbWarningLabel());
    }

    @Override
    public void renderHead(IHeaderResponse aResponse) {
        super.renderHead(aResponse);

        aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
    }

    private AutoCompleteTextField<KBHandle> createAutoCompleteTextField(AnnotatorState aState,
            AnnotationActionHandler aHandler) {
        AutoCompleteTextField<KBHandle> field = new AutoCompleteTextField<KBHandle>(MID_VALUE,
                new TextRenderer<KBHandle>("uiLabel")) {
            private static final long serialVersionUID = -1955006051950156603L;

            @Override
            protected List<KBHandle> getChoices(String input) {
                return listInstances(aState, aHandler, input != null ? input.toLowerCase() : null);
            }

            @Override
            public void onConfigure(JQueryBehavior behavior)
            {
                super.onConfigure(behavior);

                behavior.setOption("autoWidth", true);
                behavior.setOption("ignoreCase", false);
            }

            @Override
            protected IJQueryTemplate newTemplate() {
                return KendoChoiceDescriptionScriptReference.template();
            }
        };

        return field;
    }

    private List<KBHandle> listInstances(AnnotatorState aState, AnnotationActionHandler aHandler,
            String aTypedString) {
        AnnotationFeature feat = getModelObject().feature;

        Project project = feat.getProject();
        ConceptFeatureTraits traits = readFeatureTraits(feat);

        // Check if kb is actually enabled
        if (featureUsesDisabledKB(traits)) {
            return Collections.emptyList();
        }

        List<KBHandle> handles = new ArrayList<>();
        // Use concept linking if enabled
        try {
            handles = clService.getLinkingInstancesInKBScope(traits.getRepositoryId(),
                    traits.getScope(), traits.getAllowedValueType(), aTypedString,
                    aState.getSelection().getText(), aState.getSelection().getBegin(),
                    aHandler.getEditorCas(), project);
        }
        catch (IOException e) {
            LOG.error("An error occurred while retrieving entity candidates.", e);
            error("An error occurred while retrieving entity candidates: " + e.getMessage());
            RequestCycle.get()
                .find(IPartialPageRequestHandler.class)
                .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));
        }

        // if concept linking does not return any results or is disabled
        if (handles.size() == 0) {
            handles = kbService.getEntitiesInScope(traits.getRepositoryId(), traits.getScope(),
                traits.getAllowedValueType(), project);
            // Sort and filter results
            handles = handles.stream()
                .filter(handle -> handle.getUiLabel().toLowerCase().startsWith(aTypedString))
                .sorted(Comparator.comparing(KBObject::getUiLabel)).collect(Collectors.toList());
        }
        
        return KBHandle.distinctByIri(handles);
    }

    private ConceptFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature) {
        FeatureSupport<ConceptFeatureTraits> fs = featureSupportRegistry
            .getFeatureSupport(aAnnotationFeature);
        ConceptFeatureTraits traits = fs.readTraits(aAnnotationFeature);
        return traits;
    }


    private boolean featureUsesDisabledKB(ConceptFeatureTraits aTraits)
    {
        Optional<KnowledgeBase> kb = Optional.empty();
        String repositoryId = aTraits.getRepositoryId();
        if (repositoryId != null) {
            kb = kbService.getKnowledgeBaseById(getModelObject().feature.getProject(),
                aTraits.getRepositoryId());
        }
        return kb.isPresent() && !kb.get().isEnabled() || repositoryId != null && !kb.isPresent();
    }

    private DisabledKBWarning createDisabledKbWarningLabel()
    {
        AnnotationFeature feature = getModelObject().feature;
        ConceptFeatureTraits traits = readFeatureTraits(feature);

        Optional<KnowledgeBase> kb = Optional.empty();
        if (traits.getRepositoryId() != null) {
            kb = kbService.getKnowledgeBaseById(feature.getProject(), traits.getRepositoryId());
        }
        String kbName = kb.isPresent() ? kb.get().getName() : "unknown ID";

        DisabledKBWarning warning = new DisabledKBWarning("disabledKBWarning",
            new StringResourceModel("value.null.disabledKbWarning", this).setParameters(kbName));

        warning.add(LambdaBehavior
            .onConfigure(label -> label.setVisible(featureUsesDisabledKB(traits))));

        return warning;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    public Component getFocusComponent() {
        return focusComponent;
    }
}
