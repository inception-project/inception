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
package de.tudarmstadt.ukp.inception.recommendation.api.evaluation;

/**
 * Holds information on an annotated unit: its gold label and its predicted label.
 */
public class LabelPair
{
    private final String unit;
    private final String goldLabel;
    private final String predictedLabel;

    public LabelPair(String aGoldLabel, String aPredictedLabel)
    {
        this(null, aGoldLabel, aPredictedLabel);
    }

    public LabelPair(String aUnit, String aGoldLabel, String aPredictedLabel)
    {
        unit = aUnit;
        goldLabel = aGoldLabel;
        predictedLabel = aPredictedLabel;
    }

    public String getUnit()
    {
        return unit;
    }

    public String getGoldLabel()
    {
        return goldLabel;
    }

    public String getPredictedLabel()
    {
        return predictedLabel;
    }

    @Override
    public String toString()
    {
        return "[" + goldLabel + " - " + predictedLabel
                + (unit != null ? (" : " + unit + "]") : "]");
    }
}
