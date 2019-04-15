/*
 * Copyright 2019
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

package de.tudarmstadt.ukp.inception.externalsearch.cluster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.text.similarity.CosineDistance;

public class ExternalSearchSentenceClusterer {
    private CosineDistance cd;
    private double clusterDistanceTolerance;
    
    public ExternalSearchSentenceClusterer()
    {
        cd = new CosineDistance();
        clusterDistanceTolerance = 0.3;
    }
    
    public List<Set<ExtractedUnit>> getSentenceClusters (List<ExtractedUnit> sentences)
    {
        List<Set<ExtractedUnit>> sentenceClusters = new ArrayList<>();
        
        for (ExtractedUnit sentence: sentences) {
            if (sentenceClusters.size() == 0) {
                Set<ExtractedUnit> firstCluster = new HashSet<>();
                firstCluster.add(sentence);
                sentenceClusters.add(firstCluster);
            } else {
                // One giant cluster could be generated, bestDistance can make it grow element-wise.
                // Should use average!
                double bestDistance = Double.MAX_VALUE;
                
                
                
                Set<ExtractedUnit> bestCluster = new HashSet<>();
                for (Set<ExtractedUnit> cluster : sentenceClusters) {
                    double minDistance = cluster.stream()
                            .mapToDouble(unit -> cd.apply(sentence.getText(), unit.getText()))
                            .min().getAsDouble();
                    
                    if (minDistance < bestDistance) {
                        bestDistance = minDistance;
                        bestCluster = cluster;
                    }
                }
    
                if (bestDistance < clusterDistanceTolerance) {
                    bestCluster.add(sentence);
                }
                else {
                    Set<ExtractedUnit> newCluster = new HashSet<>();
                    newCluster.add(sentence);
                    sentenceClusters.add(newCluster);
                }
            }
        }
        return sentenceClusters;
    }
}
