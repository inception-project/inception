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
package de.tudarmstadt.ukp.inception.recommendation.adapter;

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
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.inception.recommendation.RecommendationEditorExtension;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.scheduling.tasks.PredictionTask;

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
        if (model == null) {
            return;
        }
        
        // TODO #176 use the document Id once it it available in the CAS
        List<List<AnnotationObject>> recommendations = model
            .getPredictions(DocumentMetaData.get(aJcas).getDocumentTitle(), layer,
                windowBegin, windowEnd, aJcas, false);
        String color = aColoringStrategy.getColor(null, null);
        String bratTypeName = TypeUtil.getUiTypeName(typeAdapter);

        AnnotationDocument annoDoc = aDocumentService
            .getAnnotationDocument(aState.getDocument(), aState.getUser());

        recommendations = PredictionTask
            .setVisibility(learningRecordService, aJcas, aState.getUser().getUsername(), annoDoc,
                layer, recommendations, windowBegin, windowEnd);

        for (List<AnnotationObject> token: recommendations) {
            Map<String, Map<Long, AnnotationObject>> labelMap = new HashMap<>();
 
            // For recommendations with the same label by the same classifier,
            // show only the confidence of the highest one
            for (AnnotationObject ao: token) {

                // Skip rendering AnnotationObjects that should not be rendered
                if (!ao.isVisible()) {
                    continue;
                }

                if (!labelMap.containsKey(ao.getLabel())
                        || !labelMap.get(ao.getLabel())
                                .containsKey(ao.getRecommenderId())
                        || labelMap.get(ao.getLabel()).get(ao.getRecommenderId())
                                .getConfidence() < ao.getConfidence()) {

                    Map<Long, AnnotationObject> confidencePerClassifier;
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
                for (Entry<Long, AnnotationObject> classifier : labelMap.get(label).entrySet()) {
                    if (classifier.getValue().getConfidence() > maxConfidence) {
                        maxConfidence = classifier.getValue().getConfidence();
                    }
                }
                maxConfidencePerLabel.put(label, maxConfidence);
            }
            
            // Sort and filter labels under threshold value
            List<String> filtered = maxConfidencePerLabel.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(recommendationService.getMaxSuggestions(aState.getUser()))
                    .map(Entry::getKey).collect(Collectors.toList());

            // Render annotations for each label
            for (String label : labelMap.keySet()) {
                if (!filtered.contains(label)) {
                    continue;
                }

                // Create VID using the recommendation with the lowest recommendationId
                AnnotationObject prediction = token.stream()
                        .filter(p -> p.getLabel().equals(label))
                        .max(Comparator.comparingInt(AnnotationObject::getId)).orElse(null);

                if (prediction == null) {
                    continue;
                }

                VID vid = new VID(RecommendationEditorExtension.BEAN_NAME, layer.getId(),
                        (int) prediction.getRecommenderId(), prediction.getId(), VID.NONE,
                        VID.NONE);
                
                boolean first = true;
                Map<Long, AnnotationObject> confidencePerClassifier = labelMap.get(label);
                for (Long recommenderId: confidencePerClassifier.keySet()) {
                    AnnotationObject ao = confidencePerClassifier.get(recommenderId);

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
                                new VRange(ao.getOffset().getBeginCharacter() - windowBegin,
                                        ao.getOffset().getEndCharacter() - windowBegin),
                                featureAnnotation, Collections.emptyMap(), color);
                        vdoc.add(v);
                        first = false;
                    }
                    vdoc.add(new VComment(vid, VCommentType.INFO, ao.getSource()));
                    if (ao.getConfidence() != -1) {
                        vdoc.add(new VComment(vid, VCommentType.INFO,
                            String.format("Confidence: %.2f", ao.getConfidence())));
                    }
                    if (ao.getUiLabel() != null && !ao.getUiLabel().isEmpty()) {
                        vdoc.add(new VComment(vid, VCommentType.INFO,
                            "Description: " + ao.getUiLabel()));
                    }
                }
            }
        }
    }
}
