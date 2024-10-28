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
package de.tudarmstadt.ukp.inception.curation.merge.strategy;

import java.io.Serializable;

public class ThresholdBasedMergeStrategyTraits
    implements Serializable
{
    private static final long serialVersionUID = -7084390245091025371L;

    private int userThreshold = 1;
    private double confidenceThreshold = 0.75d;
    private int topRanks = 1;

    public int getUserThreshold()
    {
        return userThreshold < 1 ? 1 : userThreshold;
    }

    public void setUserThreshold(int aUserThreshold)
    {
        userThreshold = aUserThreshold;
    }

    public double getConfidenceThreshold()
    {
        if (confidenceThreshold < 0) {
            return 0.0d;
        }

        if (confidenceThreshold > 1.0) {
            return 1.0d;
        }

        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double aConfidenceThreshold)
    {
        confidenceThreshold = aConfidenceThreshold;
    }

    public int getTopRanks()
    {
        return topRanks < 0 ? 0 : topRanks;
    }

    public void setTopRanks(int aTopRanks)
    {
        topRanks = aTopRanks;
    }
}
