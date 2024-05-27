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
package de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.inception.rendering.model.Range;

public class Metadata
{
    private final Range range;
    private final String layer;
    private final String feature;
    private final long projectId;
    private final String anchoringMode;
    private final boolean crossSentence;

    public Metadata(@JsonProperty(value = "layer", required = true) String aLayer,
            @JsonProperty(value = "feature", required = true) String aFeature,
            @JsonProperty(value = "projectId", required = true) long aProjectId,
            @JsonProperty(value = "anchoringMode", required = true) String aAnchoringMode,
            @JsonProperty(value = "crossSentence", required = true) boolean aCrossSentence,
            @JsonProperty(value = "range", required = true) Range aRange)
    {
        layer = aLayer;
        feature = aFeature;
        projectId = aProjectId;
        anchoringMode = aAnchoringMode;
        crossSentence = aCrossSentence;
        range = aRange;
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

    public Range getRange()
    {
        return range;
    }
}
