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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VComment;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VCommentType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VRange;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VSpan;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.util.PredictionUtil;

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
     * @param aJcas
     *            The JCAS object containing annotations
     * @param vdoc
     *            A VDocument containing annotations for the given layer
     * @param aState
     *            Data model for brat annotations
     * @param aColoringStrategy
     *            the coloring strategy to render this layer
     */
    @Override
    public void render(JCas aJcas, VDocument vdoc, AnnotatorState aState,
        ColoringStrategy aColoringStrategy, AnnotationLayer layer,
        RecommendationService recommendationService, LearningRecordService learningRecordService,
        AnnotationSchemaService aAnnotationService, FeatureSupportRegistry aFsRegistry,
        DocumentService aDocumentService)
    {
        if (aJcas == null || recommendationService == null) {
            return;
        }

        int windowBegin = aState.getWindowBeginOffset();
        int windowEnd = aState.getWindowEndOffset();

        Predictions model = recommendationService.getPredictions(aState.getUser(),
                aState.getProject());
        // No recommendations available at all
        if (model == null) {
            return;
        }
        
        // TODO #176 use the document Id once it it available in the CAS
        SuggestionDocumentGroup groups = model.getPredictions(
                DocumentMetaData.get(aJcas).getDocumentTitle(), layer, windowBegin, windowEnd);
        
        // No recommendations to render for this layer
        if (groups.isEmpty()) {
            return;
        }
        
        String color = aColoringStrategy.getColor(null, null);
        String bratTypeName = TypeUtil.getUiTypeName(typeAdapter);

        PredictionUtil.calculateVisibility(learningRecordService, aAnnotationService,
                aJcas.getCas(), aState.getUser().getUsername(), layer, groups, windowBegin,
                windowEnd);

        Preferences pref = recommendationService.getPreferences(aState.getUser(),
                layer.getProject());

        for (SuggestionGroup suggestion : groups) {
            Map<String, Map<Long, AnnotationSuggestion>> labelMap = new HashMap<>();
 
            // For recommendations with the same label by the same classifier,
            // show only the confidence of the highest one
            for (AnnotationSuggestion ao: suggestion) {

                // Skip rendering AnnotationObjects that should not be rendered
                if (!pref.isShowAllPredictions() && !ao.isVisible()) {
                    continue;
                }

                if (!labelMap.containsKey(ao.getLabel())
                        || !labelMap.get(ao.getLabel())
                                .containsKey(ao.getRecommenderId())
                        || labelMap.get(ao.getLabel()).get(ao.getRecommenderId())
                                .getConfidence() < ao.getConfidence()) {

                    Map<Long, AnnotationSuggestion> confidencePerClassifier;
                    if (labelMap.get(ao.getLabel()) == null) {
                        confidencePerClassifier = new HashMap<>();
                    } else {
                        confidencePerClassifier = labelMap.get(ao.getLabel());
                    }

                    confidencePerClassifier.put(ao.getRecommenderId(), ao);
                    labelMap.put(ao.getLabel(), confidencePerClassifier);
                }
            }
            
            // Determine the maximum confidence for per Label
            Map<String, Double> maxConfidencePerLabel = new HashMap<>();
            for (String label : labelMap.keySet()) {
                double maxConfidence = 0;
                for (Entry<Long, AnnotationSuggestion> classifier : labelMap.get(label)
                        .entrySet()) {
                    if (classifier.getValue().getConfidence() > maxConfidence) {
                        maxConfidence = classifier.getValue().getConfidence();
                    }
                }
                maxConfidencePerLabel.put(label, maxConfidence);
            }
            
            // Sort and filter labels under threshold value
            List<String> filtered = maxConfidencePerLabel.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(pref.getMaxPredictions())
                    .map(Entry::getKey).collect(Collectors.toList());

            // Render annotations for each label
            for (String label : labelMap.keySet()) {
                if (!filtered.contains(label)) {
                    continue;
                }

                // Create VID using the recommendation with the lowest recommendationId
                AnnotationSuggestion canonicalRecommendation = suggestion.stream()
                        .filter(p -> p.getLabel().equals(label))
                        .max(Comparator.comparingInt(AnnotationSuggestion::getId)).orElse(null);

                if (canonicalRecommendation == null) {
                    continue;
                }

                VID vid = canonicalRecommendation.getVID();
                
                boolean first = true;
                Map<Long, AnnotationSuggestion> confidencePerClassifier = labelMap.get(label);
                for (Long recommenderId: confidencePerClassifier.keySet()) {
                    AnnotationSuggestion ao = confidencePerClassifier.get(recommenderId);

                    // Only necessary for creating the first
                    if (first) {
                        AnnotationFeature feature = aAnnotationService
                            .getFeature(ao.getFeature(), layer);
                        // Retrieve the UI display label for the given feature value
                        String annotation = aFsRegistry.getFeatureSupport(feature)
                            .renderFeatureValue(feature, ao.getLabel());

                        Map<String, String> featureAnnotation = new HashMap<>();
                        featureAnnotation.put(ao.getFeature(), annotation);

                        VSpan v = new VSpan(layer, vid, bratTypeName,
                                new VRange(ao.getBegin() - windowBegin, ao.getEnd() - windowBegin),
                                featureAnnotation, Collections.emptyMap(), color);
                        vdoc.add(v);
                        first = false;
                    }
                    vdoc.add(new VComment(vid, VCommentType.INFO, ao.getRecommenderName()));
                    if (ao.getConfidence() != -1) {
                        vdoc.add(new VComment(vid, VCommentType.INFO,
                                String.format("Confidence: %.2f", ao.getConfidence())));
                    }
                    if (ao.getUiLabel() != null && !ao.getUiLabel().isEmpty()) {
                        vdoc.add(new VComment(vid, VCommentType.INFO,
                                "Description: " + ao.getUiLabel()));
                    }
                    if (pref.isShowAllPredictions() && !ao.isVisible()) {
                        vdoc.add(new VComment(vid, VCommentType.INFO,
                                "Hidden: " + ao.getReasonForHiding()));
                    }
                }
            }
        }
    }
}
