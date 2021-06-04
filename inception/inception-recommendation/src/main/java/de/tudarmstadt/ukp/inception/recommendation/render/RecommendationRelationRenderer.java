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
import static org.apache.uima.fit.util.CasUtil.selectAt;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VArc;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VLazyDetailQuery;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasMetadataUtils;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;

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
     * @param aCas
     *            The CAS object containing annotations
     * @param vdoc
     *            A VDocument containing annotations for the given layer
     */
    @Override
    public void render(CAS aCas, VDocument vdoc, AnnotatorState aState, int aWindowBeginOffset,
            int aWindowEndOffset)
    {
        if (aCas == null || recommendationService == null) {
            return;
        }

        Predictions predictions = recommendationService.getPredictions(aState.getUser(),
                aState.getProject());

        // No recommendations available at all
        if (predictions == null) {
            return;
        }

        AnnotationLayer layer = typeAdapter.getLayer();

        // TODO #176 use the document Id once it it available in the CAS
        String sourceDocumentName = CasMetadataUtils.getSourceDocumentName(aCas)
                .orElseGet(() -> getDocumentTitle(aCas));
        SuggestionDocumentGroup<RelationSuggestion> groupedPredictions = predictions
                .getGroupedPredictions(RelationSuggestion.class, sourceDocumentName, layer,
                        aWindowBeginOffset, aWindowEndOffset);

        // No recommendations to render for this layer
        if (groupedPredictions.isEmpty()) {
            return;
        }

        recommendationService.calculateRelationSuggestionVisibility(aCas,
                aState.getUser().getUsername(), layer, groupedPredictions, aWindowBeginOffset,
                aWindowEndOffset);

        Preferences pref = recommendationService.getPreferences(aState.getUser(),
                layer.getProject());

        String bratTypeName = typeAdapter.getEncodedTypeName();

        Type attachType = CasUtil.getType(aCas, layer.getAttachType().getName());

        // Bulk-load all the features of this layer to avoid having to do repeated DB accesses later
        Map<String, AnnotationFeature> features = annotationService.listSupportedFeatures(layer)
                .stream()
                .collect(Collectors.toMap(AnnotationFeature::getName, Function.identity()));

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
                AnnotationFS source = selectAt(aCas, attachType, sourceBegin, sourceEnd) //
                        .stream().findFirst().orElse(null);

                AnnotationFS target = selectAt(aCas, attachType, targetBegin, targetEnd) //
                        .stream().findFirst().orElse(null);

                // Retrieve the UI display label for the given feature value
                AnnotationFeature feature = features.get(suggestion.getFeature());

                FeatureSupport<?> featureSupport = fsRegistry.findExtension(feature).orElseThrow();
                String annotation = featureSupport.renderFeatureValue(feature,
                        suggestion.getLabel());

                Map<String, String> featureAnnotation = new HashMap<>();
                featureAnnotation.put(suggestion.getFeature(), annotation);

                VArc arc = new VArc(layer, suggestion.getVID(), bratTypeName, new VID(source),
                        new VID(target), "\uD83E\uDD16 " + suggestion.getUiLabel(),
                        featureAnnotation, COLOR);

                arc.addLazyDetails(featureSupport.getLazyDetails(feature, suggestion.getLabel()));
                arc.addLazyDetail(new VLazyDetailQuery(feature.getName(), suggestion.getLabel()));

                vdoc.add(arc);
            }
        }
    }
}
