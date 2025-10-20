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
package de.tudarmstadt.ukp.inception.recommendation.render;

import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.ANNOTATION;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.KEY_RECOMMENDER_GENERAL_SETTINGS;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup.groupByType;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportQuery;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderStep;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RecommenderServiceAutoConfiguration#recommendationRenderer}.
 * </p>
 */
@Order(RenderStep.RENDER_SYNTHETIC_STRUCTURE)
public class RecommendationRenderer
    implements RenderStep
{
    public static final String ID = "RecommendationRenderer";

    private final RecommendationService recommendationService;
    private final SuggestionSupportRegistry suggestionSupportRegistry;
    private final PreferencesService preferencesService;
    private final UserDao userService;

    public RecommendationRenderer(RecommendationService aRecommendationService,
            SuggestionSupportRegistry aSuggestionSupportRegistry,
            PreferencesService aPreferencesService, UserDao aUserService)
    {
        recommendationService = aRecommendationService;
        suggestionSupportRegistry = aSuggestionSupportRegistry;
        preferencesService = aPreferencesService;
        userService = aUserService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public boolean accepts(RenderRequest aRequest)
    {
        var state = aRequest.getState();

        if (aRequest.getCas() == null) {
            return false;
        }

        // Do not show predictions on curation page
        if (state != null && state.getMode() != ANNOTATION) {
            return false;
        }

        var prefs = preferencesService.loadDefaultTraitsForProject(KEY_RECOMMENDER_GENERAL_SETTINGS,
                aRequest.getProject());

        // Do not show predictions when viewing annotations of another user
        if (!prefs.isShowRecommendationsWhenViewingOtherUser()
                && !Objects.equals(aRequest.getAnnotationUser(), aRequest.getSessionOwner())) {
            return false;
        }

        // Do not show predictions when viewing annotations of curation user
        if (!prefs.isShowRecommendationsWhenViewingCurationUser()
                && Objects.equals(aRequest.getAnnotationUser(), userService.getCurationUser())) {
            return false;
        }

        return true;
    }

    @Override
    public void render(VDocument aVDoc, RenderRequest aRequest)
    {
        var cas = aRequest.getCas();

        if (cas == null || recommendationService == null) {
            return;
        }

        var predictions = recommendationService.getPredictions(aRequest.getSessionOwner(),
                aRequest.getProject());
        if (predictions == null) {
            return;
        }

        var suggestions = predictions.getSuggestionsByDocument(aRequest.getSourceDocument(),
                aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset());
        var suggestionsByLayer = suggestions.stream()
                .collect(groupingBy(AnnotationSuggestion::getLayerId));

        var recommenderCache = recommendationService.listEnabledRecommenders(aRequest.getProject())
                .stream().collect(toMap(Recommender::getId, identity()));
        var suggestionSupportCache = new HashMap<SuggestionSupportQuery, Optional<SuggestionSupport>>();

        for (var layer : aRequest.getVisibleLayers()) {
            if (!layer.isEnabled() || layer.isReadonly()) {
                continue;
            }

            var suggestionsByType = groupByType(suggestionsByLayer.get(layer.getId()));
            if (suggestionsByType.isEmpty()) {
                continue;
            }

            for (var suggestionGroup : suggestionsByType.entrySet()) {
                var suggestion = suggestionGroup.getValue().iterator().next().iterator().next();

                var recommender = recommenderCache.get(suggestion.getRecommenderId());
                if (recommender == null) {
                    continue;
                }

                var suggestionSupport = suggestionSupportCache.computeIfAbsent(
                        SuggestionSupportQuery.of(recommender),
                        suggestionSupportRegistry::findGenericExtension);
                if (suggestionSupport.isEmpty()) {
                    continue;
                }

                suggestionSupport.get().getRenderer().ifPresent(renderer -> renderer.render(aVDoc,
                        aRequest, suggestionGroup.getValue(), layer));
            }
        }
    }
}
