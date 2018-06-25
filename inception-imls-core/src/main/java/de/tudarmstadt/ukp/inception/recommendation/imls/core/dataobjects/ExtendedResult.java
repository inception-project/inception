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
    private int trainingSetSize;

    private int tp = 0;
    private int fp = 0;
    private int tn = 0;
    private int fn = 0;
    private int expectedAnnotationCount = 0;
    private int actualAnnotationCount = 0;
    
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
        
        for (int sentenceNr = 0; sentenceNr < expected.size(); sentenceNr++) {
            List<AnnotationObject> expectedSentence = expected.get(sentenceNr);
            List<AnnotationObject> actualSentence = actual.get(sentenceNr);

            if (expectedSentence.size() != actualSentence.size()) {
                throw new IllegalArgumentException("Number of expected [" + expectedSentence.size()
                        + "] and actual [" + actualSentence.size() + "] tokens is not equal!");
            }
            
            for (int i = 0; i < actualSentence.size(); i++) {
                String aoActual = actualSentence.get(i).getAnnotation();
                String aoExpected = expectedSentence.get(i).getAnnotation();
    
                if (aoActual != null && !aoActual.isEmpty()) {
                    actualAnnotationCount++;
                    labels.add(aoActual);
                }
    
                if (aoExpected != null && !aoExpected.isEmpty()) {
                    expectedAnnotationCount++;
                    labels.add(aoExpected);
                }
    
                if (aoActual != null && !aoActual.isEmpty() && aoExpected != null
                        && !aoExpected.isEmpty() && aoActual.equals(aoExpected)) {                
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
        
        fp = actualAnnotationCount - tp;
        fn = expectedAnnotationCount - tp;
        tn = expected.size() - tp - fp - fn;
        
        labelResult = new LabelResult(expected, actual, labels);
    }

    
    public int getTp()
    {
        return tp;
    }

    public int getFp()
    {
        return fp;
    }

    public int getTn()
    {
        return tn;
    }

    public int getFn()
    {
        return fn;
    }

    public int getExpectedAnnotationCount()
    {
        return expectedAnnotationCount;
    }

    public int getActualAnnotationCount()
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

    public int getTrainingSetSize()
    {
        return trainingSetSize;
    }

    public void setTrainingSetSize(int trainingSetSize)
    {
        this.trainingSetSize = trainingSetSize;
    }
    
    public LabelResult getLabelResult() {
        return labelResult;
    }

}
