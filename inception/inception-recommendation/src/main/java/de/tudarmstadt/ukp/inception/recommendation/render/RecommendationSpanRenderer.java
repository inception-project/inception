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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapter;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderProperties;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RecommenderServiceAutoConfiguration#recommendationSpanRenderer}.
 * </p>
 */
public class RecommendationSpanRenderer
    implements RecommendationTypeRenderer<SpanAdapter>
{
    private final RecommendationService recommendationService;
    private final AnnotationSchemaService annotationService;
    private final FeatureSupportRegistry fsRegistry;
    private final RecommenderProperties recommenderProperties;

    public RecommendationSpanRenderer(RecommendationService aRecommendationService,
            AnnotationSchemaService aAnnotationService, FeatureSupportRegistry aFsRegistry,
            RecommenderProperties aRecommenderProperties)
    {
        recommendationService = aRecommendationService;
        annotationService = aAnnotationService;
        fsRegistry = aFsRegistry;
        recommenderProperties = aRecommenderProperties;
    }

    /**
     * Add annotations from the CAS, which is controlled by the window size, to the VDocument
     * {@link VDocument}
     *
     * @param vdoc
     *            A VDocument containing annotations for the given layer
     * @param aPredictions
     *            the predictions to render
     */
    @Override
    public void render(VDocument vdoc, RenderRequest aRequest, Predictions aPredictions,
            SpanAdapter aTypeAdapter)
    {
        var cas = aRequest.getCas();
        var layer = aTypeAdapter.getLayer();
        var groups = aPredictions.getGroupedPredictions(SpanSuggestion.class,
                aRequest.getSourceDocument().getName(), layer, aRequest.getWindowBeginOffset(),
                aRequest.getWindowEndOffset());

        // No recommendations to render for this layer
        if (groups.isEmpty()) {
            return;
        }

        recommendationService.calculateSuggestionVisibility(
                aRequest.getSessionOwner().getUsername(), aRequest.getSourceDocument(), cas,
                aRequest.getAnnotationUser().getUsername(), layer, groups,
                aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset());

        var pref = recommendationService.getPreferences(aRequest.getAnnotationUser(),
                layer.getProject());

        // Bulk-load all the features of this layer to avoid having to do repeated DB accesses later
        var features = annotationService.listSupportedFeatures(layer).stream()
                .collect(toMap(AnnotationFeature::getName, identity()));

        for (var suggestionGroup : groups) {
            // Render annotations for each label
            for (var suggestion : suggestionGroup.bestSuggestions(pref)) {
                var range = VRange.clippedRange(vdoc, suggestion.getBegin(), suggestion.getEnd());
                if (!range.isPresent()) {
                    continue;
                }

                var feature = features.get(suggestion.getFeature());

                // Retrieve the UI display label for the given feature value
                var featureSupport = fsRegistry.findExtension(feature).orElseThrow();
                var annotation = featureSupport.renderFeatureValue(feature, suggestion.getLabel());

                Map<String, String> featureAnnotation = annotation != null
                        ? Map.of(suggestion.getFeature(), annotation)
                        : Map.of();

                var v = new VSpan(layer, suggestion.getVID(), range.get(), featureAnnotation,
                        COLOR);
                v.setScore(suggestion.getScore());
                v.setActionButtons(recommenderProperties.isActionButtonsEnabled());

                vdoc.add(v);
            }
        }
    }
}
