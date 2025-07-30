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
package de.tudarmstadt.ukp.inception.annotation.layer.relation.recommender;

import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.uima.fit.util.CasUtil.selectAt;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionRenderer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.ArcSuggestion_ImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

public abstract class ArcSuggestionRenderer_ImplBase<T extends ArcSuggestion_ImplBase<?>>
    implements SuggestionRenderer
{
    private final RecommendationService recommendationService;
    private final AnnotationSchemaService annotationService;
    private final FeatureSupportRegistry fsRegistry;

    public ArcSuggestionRenderer_ImplBase(RecommendationService aRecommendationService,
            AnnotationSchemaService aAnnotationService, FeatureSupportRegistry aFsRegistry)
    {
        recommendationService = aRecommendationService;
        annotationService = aAnnotationService;
        fsRegistry = aFsRegistry;
    }

    @Override
    public void render(VDocument aVDoc, RenderRequest aRequest,
            SuggestionDocumentGroup<? extends AnnotationSuggestion> aSuggestions,
            AnnotationLayer aLayer)
    {
        var cas = aRequest.getCas();

        // TODO #176 use the document Id once it it available in the CAS
        var groupedPredictions = (SuggestionDocumentGroup<T>) aSuggestions;

        // No recommendations to render for this layer
        if (groupedPredictions.isEmpty()) {
            return;
        }

        recommendationService.calculateSuggestionVisibility(
                aRequest.getSessionOwner().getUsername(), aRequest.getSourceDocument(), cas,
                aRequest.getAnnotationUser().getUsername(), aLayer, groupedPredictions,
                aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset());

        var pref = recommendationService.getPreferences(aRequest.getAnnotationUser(),
                aLayer.getProject());

        // Bulk-load all the features of this layer to avoid having to do repeated DB accesses later
        var features = annotationService.listSupportedFeatures(aLayer).stream()
                .collect(toMap(AnnotationFeature::getName, identity()));

        var rankerCache = new HashMap<Long, Boolean>();

        for (var group : groupedPredictions) {
            for (var suggestion : group.bestSuggestions(pref)) {
                // Skip rendering AnnotationObjects that should not be rendered
                if (!pref.isShowAllPredictions() && !suggestion.isVisible()) {
                    continue;
                }

                var feature = features.get(suggestion.getFeature());

                var hiddenValues = aRequest.getHiddenFeatureValues().getOrDefault(feature.getId(),
                        emptySet());
                if (hiddenValues.contains(suggestion.getLabel())) {
                    // If the feature value is hidden, we do not render the suggestion
                    continue;
                }

                var position = suggestion.getPosition();
                int sourceBegin = position.getSourceBegin();
                int sourceEnd = position.getSourceEnd();
                int targetBegin = position.getTargetBegin();
                int targetEnd = position.getTargetEnd();

                // Retrieve the UI display label for the given feature value
                var sourceType = getSourceType(cas, aLayer, feature);
                var targetType = getTargetType(cas, aLayer, feature);

                // FIXME: We get the first match for the (begin, end) span. With stacking, there can
                // be more than one and we need to get the right one then which does not need to be
                // the first. We wait for #2135 for a maybe fix.
                var source = selectAt(cas, sourceType, sourceBegin, sourceEnd) //
                        .stream().findFirst().orElse(null);

                var target = selectAt(cas, targetType, targetBegin, targetEnd) //
                        .stream().findFirst().orElse(null);

                if (source == null || target == null) {
                    continue;
                }

                FeatureSupport<?> featureSupport = fsRegistry.findExtension(feature).orElseThrow();
                var annotation = featureSupport.renderFeatureValue(feature, suggestion.getLabel());

                Map<String, String> featureValues = annotation != null
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

                var arc = renderArc(aLayer, suggestion, source, target, featureValues);
                arc.setScore(suggestion.getScore());
                arc.setHideScore(isRanker);

                aVDoc.add(arc);
            }
        }
    }

    protected abstract VArc renderArc(AnnotationLayer aLayer, T suggestion, AnnotationFS source,
            AnnotationFS target, Map<String, String> featureAnnotation);

    protected abstract Type getSourceType(CAS aCas, AnnotationLayer aLayer,
            AnnotationFeature aFeature);

    protected abstract Type getTargetType(CAS aCas, AnnotationLayer aLayer,
            AnnotationFeature aFeature);
}
