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
package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Metadata {

    private final String layer;
    private final String feature;
    private final long projectId;
    private final String anchoringMode;
    private final boolean crossSentence;

    public Metadata(@JsonProperty("layer") String aLayer,
                    @JsonProperty("feature") String aFeature,
                    @JsonProperty("projectId") long aProjectId,
                    @JsonProperty("anchoringMode") String aAnchoringMode,
                    @JsonProperty("crossSentence") boolean aIsCrossSentence) {
        layer = aLayer;
        feature = aFeature;
        projectId = aProjectId;
        anchoringMode = aAnchoringMode;
        crossSentence = aIsCrossSentence;
    }

    public String getLayer()
    {
        return layer;
    }

    public String getFeature()
    {
        return feature;
    }

    public long getProjectId()
    {
        return projectId;
    }

    public String getAnchoringMode()
    {
        return anchoringMode;
    }

    public boolean isCrossSentence()
    {
        return crossSentence;
    }
}
