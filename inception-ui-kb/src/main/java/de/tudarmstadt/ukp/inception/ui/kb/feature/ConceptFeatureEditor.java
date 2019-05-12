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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.ui.kb.IriInfoBadge;

/**
 * Component for editing knowledge-base-related features on annotations.
 */
public class ConceptFeatureEditor
    extends FeatureEditor
{
    private static final Logger LOG = LoggerFactory.getLogger(ConceptFeatureEditor.class);
    
    private static final long serialVersionUID = 7763348613632105600L;

    private Component focusComponent;
    private IriInfoBadge iriBadge;

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean ConceptLinkingService clService;

    public ConceptFeatureEditor(String aId, MarkupContainer aItem, IModel<FeatureState> aModel,
            IModel<AnnotatorState> aStateModel, AnnotationActionHandler aHandler)
    {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));
        add(iriBadge = new IriInfoBadge("iriInfoBadge",
                LoadableDetachableModel.of(this::iriTooltipValue)));
        iriBadge.add(visibleWhen(() -> isNotBlank(iriBadge.getModelObject())));
        add(focusComponent = new KnowledgeBaseItemAutoCompleteField(MID_VALUE, _query -> 
                getCandidates(aStateModel, aHandler, _query)));
        add(new DisabledKBWarning("disabledKBWarning", Model.of(getModelObject().feature)));
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
    }
    
    private String iriTooltipValue()
    {
        return Optional.ofNullable((KBHandle) getModelObject().value)
                .map(KBHandle::getIdentifier)
                .orElse("");
    }

    private List<KBHandle> getCandidates(IModel<AnnotatorState> aStateModel,
            AnnotationActionHandler aHandler, String aInput)
    {
        if (aInput == null) {
            return emptyList();
        }
        
        List<KBHandle> choices;
        try {
            AnnotationFeature feat = getModelObject().feature;

            ConceptFeatureTraits traits = readFeatureTraits(feat);
            String repoId = traits.getRepositoryId();
            // Check if kb is actually enabled
            if (!(repoId == null
                || kbService.isKnowledgeBaseEnabled(feat.getProject(), repoId)))
            {
                return Collections.emptyList();
            }

            // If there is a selection, we try obtaining its text from the CAS and use it as an
            // additional item in the query. Note that there is not always a mention, e.g. when the
            // feature is used in a document-level annotations.
            CAS cas = aHandler != null ? aHandler.getEditorCas() : null;
            String mention = aStateModel != null ? aStateModel.getObject().getSelection().getText()
                    : null;
            int mentionBegin = aStateModel != null
                    ? aStateModel.getObject().getSelection().getBegin()
                    : -1;
            
            choices = clService.getLinkingInstancesInKBScope(traits.getRepositoryId(),
                    traits.getScope(), traits.getAllowedValueType(), aInput, mention, mentionBegin,
                    cas, feat.getProject());
        }
        catch (Exception e) {
            choices = asList(new KBHandle("http://ERROR", "ERROR", e.getMessage(), "en"));
            error("An error occurred while retrieving entity candidates: " + e.getMessage());
            LOG.error("An error occurred while retrieving entity candidates", e);
            RequestCycle.get()
                .find(IPartialPageRequestHandler.class)
                .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));
        }
        return choices;
    }

    private ConceptFeatureTraits readFeatureTraits(AnnotationFeature aAnnotationFeature)
    {
        FeatureSupport<ConceptFeatureTraits> fs = featureSupportRegistry
                .getFeatureSupport(aAnnotationFeature);
        ConceptFeatureTraits traits = fs.readTraits(aAnnotationFeature);
        return traits;
    }

    @Override
    public Component getFocusComponent()
    {
        return focusComponent;
    }
}
