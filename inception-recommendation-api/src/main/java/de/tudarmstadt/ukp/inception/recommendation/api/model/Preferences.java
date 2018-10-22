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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import java.io.Serializable;

public class Preferences
    implements Serializable
{
    private static final long serialVersionUID = 979498856625982141L;

    private int maxPredictions = 3;
    private boolean showAllPredictions = false;

    public void setMaxPredictions(int aMaxPredictions)
    {
        maxPredictions = aMaxPredictions;
    }

    public int getMaxPredictions()
    {
        return maxPredictions;
    }

    public boolean isShowAllPredictions()
    {
        return showAllPredictions;
    }

    public void setShowAllPredictions(boolean aShowAllPredictions)
    {
        showAllPredictions = aShowAllPredictions;
    }
}
