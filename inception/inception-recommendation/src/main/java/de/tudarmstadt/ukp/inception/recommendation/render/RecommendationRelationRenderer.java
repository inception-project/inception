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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getDocumentTitle;
import static de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils.getSourceDocumentName;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.uima.fit.util.CasUtil.selectAt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailQuery;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;

/**
 * Render spans.
 */
public class RecommendationRelationRenderer
    implements RecommendationTypeRenderer
{
    private final RelationAdapter typeAdapter;
    private final RecommendationService recommendationService;
    private final AnnotationSchemaService annotationService;
    private final FeatureSupportRegistry fsRegistry;

    public RecommendationRelationRenderer(RelationAdapter aTypeAdapter,
            RecommendationService aRecommendationService,
            AnnotationSchemaService aAnnotationService, FeatureSupportRegistry aFsRegistry)
    {
        typeAdapter = aTypeAdapter;
        recommendationService = aRecommendationService;
        annotationService = aAnnotationService;
        fsRegistry = aFsRegistry;
    }

    /**
     * Add annotations from the CAS, which is controlled by the window size, to the VDocument
     * {@link VDocument}
     *
     * @param aVDoc
     *            A VDocument containing annotations for the given layer
     */
    @Override
    public void render(VDocument aVDoc, RenderRequest aRequest)
    {
        if (aRequest.getCas() == null || recommendationService == null) {
            return;
        }

        Predictions predictions = recommendationService.getPredictions(aRequest.getAnnotationUser(),
                aRequest.getProject());

        // No recommendations available at all
        if (predictions == null) {
            return;
        }

        CAS cas = aRequest.getCas();

        AnnotationLayer layer = typeAdapter.getLayer();

        // TODO #176 use the document Id once it it available in the CAS
        String sourceDocumentName = getSourceDocumentName(cas)
                .orElseGet(() -> getDocumentTitle(cas));
        SuggestionDocumentGroup<RelationSuggestion> groupedPredictions = predictions
                .getGroupedPredictions(RelationSuggestion.class, sourceDocumentName, layer,
                        aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset());

        // No recommendations to render for this layer
        if (groupedPredictions.isEmpty()) {
            return;
        }

        recommendationService.calculateRelationSuggestionVisibility(cas,
                aRequest.getAnnotationUser().getUsername(), layer, groupedPredictions,
                aRequest.getWindowBeginOffset(), aRequest.getWindowEndOffset());

        Preferences pref = recommendationService.getPreferences(aRequest.getAnnotationUser(),
                layer.getProject());

        Type attachType = CasUtil.getType(cas, layer.getAttachType().getName());

        // Bulk-load all the features of this layer to avoid having to do repeated DB accesses later
        var features = annotationService.listSupportedFeatures(layer).stream()
                .collect(toMap(AnnotationFeature::getName, identity()));

        for (SuggestionGroup<RelationSuggestion> group : groupedPredictions) {
            for (RelationSuggestion suggestion : group.bestSuggestions(pref)) {

                // Skip rendering AnnotationObjects that should not be rendered
                if (!pref.isShowAllPredictions() && !suggestion.isVisible()) {
                    continue;
                }

                RelationPosition position = suggestion.getPosition();
                int sourceBegin = position.getSourceBegin();
                int sourceEnd = position.getSourceEnd();
                int targetBegin = position.getTargetBegin();
                int targetEnd = position.getTargetEnd();

                // FIXME: We get the first match for the (begin, end) span. With stacking, there can
                // be more than one and we need to get the right one then which does not need to be
                // the first. We wait for #2135 for a maybe fix.
                AnnotationFS source = selectAt(cas, attachType, sourceBegin, sourceEnd) //
                        .stream().findFirst().orElse(null);

                AnnotationFS target = selectAt(cas, attachType, targetBegin, targetEnd) //
                        .stream().findFirst().orElse(null);

                // Retrieve the UI display label for the given feature value
                AnnotationFeature feature = features.get(suggestion.getFeature());

                FeatureSupport<?> featureSupport = fsRegistry.findExtension(feature).orElseThrow();
                String annotation = featureSupport.renderFeatureValue(feature,
                        suggestion.getLabel());

                Map<String, String> featureAnnotation = new HashMap<>();
                featureAnnotation.put(suggestion.getFeature(), annotation);

                VArc arc = new VArc(layer, suggestion.getVID(), new VID(source), new VID(target),
                        "\uD83E\uDD16 " + suggestion.getUiLabel(), featureAnnotation, COLOR);

                List<VLazyDetailQuery> lazyDetails = featureSupport.getLazyDetails(feature,
                        suggestion.getLabel());
                if (!lazyDetails.isEmpty()) {
                    arc.addLazyDetails(lazyDetails);
                }
                else {
                    arc.addLazyDetail(
                            new VLazyDetailQuery(feature.getName(), suggestion.getLabel()));
                }

                aVDoc.add(arc);
            }
        }
    }
}
