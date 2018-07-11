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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;

public class LabelResult implements Serializable
{
    private static final long serialVersionUID = 5321986223804608554L;
    
    private final Map<String, Double> fScore = new HashMap<>();
    private final Map<String, Double> precision = new HashMap<>();
    private final Map<String, Double> recall = new HashMap<>();
    private long iterationNumber;
    private long trainingDuration;
    private long classifyingDuration;
    private int trainingSetSize;

    private int[][] counts;
    private int[] totalGenerated;
    private int[] totalExpected;
    
    private Map<String, Integer> dict = new HashMap<>();
    private List<String> labels;
    
    public LabelResult(List<List<AnnotationObject>> expected,
            List<List<AnnotationObject>> generated, Set<String> labels)
    {
        this.labels = new ArrayList<String>(labels);
        java.util.Collections.sort(this.labels);
        counts = new int[labels.size()][labels.size()];
        totalGenerated = new int[labels.size()];
        totalExpected = new int[labels.size()];
        
        if (expected != null && generated != null && expected.size() != generated.size()) {
            throw new IllegalStateException(
                    "Expected and actual list size is not equal! This seems wrong.");
        }
        
        for (int sentenceNr = 0; sentenceNr < expected.size(); sentenceNr++) {
            List<AnnotationObject> expectedSentence = expected.get(sentenceNr);
            List<AnnotationObject> actualSentence = generated.get(sentenceNr);
            
            assert expectedSentence.size() == actualSentence.size() :
                "Expected sentence and actual sentence size should be equal! Seems there "
                        + "is something wrong.";
            
            for (int i = 0; i < actualSentence.size(); i++) {
                String aoGenerated = actualSentence.get(i).getLabel();
                String aoExpected = expectedSentence.get(i).getLabel();
    
                if (aoGenerated != null && !aoGenerated.isEmpty()) {
                    totalGenerated[getId(aoGenerated)]++;
                }
    
                if (aoExpected != null && !aoExpected.isEmpty()) {
                    totalExpected[getId(aoExpected)]++;
                }
                
                if (aoGenerated != null && !aoGenerated.isEmpty() && aoExpected != null
                        && !aoExpected.isEmpty()) {                
                    counts[getId(aoExpected)][getId(aoGenerated)]++;
                }
            }
        }

        calculatePrecision();
        calculateRecall();
        calculateFScore();

    }
    
    public int[][] getCounts() {
        return counts;
    }

    private void calculatePrecision()
    {
        for (String label: labels) {
            if (totalGenerated[getId(label)] == 0)  {
                precision.put(label, 0.);
            } else {
                precision.put(label, ((double) tp(label) / (double) totalGenerated[getId(label)])); 
            }
        }
    }
    
    private void calculateRecall()
    {
        for (String label: labels) {
            if (totalExpected[getId(label)] == 0)  {
                recall.put(label, 0.);
            } else {
                recall.put(label, ((double) tp(label) / (double) totalExpected[getId(label)])); 
            }
        }
    }

    private void calculateFScore()
    {
        for (String label: labels) {
            if (precision.get(label) + recall.get(label) == 0)  {
                fScore.put(label, -1.);
            } else {
                fScore.put(label, 2 * precision.get(label) * recall.get(label) 
                        / (precision.get(label) + recall.get(label))); 
            }
        }
    }

    private int tp(String label) {
        return counts[getId(label)][getId(label)];
    }
    
    public int getId (String label) 
    {
        if (dict.get(label) != null) {
            return dict.get(label);
        }
        
        for (int i = 0; i < labels.size(); i++) {
            if (label.equals(labels.get(i))) {
                dict.put(label, i);
                return i;
            }
        }
        return -1;
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

    public List<String> getLabels () {
        return labels;
    }
    
    public Map<String, Double> getFScore() {
        return fScore;
    }
    
    public Map<String, Double> getPrecision() {
        return precision;
    }
    
    public Map<String, Double> getRecall() {
        return recall;
    }
    
    public int getSummedExpectedforLabel(int row)
    {
        int sum = 0;
        for (int i : counts[row]) {
            sum += i;
        }

        if (sum == 0) {
            return 1;
        }
        return sum;
    }
    
    public int getSummedGeneratedforLabel(int col)
    {
        int sum = 0;
        for (int[] i : counts) {
            sum += i[col];
        }

        if (sum == 0) {
            return 1;
        }
        return sum;
    }
}
