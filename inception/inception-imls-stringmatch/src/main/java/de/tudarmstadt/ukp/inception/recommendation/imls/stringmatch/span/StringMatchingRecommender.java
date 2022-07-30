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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectOverlapping;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult.toEvaluationResult;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparingInt;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.LabelPair;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.GazeteerService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.model.Gazeteer;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.model.GazeteerEntry;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.trie.Trie;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.trie.WhitespaceNormalizingSanitizer;
import de.tudarmstadt.ukp.inception.rendering.model.Range;

public class StringMatchingRecommender
    extends RecommendationEngine
{
    public static final Key<Trie<DictEntry>> KEY_MODEL = new Key<>("model");

    private static final String UNKNOWN_LABEL = "unknown";
    private static final String NO_LABEL = "O";

    private static final Class<Sentence> SAMPLE_UNIT = Sentence.class;
    private static final Class<Token> DATAPOINT_UNIT = Token.class;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final StringMatchingRecommenderTraits traits;

    private final GazeteerService gazeteerService;

    public StringMatchingRecommender(Recommender aRecommender,
            StringMatchingRecommenderTraits aTraits)
    {
        this(aRecommender, aTraits, null);
    }

    public StringMatchingRecommender(Recommender aRecommender,
            StringMatchingRecommenderTraits aTraits, GazeteerService aGazeteerService)
    {
        super(aRecommender);

        traits = aTraits;
        gazeteerService = aGazeteerService;
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        return aContext.get(KEY_MODEL).map(Objects::nonNull).orElse(false);
    }

    @Override
    public void exportModel(RecommenderContext aContext, OutputStream aOutput) throws IOException
    {
        Trie<DictEntry> dict = aContext.get(KEY_MODEL)
                .orElseThrow(() -> new IOException("No model trained yet."));

        OutputStreamWriter out = new OutputStreamWriter(aOutput);
        List<String> sortedKeys = new ArrayList<>(dict.keys());
        Collections.sort(sortedKeys);
        for (String key : sortedKeys) {
            DictEntry value = dict.get(key);
            for (int i = 0; i < value.labels.length; i++) {
                out.append(key);
                out.append("\t");
                out.append(value.labels[i]);
                out.append("\t");
                out.append(Integer.toString(value.counts[i]));
                out.append("\n");
            }
        }
        out.flush();
    }

    public void pretrain(List<GazeteerEntry> aData, RecommenderContext aContext)
    {
        Trie<DictEntry> dict = aContext.get(KEY_MODEL).orElseGet(this::createTrie);

        if (aData != null) {
            for (GazeteerEntry entry : aData) {
                learn(dict, entry.text, entry.label);
            }
        }

        aContext.put(KEY_MODEL, dict);
    }

    private <T> Trie<T> createTrie()
    {
        return new Trie<>(WhitespaceNormalizingSanitizer.factory());
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses) throws RecommendationException
    {
        // Pre-load the gazeteers into the model
        if (gazeteerService != null) {
            for (Gazeteer gaz : gazeteerService.listGazeteers(recommender)) {
                try {
                    pretrain(gazeteerService.readGazeteerFile(gaz), aContext);
                }
                catch (IOException e) {
                    log.info(
                            "Unable to load gazeteer [{}] for recommender [{}]({}) in project [{}]({})",
                            gaz.getName(), gaz.getRecommender().getName(),
                            gaz.getRecommender().getId(),
                            gaz.getRecommender().getProject().getName(),
                            gaz.getRecommender().getProject().getId(), e);
                }
            }
        }

        Trie<DictEntry> dict = aContext.get(KEY_MODEL).orElseGet(this::createTrie);

        for (CAS cas : aCasses) {
            Type predictedType = getPredictedType(cas);
            Feature predictedFeature = getPredictedFeature(cas);
            boolean isStringMultiValue = CAS.TYPE_NAME_STRING_ARRAY
                    .equals(predictedFeature.getRange().getName());

            for (AnnotationFS ann : select(cas, predictedType)) {
                if (isStringMultiValue) {
                    for (String label : FSUtil.getFeature(ann, predictedFeature, String[].class)) {
                        learn(dict, ann.getCoveredText(), label);
                    }
                }
                else {
                    learn(dict, ann.getCoveredText(),
                            ann.getFeatureValueAsString(predictedFeature));
                }
            }
        }

        aContext.info("Learned dictionary model with %d entries on %d documents", dict.size(),
                aCasses.size());

        aContext.put(KEY_MODEL, dict);
    }

    @Override
    public Range predict(RecommenderContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        Trie<DictEntry> dict = aContext.get(KEY_MODEL).orElseThrow(
                () -> new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));

        Type predictedType = getPredictedType(aCas);
        Feature predictedFeature = getPredictedFeature(aCas);
        Feature isPredictionFeature = getIsPredictionFeature(aCas);
        Feature scoreFeature = getScoreFeature(aCas);
        Type sampleUnitType = getType(aCas, SAMPLE_UNIT);

        var units = selectOverlapping(aCas, sampleUnitType, aBegin, aEnd);

        List<Sample> data = predict(aCas, units, dict);

        for (Sample sample : data) {
            for (Span span : sample.getSpans()) {
                AnnotationFS annotation = aCas.createAnnotation(predictedType, span.getBegin(),
                        span.getEnd());
                // Not using setStringValue because we want to handle the case that the predicted
                // feature is a multi-valued feature. Unfortunately, uimaFIT doesn't have a
                // setFeature(..., Feature, ...) call yet...
                FSUtil.setFeature(annotation, predictedFeature.getShortName(), span.getLabel());
                annotation.setDoubleValue(scoreFeature, span.getScore());
                annotation.setBooleanValue(isPredictionFeature, true);
                aCas.addFsToIndexes(annotation);
            }
        }

        return new Range(units);
    }

    private List<Sample> predict(CAS aCas, List<AnnotationFS> units, Trie<DictEntry> aDict)
    {
        boolean requireEndAtTokenBoundary = !CHARACTERS
                .equals(getRecommender().getLayer().getAnchoringMode());

        boolean requireSingleSentence = !getRecommender().getLayer().isCrossSentence();

        Type tokenType = getType(aCas, Token.class);

        List<Sample> data = new ArrayList<>();
        String text = aCas.getDocumentText();
        if (traits != null && traits.isIgnoreCase()) {
            text = text.toLowerCase(Locale.ROOT);
        }

        for (AnnotationFS sampleUnit : units) {
            List<Span> spans = new ArrayList<>();
            List<Annotation> tokens = aCas.<Annotation> select(tokenType) //
                    .coveredBy(sampleUnit) //
                    .asList();
            for (Annotation token : tokens) {
                Trie<DictEntry>.MatchedNode match = aDict.getNode(text, token.getBegin());
                if (match != null) {
                    int begin = token.getBegin();
                    int end = begin + match.matchLength;

                    // If the end is not in the same sentence as the start, skip
                    if (requireSingleSentence && !(end <= sampleUnit.getEnd())) {
                        continue;
                    }

                    // Need to check that the match actually ends at a token boundary!
                    if (requireEndAtTokenBoundary && !aCas.<Annotation> select(tokenType)
                            .startAt(token).filter(t -> t.getEnd() == end).findAny().isPresent()) {
                        continue;
                    }

                    for (LabelStats lc : match.node.value.getBest(maxRecommendations)) {
                        String label = lc.getLabel();
                        // check instance equality to avoid collision with user labels
                        if (label == UNKNOWN_LABEL) {
                            label = null;
                        }
                        spans.add(new Span(begin, end, aCas.getDocumentText().substring(begin, end),
                                label, lc.getRelFreq()));
                    }
                }
            }

            data.add(new Sample(0, aCas.getDocumentText(), tokens, spans));
        }

        return data;
    }

    @Override
    public int estimateSampleCount(List<CAS> aCasses)
    {
        return extractData(aCasses, layerName, featureName).size();
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        List<Sample> data = extractData(aCasses, layerName, featureName);
        List<Sample> trainingSet = new ArrayList<>();
        List<Sample> testSet = new ArrayList<>();

        for (Sample sample : data) {
            switch (aDataSplitter.getTargetSet(sample)) {
            case TRAIN:
                trainingSet.add(sample);
                break;
            case TEST:
                testSet.add(sample);
                break;
            default:
                // Do nothing
                break;
            }
        }

        int trainingSetSize = trainingSet.size();
        int testSetSize = testSet.size();
        double overallTrainingSize = data.size() - testSetSize;
        double trainRatio = (overallTrainingSize > 0) ? trainingSetSize / overallTrainingSize : 0.0;

        // If we have just started and do not have sufficient data to create a test set, we evaluate
        // against the training set. For the string matcher, this is an ok approach in this
        // situation.
        if (!trainingSet.isEmpty() && testSet.isEmpty()) {
            testSet.addAll(trainingSet);
        }

        final int minTrainingSetSize = 1;
        final int minTestSetSize = 1;
        if (trainingSetSize < minTrainingSetSize || testSetSize < minTestSetSize) {
            if ((getRecommender().getThreshold() <= 0.0d) || (gazeteerService != null
                    && !gazeteerService.listGazeteers(recommender).isEmpty())) {
                // We cannot evaluate, but the user expects to see immediate results from the
                // gazeteer - so we return with an "unknown" result but without marking it as
                // skipped so that the selection task allows the recommender to activate.
                return new EvaluationResult(DATAPOINT_UNIT.getSimpleName(),
                        SAMPLE_UNIT.getSimpleName());
            }

            String info = String.format(
                    "Not enough evaluation data: training set size [%d] (min. %d), test set size [%d] (min. %d) of total [%d] (min. %d)",
                    trainingSetSize, minTrainingSetSize, testSetSize, minTestSetSize, data.size(),
                    (minTrainingSetSize + minTestSetSize));
            log.info(info);
            EvaluationResult result = new EvaluationResult(DATAPOINT_UNIT.getSimpleName(),
                    SAMPLE_UNIT.getSimpleName(), trainingSetSize, testSetSize, trainRatio);
            result.setEvaluationSkipped(true);
            result.setErrorMsg(info);
            return result;
        }

        log.info("Training on [{}] items, predicting on [{}] of total [{}].", trainingSet.size(),
                testSet.size(), data.size());

        // Train
        Trie<DictEntry> dict = createTrie();
        for (Sample sample : trainingSet) {
            for (Span span : sample.getSpans()) {
                learn(dict, span.getText(), span.getLabel());
            }
        }

        // Predict
        List<LabelPair> labelPairs = new ArrayList<>();
        for (Sample sample : testSet) {
            for (TokenSpan token : sample.getTokens()) {
                Trie<DictEntry>.MatchedNode match = dict.getNode(sample.getText(),
                        token.getBegin());
                int begin = token.getBegin();
                int end = token.getEnd();

                String predictedLabel = NO_LABEL;
                if (match != null
                        && sample.hasTokenEndingAt(token.getBegin() + match.matchLength)) {
                    List<LabelStats> labelStats = match.node.value.getBest(1);
                    if (!labelStats.isEmpty()) {
                        predictedLabel = labelStats.get(0).getLabel();
                    }
                }
                Optional<Span> coveringSpan = sample.getCoveringSpan(begin, end);
                String goldLabel = NO_LABEL;
                if (coveringSpan.isPresent()) {
                    goldLabel = coveringSpan.get().getLabel();
                }
                labelPairs.add(
                        new LabelPair(sample.getText().substring(token.getBegin(), token.getEnd()),
                                goldLabel, predictedLabel));
            }
        }

        return labelPairs.stream().collect(toEvaluationResult(DATAPOINT_UNIT.getSimpleName(),
                SAMPLE_UNIT.getSimpleName(), trainingSetSize, testSetSize, trainRatio, NO_LABEL));
    }

    private void learn(Trie<DictEntry> aDict, String aText, String aLabel)
    {
        if (isBlank(aText)) {
            return;
        }

        String label = isBlank(aLabel) ? UNKNOWN_LABEL : aLabel;

        String text = aText;
        if (traits != null && traits.isIgnoreCase()) {
            text = text.toLowerCase(Locale.ROOT);
        }

        DictEntry entry = aDict.get(text);
        if (entry == null) {
            entry = new DictEntry(text);
            aDict.put(text, entry);
        }

        entry.put(label);
    }

    private List<Sample> extractData(List<CAS> aCasses, String aLayerName, String aFeatureName)
    {
        long start = System.currentTimeMillis();

        List<Sample> data = new ArrayList<>();

        int docNo = 0;
        for (CAS cas : aCasses) {
            Type sentenceType = getType(cas, Sentence.class);
            Type tokenType = getType(cas, Token.class);
            Type annotationType = getType(cas, aLayerName);
            Feature predictedFeature = annotationType.getFeatureByBaseName(aFeatureName);
            boolean isStringMultiValue = CAS.TYPE_NAME_STRING_ARRAY
                    .equals(predictedFeature.getRange().getName());

            for (AnnotationFS sentence : select(cas, sentenceType)) {
                List<Span> spans = new ArrayList<>();

                for (AnnotationFS annotation : selectCovered(annotationType, sentence)) {
                    if (isBlank(annotation.getCoveredText())) {
                        continue;
                    }

                    if (isStringMultiValue) {
                        for (String label : FSUtil.getFeature(annotation, predictedFeature,
                                String[].class)) {
                            if (isEmpty(label)) {
                                continue;
                            }
                            spans.add(new Span(annotation, label, -1.0));
                        }
                    }
                    else {
                        String label = annotation.getFeatureValueAsString(predictedFeature);
                        if (isEmpty(label)) {
                            continue;
                        }
                        spans.add(new Span(annotation, label, -1.0));
                    }
                }

                if (spans.isEmpty()) {
                    continue;
                }

                Collection<AnnotationFS> tokens = selectCovered(tokenType, sentence);
                data.add(new Sample(docNo, cas.getDocumentText(), tokens, spans));
            }

            docNo++;
        }

        log.trace("Extracting data took {}ms", System.currentTimeMillis() - start);

        return data;
    }

    private static class Sample
    {
        private final int docNo;
        private final String text;
        private final List<TokenSpan> tokens;
        private final List<Span> spans;

        public Sample(int aDocNo, String aText, Collection<? extends AnnotationFS> aTokens,
                Collection<Span> aSpans)
        {
            super();
            docNo = aDocNo;
            text = aText;
            tokens = aTokens.stream().map(fs -> new TokenSpan(fs.getBegin(), fs.getEnd()))
                    .collect(Collectors.toList());
            spans = asList(aSpans.toArray(new Span[aSpans.size()]));
        }

        public Optional<Span> getCoveringSpan(int aBegin, int aEnd)
        {
            return spans.stream().filter(s -> (s.getBegin() <= aBegin && s.getEnd() >= aEnd))
                    .findFirst();
        }

        public int getDocNo()
        {
            return docNo;
        }

        public String getText()
        {
            return text;
        }

        public List<TokenSpan> getTokens()
        {
            return tokens;
        }

        public List<Span> getSpans()
        {
            return spans;
        }

        public boolean hasTokenEndingAt(int aOffset)
        {
            return tokens.stream().filter(t -> t.end == aOffset).findAny().isPresent();
        }
    }

    private static class TokenSpan
    {
        private final int begin;
        private final int end;

        public TokenSpan(int aBegin, int aEnd)
        {
            super();
            begin = aBegin;
            end = aEnd;
        }

        public int getBegin()
        {
            return begin;
        }

        public int getEnd()
        {
            return end;
        }
    }

    private static class LabelStats
    {
        private final String label;
        private final int count;
        private final double relFreq;

        public LabelStats(String aLabel, int aCount, double aRelFreq)
        {
            super();
            label = aLabel;
            count = aCount;
            relFreq = aRelFreq;
        }

        /**
         * The label (e.g. NN, PER, OTH, etc.)
         */
        public String getLabel()
        {
            return label;
        }

        /**
         * How often the label was observed.
         */
        public int getCount()
        {
            return count;
        }

        /**
         * How often the label was observed in relation to the total number of observations of the
         * mention.
         */
        public double getRelFreq()
        {
            return relFreq;
        }
    }

    private static class Span
    {
        private final int begin;
        private final int end;
        private final String text;
        private final String label;
        private final double score;

        public Span(AnnotationFS aAnn, String aLabel, double aScore)
        {
            this(aAnn.getBegin(), aAnn.getEnd(), aAnn.getCoveredText(), aLabel, aScore);
        }

        public Span(int aBegin, int aEnd, String aText, String aLabel, double aScore)
        {
            super();
            begin = aBegin;
            end = aEnd;
            text = aText;
            label = aLabel;
            score = aScore;
        }

        public int getBegin()
        {
            return begin;
        }

        public int getEnd()
        {
            return end;
        }

        public String getText()
        {
            return text;
        }

        public String getLabel()
        {
            return label;
        }

        public double getScore()
        {
            return score;
        }

        @Override
        public String toString()
        {
            return new ToStringBuilder(this, ToStringStyle.NO_FIELD_NAMES_STYLE)
                    .append("begin", begin).append("end", end).append("text", text)
                    .append("label", label).append("score", score).toString();
        }
    }

    public static class DictEntry
    {
        private String key;
        private String[] labels;
        private int[] counts;

        public DictEntry(String aKey)
        {
            key = aKey;
        }

        public void put(String aLabel)
        {
            // No data yet - create it
            if (labels == null) {
                labels = new String[] { aLabel };
                counts = new int[] { 1 };
                return;
            }

            // Data is available
            int i = asList(labels).indexOf(aLabel);

            // Label already exists
            if (i != -1) {
                counts[i]++;
                return;
            }

            // Label does not exist yet
            String[] newLabels = new String[labels.length + 1];
            System.arraycopy(labels, 0, newLabels, 0, labels.length);
            labels = newLabels;

            int[] newCounts = new int[counts.length + 1];
            System.arraycopy(counts, 0, newCounts, 0, counts.length);
            counts = newCounts;

            labels[labels.length - 1] = aLabel;
            counts[counts.length - 1] = 1;
        }

        public List<LabelStats> getBest(int aN)
        {
            int total = IntStream.of(counts).sum();

            List<LabelStats> best = new ArrayList<>();
            for (int i = 0; i < labels.length; i++) {
                best.add(new LabelStats(labels[i], counts[i], (double) counts[i] / (double) total));
            }

            return best.stream().sorted(comparingInt(LabelStats::getCount).reversed()).limit(aN)
                    .collect(Collectors.toList());
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("DictEntry [key=");
            builder.append(key);
            builder.append("]");
            return builder.toString();
        }
    }
}
