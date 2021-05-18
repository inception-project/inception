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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VLazyDetailQuery;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasMetadataUtils;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;

/**
 * Render spans.
 */
public class RecommendationSpanRenderer
    implements RecommendationTypeRenderer
{
    private SpanAdapter typeAdapter;

    public RecommendationSpanRenderer(SpanAdapter aTypeAdapter)
    {
        typeAdapter = aTypeAdapter;
    }

    /**
     * Add annotations from the CAS, which is controlled by the window size, to the VDocument
     * {@link VDocument}
     *
     * @param aCas
     *            The CAS object containing annotations
     * @param vdoc
     *            A VDocument containing annotations for the given layer
     * @param aState
     *            Data model for brat annotations
     */
    @Override
    public void render(CAS aCas, VDocument vdoc, AnnotatorState aState, AnnotationLayer aLayer,
            RecommendationService aRecommendationService,
            LearningRecordService learningRecordService, AnnotationSchemaService aAnnotationService,
            FeatureSupportRegistry aFsRegistry, DocumentService aDocumentService,
            int aWindowBeginOffset, int aWindowEndOffset)
    {
        if (aCas == null || aRecommendationService == null) {
            return;
        }

        Predictions predictions = aRecommendationService.getPredictions(aState.getUser(),
                aState.getProject());
        // No recommendations available at all
        if (predictions == null) {
            return;
        }

        // TODO #176 use the document Id once it it available in the CAS
        String sourceDocumentName = CasMetadataUtils.getSourceDocumentName(aCas)
                .orElse(getDocumentTitle(aCas));
        SuggestionDocumentGroup<SpanSuggestion> groups = predictions.getGroupedPredictions(
                SpanSuggestion.class, sourceDocumentName, aLayer, aWindowBeginOffset,
                aWindowEndOffset);

        // No recommendations to render for this layer
        if (groups.isEmpty()) {
            return;
        }

        String bratTypeName = typeAdapter.getEncodedTypeName();

        aRecommendationService.calculateSpanSuggestionVisibility(aCas,
                aState.getUser().getUsername(), aLayer, groups, aWindowBeginOffset,
                aWindowEndOffset);

        Preferences pref = aRecommendationService.getPreferences(aState.getUser(),
                aLayer.getProject());

        // Bulk-load all the features of this layer to avoid having to do repeated DB accesses later
        Map<String, AnnotationFeature> features = aAnnotationService.listSupportedFeatures(aLayer)
                .stream()
                .collect(Collectors.toMap(AnnotationFeature::getName, Function.identity()));

        for (SuggestionGroup<SpanSuggestion> suggestionGroup : groups) {
            // Render annotations for each label
            for (SpanSuggestion ao : suggestionGroup.bestSuggestions(pref)) {
                VID vid = ao.getVID();

                // Here, we generate a visual suggestion representation based on the first
                // suggestion with a given label. We can later get info about the other
                // recommendations for that label via the lazy details
                AnnotationFeature feature = features.get(ao.getFeature());

                // Retrieve the UI display label for the given feature value
                FeatureSupport<?> featureSupport = aFsRegistry.findExtension(feature).orElseThrow();
                String annotation = featureSupport.renderFeatureValue(feature, ao.getLabel());

                Map<String, String> featureAnnotation = new HashMap<>();
                featureAnnotation.put(ao.getFeature(), annotation);

                VSpan v = new VSpan(aLayer, vid, bratTypeName,
                        new VRange(ao.getBegin() - aWindowBeginOffset,
                                ao.getEnd() - aWindowBeginOffset),
                        featureAnnotation, COLOR);
                v.addLazyDetails(featureSupport.getLazyDetails(feature, ao.getLabel()));
                v.addLazyDetail(new VLazyDetailQuery(feature.getName(), ao.getLabel()));
                vdoc.add(v);
            }
        }
    }
}
