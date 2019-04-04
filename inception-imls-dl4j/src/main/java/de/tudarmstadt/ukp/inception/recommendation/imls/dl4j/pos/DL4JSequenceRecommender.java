/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.dl4j.pos;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.CasUtil.getAnnotationType;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.nd4j.linalg.indexing.NDArrayIndex.all;
import static org.nd4j.linalg.indexing.NDArrayIndex.point;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.conf.layers.recurrent.Bidirectional;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.dkpro.core.api.embeddings.binary.BinaryVectorizer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.schedule.ScheduleType;
import org.nd4j.linalg.schedule.StepSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class DL4JSequenceRecommender
    implements RecommendationEngine
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String NO_LABEL = "*NO-LABEL*";

    public static final Key<String[]> KEY_TAGSET = new Key<>("labelDict");
    public static final Key<MultiLayerNetwork> KEY_MODEL = new Key<>("model");
    public static final Key<INDArray> KEY_UNKNOWN = new Key<>("unknown");
    
    private final String layerName;
    private final String featureName;
    private final File datasetCache;
    private DL4JSequenceRecommenderTraits traits;
    private BinaryVectorizer wordVectors;
    private INDArray randUnk;
    
    public DL4JSequenceRecommender(Recommender aRecommender, DL4JSequenceRecommenderTraits aTraits,
            File aDatasetCache)
    {
        layerName = aRecommender.getLayer().getName();
        featureName = aRecommender.getFeature().getName();
        traits = aTraits;
        datasetCache = aDatasetCache;
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
    {
        // Prepare a map where we store the mapping from labels to numeric label IDs - i.e.
        // which index in the label vector represents which label
        Object2IntMap<String> tagsetCollector = new Object2IntOpenHashMap<>();
        
        try {
            ensureEmbeddingsAreAvailable();
            
            // Extract the training data from the CASes
            List<Sample> trainingData = extractData(aCasses, true);
            
            // Use the training data to train the network
            MultiLayerNetwork model = train(trainingData, tagsetCollector);
                        
            aContext.put(KEY_MODEL, model);
            aContext.put(KEY_TAGSET, compileTagset(tagsetCollector));
            aContext.put(KEY_UNKNOWN, randUnk);
            aContext.markAsReadyForPrediction();
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to train model", e);
        }
    }
    
    private void ensureEmbeddingsAreAvailable() throws IOException
    {
        if (wordVectors == null) {
            // Load the embeddings. Mind that we are using a memory-mapped embedding store, so this
            // is a fast operation and also doesn't consume lots of memory. Hence we can do it for
            // each recommender instance and do not have to share it between recommenders.
            DatasetFactory loader = new DatasetFactory(datasetCache); 
            File embeddingsFile = loader.load("glove.6B.50d.dl4jw2v").getDataFiles()[0];
            wordVectors = BinaryVectorizer.load(embeddingsFile);
        }
        
        if (randUnk == null) {
            // Initialize the "unknown word" vector to a random vector
            int embeddingSize = wordVectors.dimensions();
            randUnk = Nd4j.rand(1, embeddingSize, Nd4j.getRandom()).subi(0.5).divi(embeddingSize);
        }
    }
    
    private String[] compileTagset(Object2IntMap<String> aTagsetCollector)
    {
        String[] tagset = new String[aTagsetCollector.size()];
        for (Entry<String> e : aTagsetCollector.object2IntEntrySet()) {
            tagset[e.getIntValue()] = e.getKey();
        }
        return tagset;
    }
    
    private List<Sample> extractData(List<CAS> aCasses, boolean aExtractLabels)
    {
        long start = System.currentTimeMillis();
        
        List<Sample> data = new ArrayList<>();
        
        for (CAS cas : aCasses) {
            Type sentenceType = getType(cas, Sentence.class);
            Type tokenType = getType(cas, Token.class);
            Type annotationType = getType(cas, layerName);
            
            for (AnnotationFS sentence : select(cas, sentenceType)) {
                List<AnnotationFS> tokenFSes = selectCovered(tokenType, sentence);
                List<AnnotationFS> annotationFSes = selectCovered(annotationType, sentence);
                
                List<String> tokens = CasUtil.toText(tokenFSes);
                
                if (aExtractLabels) {
                    List<String> labels = extractTokenLabels(tokenFSes, annotationFSes);
                    data.add(new Sample(tokens, labels));
                }
                else {
                    data.add(new Sample(tokens, null));
                }
            }
        }
        
        log.trace("Extracting data took {}ms", System.currentTimeMillis() - start);
        
        return data;
    }
    
    private MultiLayerNetwork train(List<Sample> aTrainingData, Object2IntMap<String> aTagset)
        throws IOException
    {
        // Configure the neural network
        MultiLayerNetwork model = createConfiguredNetwork(traits, wordVectors.dimensions());

        final int limit = traits.getTrainingSetSizeLimit();
        final int batchSize = traits.getBatchSize();

        // First vectorizing all sentences and then passing them to the model would consume
        // huge amounts of memory. Thus, every sentence is vectorized and then immediately
        // passed on to the model.
        nextEpoch: for (int epoch = 0; epoch < traits.getnEpochs(); epoch++) {
            int sentNum = 0;
            Iterator<Sample> sampleIterator = aTrainingData.iterator();
            while (sampleIterator.hasNext()) {
                List<DataSet> batch = new ArrayList<>();
                while (sampleIterator.hasNext() && batch.size() < batchSize && sentNum < limit) {
                    Sample sample = sampleIterator.next();
                    DataSet trainingData = vectorize(asList(sample), aTagset, true);
                    batch.add(trainingData);
                    sentNum++;
                }
                
                model.fit(new ListDataSetIterator<DataSet>(batch, batch.size()));
                log.trace("Epoch {}: processed {} of {} sentences", epoch, sentNum,
                        aTrainingData.size());
                
                if (sentNum >= limit) {
                    continue nextEpoch;
                }
            }
        }

        return model;
    }

    private DataSet vectorize(List<? extends Sample> aData)
        throws IOException
    {
        return vectorize(aData, null, false);
    }

    private DataSet vectorize(List<? extends Sample> aData, Object2IntMap<String> aTagset,
            boolean aIncludeLabels)
        throws IOException
    {
        // vectorize is pretty fast taking around 1-2ms
        
        // long start = System.currentTimeMillis();
        int maxSentenceLength = traits.getMaxSentenceLength();
        
        // Create data for training
        int embeddingSize = wordVectors.dimensions(); 
        INDArray featureVec = Nd4j.create(aData.size(), embeddingSize, maxSentenceLength);

        // Tags are using a 1-hot encoding
        INDArray labelVec = Nd4j.create(aData.size(), traits.getMaxTagsetSize(), maxSentenceLength);
        
        // Sentences have variable length, so we we need to mask positions not used in short
        // sentences.
        INDArray featureMask = Nd4j.zeros(aData.size(), maxSentenceLength);
        INDArray labelMask = Nd4j.zeros(aData.size(), maxSentenceLength);
        
        // Get word vectors for each word in review, and put them in the training data
        int sampleIdx = 0;
        for (Sample sample : aData) {
            List<String> tokens = sample.getSentence();
            List<String> labels = sample.getTags();
            for (int t = 0; t < Math.min(tokens.size(), maxSentenceLength); t++) {
                String word = tokens.get(t);
                INDArray vector = Nd4j.create(wordVectors.vectorize(word));
    
                if (vector == null) {
                    vector = randUnk;
                }
    
                featureVec.put(new INDArrayIndex[] { point(sampleIdx), all(), point(t) }, vector);
                

                // FIXME: exclude padding labels from training
                // compare instances to avoid collision with possible no_label user label
                if (labels == null || labels.get(t) == NO_LABEL) {
                    labelMask.putScalar(new int[] { sampleIdx, t }, 0.0);
                    featureMask.putScalar(new int[] { sampleIdx, t }, 0.0);
                }
                else {
                    labelMask.putScalar(new int[] { sampleIdx, t }, 1.0);
                    featureMask.putScalar(new int[] { sampleIdx, t }, 1.0);
                }

                if (aIncludeLabels) {
                    String label = labels.get(t);
                    if (!aTagset.containsKey(label)) {
                        aTagset.put(label, aTagset.size());
                    }
                    labelVec.putScalar(sampleIdx, aTagset.get(label), t, 1.0);
                }
            }
            
            sampleIdx++;
        }

        // log.trace("Vectorizing took {}ms", System.currentTimeMillis() - start);
        
        return new DataSet(featureVec, labelVec, featureMask, labelMask);
    }
    
    public List<String> extractTokenLabels(List<AnnotationFS> aTokens,
            List<AnnotationFS> aLabels)
    {
        Type annotationType = getType(aTokens.get(0).getCAS(), layerName);
        Feature feature = annotationType.getFeatureByBaseName(featureName);
        
        String[] labels = new String[aTokens.size()];
        int tokenIdx = 0;
        int labelIdx = 0;
        
        boolean seenBeginMatch = false;
        boolean seenEndMatch = false;
        int maxOffset = -1;

        // This loop assumes that labels start and end at token offsets. Labels that span over
        // multiple tokens are supported as well.
        while (tokenIdx < aTokens.size() && labelIdx < aLabels.size()) {
            AnnotationFS token = aTokens.get(tokenIdx);
            AnnotationFS label = aLabels.get(labelIdx);
            
            if (Math.min(label.getBegin(), label.getEnd()) < maxOffset) {
                throw new IllegalArgumentException("Overlapping labels are not supported!");
            }
            
            // Check if we have seen the begin/end of the label matching a token boundary
            seenBeginMatch |= label.getBegin() == token.getBegin();
            seenEndMatch |= label.getEnd() == token.getEnd();
            
            // First step: collect the label
            if (label.getBegin() <= token.getBegin() && token.getEnd() <= label.getEnd()) {
                String value = label.getFeatureValueAsString(feature);
                labels[tokenIdx] = StringUtils.defaultIfEmpty(value, NO_LABEL);
            }
            else {
                labels[tokenIdx] = NO_LABEL;
            }
            
            // Second step: move to next label (if necessary)
            if (label.getEnd() <= token.getEnd()) {
                labelIdx++;
                
                if (!seenBeginMatch || !seenEndMatch) {
                    throw new IllegalArgumentException("Labels must start/end at token boundaries!");
                }
                
                seenBeginMatch = false;
                seenEndMatch = false;
                maxOffset = Math.max(label.getBegin(), label.getEnd());
            }
                
            // In any case, we move to the next token
            tokenIdx++;
        }
        
        if (labelIdx < aLabels.size()) {
            throw new IllegalArgumentException("Overlapping labels are not supported!");
        }
        
        // If we ran out of labels before seeing all tokens, set the label for the remaining 
        // tokens here.
        while (tokenIdx < aTokens.size()) {
            labels[tokenIdx] = NO_LABEL;
            tokenIdx++;
        }
        
        return asList(labels);
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        String[] tagset = aContext.get(KEY_TAGSET).orElseThrow(() ->
                new RecommendationException("Key [" + KEY_TAGSET + "] not found in context"));
        MultiLayerNetwork classifier = aContext.get(KEY_MODEL).orElseThrow(() ->
                new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));
        
        try {
            Type sentenceType = getType(aCas, Sentence.class);
            Type tokenType = getType(aCas, Token.class);
            Type predictionType = getAnnotationType(aCas, PredictedSpan.class);
            Feature confidenceFeature = predictionType.getFeatureByBaseName("score");
            Feature labelFeature = predictionType.getFeatureByBaseName("label");
    
            final int limit = Integer.MAX_VALUE;
            final int batchSize = traits.getBatchSize();

            Collection<AnnotationFS> sentences = select(aCas, sentenceType);
            int sentNum = 0;
            
            Iterator<AnnotationFS> sentenceIterator = sentences.iterator();
            while (sentenceIterator.hasNext()) {
                // Prepare a batch of sentences that we want to predict because calling the
                // prediction is expensive
                List<CasSample> batch = new ArrayList<>();
                while (sentenceIterator.hasNext() && batch.size() < batchSize && sentNum < limit) {
                    AnnotationFS sentence = sentenceIterator.next();
                    List<AnnotationFS> tokenFSes = selectCovered(tokenType, sentence);
                    List<String> tokens = CasUtil.toText(tokenFSes);
                    batch.add(new CasSample(tokens, tokenFSes));
                    sentNum++;
                }
                
                // If a limit was set that is smaller than the number of sentence, then we
                // eventually start producing empty batches. At this point, we are done.
                if (batch.isEmpty()) {
                    break;
                }

                List<Outcome<CasSample>> outcomes = predict(classifier, tagset, batch);
                
                int outcomeIdx = 0;
                for (Outcome<CasSample> outcome : outcomes) {
                    List<AnnotationFS> tokenFSes = outcome.getSample().getTokens();
                    for (int tokenIdx = 0; tokenIdx < tokenFSes.size(); tokenIdx ++) {
                        AnnotationFS token = tokenFSes.get(tokenIdx);
                        AnnotationFS annotation = aCas.createAnnotation(predictionType,
                                token.getBegin(), token.getEnd());
                        //annotation.setDoubleValue(confidenceFeature, prediction.getProb());
                        annotation.setStringValue(labelFeature,
                                outcomes.get(outcomeIdx).getLabels().get(tokenIdx));
                        aCas.addFsToIndexes(annotation);
                    }
                    outcomeIdx++;
                }
                
                log.trace("Predicted {} of {} sentences", sentNum, sentences.size());
            }
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to predict", e);
        }
    }
    
    private <T extends Sample> List<Outcome<T>> predict(MultiLayerNetwork aClassifier,
            String[] aTagset, List<T> aData)
        throws IOException
    {
        if (aData.isEmpty()) {
            return Collections.emptyList();
        }
        
        DataSet data = vectorize(aData);
        
        // Predict labels
        long predictionStart = System.currentTimeMillis();
        INDArray predicted = aClassifier.output(data.getFeatures(), false,
                data.getFeaturesMaskArray(), data.getLabelsMaskArray());
        log.trace("Prediction took {}ms", System.currentTimeMillis() - predictionStart);
        
        // This is a brute-force hack to ensue that argmax doesn't predict tags that are not 
        // in the tagset. Actually, this should be necessary at all if the network is properly
        // configured...
        predicted = predicted.get(NDArrayIndex.all(), NDArrayIndex.interval(0, aTagset.length),
                NDArrayIndex.all());
        
        List<Outcome<T>> outcomes = new ArrayList<>();
        int sampleIdx = 0;
        for (Sample sample : aData) {
            INDArray argMax = Nd4j.argMax(predicted, 1);
    
            List<String> tokens = sample.getSentence();
            String[] labels = new String[tokens.size()];
            for (int tokenIdx = 0; tokenIdx < tokens.size(); tokenIdx ++) {
                labels[tokenIdx] = aTagset[argMax.getInt(sampleIdx, tokenIdx)];
            }
            
            outcomes.add(new Outcome(sample, asList(labels)));
            
            sampleIdx ++;
        }
        
        return outcomes;
    }

    @Override
    public double evaluate(List<CAS> aCas, DataSplitter aDataSplitter)
    {
        // Prepare a map where we store the mapping from labels to numeric label IDs - i.e.
        // which index in the label vector represents which label
        Object2IntMap<String> tagsetCollector = new Object2IntOpenHashMap<>();

        // Extract the data from the CASes
        List<Sample> data = extractData(aCas, true);
        List<Sample> trainingSet = new ArrayList<>();
        List<Sample> testSet = new ArrayList<>();

        // Split the data up into training and test sets
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

        if (trainingSet.size() < 2 || testSet.size() < 2) {
            log.info("Not enough data to evaluate, skipping!");
            return 0.0;
        }

        log.info("Training on [{}] items, predicting on [{}] of total [{}]", trainingSet.size(),
                testSet.size(), data.size());

        try {
            ensureEmbeddingsAreAvailable();
            
            MultiLayerNetwork classifier = train(trainingSet, tagsetCollector);
            String[] tagset = compileTagset(tagsetCollector);
            
            final int limit = Integer.MAX_VALUE;
            final int batchSize = 250;
            
            int sentNum = 0;
            double total = 0;
            double correct = 0;
            Iterator<Sample> testSetIterator = testSet.iterator();
            while (testSetIterator.hasNext()) {
                // Prepare a batch of sentences that we want to predict because calling the
                // prediction is expensive
                List<Sample> batch = new ArrayList<>();
                while (testSetIterator.hasNext() && batch.size() < batchSize && sentNum < limit) {
                    batch.add(testSetIterator.next());
                    sentNum++;
                }
                
                List<Outcome<Sample>> outcomes = predict(classifier, tagset, batch);
                
                for (Outcome<Sample> outcome : outcomes) {
                    List<String> expectedLabels = outcome.getSample().getTags();
                    List<String> actualLabels = outcome.getLabels();
                    for (int i = 0; i < expectedLabels.size(); i++) {
                        total++;
                        if (expectedLabels.get(i).equals(actualLabels.get(i))) {
                            correct++;
                        }
                    }
                }
            }
            
            return correct / total;
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to evaluate", e);
        }
    }
    
    private MultiLayerNetwork createConfiguredNetwork(DL4JSequenceRecommenderTraits aTraits,
            int aEmbeddingsDim)
    {
        long start = System.currentTimeMillis();
        
        // Set up network configuration
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(aTraits.getOptimizationAlgorithm())
                .updater(new Nesterovs(
                        new StepSchedule(ScheduleType.ITERATION, 1e-2, 0.1, 100000), 0.9))
                .biasUpdater(new Nesterovs(
                        new StepSchedule(ScheduleType.ITERATION, 2e-2, 0.1, 100000), 0.9))
                .l2(aTraits.getL2())
                .weightInit(aTraits.getWeightInit())
                .gradientNormalization(aTraits.getGradientNormalization())
                .gradientNormalizationThreshold(aTraits.getGradientNormalizationThreshold())
                .list()
                .layer(0, new Bidirectional(Bidirectional.Mode.ADD, new LSTM.Builder()
                        .nIn(aEmbeddingsDim)
                        .nOut(200)
                        .activation(aTraits.getActivationL0())
                        .build()))
                .layer(1, new RnnOutputLayer.Builder()
                        .nIn(200)
                        .nOut(aTraits.getMaxTagsetSize())
                        .activation(aTraits.getActivationL1())
                        .lossFunction(aTraits.getLossFunction())
                        .build())
                .build();
        
        // log.info("Network configuration: {}", conf.toYaml());

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        // net.setListeners(new ScoreIterationListener(1));
        
        log.trace("Setting up the model took {}ms", System.currentTimeMillis() - start);
        
        return net;
    }
    
    private static class Outcome<T extends Sample>
    {
        private final T sample;
        private final List<String> labels;
        public Outcome(T aSample, List<String> aLabels)
        {
            super();
            sample = aSample;
            labels = aLabels;
        }
        
        public T getSample()
        {
            return sample;
        }
        
        public List<String> getLabels()
        {
            return labels;
        }
    }
    
    private static class Sample
    {
        private final String[] sentence;
        private final String[] tags;

        public Sample(Collection<String> aSentence)
        {
            this(aSentence, null);
        }

        public Sample(Collection<String> aSentence, Collection<String> aTags)
        {
            sentence = aSentence.toArray(new String[aSentence.size()]);
            tags = aTags != null ? aTags.toArray(new String[aTags.size()]) : null;
        }
        
        public List<String> getSentence()
        {
            return asList(sentence);
        }
        
        public List<String> getTags()
        {
            if (tags != null) {
                return asList(tags);
            }
            else {
                return null;
            }
        }
    }
    
    private static class CasSample extends Sample
    {
        private final List<AnnotationFS> tokens;

        public CasSample(Collection<String> aSentence, List<AnnotationFS> aTokens)
        {
            super(aSentence);
            tokens = aTokens;
        }
        
        public List<AnnotationFS> getTokens()
        {
            return tokens;
        }
    }
}
