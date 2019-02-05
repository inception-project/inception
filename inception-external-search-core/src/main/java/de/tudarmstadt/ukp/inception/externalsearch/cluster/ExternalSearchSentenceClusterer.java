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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.text.similarity.CosineDistance;

public class ExternalSearchSentenceClusterer {
    private CosineDistance cd;
    private double clusterDistanceTolerance;
    private List<Set<Triple<String, Double, String>>> sentenceClusters;
    
    public ExternalSearchSentenceClusterer()
    {
        cd = new CosineDistance();
        sentenceClusters = new ArrayList<>();
        clusterDistanceTolerance = 0.3;
    }
    
    public void cluster(List<Triple<String, Double, String>> sentences)
    {
        for (Triple<String, Double, String> sentence: sentences) {
            if (sentenceClusters.size() == 0) {
                Set<Triple<String, Double, String>> firstCluster = new HashSet<>();
                firstCluster.add(sentence);
                sentenceClusters.add(firstCluster);
            } else {
                // One giant cluster could be generated, bestDistance can make it grow element-wise.
                // Should use average!
                double bestDistance = Double.MAX_VALUE;
                Set<Triple<String, Double, String>> bestCluster = new HashSet<>();
                for (Set<Triple<String, Double, String>> cluster : sentenceClusters) {
                    List<Double> cmpDistances = new ArrayList<>();
                    for (Triple<String, Double, String> compareSentence : cluster) {
                        cmpDistances.add(cd.apply(compareSentence.getLeft(), sentence.getLeft()));
                    }
                    if (bestDistance > Collections.min(cmpDistances)) {
                        bestDistance = Collections.min(cmpDistances);
                        bestCluster = cluster;
                    }
                }
    
                if (bestDistance < clusterDistanceTolerance) {
                    bestCluster.add(sentence);
                } else {
                    Set<Triple<String, Double, String>> newCluster = new HashSet<>();
                    newCluster.add(sentence);
                    sentenceClusters.add(newCluster);
                }
            }
        }
    }
    
    public List<Set<Triple<String, Double, String>>> getSentenceClusters()
    {
        return sentenceClusters;
    }
}
