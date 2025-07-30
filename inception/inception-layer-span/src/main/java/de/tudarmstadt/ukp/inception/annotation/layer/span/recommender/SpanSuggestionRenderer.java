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
package de.tudarmstadt.ukp.inception.annotation.layer.span.recommender;

import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.span.config.SpanRecommenderProperties;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionRenderer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VAnnotationMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

public class SpanSuggestionRenderer
    implements SuggestionRenderer
{
    private final RecommendationService recommendationService;
    private final AnnotationSchemaService schemaService;
    private final FeatureSupportRegistry fsRegistry;
    private final SpanRecommenderProperties recommenderProperties;

    public SpanSuggestionRenderer(RecommendationService aRecommendationService,
            AnnotationSchemaService aAnnotationService, FeatureSupportRegistry aFsRegistry,
            SpanRecommenderProperties aRecommenderProperties)
    {
        recommendationService = aRecommendationService;
        schemaService = aAnnotationService;
        fsRegistry = aFsRegistry;
        recommenderProperties = aRecommenderProperties;
    }

    @Override
    public void render(VDocument aVDoc, RenderRequest aRequest,
            SuggestionDocumentGroup<? extends AnnotationSuggestion> aSuggestions,
            AnnotationLayer aLayer)
    {
        var groups = (SuggestionDocumentGroup<SpanSuggestion>) aSuggestions;

        // No recommendations to render for this layer
        if (groups.isEmpty()) {
            return;
        }

        var cas = aRequest.getCas();

        recommendationService.calculateSuggestionVisibility(
                aRequest.getSessionOwner().getUsername(), aRequest.getSourceDocument(), cas,
                aRequest.getAnnotationUser().getUsername(), aLayer, groups,
                aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset());

        var pref = recommendationService.getPreferences(aRequest.getAnnotationUser(),
                aLayer.getProject());

        // Bulk-load all the features of this layer to avoid having to do repeated DB accesses later
        var features = schemaService.listSupportedFeatures(aLayer).stream()
                .collect(toMap(AnnotationFeature::getName, identity()));

        var rankerCache = new HashMap<Long, Boolean>();

        for (var suggestionGroup : groups) {
            // Render annotations for each label
            for (var suggestion : suggestionGroup.bestSuggestions(pref)) {
                var range = VRange.clippedRange(aVDoc, suggestion.getBegin(), suggestion.getEnd());
                if (!range.isPresent()) {
                    continue;
                }

                var feature = features.get(suggestion.getFeature());

                var hiddenValues = aRequest.getHiddenFeatureValues().getOrDefault(feature.getId(),
                        emptySet());
                if (hiddenValues.contains(suggestion.getLabel())) {
                    // If the feature value is hidden, we do not render the suggestion
                    continue;
                }

                // Retrieve the UI display label for the given feature value
                var featureSupport = fsRegistry.findExtension(feature).orElseThrow();
                var annotation = featureSupport.renderFeatureValue(feature, suggestion.getLabel());

                Map<String, String> featureAnnotation = annotation != null
                        ? Map.of(suggestion.getFeature(), annotation)
                        : Map.of();

                var isRanker = rankerCache.computeIfAbsent(suggestion.getRecommenderId(), id -> {
                    var recommender = recommendationService.getRecommender(id);
                    if (recommender != null) {
                        var factory = recommendationService.getRecommenderFactory(recommender);
                        return factory.map(f -> f.isRanker(recommender)).orElse(false);
                    }
                    return false;
                });

                var v = new VSpan(aLayer, suggestion.getVID(), range.get(), featureAnnotation,
                        COLOR);
                v.setScore(suggestion.getScore());
                v.setHideScore(isRanker);
                v.setActionButtons(recommenderProperties.isActionButtonsEnabled());
                aVDoc.add(v);

                if (suggestion.isCorrection()) {
                    aVDoc.add(new VAnnotationMarker(VMarker.WARN, v.getVid()));
                }
            }
        }
    }
}
