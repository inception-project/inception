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
package de.tudarmstadt.ukp.inception.experimental.api.messages.response.arc;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.inception.experimental.api.model.FeatureX;

public class SelectArcResponse
{
    private VID arcId;
    private VID sourceId;
    private String sourceCoveredText;
    private VID targetId;
    private String targetCoveredText;
    private String color;
    private String type;
    private List<FeatureX> features;

    public SelectArcResponse(VID aArcId, VID aSourceId, String aSourceCoveredText, VID aTargetId,
            String aTargetCoveredText, String aColor, String aType, List<FeatureX> aFeatures)
    {
        arcId = aArcId;
        sourceId = aSourceId;
        sourceCoveredText = aSourceCoveredText;
        targetId = aTargetId;
        targetCoveredText = aTargetCoveredText;
        color = aColor;
        type = aType;
        features = aFeatures;
    }

    public VID getArcId()
    {
        return arcId;
    }

    public void setArcId(VID aArcId)
    {
        arcId = aArcId;
    }

    public VID getSourceId()
    {
        return sourceId;
    }

    public void setSourceId(VID aSourceId)
    {
        sourceId = aSourceId;
    }

    public VID getTargetId()
    {
        return targetId;
    }

    public void setTargetId(VID aTargetId)
    {
        targetId = aTargetId;
    }

    public String getSourceCoveredText()
    {
        return sourceCoveredText;
    }

    public void setSourceCoveredText(String aSourceCoveredText)
    {
        sourceCoveredText = aSourceCoveredText;
    }

    public String getTargetCoveredText()
    {
        return targetCoveredText;
    }

    public void setTargetCoveredText(String aTargetCoveredText)
    {
        targetCoveredText = aTargetCoveredText;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String aColor)
    {
        color = aColor;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    public List<FeatureX> getFeatures()
    {
        return features;
    }

    public void setFeatures(List<FeatureX> aFeatures)
    {
        features = aFeatures;
    }
}
