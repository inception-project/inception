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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SENTENCES;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult.toEvaluationResult;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectOverlapping;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparingInt;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.uima.cas.CAS;
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
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.GazeteerService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.model.GazeteerEntry;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.text.KeySanitizerFactory;
import de.tudarmstadt.ukp.inception.support.text.Trie;
import de.tudarmstadt.ukp.inception.support.text.WhitespaceNormalizingSanitizer;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

public class StringMatchingRecommender
    extends RecommendationEngine
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final Key<Trie<DictEntry>> KEY_MODEL = new Key<>("model");

    private static final String NO_LABEL = "O";

    private static final Class<Sentence> SAMPLE_UNIT = Sentence.class;
    private static final Class<Token> DATAPOINT_UNIT = Token.class;

    private final StringMatchingRecommenderTraits traits;

    private final GazeteerService gazeteerService;

    private final KeySanitizerFactory keySanitizerFactory;

    private Pattern excludePattern;
    private String excludePatternError;

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
        keySanitizerFactory = WhitespaceNormalizingSanitizer.factory();

        if (traits != null && traits.getExcludePattern() != null) {
            try {
                excludePattern = Pattern.compile(traits.getExcludePattern());
            }
            catch (PatternSyntaxException e) {
                excludePatternError = e.getMessage();
            }
        }
        else {
            excludePattern = null;
        }
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        return aContext.get(KEY_MODEL).map(Objects::nonNull).orElse(false);
    }

    @Override
    public void exportModel(RecommenderContext aContext, OutputStream aOutput) throws IOException
    {
        var dict = aContext.get(KEY_MODEL)
                .orElseThrow(() -> new IOException("No model trained yet."));

        var out = new OutputStreamWriter(aOutput);
        var sortedKeys = dict.keys().stream().sorted().toList();
        for (var key : sortedKeys) {
            var value = dict.get(key);
            for (var i = 0; i < value.labels.length; i++) {
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
        var dict = aContext.get(KEY_MODEL).orElseGet(this::createTrie);

        if (aData != null) {
            for (var entry : aData) {
                learn(dict, entry.text, entry.label, true);
            }

            aContext.log(LogMessage.info(getRecommender().getName(),
                    "Loaded [%d] entries from gazeteer", aData.size()));
        }

        aContext.put(KEY_MODEL, dict);
    }

    private <T> Trie<T> createTrie()
    {
        return new Trie<>(keySanitizerFactory);
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses) throws RecommendationException
    {
        if (excludePatternError != null) {
            aContext.log(LogMessage.error(getRecommender().getName(),
                    "Ignoring bad exclude pattern: %s", excludePatternError));
        }

        // Pre-load the gazeteers into the model
        if (gazeteerService != null) {
            for (var gaz : gazeteerService.listGazeteers(recommender)) {
                try {
                    pretrain(gazeteerService.readGazeteerFile(gaz), aContext);
                }
                catch (IOException e) {
                    aContext.log(LogMessage.error(getRecommender().getName(),
                            "Unable to load gazeteer [%s]: %s", gaz.getName(), e.getMessage()));
                    LOG.error("Unable to load gazeteer [{}] for recommender {} in project {}",
                            gaz.getName(), gaz.getRecommender(), gaz.getRecommender().getProject(),
                            e);
                }
            }
        }

        var dict = aContext.get(KEY_MODEL).orElseGet(this::createTrie);

        for (var cas : aCasses) {
            var predictedType = getPredictedType(cas);
            var predictedFeature = getPredictedFeature(cas);
            var isMultiValue = TYPE_NAME_STRING_ARRAY.equals(predictedFeature.getRange().getName());

            for (var ann : select(cas, predictedType)) {
                if (isMultiValue) {
                    var labels = FSUtil.getFeature(ann, predictedFeature, String[].class);
                    if (labels != null) {
                        for (var label : labels) {
                            learn(dict, ann.getCoveredText(), label, false);
                        }
                    }
                }
                else {
                    learn(dict, ann.getCoveredText(), ann.getFeatureValueAsString(predictedFeature),
                            false);
                }
            }
        }

        aContext.log(LogMessage.info(getRecommender().getName(),
                "Learned dictionary model with %d entries on %d documents", dict.size(),
                aCasses.size()));

        aContext.put(KEY_MODEL, dict);
    }

    @Override
    public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        var dict = aContext.get(KEY_MODEL).orElseThrow(
                () -> new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));

        var predictedType = getPredictedType(aCas);
        var predictedFeature = getPredictedFeature(aCas);
        var isPredictionFeature = getIsPredictionFeature(aCas);
        var scoreFeature = getScoreFeature(aCas);
        var sampleUnitType = getType(aCas, SAMPLE_UNIT);

        var units = selectOverlapping(aCas, sampleUnitType, aBegin, aEnd);

        var data = predict(aCas, units, dict);

        for (var sample : data) {
            for (var span : sample.getSpans()) {
                var annotation = aCas.createAnnotation(predictedType, span.begin(), span.end());
                // Not using setStringValue because we want to handle the case that the predicted
                // feature is a multi-valued feature.
                if (!BLANK_LABEL.equals(span.label())) {
                    ICasUtil.setFeature(annotation, predictedFeature, span.label());
                }
                annotation.setDoubleValue(scoreFeature, span.score());
                annotation.setBooleanValue(isPredictionFeature, true);
                aCas.addFsToIndexes(annotation);
            }
        }

        return Range.rangeCoveringAnnotations(units);
    }

    private List<Sample> predict(CAS aCas, List<AnnotationFS> units, Trie<DictEntry> aDict)
    {
        var requireEndAtTokenBoundary = Set.of(SINGLE_TOKEN, TOKENS, SENTENCES)
                .contains(getRecommender().getLayer().getAnchoringMode());

        var requireSingleSentence = !getRecommender().getLayer().isCrossSentence();

        var tokenType = getType(aCas, Token.class);

        var data = new ArrayList<Sample>();
        var text = aCas.getDocumentText();
        if (traits != null && traits.isIgnoreCase()) {
            text = text.toLowerCase(Locale.ROOT);
        }

        for (var sampleUnit : units) {
            var spans = new ArrayList<Span>();

            var tokens = aCas.<Annotation> select(tokenType).coveredBy(sampleUnit).asList();
            for (var token : tokens) {
                var match = aDict.getNode(text, token.getBegin());
                if (match != null) {
                    var begin = token.getBegin();
                    var end = begin + match.matchLength;

                    // If the end is not in the same sentence as the start, skip
                    if (requireSingleSentence && !(end <= sampleUnit.getEnd())) {
                        continue;
                    }

                    // Need to check that the match actually ends at a token boundary!
                    if (requireEndAtTokenBoundary && !aCas.<Annotation> select(tokenType)
                            .startAt(token).filter(t -> t.getEnd() == end).findAny().isPresent()) {
                        continue;
                    }

                    for (var lc : match.node.value.getBest(maxRecommendations)) {
                        spans.add(new Span(begin, end, aCas.getDocumentText().substring(begin, end),
                                lc.label(), lc.relFreq()));
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
        return extractSamples(aCasses, layerName, featureName).size();
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        var samples = extractSamples(aCasses, layerName, featureName);
        var trainingSet = new ArrayList<Sample>();
        var testSet = new ArrayList<Sample>();

        for (var sample : samples) {
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

        var trainingSetSize = trainingSet.size();
        var testSetSize = testSet.size();
        var overallTrainingSize = samples.size() - testSetSize;
        var trainRatio = (overallTrainingSize > 0) ? trainingSetSize / overallTrainingSize : 0.0;

        // If we have just started and do not have sufficient data to create a test set, we evaluate
        // against the training set. For the string matcher, this is an ok approach in this
        // situation.
        if (!trainingSet.isEmpty() && testSet.isEmpty()) {
            testSet.addAll(trainingSet);
        }

        final var minTrainingSetSize = 1;
        final var minTestSetSize = 1;
        if (trainingSetSize < minTrainingSetSize || testSetSize < minTestSetSize) {
            if ((getRecommender().getThreshold() <= 0.0d) || (gazeteerService != null
                    && !gazeteerService.listGazeteers(recommender).isEmpty())) {
                // We cannot evaluate, but the user expects to see immediate results from the
                // gazeteer - so we return with an "unknown" result but without marking it as
                // skipped so that the selection task allows the recommender to activate.
                return new EvaluationResult(DATAPOINT_UNIT.getSimpleName(),
                        SAMPLE_UNIT.getSimpleName());
            }

            var info = String.format(
                    "Not enough evaluation data: training set size [%d] (min. %d), test set size [%d] (min. %d) of total [%d] (min. %d)",
                    trainingSetSize, minTrainingSetSize, testSetSize, minTestSetSize,
                    samples.size(), (minTrainingSetSize + minTestSetSize));
            LOG.info(info);
            var result = new EvaluationResult(DATAPOINT_UNIT.getSimpleName(),
                    SAMPLE_UNIT.getSimpleName(), trainingSetSize, testSetSize, trainRatio);
            result.setEvaluationSkipped(true);
            result.setErrorMsg(info);
            return result;
        }

        LOG.info("Training on [{}] items, predicting on [{}] of total [{}].", trainingSet.size(),
                testSet.size(), samples.size());

        // Train
        Trie<DictEntry> dict = createTrie();
        for (var sample : trainingSet) {
            for (var span : sample.getSpans()) {
                learn(dict, span.text(), span.label(), false);
            }
        }

        // Predict
        var labelPairs = new ArrayList<LabelPair>();
        for (var sample : testSet) {
            for (var token : sample.getTokens()) {
                var match = dict.getNode(sample.getText(), token.begin());
                var begin = token.begin();
                var end = token.end();

                var predictedLabel = NO_LABEL;
                if (match != null && sample.hasTokenEndingAt(token.begin() + match.matchLength)) {
                    var labelStats = match.node.value.getBest(1);
                    if (!labelStats.isEmpty()) {
                        predictedLabel = labelStats.get(0).label();
                    }
                }

                var coveringSpan = sample.getCoveringSpan(begin, end);
                var goldLabel = NO_LABEL;
                if (coveringSpan.isPresent()) {
                    goldLabel = coveringSpan.get().label();
                }

                labelPairs.add(new LabelPair(sample.getText().substring(token.begin(), token.end()),
                        goldLabel, predictedLabel));
            }
        }

        return labelPairs.stream().collect(toEvaluationResult(DATAPOINT_UNIT.getSimpleName(),
                SAMPLE_UNIT.getSimpleName(), trainingSetSize, testSetSize, trainRatio, NO_LABEL));
    }

    private void learn(Trie<DictEntry> aDict, String aText, String aLabel, boolean aBypassLimits)
    {
        if (isBlank(aText)) {
            return;
        }

        if (!aBypassLimits && traits != null) {
            if (excludePattern != null && excludePattern.matcher(aText).matches()) {
                return;
            }

            if (keySanitizerFactory.create().sanitize(aText).length() < traits.getMinLength()) {
                return;
            }
        }

        var label = isBlank(aLabel) ? BLANK_LABEL : aLabel;

        var text = aText;
        if (traits != null && traits.isIgnoreCase()) {
            text = text.toLowerCase(Locale.ROOT);
        }

        var entry = aDict.get(text);
        if (entry == null) {
            entry = new DictEntry(text);
            try {
                aDict.put(text, entry);
            }
            catch (IllegalArgumentException e) {
                // This can happen if the text is empty after sanitization
            }
        }

        entry.put(label);
    }

    private List<Sample> extractSamples(List<CAS> aCasses, String aLayerName, String aFeatureName)
    {
        var start = System.currentTimeMillis();

        var data = new ArrayList<Sample>();

        int docNo = 0;
        for (var cas : aCasses) {
            var annotationType = getType(cas, aLayerName);
            var predictedFeature = annotationType.getFeatureByBaseName(aFeatureName);
            var isMultiValue = TYPE_NAME_STRING_ARRAY.equals(predictedFeature.getRange().getName());

            for (var sentence : cas.select(Sentence.class)) {
                var spans = new ArrayList<Span>();

                for (var ann : cas.<Annotation> select(annotationType).coveredBy(sentence)) {
                    if (isBlank(ann.getCoveredText())) {
                        continue;
                    }

                    if (isMultiValue) {
                        var value = FSUtil.getFeature(ann, predictedFeature, String[].class);
                        if (value != null) {
                            Stream.of(value).distinct() //
                                    .forEach(l -> spans.add(new Span(ann, l)));
                        }
                    }
                    else {
                        spans.add(new Span(ann, ann.getFeatureValueAsString(predictedFeature)));
                    }
                }

                if (spans.isEmpty()) {
                    continue;
                }

                var tokens = cas.select(Token.class).coveredBy(sentence).asList();
                data.add(new Sample(docNo, cas.getDocumentText(), tokens, spans));
            }

            docNo++;
        }

        LOG.trace("Extracting data took {}ms", System.currentTimeMillis() - start);

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
            tokens = aTokens.stream().map(fs -> new TokenSpan(fs.getBegin(), fs.getEnd())).toList();
            spans = asList(aSpans.toArray(Span[]::new));
        }

        public Optional<Span> getCoveringSpan(int aBegin, int aEnd)
        {
            return spans.stream() //
                    .filter(s -> s.begin() <= aBegin && s.end() >= aEnd)//
                    .findFirst();
        }

        @SuppressWarnings("unused")
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

    private static record TokenSpan(int begin, int end) {}

    /**
     * @param label
     *            the label (e.g. NN, PER, OTH, etc.)
     * @param count
     *            how often the label was observed.
     * @param relFreq
     *            how often the label was observed in relation to the total number of observations
     *            of the mention.
     */
    private static record LabelStats(String label, int count, double relFreq) {}

    private static record Span(int begin, int end, String text, String label, double score) {
        public Span(AnnotationFS aAnn, String aLabel)
        {
            this(aAnn.getBegin(), aAnn.getEnd(), aAnn.getCoveredText(),
                    isBlank(aLabel) ? BLANK_LABEL : aLabel, 0.0);
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
            var newLabels = new String[labels.length + 1];
            System.arraycopy(labels, 0, newLabels, 0, labels.length);
            labels = newLabels;

            var newCounts = new int[counts.length + 1];
            System.arraycopy(counts, 0, newCounts, 0, counts.length);
            counts = newCounts;

            labels[labels.length - 1] = aLabel;
            counts[counts.length - 1] = 1;
        }

        public List<LabelStats> getBest(int aN)
        {
            int total = IntStream.of(counts).sum();

            var best = new ArrayList<LabelStats>();
            for (int i = 0; i < labels.length; i++) {
                best.add(new LabelStats(labels[i], counts[i], (double) counts[i] / (double) total));
            }

            return best.stream() //
                    .sorted(comparingInt(LabelStats::count).reversed()) //
                    .limit(aN) //
                    .toList();
        }

        @Override
        public String toString()
        {
            var builder = new StringBuilder();
            builder.append("DictEntry [key=");
            builder.append(key);
            builder.append("]");
            return builder.toString();
        }
    }
}
