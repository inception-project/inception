/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.imls.mira.pos;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import edu.lium.mira.Example;
import edu.lium.mira.Mira;
import edu.lium.mira.Mira.FScorer;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectIntHashMap;

public class IncrementalMira
{
    private static final long serialVersionUID = 1L;
    
    private Mira mira;
    
    public IncrementalMira()
    {
        mira = new Mira();
    }

    public void train(List<List<AnnotationObject>> input, int numIters, int numExamples,
            int iteration)
        throws IOException
    {
       
        int num = 0;
        double totalLoss = 0;
        double maxLoss = 0;
        Example example;
        FScorer scorer = mira.new FScorer();
        if (mira.iobScorer) {
            scorer = mira.new IOBScorer();
        }

        for (List<AnnotationObject> sentence : input) {
            example = nextExample(sentence, false);
            Example prediction;
            if (mira.bigramIds.size() > 0) {
                prediction = mira.decodeViterbi(example);
            }
            else {
                prediction = mira.decodeUnigram(example);
            }
            double avgUpdate = (double) (numIters * numExamples
                    - (numExamples * ((iteration + 1) - 1) + (num + 1)) + 1);
            double loss = mira.computeLoss(example, prediction);
            loss = mira.update(example, prediction, avgUpdate, loss);
            totalLoss += loss;
            maxLoss += example.labels.length;
            num++;
            scorer.assess(example, prediction);
            if (num % 100 == 0) {
                System.err.print("\r  train: " + num + " examples, terr="
                        + mira.formatter.format(totalLoss / maxLoss) + " fscore="
                        + mira.formatter.format(scorer.fscore()));
            }
        }
        System.err.println("\r  train: " + num + " examples, terr="
                + mira.formatter.format(totalLoss / maxLoss) + " fscore="
                + mira.formatter.format(scorer.fscore()));
    }

    public Example nextExample(List<AnnotationObject> sentence, boolean newFeatures)
        throws IOException
    {
        Vector<String> lines = new Vector<>();
        Vector<String[]> parts = new Vector<>();
        String line;

        for (AnnotationObject ao : sentence) {
            String[] tokens = new String[2];
            tokens[0] = ao.getCoveredText();
            tokens[1] = ao.getAnnotation();
            if (tokens.length > mira.xsize + 1 && newFeatures) {
                mira.xsize = tokens.length - 1;
            }
            parts.add(tokens);
            line = tokens[0] + " " + tokens[1];
            lines.add(line);
        }

        if (parts.isEmpty()) {
            return null;
        }
        Example example = mira.encodeFeatures(parts, newFeatures, true);
        example.lines = lines;
        return example;
    }

    public int count(List<List<AnnotationObject>> input, int cutoff)
        throws IOException
    {
        int num = 0;
        Example example;
        TIntIntHashMap unigramCounts = new TIntIntHashMap();
        TIntIntHashMap bigramCounts = new TIntIntHashMap();
        for (List<AnnotationObject> sentence : input) {
            example = nextExample(sentence, true);
            for (int slot = 0; slot < example.unigrams.length; slot++) {
                for (int i = 0; i < example.unigrams[slot].length; i++) {
                    unigramCounts.adjustOrPutValue(example.unigrams[slot][i], 1, 1);
                }
                for (int i = 0; i < example.bigrams[slot].length; i++) {
                    bigramCounts.adjustOrPutValue(example.bigrams[slot][i], 1, 1);
                }
            }
            if (num % 100 == 0) {
                System.err.print("\rcounting: " + num);
            }
            num += 1;
        }
        System.err.println("\rcounting: " + num);
        System.err.println(
                "unigrams: " + mira.unigramIds.size() + ", bigrams: " + mira.bigramIds.size());
        mira.numLabels = mira.knownLabels.size();

        TObjectIntHashMap<String> newUnigrams = new TObjectIntHashMap<>();
        String[] keys = mira.unigramIds.keys(new String[0]);
        for (int i = 0; i < keys.length; i++) {
            int id = mira.unigramIds.get(keys[i]);
            if (unigramCounts.get(id) >= cutoff) {
                newUnigrams.put(keys[i], newUnigrams.size() * mira.numLabels);
            }
        }
        mira.unigramIds = newUnigrams;
        mira.numUnigramFeatures = mira.unigramIds.size();

        TObjectIntHashMap<String> newBigrams = new TObjectIntHashMap<>();
        keys = mira.bigramIds.keys(new String[0]);
        for (int i = 0; i < keys.length; i++) {
            int id = mira.bigramIds.get(keys[i]);
            if (bigramCounts.get(id) >= cutoff) {
                newBigrams.put(keys[i], newBigrams.size() * mira.numLabels * mira.numLabels
                        + mira.numUnigramFeatures * mira.numLabels);
            }
        }
        mira.bigramIds = newBigrams;
        mira.numBigramFeatures = mira.bigramIds.size();

        System.err.println("unigrams: " + mira.unigramIds.size() + ", bigrams: "
                + mira.bigramIds.size() + ", cutoff: " + cutoff);
        System.err.println("labels: " + mira.knownLabels.size());
        mira.labels = new String[mira.numLabels];
        for (int i = 0; i < mira.numLabels; i++) {
            mira.labels[mira.knownLabels.get(
                    (String) mira.knownLabels.keys()[i])] = (String) mira.knownLabels.keys()[i];
        }
        return num;
    }

    public List<List<List<AnnotationObject>>> test(List<List<AnnotationObject>> input)
        throws IOException
    {
        List<List<List<AnnotationObject>>> output = new LinkedList<>();
        Example example;

        int id = 0;
        for (List<AnnotationObject> sentence : input) {
            List<List<AnnotationObject>> generatedSentence = new LinkedList<>();

            example = nextExample(sentence, false);
            Example prediction;
            if (mira.bigramIds.size() > 0) {
                prediction = mira.decodeViterbi(example);
            }
            else {
                prediction = mira.decodeUnigram(example);
            }

            if (prediction != null && mira.labels.length > 0) {
                for (int i = 0; i < sentence.size(); i++) {
                    AnnotationObject generatedAo = new AnnotationObject(sentence.get(i), id,
                            "PosValue", "MiraPosClassifier");
                    generatedAo.setAnnotation(mira.labels[prediction.labels[i]]);
                    List<AnnotationObject> word = new LinkedList<>();
                    word.add(generatedAo);
                    generatedSentence.add(word);
                    id++;
                }
                output.add(generatedSentence);
            }
        }

        return output;
    }

    public void loadModel(String aFilename) throws ClassNotFoundException, IOException
    {
        mira.loadModel(aFilename);
    }

    public void initModel(boolean aRandomInit)
    {
        mira.initModel(aRandomInit);
    }

    public void averageWeights(int aFactor)
    {
        mira.averageWeights(aFactor);
    }

    public void saveModel(String aFilename) throws IOException
    {
        mira.saveModel(aFilename);
    }

    public void loadTemplates(String aFilename) throws IOException
    {
        mira.loadTemplates(aFilename);
    }

    public void setClip(int aClip)
    {
        mira.setClip(aClip);
    }

    public void setMaxPosteriors(boolean aMaxPosteriors)
    {
        mira.maxPosteriors = aMaxPosteriors;
    }

    public void setBeamSize(int aBeamSize)
    {
        mira.beamSize = aBeamSize;
    }
}
