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

import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.NONE;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_OVERLAP;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparingInt;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationPredicates;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.config.SpanRecommenderProperties;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionRenderer;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportQuery;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupport_ImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.ExtractionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.util.OverlapIterator;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.text.TrimUtils;

public class SpanSuggestionSupport
    extends SuggestionSupport_ImplBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TYPE = "SPAN";

    private final FeatureSupportRegistry featureSupportRegistry;
    private final SpanRecommenderProperties recommenderProperties;

    public SpanSuggestionSupport(RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher,
            AnnotationSchemaService aSchemaService, FeatureSupportRegistry aFeatureSupportRegistry,
            SpanRecommenderProperties aRecommenderProperties)
    {
        super(aRecommendationService, aLearningRecordService, aApplicationEventPublisher,
                aSchemaService);
        featureSupportRegistry = aFeatureSupportRegistry;
        recommenderProperties = aRecommenderProperties;
    }

    @Override
    public boolean accepts(SuggestionSupportQuery aContext)
    {
        if (!SpanLayerSupport.TYPE.equals(aContext.layer().getType())) {
            return false;
        }

        var feature = aContext.feature();
        if (asList(CAS.TYPE_NAME_STRING, CAS.TYPE_NAME_BOOLEAN).contains(feature.getType())
                // || not all supported yet - ICasUtil.isPrimitive(feature.getType())
                || CAS.TYPE_NAME_STRING_ARRAY.equals(feature.getType())
                || feature.isVirtualFeature()) {
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

        if (aSuggestion instanceof SpanSuggestion suggestion
                && aAdapter instanceof SpanAdapter adapter) {
            return Optional.of(acceptOrCorrectSuggestion(aSessionOwner, aDocument, aDataOwner, aCas,
                    adapter, aFeature, aPredictions, suggestion, aLocation, aAction));
        }

        return Optional.empty();

    }

    public AnnotationFS acceptOrCorrectSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, SpanAdapter aAdapter, AnnotationFeature aFeature,
            Predictions aPredictions, SpanSuggestion aSuggestion,
            LearningRecordChangeLocation aLocation, LearningRecordUserAction aAction)
        throws AnnotationException
    {
        var begin = aSuggestion.getBegin();
        var end = aSuggestion.getEnd();

        var candidates = aCas.<Annotation> select(aAdapter.getAnnotationTypeName()) //
                .at(begin, end) //
                .asList();

        var candidateWithEmptyLabel = candidates.stream() //
                .filter(c -> aAdapter.getFeatureValue(aFeature, c) == null) //
                .findFirst();

        try (var eventBatch = aAdapter.batchEvents()) {
            var annotationCreated = false;
            AnnotationFS annotation;
            if (candidateWithEmptyLabel.isPresent()) {
                // If there is an annotation where the predicted feature is unset, use it ...
                annotation = candidateWithEmptyLabel.get();
            }
            else if (candidates.isEmpty() || (aAdapter.getLayer().isAllowStacking()
                    && aFeature.getMultiValueMode() == NONE)) {
                // ... if not or if stacking is allowed, then we create a new annotation - this also
                // takes care of attaching to an annotation if necessary
                annotation = aAdapter.handle(CreateSpanAnnotationRequest.builder() //
                        .withDocument(aDocument, aDataOwner, aCas) //
                        .withRange(begin, end) //
                        .build());
                annotationCreated = true;
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
        LOG.trace(
                "calculateSpanSuggestionVisibility() for layer {} on document {} in range [{}, {}]",
                aLayer, aDocument, aWindowBegin, aWindowEnd);

        var type = getAnnotationType(aCas, aLayer);
        if (type == null) {
            // The type does not exist in the type system of the CAS. Probably it has not
            // been upgraded to the latest version of the type system yet. If this is the case,
            // we'll just skip.
            return;
        }

        var annotationsInWindow = getAnnotationsInWindow(aCas, type, aWindowBegin, aWindowEnd);

        // Collect all suggestions of the given layer within the view window
        var suggestionsInWindow = aRecommendations.stream()
                // Only suggestions for the given layer
                .filter(group -> group.getLayerId() == aLayer.getId())
                // ... and in the given window
                .filter(group -> {
                    var offset = (Offset) group.getPosition();
                    return AnnotationPredicates.coveredBy(offset.getBegin(), offset.getEnd(),
                            aWindowBegin, aWindowEnd);
                }) //
                .toList();

        // Get all the skipped/rejected entries for the current layer
        var recordedAnnotations = learningRecordService.listLearningRecords(aSessionOwner,
                aDataOwner, aLayer);

        for (var feature : schemaService.listSupportedFeatures(aLayer)) {
            var feat = type.getFeatureByBaseName(feature.getName());

            if (feat == null) {
                // The feature does not exist in the type system of the CAS. Probably it has not
                // been upgraded to the latest version of the type system yet. If this is the case,
                // we'll just skip.
                return;
            }

            // Reduce the suggestions to the ones for the given feature. We can use the tree here
            // since we only have a single SuggestionGroup for every position
            var suggestions = new TreeMap<Offset, SuggestionGroup<SpanSuggestion>>(
                    comparingInt(Offset::getBegin).thenComparingInt(Offset::getEnd));
            suggestionsInWindow.stream()
                    .filter(group -> group.getFeature().equals(feature.getName())) //
                    .map(group -> {
                        group.showAll(AnnotationSuggestion.FLAG_ALL);
                        return group;
                    }) //
                    .forEach(group -> suggestions.put((Offset) group.getPosition(),
                            (SuggestionGroup) group));

            hideSpanSuggestionsThatOverlapWithAnnotations(annotationsInWindow, feature, feat,
                    suggestions);

            // Anything that was not hidden so far might still have been rejected
            suggestions.values().stream() //
                    .flatMap(SuggestionGroup::stream) //
                    .filter(AnnotationSuggestion::isVisible) //
                    .forEach(suggestion -> hideSuggestionsRejectedOrSkipped(suggestion,
                            recordedAnnotations));
        }
    }

    private void hideSpanSuggestionsThatOverlapWithAnnotations(
            List<AnnotationFS> annotationsInWindow, AnnotationFeature feature, Feature feat,
            Map<Offset, SuggestionGroup<SpanSuggestion>> suggestions)
    {
        // If there are no suggestions or annotations, there is nothing to do here
        if (annotationsInWindow.isEmpty() || suggestions.isEmpty()) {
            return;
        }

        // Reduce the annotations to the ones which have a non-null feature value. We need to
        // use a multi-valued map here because there may be multiple annotations at a
        // given position.
        var annotations = new ArrayListValuedHashMap<Offset, AnnotationFS>();
        annotationsInWindow
                .forEach(fs -> annotations.put(new Offset(fs.getBegin(), fs.getEnd()), fs));

        // We need to constructed a sorted list of the keys for the OverlapIterator below
        var sortedAnnotationKeys = new ArrayList<Offset>(annotations.keySet());
        sortedAnnotationKeys.sort(comparingInt(Offset::getBegin).thenComparingInt(Offset::getEnd));

        // This iterator gives us pairs of annotations and suggestions. Note that both lists
        // must be sorted in the same way. The suggestion offsets are sorted because they are
        // the keys in a TreeSet - and the annotation offsets are sorted in the same way manually
        var oi = new OverlapIterator(new ArrayList<>(suggestions.keySet()), sortedAnnotationKeys);

        var stackableSuggestions = feature.getLayer().isAllowStacking()
                || feature.getMultiValueMode() != MultiValueMode.NONE;

        // Bulk-hide any groups that overlap with existing annotations on the current layer
        // and for the current feature
        var hiddenForOverlap = new ArrayList<AnnotationSuggestion>();
        while (oi.hasNext()) {
            var pair = oi.next();
            var suggestionOffset = pair.getKey();
            var annotationOffset = pair.getValue();

            // Fetch the current suggestion and annotation
            var group = suggestions.get(suggestionOffset);
            for (var annotation : annotations.get(annotationOffset)) {
                var featureSupport = featureSupportRegistry.findExtension(feature).get();
                var wrappedValue = featureSupport.getFeatureValue(feature, annotation);
                var value = featureSupport.unwrapFeatureValue(feature, wrappedValue);

                var labelObjects = value instanceof Iterable values ? values : asList(value);

                for (var labelObject : labelObjects) {
                    var label = Objects.toString(labelObject, null);

                    for (var suggestion : group) {
                        // The suggestion would just create an annotation and not set any
                        // feature
                        var colocated = colocated(annotation, suggestion.getBegin(),
                                suggestion.getEnd());
                        if (suggestion.getLabel() == null) {
                            // If there is already an annotation, then we hide any suggestions
                            // that would just trigger the creation of the same annotation and
                            // not set any new feature. This applies whether stacking is allowed
                            // or not.
                            if (colocated) {
                                suggestion.hide(FLAG_OVERLAP);
                                hiddenForOverlap.add(suggestion);
                                continue;
                            }

                            // If stacking is enabled, we do allow suggestions that create an
                            // annotation with no label, but only if the offsets differ
                            if (!(stackableSuggestions && !colocated)) {
                                suggestion.hide(FLAG_OVERLAP);
                                hiddenForOverlap.add(suggestion);
                                continue;
                            }
                        }
                        // The suggestion would merge the suggested feature value into an
                        // existing annotation or create a new annotation with the feature if
                        // stacking were enabled.
                        else {
                            // Is the feature still unset in the current annotation - i.e. would
                            // accepting the suggestion merge the feature into it? If yes, we do
                            // not hide
                            if (label == null && suggestion.getLabel() != null && colocated) {
                                continue;
                            }

                            // Does the suggested label match the label of an existing annotation
                            // at the same position then we hide
                            if (label != null && label.equals(suggestion.getLabel()) && colocated) {
                                suggestion.hide(FLAG_OVERLAP);
                                hiddenForOverlap.add(suggestion);
                                continue;
                            }

                            // Would accepting the suggestion create a new annotation but
                            // stacking is not enabled - then we need to hide
                            if (!stackableSuggestions && !suggestion.isCorrection()) {
                                suggestion.hide(FLAG_OVERLAP);
                                hiddenForOverlap.add(suggestion);
                                continue;
                            }
                        }
                    }
                }
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Hidden due to overlapping: {}", hiddenForOverlap.size());
            for (var s : hiddenForOverlap) {
                LOG.trace("- {}", s);
            }
        }
    }

    private List<AnnotationFS> getAnnotationsInWindow(CAS aCas, Type type, int aWindowBegin,
            int aWindowEnd)
    {
        if (type == null) {
            return Collections.emptyList();
        }

        return select(aCas, type).stream() //
                .filter(fs -> fs.coveredBy(aWindowBegin, aWindowEnd)) //
                .toList();
    }

    @Nullable
    private Type getAnnotationType(CAS aCas, AnnotationLayer aLayer)
    {
        // NOTE: In order to avoid having to upgrade the "original CAS" in computePredictions,this
        // method is implemented in such a way that it gracefully handles cases where the CAS and
        // the project type system are not in sync - specifically the CAS where the project defines
        // layers or features which do not exist in the CAS.

        try {
            return CasUtil.getAnnotationType(aCas, aLayer.getName());
        }
        catch (IllegalArgumentException e) {
            // Type does not exist in the type system of the CAS. Probably it has not been upgraded
            // to the latest version of the type system yet. If this is the case, we'll just skip.
            return null;
        }
    }

    static void hideSuggestionsRejectedOrSkipped(SpanSuggestion aSuggestion,
            List<LearningRecord> aRecordedRecommendations)
    {
        aRecordedRecommendations.stream() //
                .filter(r -> Objects.equals(r.getLayer().getId(), aSuggestion.getLayerId())) //
                .filter(r -> Objects.equals(r.getAnnotationFeature().getName(),
                        aSuggestion.getFeature())) //
                .filter(r -> Objects.equals(r.getSourceDocument().getId(),
                        aSuggestion.getDocumentId())) //
                .filter(r -> aSuggestion.labelEquals(r.getAnnotation())) //
                .filter(r -> r.getOffsetBegin() == aSuggestion.getBegin()
                        && r.getOffsetEnd() == aSuggestion.getEnd()) //
                .filter(r -> aSuggestion.hideSuggestion(r.getUserAction())) //
                .findAny();
    }

    @Override
    public LearningRecord toLearningRecord(SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion aSuggestion, AnnotationFeature aFeature,
            LearningRecordUserAction aUserAction, LearningRecordChangeLocation aLocation)
    {
        var pos = ((SpanSuggestion) aSuggestion).getPosition();
        var record = new LearningRecord();
        record.setUser(aDataOwner);
        record.setSourceDocument(aDocument);
        record.setUserAction(aUserAction);
        record.setOffsetBegin(pos.getBegin());
        record.setOffsetEnd(pos.getEnd());
        record.setOffsetBegin2(-1);
        record.setOffsetEnd2(-1);
        record.setTokenText(((SpanSuggestion) aSuggestion).getCoveredText());
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
        return Optional.of(new SpanSuggestionRenderer(recommendationService, schemaService,
                featureSupportRegistry, recommenderProperties));
    }

    @Override
    public List<AnnotationSuggestion> extractSuggestions(ExtractionContext ctx)
    {
        var suggestions = new ArrayList<AnnotationSuggestion>();
        for (var predictedFS : ctx.getPredictionCas().<Annotation> select(ctx.getPredictedType())) {
            extractSuggestions(ctx, suggestions, predictedFS);
        }
        return suggestions;
    }

    private void extractSuggestions(ExtractionContext aCtx, List<AnnotationSuggestion> aSuggestions,
            Annotation aPredictedFS)
    {
        if (!aPredictedFS.getBooleanValue(aCtx.getPredictionFeature())) {
            return;
        }

        var anchoringMode = aCtx.getLayer().getAnchoringMode();
        var targetOffsets = getOffsets(anchoringMode, aCtx.getOriginalCas(), aPredictedFS);
        if (targetOffsets.isEmpty()) {
            LOG.debug("Prediction cannot be anchored to [{}]: {}", anchoringMode, aPredictedFS);
            return;
        }

        var labels = getPredictedLabels(aPredictedFS, aCtx.getLabelFeature(), aCtx.isMultiLabels());
        if (isEmpty(labels)) {
            return;
        }

        var offsets = targetOffsets.get();
        var coveredText = aCtx.getDocumentText().substring(offsets.getBegin(), offsets.getEnd());

        if (!coveredText.equals(aPredictedFS.getCoveredText())) {
            LOG.trace("Offsets were adjusted [{}] -> [{}]", aPredictedFS.getCoveredText(),
                    coveredText);
        }

        var autoAcceptMode = getAutoAcceptMode(aPredictedFS, aCtx.getModeFeature());
        var correction = aPredictedFS.getBooleanValue(aCtx.getCorrectionFeature());
        var correctionExplanation = aPredictedFS
                .getStringValue(aCtx.getCorrectionExplanationFeature());
        var score = aPredictedFS.getDoubleValue(aCtx.getScoreFeature());
        var scoreExplanation = aPredictedFS.getStringValue(aCtx.getScoreExplanationFeature());

        for (var label : labels) {
            aSuggestions.add(SpanSuggestion.builder() //
                    .withId(SpanSuggestion.NEW_ID) //
                    .withGeneration(aCtx.getGeneration()) //
                    .withRecommender(aCtx.getRecommender()) //
                    .withDocument(aCtx.getDocument()) //
                    .withPosition(offsets) //
                    .withCoveredText(coveredText) //
                    .withLabel(label) //
                    .withUiLabel(label) //
                    .withCorrection(correction) //
                    .withCorrectionExplanation(correctionExplanation) //
                    .withScore(score) //
                    .withScoreExplanation(scoreExplanation) //
                    .withAutoAcceptMode(autoAcceptMode) //
                    .build());
        }
    }

    /**
     * Calculates the offsets of the given predicted annotation in the original CAS .
     *
     * @param aMode
     *            the anchoring mode of the target layer
     * @param aOriginalCas
     *            the original CAS.
     * @param aPredictedAnnotation
     *            the predicted annotation.
     * @return the proper offsets.
     */
    static Optional<Offset> getOffsets(AnchoringMode aMode, CAS aOriginalCas,
            Annotation aPredictedAnnotation)
    {
        switch (aMode) {
        case CHARACTERS: {
            return getOffsetsAnchoredOnCharacters(aOriginalCas, aPredictedAnnotation);
        }
        case SINGLE_TOKEN: {
            return getOffsetsAnchoredOnSingleTokens(aOriginalCas, aPredictedAnnotation);
        }
        case TOKENS: {
            return getOffsetsAnchoredOnTokens(aOriginalCas, aPredictedAnnotation);
        }
        case SENTENCES: {
            return getOffsetsAnchoredOnSentences(aOriginalCas, aPredictedAnnotation);
        }
        default:
            throw new IllegalStateException("Unsupported anchoring mode: [" + aMode + "]");
        }
    }

    private static Optional<Offset> getOffsetsAnchoredOnCharacters(CAS aOriginalCas,
            Annotation aPredictedAnnotation)
    {
        int[] offsets = { max(aPredictedAnnotation.getBegin(), 0),
                min(aOriginalCas.getDocumentText().length(), aPredictedAnnotation.getEnd()) };
        TrimUtils.trim(aPredictedAnnotation.getCAS().getDocumentText(), offsets);
        var begin = offsets[0];
        var end = offsets[1];
        return Optional.of(new Offset(begin, end));
    }

    private static Optional<Offset> getOffsetsAnchoredOnSentences(CAS aOriginalCas,
            Annotation aPredictedAnnotation)
    {
        var sentences = aOriginalCas.select(Sentence.class) //
                .coveredBy(aPredictedAnnotation) //
                .asList();

        if (sentences.isEmpty()) {
            // This can happen if a recommender uses different token boundaries (e.g. if a
            // remote service performs its own tokenization). We might be smart here by
            // looking for overlapping sentences instead of covered sentences.
            LOG.trace("Discarding suggestion because no covered sentences were found: {}",
                    aPredictedAnnotation);
            return Optional.empty();
        }

        var begin = sentences.get(0).getBegin();
        var end = sentences.get(sentences.size() - 1).getEnd();
        return Optional.of(new Offset(begin, end));
    }

    private static Optional<Offset> getOffsetsAnchoredOnSingleTokens(CAS aOriginalCas,
            Annotation aPredictedAnnotation)
    {
        Type tokenType = getType(aOriginalCas, Token.class);
        var tokens = aOriginalCas.<Annotation> select(tokenType) //
                .coveredBy(aPredictedAnnotation) //
                .limit(2).asList();

        if (tokens.isEmpty()) {
            // This can happen if a recommender uses different token boundaries (e.g. if a
            // remote service performs its own tokenization). We might be smart here by
            // looking for overlapping tokens instead of contained tokens.
            LOG.trace("Discarding suggestion because no covering token was found: {}",
                    aPredictedAnnotation);
            return Optional.empty();
        }

        if (tokens.size() > 1) {
            // We only want to accept single-token suggestions
            LOG.trace("Discarding suggestion because only single-token suggestions are "
                    + "accepted: {}", aPredictedAnnotation);
            return Optional.empty();
        }

        AnnotationFS token = tokens.get(0);
        var begin = token.getBegin();
        var end = token.getEnd();
        return Optional.of(new Offset(begin, end));
    }

    public static Optional<Offset> getOffsetsAnchoredOnTokens(CAS aOriginalCas,
            Annotation aPredictedAnnotation)
    {
        var tokens = aOriginalCas.select(Token.class) //
                .coveredBy(aPredictedAnnotation) //
                .asList();

        if (tokens.isEmpty()) {
            if (aPredictedAnnotation.getBegin() == aPredictedAnnotation.getEnd()) {
                var pos = aPredictedAnnotation.getBegin();
                var allTokens = aOriginalCas.select(Token.class).asList();
                Token prevToken = null;
                for (var token : allTokens) {
                    if (prevToken == null && pos < token.getBegin()) {
                        return Optional.of(new Offset(token.getBegin(), token.getBegin()));
                    }

                    if (token.covering(aPredictedAnnotation)) {
                        return Optional.of(new Offset(pos, pos));
                    }

                    if (prevToken != null && pos < token.getBegin()) {
                        return Optional.of(new Offset(prevToken.getEnd(), prevToken.getEnd()));
                    }

                    prevToken = token;
                }

                if (prevToken != null && pos >= prevToken.getEnd()) {
                    return Optional.of(new Offset(prevToken.getEnd(), prevToken.getEnd()));
                }
            }

            // This can happen if a recommender uses different token boundaries (e.g. if a
            // remote service performs its own tokenization). We might be smart here by
            // looking for overlapping tokens instead of covered tokens.
            LOG.trace("Discarding suggestion because no covered tokens were found: {}",
                    aPredictedAnnotation);
            return Optional.empty();
        }

        var begin = tokens.get(0).getBegin();
        var end = tokens.get(tokens.size() - 1).getEnd();
        return Optional.of(new Offset(begin, end));
    }
}
