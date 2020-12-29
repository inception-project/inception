/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.render;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getDocumentTitle;
import static java.util.Comparator.comparingInt;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
import de.tudarmstadt.ukp.clarin.webanno.api.dao.CasMetadataUtils;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
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

        for (SuggestionGroup<SpanSuggestion> suggestion : groups) {
            Map<LabelMapKey, Map<Long, SpanSuggestion>> labelMap = new HashMap<>();

            // For recommendations with the same label by the same classifier,
            // show only the confidence of the highest one
            for (SpanSuggestion ao : suggestion) {

                // Skip rendering AnnotationObjects that should not be rendered
                if (!pref.isShowAllPredictions() && !ao.isVisible()) {
                    continue;
                }

                LabelMapKey label = new LabelMapKey(ao);

                if (!labelMap.containsKey(label)
                        || !labelMap.get(label).containsKey(ao.getRecommenderId())
                        || labelMap.get(label).get(ao.getRecommenderId()).getConfidence() < ao
                                .getConfidence()) {

                    Map<Long, SpanSuggestion> confidencePerClassifier;
                    if (labelMap.get(label) == null) {
                        confidencePerClassifier = new HashMap<>();
                    }
                    else {
                        confidencePerClassifier = labelMap.get(label);
                    }

                    confidencePerClassifier.put(ao.getRecommenderId(), ao);
                    labelMap.put(label, confidencePerClassifier);
                }
            }

            // Determine the maximum confidence per Label
            Map<LabelMapKey, Double> maxConfidencePerLabel = new HashMap<>();
            for (LabelMapKey label : labelMap.keySet()) {
                double maxConfidence = 0;
                for (Entry<Long, SpanSuggestion> classifier : labelMap.get(label).entrySet()) {
                    if (classifier.getValue().getConfidence() > maxConfidence) {
                        maxConfidence = classifier.getValue().getConfidence();
                    }
                }
                maxConfidencePerLabel.put(label, maxConfidence);
            }

            // Sort and filter labels under threshold value
            // Note: the order in which annotations are rendered is only indicative to the
            // frontend (e.g. brat) which may choose to re-order them (e.g. for layout reasons).
            List<LabelMapKey> sortedAndfiltered = maxConfidencePerLabel.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(pref.getMaxPredictions()).map(Entry::getKey)
                    .collect(Collectors.toList());

            // Render annotations for each label
            for (LabelMapKey label : sortedAndfiltered) {
                // Create VID using the recommendation with the lowest recommendationId
                AnnotationSuggestion canonicalRecommendation = suggestion.stream()
                        // check for label or feature for no-label annotations as key
                        .filter(p -> label.equalsAnnotationSuggestion(p))
                        .max(comparingInt(SpanSuggestion::getId)).orElse(null);

                if (canonicalRecommendation == null) {
                    continue;
                }

                VID vid = canonicalRecommendation.getVID();

                // Here, we generate a visual suggestion representation based on the first
                // suggestion with a given label. We can later get info about the other
                // recommendations for that label via the lazy details
                SpanSuggestion ao = labelMap.get(label).values().stream().findFirst().get();
                AnnotationFeature feature = features.get(ao.getFeature());

                // Retrieve the UI display label for the given feature value
                FeatureSupport<?> featureSupport = aFsRegistry.findExtension(feature);
                String annotation = featureSupport.renderFeatureValue(feature, ao.getLabel());

                Map<String, String> featureAnnotation = new HashMap<>();
                featureAnnotation.put(ao.getFeature(), annotation);

                VSpan v = new VSpan(aLayer, vid, bratTypeName,
                        new VRange(ao.getBegin() - aWindowBeginOffset,
                                ao.getEnd() - aWindowBeginOffset),
                        featureAnnotation, Collections.emptyMap(), COLOR);
                v.addLazyDetails(featureSupport.getLazyDetails(feature, ao.getLabel()));
                v.addLazyDetail(new VLazyDetailQuery(feature.getName(), ao.getLabel()));
                vdoc.add(v);
            }
        }
    }

    /**
     * 
     * A Key identifying an AnnotationSuggestion by its label or as a suggestion without label.
     *
     */
    protected class LabelMapKey
    {

        private String label;

        private boolean hasNoLabel;

        public LabelMapKey(AnnotationSuggestion aSuggestion)
        {
            if (aSuggestion.getLabel() == null) {
                hasNoLabel = true;
                label = aSuggestion.getFeature();
            }
            else {
                label = aSuggestion.getLabel();
            }
        }

        @Override
        public boolean equals(Object aObj)
        {
            if (aObj == null || getClass() != aObj.getClass()) {
                return false;
            }

            LabelMapKey aKey = (LabelMapKey) aObj;
            return label.equals(aKey.getLabel()) && hasNoLabel == aKey.hasNoLabel();
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(label, hasNoLabel);
        }

        public String getLabel()
        {
            return label;
        }

        public boolean hasNoLabel()
        {
            return hasNoLabel;
        }

        public boolean equalsAnnotationSuggestion(AnnotationSuggestion aSuggestion)
        {
            // annotation is label-less
            if (aSuggestion.getLabel() == null) {
                return hasNoLabel && label.equals(aSuggestion.getFeature());
            }
            else {
                return !hasNoLabel && label.equals(aSuggestion.getLabel());
            }
        }

    }
}
