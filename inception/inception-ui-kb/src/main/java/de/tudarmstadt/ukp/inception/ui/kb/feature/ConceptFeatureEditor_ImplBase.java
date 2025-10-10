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

import static java.lang.System.currentTimeMillis;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.conceptlinking.config.EntityLinkingProperties;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits_ImplBase;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.kendo.KendoChoiceDescriptionScriptReference;
import de.tudarmstadt.ukp.inception.support.wicket.WicketUtil;

/**
 * Component for editing knowledge-base-related features on annotations.
 */
public abstract class ConceptFeatureEditor_ImplBase
    extends FeatureEditor
{
    private static final long serialVersionUID = 6118093030106338883L;

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean ConceptLinkingService clService;
    private @SpringBean EntityLinkingProperties entityLinkingProperties;

    public ConceptFeatureEditor_ImplBase(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel)
    {
        super(aId, aItem, new CompoundPropertyModel<>(aModel));

        var feat = getModelObject().feature;
        var traits = readFeatureTraits(feat);

        add(new DisabledKBWarning("disabledKBWarning", Model.of(feat),
                Model.of(traits.getRepositoryId())));
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(KendoChoiceDescriptionScriptReference.get()));
    }

    protected List<KBHandle> getCandidates(IModel<AnnotatorState> aStateModel,
            AnnotationActionHandler aHandler, String aInput)
    {
        var startTime = currentTimeMillis();

        if (aInput == null) {
            return emptyList();
        }

        String input = aInput;

        // Extract filter on the description and/or canonical term
        final String secondaryFilter;
        if (input.contains("::")) {
            secondaryFilter = substringAfter(input, "::").trim();
            input = substringBefore(input, "::");
        }
        else {
            secondaryFilter = null;
        }

        // Extract exact match filter on the query
        var labelFilter = false;
        var trimmedInput = input.trim();
        if (trimmedInput.length() > 2 && trimmedInput.startsWith("\"")
                && trimmedInput.endsWith("\"")) {
            input = StringUtils.substring(trimmedInput, 1, -1).trim();
            labelFilter = true;
        }

        final var finalInput = input;

        List<KBHandle> choices;
        try {
            var feat = getModelObject().feature;

            var traits = readFeatureTraits(feat);
            var repoId = traits.getRepositoryId();
            // Check if kb is actually enabled
            if (!(repoId == null || kbService.isKnowledgeBaseEnabled(feat.getProject(), repoId))) {
                return Collections.emptyList();
            }

            // If there is a selection, we try obtaining its text from the CAS and use it as an
            // additional item in the query. Note that there is not always a mention, e.g. when the
            // feature is used in a document-level annotations.
            var cas = aHandler != null ? aHandler.getEditorCas() : null;

            String mention = null;
            int mentionBegin = -1;

            if (aStateModel != null) {
                var selection = aStateModel.getObject().getSelection();
                if (selection.isSpan()) {
                    mention = selection.getText();
                    mentionBegin = selection.getBegin();
                }

                if (selection.isArc()) {
                    mention = selection.getOriginText() + " " + selection.getTargetText();
                    mentionBegin = selection.getBegin();
                }
            }

            choices = clService.getLinkingInstancesInKBScope(traits.getRepositoryId(),
                    traits.getScope(), traits.getAllowedValueType(), finalInput, mention,
                    mentionBegin, cas, feat.getProject());
        }
        catch (Exception e) {
            choices = asList(new KBHandle("http://ERROR", "ERROR", e.getMessage(), "en"));
            error("An error occurred while retrieving entity candidates: " + e.getMessage());
            LOG.error("An error occurred while retrieving entity candidates", e);
            RequestCycle.get().find(IPartialPageRequestHandler.class)
                    .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));
        }

        if (labelFilter) {
            choices = choices.stream() //
                    .filter(kb -> Strings.CI.contains(kb.getUiLabel(), finalInput)) //
                    .toList();
        }

        if (isNotBlank(secondaryFilter)) {
            choices = choices.stream() //
                    .filter(kb -> applySecondaryFilter(kb, secondaryFilter)) //
                    .toList();
        }

        var result = choices.stream()//
                .limit(entityLinkingProperties.getCandidateDisplayLimit()) //
                .toList();

        WicketUtil.serverTiming("getCandidates", currentTimeMillis() - startTime);

        return result;
    }

    private boolean applySecondaryFilter(KBHandle aObject, String aFilter)
    {
        if (Strings.CI.contains(aObject.getDescription(), aFilter)) {
            return true;
        }

        if (aObject.getQueryBestMatchTerm() != null
                && Strings.CI.contains(aObject.getUiLabel(), aFilter)) {
            return true;
        }

        return false;
    }

    protected abstract ConceptFeatureTraits_ImplBase readFeatureTraits(
            AnnotationFeature aAnnotationFeature);
}
