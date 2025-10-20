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
package de.tudarmstadt.ukp.inception.annotation.layer.document.recommender;

import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_ALL;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_OVERLAP;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerTraits;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionRenderer;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportQuery;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupport_ImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.MetadataSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.ExtractionContext;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;

public class MetadataSuggestionSupport
    extends SuggestionSupport_ImplBase

{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TYPE = "META";

    public MetadataSuggestionSupport(RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher,
            AnnotationSchemaService aSchemaService)
    {
        super(aRecommendationService, aLearningRecordService, aApplicationEventPublisher,
                aSchemaService);
    }

    @Override
    public boolean accepts(SuggestionSupportQuery aContext)
    {
        if (!DocumentMetadataLayerSupport.TYPE.equals(aContext.layer().getType())) {
            return false;
        }

        var feature = aContext.feature();
        if (TYPE_NAME_STRING.equals(feature.getType()) || feature.isVirtualFeature()) {
            return true;
        }

        return false;
    }

    @Override
    public Optional<AnnotationBaseFS> acceptSuggestion(String aSessionOwner,
            SourceDocument aDocument, String aDataOwner, CAS aCas, TypeAdapter aAdapter,
            AnnotationFeature aFeature, Predictions aPredictions, AnnotationSuggestion aSuggestion,
            LearningRecordChangeLocation aLocation, LearningRecordUserAction aAction)
        throws AnnotationException
    {
        if (aSuggestion instanceof MetadataSuggestion suggestion
                && aAdapter instanceof DocumentMetadataLayerAdapter adapter) {
            return Optional.of(acceptSuggestion(aSessionOwner, aDocument, aDataOwner, aCas,
                    aFeature, aPredictions, suggestion, aLocation, aAction, adapter));
        }

        return Optional.empty();
    }

    private AnnotationBaseFS acceptSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, AnnotationFeature aFeature, Predictions aPredictions,
            MetadataSuggestion aSuggestion, LearningRecordChangeLocation aLocation,
            LearningRecordUserAction aAction, DocumentMetadataLayerAdapter aAdapter)
        throws AnnotationException
    {
        var candidates = aCas.<AnnotationBase> select(aAdapter.getAnnotationTypeName()) //
                .asList();

        var candidateWithEmptyLabel = candidates.stream() //
                .filter(c -> aAdapter.getFeatureValue(aFeature, c) == null) //
                .findFirst();

        try (var eventBatch = aAdapter.batchEvents()) {
            var annotationCreated = false;
            AnnotationBaseFS annotation;
            if (candidateWithEmptyLabel.isPresent()) {
                // If there is an annotation where the predicted feature is unset, use it ...
                annotation = candidateWithEmptyLabel.get();
            }
            else if (candidates.isEmpty() || !aAdapter.getTraits(DocumentMetadataLayerTraits.class)
                    .map(DocumentMetadataLayerTraits::isSingleton).orElse(false)) {
                // ... if not or if stacking is allowed, then we create a new annotation - this also
                // takes care of attaching to an annotation if necessary
                var newAnnotation = aAdapter.add(aDocument, aDataOwner, aCas);
                annotation = newAnnotation;
            }
            else {
                // ... if yes and stacking is not allowed, then we update the feature on the
                // existing annotation
                annotation = candidates.get(0);
            }

            try {
                commitLabel(aDocument, aDataOwner, aAdapter, annotation, aFeature, aPredictions,
                        aSuggestion);
            }
            catch (Exception e) {
                if (annotationCreated) {
                    aAdapter.delete(aDocument, aDataOwner, aCas, VID.of(annotation));
                }
                throw e;
            }

            hideSuggestion(aSuggestion, aAction);
            recordAndPublishAcceptance(aSessionOwner, aDocument, aDataOwner, aAdapter, aFeature,
                    aSuggestion, annotation, aLocation, aAction);

            eventBatch.commit();
            return annotation;
        }
    }

    @Override
    public <T extends AnnotationSuggestion> void calculateSuggestionVisibility(String aSessionOwner,
            SourceDocument aDocument, CAS aCas, String aDataOwner, AnnotationLayer aLayer,
            Collection<SuggestionGroup<T>> aRecommendations, int aWindowBegin, int aWindowEnd)
    {
        LOG.trace("calculateSuggestionVisibility() for layer {} on document {}", aLayer, aDocument);

        var predictedType = aCas.getTypeSystem().getType(aLayer.getName());
        if (predictedType == null) {
            // The type does not exist in the type system of the CAS. Probably it has not
            // been upgraded to the latest version of the type system yet. If this is the case,
            // we'll just skip.
            return;
        }

        var annotations = aCas.<AnnotationBase> select(predictedType).asList();

        var suggestionsForLayer = aRecommendations.stream()
                // Only suggestions for the given layer
                .filter(group -> group.getLayerId() == aLayer.getId()) //
                .toList();

        // Get all the skipped/rejected entries for the current layer
        var recordedAnnotations = learningRecordService.listLearningRecords(aSessionOwner,
                aDataOwner, aLayer);

        var adapter = schemaService.getAdapter(aLayer);
        var traits = adapter.getTraits(DocumentMetadataLayerTraits.class).get();
        for (var feature : schemaService.listSupportedFeatures(aLayer)) {
            var feat = predictedType.getFeatureByBaseName(feature.getName());

            if (feat == null) {
                // The feature does not exist in the type system of the CAS. Probably it has not
                // been upgraded to the latest version of the type system yet. If this is the case,
                // we'll just skip.
                return;
            }

            // Reduce the suggestions to the ones for the given feature. We can use the tree here
            // since we only have a single SuggestionGroup for every position
            var suggestionsForFeature = suggestionsForLayer.stream()
                    .filter(group -> group.getFeature().equals(feature.getName())) //
                    .map(group -> {
                        group.showAll(FLAG_ALL);
                        return (SuggestionGroup<MetadataSuggestion>) group;
                    }) //
                    .toList();

            hideSpanSuggestionsThatMatchAnnotations(traits.isSingleton(), annotations, feature,
                    feat, suggestionsForFeature);

            // Anything that was not hidden so far might still have been rejected
            suggestionsForFeature.stream() //
                    .flatMap(SuggestionGroup::stream) //
                    .filter(AnnotationSuggestion::isVisible) //
                    .forEach(suggestion -> hideSuggestionsRejectedOrSkipped(suggestion,
                            recordedAnnotations));
        }
    }

    private void hideSpanSuggestionsThatMatchAnnotations(boolean singleton,
            List<AnnotationBase> aAnnotations, AnnotationFeature aFeature, Feature aFeat,
            List<SuggestionGroup<MetadataSuggestion>> aSuggestionsForFeature)
    {

        for (var annotation : aAnnotations) {
            // FIXME: This will not work for multi-valued features
            var label = annotation.getFeatureValueAsString(aFeat);

            if (label == null) {
                continue;
            }

            for (var sugGroup : aSuggestionsForFeature) {
                if (singleton) {
                    sugGroup.hideAll(FLAG_OVERLAP);
                }
                else {
                    for (var suggestion : sugGroup) {
                        if (label.equals(suggestion.getLabel())) {
                            suggestion.hide(FLAG_OVERLAP);
                        }
                    }
                }
            }
        }
    }

    static void hideSuggestionsRejectedOrSkipped(MetadataSuggestion aSuggestion,
            List<LearningRecord> aRecordedRecommendations)
    {
        aRecordedRecommendations.stream() //
                .filter(r -> Objects.equals(r.getLayer().getId(), aSuggestion.getLayerId())) //
                .filter(r -> Objects.equals(r.getAnnotationFeature().getName(),
                        aSuggestion.getFeature())) //
                .filter(r -> Objects.equals(r.getSourceDocument().getId(),
                        aSuggestion.getDocumentId())) //
                .filter(r -> aSuggestion.labelEquals(r.getAnnotation())) //
                .filter(r -> aSuggestion.hideSuggestion(r.getUserAction())) //
                .findAny();
    }

    @Override
    public LearningRecord toLearningRecord(SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, AnnotationFeature aFeature,
            LearningRecordUserAction aUserAction, LearningRecordChangeLocation aLocation)
    {
        var record = new LearningRecord();
        record.setUser(aDataOwner);
        record.setSourceDocument(aDocument);
        record.setUserAction(aUserAction);
        record.setOffsetBegin(-1);
        record.setOffsetEnd(-1);
        record.setOffsetBegin2(-1);
        record.setOffsetEnd2(-1);
        record.setAnnotation(aSuggestion.getLabel());
        record.setLayer(aFeature.getLayer());
        record.setSuggestionType(TYPE);
        record.setChangeLocation(aLocation);
        record.setAnnotationFeature(aFeature);
        return record;
    }

    @Override
    public Optional<SuggestionRenderer> getRenderer()
    {
        return Optional.empty();
    }

    @Override
    public List<AnnotationSuggestion> extractSuggestions(ExtractionContext ctx)
    {
        var result = new ArrayList<AnnotationSuggestion>();
        for (var predictedFS : ctx.getPredictionCas().select(ctx.getPredictedType())) {
            if (!predictedFS.getBooleanValue(ctx.getPredictionFeature())) {
                continue;
            }

            var autoAcceptMode = getAutoAcceptMode(predictedFS, ctx.getModeFeature());
            var labels = getPredictedLabels(predictedFS, ctx.getLabelFeature(),
                    ctx.isMultiLabels());
            var score = predictedFS.getDoubleValue(ctx.getScoreFeature());
            var scoreExplanation = predictedFS.getStringValue(ctx.getScoreExplanationFeature());
            var correction = predictedFS.getBooleanValue(ctx.getCorrectionFeature());
            var correctionExplanation = predictedFS
                    .getStringValue(ctx.getCorrectionExplanationFeature());

            for (var label : labels) {
                var suggestion = MetadataSuggestion.builder() //
                        .withId(MetadataSuggestion.NEW_ID) //
                        .withGeneration(ctx.getGeneration()) //
                        .withRecommender(ctx.getRecommender()) //
                        .withDocument(ctx.getDocument()) //
                        .withLabel(label) //
                        .withUiLabel(label) //
                        .withCorrection(correction) //
                        .withCorrectionExplanation(correctionExplanation) //
                        .withScore(score) //
                        .withScoreExplanation(scoreExplanation) //
                        .withAutoAcceptMode(autoAcceptMode) //
                        .build();
                result.add(suggestion);
            }
        }
        return result;
    }
}
