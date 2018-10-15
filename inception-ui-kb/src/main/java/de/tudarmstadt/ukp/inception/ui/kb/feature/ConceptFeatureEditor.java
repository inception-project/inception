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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.core.JQueryBehavior;
import com.googlecode.wicket.jquery.core.renderer.TextRenderer;
import com.googlecode.wicket.jquery.core.template.IJQueryTemplate;
import com.googlecode.wicket.kendo.ui.form.autocomplete.AutoCompleteTextField;

import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

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
    private @SpringBean ConceptLinkingService clService;

    public ConceptFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel,
            IModel<AnnotatorState> aStateModel, AnnotationActionHandler aHandler) {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));
        add(new Label(MID_FEATURE, getModelObject().feature.getUiName()));
        add(focusComponent = createAutoCompleteTextField(aStateModel.getObject(), aHandler));
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

    private JCas getEditorCas(AnnotationActionHandler aHandler) throws IOException {
        return aHandler.getEditorCas();
    }

    // TODO: (issue #122 )this method is similar to the method listInstances in
    // SubjectObjectFeatureEditor and QualifierFeatureEditor. It should be refactored.
    private List<KBHandle> listInstances(AnnotatorState aState, AnnotationActionHandler aHandler,
            String aTypedString) {
        AnnotationFeature feat = getModelObject().feature;

        List<KBHandle> handles = new ArrayList<>();
        try {
            Project project = feat.getProject();
            FeatureSupport<ConceptFeatureTraits> fs = featureSupportRegistry
                    .getFeatureSupport(feat);
            ConceptFeatureTraits traits = fs.readTraits(feat);
            switch (traits.getAllowedValueType()) {
            case INSTANCE:
                handles = getInstances(traits, project, aState, aHandler, aTypedString);
                break;
            case CONCEPT:
                handles = getConcepts(traits, project, aState, aHandler, aTypedString);
                break;
            default:
                // Allows both
                handles.addAll(getInstances(traits, project, aState, aHandler, aTypedString));
                handles.addAll(getConcepts(traits, project, aState, aHandler, aTypedString));
            }
        } catch (Exception e) {
            LOG.error("Unable to read traits", e);
            error("Unable to read traits: " + ExceptionUtils.getRootCauseMessage(e));
            RequestCycle.get()
                    .find(IPartialPageRequestHandler.class)
                    .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));
        }
        // Sort results
        handles.sort(Comparator.comparing(KBObject::getUiLabel));
        return handles;
    }

    private List<KBHandle> getInstances(ConceptFeatureTraits traits, Project project,
            AnnotatorState aState, AnnotationActionHandler aHandler, String aTypedString) {
        List<KBHandle> handles = new ArrayList<>();
        if (traits.getRepositoryId() != null) {
            // If a specific KB is selected, get its instances
            Optional<KnowledgeBase> kb = kbService.getKnowledgeBaseById(project,
                    traits.getRepositoryId());
            if (kb.isPresent()) {
                // TODO: (#122) distinguish between concepts & instances when concept linking
                // enabled
                if (kb.get().isSupportConceptLinking()) {
                    handles.addAll(listLinkingInstances(kb.get(), aState,
                        () -> getEditorCas(aHandler), aTypedString));
                } else if (traits.getScope() != null) {
                    handles = kbService
                            .listInstancesForChildConcepts(kb.get(), traits.getScope(), false, 50)
                            .stream()
                            .filter(inst -> inst.getUiLabel().toLowerCase().contains(aTypedString))
                            .collect(Collectors.toList());

                    handles.addAll(kbService.listInstances(kb.get(), traits.getScope(), false)
                            .stream()
                            .filter(inst -> inst.getUiLabel().toLowerCase().contains(aTypedString))
                            .collect(Collectors.toList()));
                } else {
                    for (KBHandle concept : kbService.listConcepts(kb.get(), false)) {
                        handles.addAll(
                                kbService.listInstances(kb.get(), concept.getIdentifier(), false)
                                        .stream()
                                        .filter(inst -> inst.getUiLabel().toLowerCase()
                                                .contains(aTypedString))
                                        .collect(Collectors.toList()));
                    }
                }
            }
        } else {
            // If no specific KB is selected, collect instances from all KBs
            for (KnowledgeBase kb : kbService.getEnabledKnowledgeBases(project)) {
                // TODO: (#122) distinguish between concepts & instances when concept linking
                // enabled
                if (kb.isSupportConceptLinking()) {
                    handles.addAll(listLinkingInstances(kb, aState, () -> getEditorCas(aHandler),
                            aTypedString));
                } else if (traits.getScope() != null) {
                    handles.addAll(kbService
                            .listInstancesForChildConcepts(kb, traits.getScope(), false, 50)
                            .stream()
                            .filter(inst -> inst.getUiLabel().toLowerCase().contains(aTypedString))
                            .collect(Collectors.toList()));
                    handles.addAll(kbService.listInstances(kb, traits.getScope(), false).stream()
                            .filter(inst -> inst.getUiLabel().toLowerCase().contains(aTypedString))
                            .collect(Collectors.toList()));
                } else {
                    for (KBHandle concept : kbService.listConcepts(kb, false)) {
                        handles.addAll(
                                kbService.listInstances(kb, concept.getIdentifier(), false).stream()
                                        .filter(inst -> inst.getUiLabel().toLowerCase()
                                                .contains(aTypedString))
                                        .collect(Collectors.toList()));
                    }
                }

            }
        }
        return handles;
    }

    private List<KBHandle> getConcepts(ConceptFeatureTraits traits, Project project,
            AnnotatorState aState, AnnotationActionHandler aHandler, String aTypedString) {
        List<KBHandle> handles = new ArrayList<>();
        if (traits.getRepositoryId() != null) {
            // If a specific KB is selected, get its instances
            Optional<KnowledgeBase> kb = kbService.getKnowledgeBaseById(project,
                    traits.getRepositoryId());
            if (kb.isPresent()) {
                // TODO: (#122) distinguish between concepts & instances when concept linking
                // enabled
                if (kb.get().isSupportConceptLinking()) {
                    handles.addAll(listLinkingInstances(kb.get(), aState,
                        () -> getEditorCas(aHandler), aTypedString));
                } else if (traits.getScope() != null) {
                    handles = kbService.listChildConcepts(kb.get(), traits.getScope(), false)
                            .stream()
                            .filter(conc -> conc.getUiLabel().toLowerCase().contains(aTypedString))
                            .collect(Collectors.toList());
                } else {
                    handles.addAll(kbService.listConcepts(kb.get(), false).stream()
                            .filter(conc -> conc.getUiLabel().toLowerCase().contains(aTypedString))
                            .collect(Collectors.toList()));
                }
            }
        } else {
            // If no specific KB is selected, collect instances from all KBs
            for (KnowledgeBase kb : kbService.getEnabledKnowledgeBases(project)) {
                // TODO: (#122) distinguish between concepts & instances when concept linking
                // enabled
                if (kb.isSupportConceptLinking()) {
                    handles.addAll(listLinkingInstances(kb, aState, () -> getEditorCas(aHandler),
                            aTypedString));
                } else if (traits.getScope() != null) {
                    handles = kbService.listChildConcepts(kb, traits.getScope(), false).stream()
                            .filter(conc -> conc.getUiLabel().toLowerCase().contains(aTypedString))
                            .collect(Collectors.toList());
                } else {
                    handles.addAll(kbService.listConcepts(kb, false).stream()
                            .filter(conc -> conc.getUiLabel().toLowerCase().contains(aTypedString))
                            .collect(Collectors.toList()));
                }

            }
        }
        handles.sort(Comparator.comparing(KBObject::getUiLabel));
        return handles;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    public Component getFocusComponent() {
        return focusComponent;
    }

    // TODO: (issue #122 )this method is similar to the method listInstances in
    // SubjectObjectFeatureEditor and QualifierFeatureEditor. It should be refactored.
    private List<KBHandle> listLinkingInstances(KnowledgeBase kb, AnnotatorState aState,
            JCasProvider aJCas, String aTypedString) {
        return kbService.read(kb, (conn) -> {
            try {
                return clService.disambiguate(kb, aTypedString, aState.getSelection().getText(),
                        aState.getSelection().getBegin(), aJCas.get());
            } catch (IOException e) {
                log.error("An error occurred while retrieving entity candidates.", e);
                error(e);
                return Collections.emptyList();
            }
        });
    }
}
