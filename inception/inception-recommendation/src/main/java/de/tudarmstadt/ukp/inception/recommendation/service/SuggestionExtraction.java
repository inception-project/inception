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
package de.tudarmstadt.ukp.inception.recommendation.service;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_SUFFIX;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.TrimUtils;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AutoAcceptMode;
import de.tudarmstadt.ukp.inception.recommendation.api.model.MetadataSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationPosition;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationComparisonUtils;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;

public class SuggestionExtraction
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String AUTO_ACCEPT_ON_FIRST_ACCESS = "on-first-access";

    static final class ExtractionContext
    {
        private final int generation;

        private final SourceDocument document;
        private final CAS originalCas;
        private final CAS predictionCas;
        private final String documentText;

        private final Recommender recommender;

        private final AnnotationLayer layer;
        private final String typeName;
        private final String featureName;

        private final Type predictedType;

        private final Feature labelFeature;
        private final Feature sourceFeature;
        private final Feature targetFeature;
        private final Feature scoreFeature;
        private final Feature scoreExplanationFeature;
        private final Feature modeFeature;
        private final Feature predictionFeature;

        private final boolean isMultiLabels;

        private final List<AnnotationSuggestion> result;

        ExtractionContext(int aGeneration, Recommender aRecommender, SourceDocument aDocument,
                CAS aOriginalCas, CAS aPredictionCas)
        {
            recommender = aRecommender;

            document = aDocument;
            originalCas = aOriginalCas;
            documentText = originalCas.getDocumentText();
            predictionCas = aPredictionCas;

            generation = aGeneration;
            layer = aRecommender.getLayer();
            featureName = aRecommender.getFeature().getName();
            typeName = layer.getName();

            predictedType = CasUtil.getType(aPredictionCas, typeName);
            labelFeature = predictedType.getFeatureByBaseName(featureName);
            sourceFeature = predictedType.getFeatureByBaseName(FEAT_REL_SOURCE);
            targetFeature = predictedType.getFeatureByBaseName(FEAT_REL_TARGET);
            scoreFeature = predictedType
                    .getFeatureByBaseName(featureName + FEATURE_NAME_SCORE_SUFFIX);
            scoreExplanationFeature = predictedType
                    .getFeatureByBaseName(featureName + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX);
            modeFeature = predictedType
                    .getFeatureByBaseName(featureName + FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX);
            predictionFeature = predictedType.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);
            isMultiLabels = TYPE_NAME_STRING_ARRAY.equals(labelFeature.getRange().getName());

            result = new ArrayList<AnnotationSuggestion>();
        }
    }

    private static AutoAcceptMode getAutoAcceptMode(FeatureStructure aFS, Feature aModeFeature)
    {
        var autoAcceptMode = AutoAcceptMode.NEVER;
        var autoAcceptFeatureValue = aFS.getStringValue(aModeFeature);
        if (autoAcceptFeatureValue != null) {
            switch (autoAcceptFeatureValue) {
            case AUTO_ACCEPT_ON_FIRST_ACCESS:
                autoAcceptMode = AutoAcceptMode.ON_FIRST_ACCESS;
            }
        }
        return autoAcceptMode;
    }

    private static String[] getPredictedLabels(FeatureStructure predictedFS,
            Feature predictedFeature, boolean isStringMultiValue)
    {
        if (isStringMultiValue) {
            return FSUtil.getFeature(predictedFS, predictedFeature, String[].class);
        }

        return new String[] { predictedFS.getFeatureValueAsString(predictedFeature) };
    }

    static void extractDocumentMetadataSuggestion(ExtractionContext ctx, TOP predictedFS)
    {
        var autoAcceptMode = getAutoAcceptMode(predictedFS, ctx.modeFeature);
        var labels = getPredictedLabels(predictedFS, ctx.labelFeature, ctx.isMultiLabels);
        var score = predictedFS.getDoubleValue(ctx.scoreFeature);
        var scoreExplanation = predictedFS.getStringValue(ctx.scoreExplanationFeature);

        for (var label : labels) {
            var suggestion = MetadataSuggestion.builder() //
                    .withId(MetadataSuggestion.NEW_ID) //
                    .withGeneration(ctx.generation) //
                    .withRecommender(ctx.recommender) //
                    .withDocumentName(ctx.document.getName()) //
                    .withLabel(label) //
                    .withUiLabel(label) //
                    .withScore(score) //
                    .withScoreExplanation(scoreExplanation) //
                    .withAutoAcceptMode(autoAcceptMode) //
                    .build();
            ctx.result.add(suggestion);
        }
    }

    static void extractRelationSuggestion(ExtractionContext ctx, TOP predictedFS)
    {
        var autoAcceptMode = getAutoAcceptMode(predictedFS, ctx.modeFeature);
        var labels = getPredictedLabels(predictedFS, ctx.labelFeature, ctx.isMultiLabels);
        var score = predictedFS.getDoubleValue(ctx.scoreFeature);
        var scoreExplanation = predictedFS.getStringValue(ctx.scoreExplanationFeature);

        var source = (AnnotationFS) predictedFS.getFeatureValue(ctx.sourceFeature);
        var target = (AnnotationFS) predictedFS.getFeatureValue(ctx.targetFeature);

        var originalSource = findEquivalentSpan(ctx.originalCas, source);
        var originalTarget = findEquivalentSpan(ctx.originalCas, target);
        if (originalSource.isEmpty() || originalTarget.isEmpty()) {
            LOG.debug("Unable to find source or target of predicted relation in original CAS");
            return;
        }

        var position = new RelationPosition(originalSource.get(), originalTarget.get());

        for (var label : labels) {
            var suggestion = RelationSuggestion.builder() //
                    .withId(RelationSuggestion.NEW_ID) //
                    .withGeneration(ctx.generation) //
                    .withRecommender(ctx.recommender) //
                    .withDocumentName(ctx.document.getName()) //
                    .withPosition(position) //
                    .withLabel(label) //
                    .withUiLabel(label) //
                    .withScore(score) //
                    .withScoreExplanation(scoreExplanation) //
                    .withAutoAcceptMode(autoAcceptMode) //
                    .build();
            ctx.result.add(suggestion);
        }
    }

    static void extractSpanSuggestion(ExtractionContext ctx, TOP predictedFS)
    {
        var autoAcceptMode = getAutoAcceptMode(predictedFS, ctx.modeFeature);
        var labels = getPredictedLabels(predictedFS, ctx.labelFeature, ctx.isMultiLabels);
        var score = predictedFS.getDoubleValue(ctx.scoreFeature);
        var scoreExplanation = predictedFS.getStringValue(ctx.scoreExplanationFeature);

        var predictedAnnotation = (Annotation) predictedFS;
        var targetOffsets = getOffsets(ctx.layer.getAnchoringMode(), ctx.originalCas,
                predictedAnnotation);

        if (targetOffsets.isEmpty()) {
            LOG.debug("Prediction cannot be anchored to [{}]: {}", ctx.layer.getAnchoringMode(),
                    predictedAnnotation);
            return;
        }

        var offsets = targetOffsets.get();
        var coveredText = ctx.documentText.substring(offsets.getBegin(), offsets.getEnd());

        for (var label : labels) {
            var suggestion = SpanSuggestion.builder() //
                    .withId(RelationSuggestion.NEW_ID) //
                    .withGeneration(ctx.generation) //
                    .withRecommender(ctx.recommender) //
                    .withDocumentName(ctx.document.getName()) //
                    .withPosition(offsets) //
                    .withCoveredText(coveredText) //
                    .withLabel(label) //
                    .withUiLabel(label) //
                    .withScore(score) //
                    .withScoreExplanation(scoreExplanation) //
                    .withAutoAcceptMode(autoAcceptMode) //
                    .build();
            ctx.result.add(suggestion);
        }
    }

    public static List<AnnotationSuggestion> extractSuggestions(int aGeneration, CAS aOriginalCas,
            CAS aPredictionCas, SourceDocument aDocument, Recommender aRecommender)
    {
        var ctx = new ExtractionContext(aGeneration, aRecommender, aDocument, aOriginalCas,
                aPredictionCas);

        for (var predictedFS : aPredictionCas.select(ctx.predictedType)) {
            if (!predictedFS.getBooleanValue(ctx.predictionFeature)) {
                continue;
            }

            switch (ctx.layer.getType()) {
            case SpanLayerSupport.TYPE: {
                extractSpanSuggestion(ctx, predictedFS);
                break;
            }
            case RelationLayerSupport.TYPE: {
                extractRelationSuggestion(ctx, predictedFS);
                break;
            }
            case DocumentMetadataLayerSupport.TYPE: {
                extractDocumentMetadataSuggestion(ctx, predictedFS);
                break;
            }
            default:
                throw new IllegalStateException(
                        "Unsupported layer type [" + ctx.layer.getType() + "]");
            }
        }

        return ctx.result;
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

    /**
     * Locates an annotation in the given CAS which is equivalent of the provided annotation.
     *
     * @param aOriginalCas
     *            the original CAS.
     * @param aAnnotation
     *            an annotation in the prediction CAS. return the equivalent in the original CAS.
     */
    private static Optional<Annotation> findEquivalentSpan(CAS aOriginalCas,
            AnnotationFS aAnnotation)
    {
        return aOriginalCas.<Annotation> select(aAnnotation.getType()) //
                .at(aAnnotation) //
                .filter(candidate -> AnnotationComparisonUtils.isEquivalentSpanAnnotation(candidate,
                        aAnnotation, null))
                .findFirst();
    }
}
