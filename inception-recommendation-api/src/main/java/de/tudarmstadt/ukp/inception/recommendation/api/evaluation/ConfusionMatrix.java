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
package de.tudarmstadt.ukp.inception.recommendation.api.evaluation;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ConfusionMatrix implements Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = -5181354025073954428L;
    /**
     * Stores number of predicted labels for each gold label
     */
    private Object2IntOpenHashMap<ConfMatrixKey> confusionMatrix;
    private Set<String> labels;
    private int total;

    public ConfusionMatrix()
    {
        confusionMatrix = new Object2IntOpenHashMap<>();
        labels = new LinkedHashSet<>();
    }
    
    
    public int getEntryCount(String aPredictedLabel, String aGoldLabel) {
        return confusionMatrix.getInt(new ConfMatrixKey(aGoldLabel, aPredictedLabel));
    }
    
    public boolean containsEntry(String aPredictedLabel, String aGoldLabel) {
        return confusionMatrix.containsKey(new ConfMatrixKey(aGoldLabel, aPredictedLabel));
    }
    
    /**
     * Increment the confusion matrix entries according to a result with the given predicted and the
     * given gold label.
     */
    public void incrementCounts(String aPredictedLabel, String aGoldLabel)
    {
        labels.add(aGoldLabel);
        labels.add(aPredictedLabel);

        total++;

        // annotated pair is true positive
        if (aGoldLabel.equals(aPredictedLabel)) {
            confusionMatrix.addTo(new ConfMatrixKey(aGoldLabel, aGoldLabel), 1);
        }
        else {
            // annotated pair is false negative for gold class = annotated pair is false
            // positive for predicted class
            confusionMatrix.addTo(new ConfMatrixKey(aGoldLabel, aPredictedLabel), 1);
        }
    }

    public int getTotal()
    {
        return total;
    }

    public Set<String> getLabels()
    {
        return labels;
    }

    public Object2IntOpenHashMap<ConfMatrixKey> getConfusionMatrix()
    {
        return confusionMatrix;
    }

    public void addMatrix(ConfusionMatrix aMatrix) {
        for (Entry<ConfMatrixKey> entry : aMatrix.getConfusionMatrix().object2IntEntrySet()) {
            confusionMatrix.addTo(entry.getKey(), entry.getIntValue());
        }
    }

    @Override
    public String toString()
    {
        StringBuilder matrixStr = new StringBuilder();
        // header
        matrixStr.append("Gold\\Predicted\n\t");
        labels.forEach(l -> {
            matrixStr.append(l);
            matrixStr.append("  ");
        });
        matrixStr.append("\n");
        // table
        for (String goldLabel : labels) {
            matrixStr.append(goldLabel);
            matrixStr.append("\t| ");
            for (String predictedLabel : labels) {
                matrixStr.append(
                        confusionMatrix.getInt(new ConfMatrixKey(goldLabel, predictedLabel)));
                matrixStr.append("\t| ");
            }
            matrixStr.append("\n");
        }
        return matrixStr.toString();
    }

    /**
     * Key identifying a confusion-matrix entry by predicted and gold label.
     */
    protected class ConfMatrixKey implements Serializable
    {
        /**
         * 
         */
        private static final long serialVersionUID = 7241471544567740440L;
        private String predictedLabel;
        private String goldLabel;
        
        public ConfMatrixKey(String aGoldLabel, String aPredictedLabel)
        {
            predictedLabel = aPredictedLabel;
            goldLabel = aGoldLabel;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(predictedLabel, goldLabel);
        }

        @Override
        public boolean equals(Object aObj)
        {
            if (aObj == null || getClass() != aObj.getClass()) {
                return false;
            }

            ConfMatrixKey aKey = (ConfMatrixKey) aObj;
            return predictedLabel.equals(aKey.getPredictedLabel())
                    && goldLabel.equals(aKey.getGoldLabel());
        }

        public String getPredictedLabel()
        {
            return predictedLabel;
        }

        public String getGoldLabel()
        {
            return goldLabel;
        }
    }


}
