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
package de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;

public class ExtendedResult
    implements Serializable
{
    private static final long serialVersionUID = -4720786772437178421L;

    private final double fScore;
    private final double precision;
    private final double recall;
    private long iterationNumber;
    private long trainingDuration;
    private long classifyingDuration;
    
    // These annotations are micro annotations, e.g. POS or NEs within the sentences
    private long trainingSetAnnotationCount = 0;
    private long expectedAnnotationCount = 0;
    private long actualAnnotationCount = 0;
    
    // The training/test set size is measured in sentences
    private long trainingSetSize;
    private long testSetSize;
    
    // The split on the sentence level
    private double split;
    private boolean shuffleTrainingSet;

    private long tp = 0;
    private long fp = 0;
    private long tn = 0;
    private long fn = 0;
    
    private Set<String> labels = new HashSet<String>();
    private LabelResult labelResult;
    
    /**
     * 
     * @param expected - List of sentences including annotated words
     * @param actual - List of sentences including annotated words
     */
    public ExtendedResult(List<List<AnnotationObject>> expected,
            List<List<AnnotationObject>> actual)
    {
        Validate.notNull(expected, "Expected sequences must not be null");
        Validate.notNull(actual, "Actual sequences must not be null");
        
        if (expected.size() != actual.size()) {
            throw new IllegalArgumentException("Number of expected [" + expected.size()
                    + "] and actual [" + actual.size() + "] sequences is not equal!");
        }
        
        long total = 0;
        for (int sentenceNr = 0; sentenceNr < expected.size(); sentenceNr++) {
            List<AnnotationObject> expectedSentence = expected.get(sentenceNr);
            List<AnnotationObject> actualSentence = actual.get(sentenceNr);

            if (expectedSentence.size() != actualSentence.size()) {
                throw new IllegalArgumentException("Number of expected [" + expectedSentence.size()
                        + "] and actual [" + actualSentence.size() + "] tokens is not equal!");
            }
            
            for (int i = 0; i < actualSentence.size(); i++) {
                String aoActual = actualSentence.get(i).getLabel();
                String aoExpected = expectedSentence.get(i).getLabel();
   
                total++;
                
                if (StringUtils.isNotEmpty(aoActual)) {
                    actualAnnotationCount++;
                    labels.add(aoActual);
                }
    
                if (StringUtils.isNotEmpty(aoExpected)) {
                    expectedAnnotationCount++;
                    labels.add(aoExpected);
                }
    
                if (
                        StringUtils.isNotEmpty(aoActual) && 
                        StringUtils.isNotEmpty(aoExpected) && 
                        aoActual.equals(aoExpected)
                ) {
                    tp++;
                }
            }
        }
        
        if (actualAnnotationCount == 0) {
            precision = 0;
        }
        else {
            precision = (double) tp / (double) actualAnnotationCount;
        }

        if (expectedAnnotationCount == 0) {
            recall = 0;
        }
        else {
            recall = (double) tp / (double) expectedAnnotationCount;
        }

        if (precision + recall == 0) {
            fScore = -1;
        }
        else {
            fScore = 2 * precision * recall / (precision + recall);
        }
        
        // Everything that the classifier predicted wrongly is a FP
        fp = actualAnnotationCount - tp;
        
        // Everything that the classifier did not predict at all is a FN
        fn = expectedAnnotationCount - tp;
        
        // Everything that the classifier did not predict and is not in the gold standard is a TN
        tn = total - tp - fp - fn;
        
        Validate.isTrue(fp >= 0, "FP cannot be negative: %d", fp);
        Validate.isTrue(fn >= 0, "FN cannot be negative: %d", fn);
        Validate.isTrue(tp >= 0, "FP cannot be negative: %d", tp);
        Validate.isTrue(tn >= 0, "TN cannot be negative: %d", tn);
        
        labelResult = new LabelResult(expected, actual, labels);
    }

    
    public long getTp()
    {
        return tp;
    }

    public long getFp()
    {
        return fp;
    }

    public long getTn()
    {
        return tn;
    }

    public long getFn()
    {
        return fn;
    }

    /**
     * @return number of annotations in the test set.
     */
    public long getTestSetAnnotationCount()
    {
        return expectedAnnotationCount;
    }

    /**
     * @return number of annotations produced by the classifier on the test set.
     */
    public long getClassifierTestResultAnnotationCount()
    {
        return actualAnnotationCount;
    }
    
    public double getFscore()
    {
        return fScore;
    }
    
    public double getPrecision()
    {
        return precision;
    }

    public double getRecall()
    {
        return recall;
    }
    
    public long getIterationNumber()
    {
        return iterationNumber;
    }

    public void setIterationNumber(long iterationNumber)
    {
        this.iterationNumber = iterationNumber;
    }

    public long getTrainingDuration()
    {
        return trainingDuration;
    }

    public String getTrainingDuration(String format)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date d = new Date(trainingDuration);
        return sdf.format(d);
    }

    public void setTrainingDuration(long processingDuration)
    {
        this.trainingDuration = processingDuration;
    }

    public long getClassifyingDuration()
    {
        return classifyingDuration;
    }

    public String getClassifyingDuration(String format)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date d = new Date(classifyingDuration);
        return sdf.format(d);
    }

    public void setClassifyingDuration(long classifyingDuration)
    {
        this.classifyingDuration = classifyingDuration;
    }

    /**
     * @return number of sentences in the training set.
     */
    public long getTrainingSetSize()
    {
        return trainingSetSize;
    }

    public void setTrainingSetSize(long trainingSetSize)
    {
        this.trainingSetSize = trainingSetSize;
    }
    
    /**
     * @return number of sentences in the test set.
     */
    public long getTestSetSize()
    {
        return testSetSize;
    }

    public void setTestSetSize(long aTestSetSize)
    {
        testSetSize = aTestSetSize;
    }

    public LabelResult getLabelResult() {
        return labelResult;
    }

    public double getSplit()
    {
        return split;
    }

    public void setSplit(double aSplit)
    {
        split = aSplit;
    }

    public void setShuffleTrainingSet(boolean aShuffleTrainingSet)
    {
        shuffleTrainingSet = aShuffleTrainingSet;
    }
    
    public boolean isShuffleTrainingSet()
    {
        return shuffleTrainingSet;
    }

    public long getTrainingSetAnnotationCount()
    {
        return trainingSetAnnotationCount;
    }

    public void setTrainingSetAnnotationCount(long aTrainingSetAnnotationCount)
    {
        trainingSetAnnotationCount = aTrainingSetAnnotationCount;
    }
}
