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
package de.tudarmstadt.ukp.inception.recommendation.exporter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedRecommender
{
    @JsonProperty("name")
    private String name;

    @JsonProperty("feature")
    private String feature;

    @JsonProperty("layerName")
    private String layerName;

    @JsonProperty("tool")
    private String tool;

    @JsonProperty("threshold")
    private double threshold;

    @JsonProperty("alwaysSelected")
    private boolean alwaysSelected;

    @JsonProperty("skipEvaluation")
    private boolean skipEvaluation;
    
    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("maxRecommendations")
    private int maxRecommendations;
    
    @JsonProperty("traits")
    private String traits;

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public String getFeature()
    {
        return feature;
    }

    public void setFeature(String aFeature)
    {
        feature = aFeature;
    }

    public String getLayerName()
    {
        return layerName;
    }

    public void setLayerName(String aLayerName)
    {
        layerName = aLayerName;
    }

    public String getTool()
    {
        return tool;
    }

    public void setTool(String aTool)
    {
        tool = aTool;
    }

    public double getThreshold()
    {
        return threshold;
    }

    public void setThreshold(double aThreshold)
    {
        threshold = aThreshold;
    }

    public boolean isAlwaysSelected()
    {
        return alwaysSelected;
    }

    public void setAlwaysSelected(boolean aAlwaysSelected)
    {
        alwaysSelected = aAlwaysSelected;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean aEnabled)
    {
        enabled = aEnabled;
    }

    public boolean isSkipEvaluation()
    {
        return skipEvaluation;
    }

    public void setSkipEvaluation(boolean aSkipEvaluation)
    {
        skipEvaluation = aSkipEvaluation;
    }

    public int getMaxRecommendations()
    {
        return maxRecommendations;
    }

    public void setMaxRecommendations(int aMaxRecommendations)
    {
        maxRecommendations = aMaxRecommendations;
    }

    public String getTraits()
    {
        return traits;
    }

    public void setTraits(String aTraits)
    {
        traits = aTraits;
    }
}
