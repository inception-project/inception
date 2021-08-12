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

public class CreateArcMessage
{
    private VID arcId;
    private String clientName;
    private String userName;
    private long projectId;
    private long documentId;
    private VID sourceId;
    private VID targetId;
    private String color;
    private String sourceCoveredText;
    private String targetCoveredText;
    private String type;
    private List<FeatureX> features;

    public CreateArcMessage(VID aArcId, String aClientName, String aUserName, long aProjectId,
            long aDocumentId, VID aSourceId, VID aTargetId, String aColor,
            String aSourceCoveredText, String aTargetCoveredText, String aType,
            List<FeatureX> aFeatures)
    {
        arcId = aArcId;
        clientName = aClientName;
        userName = aUserName;
        projectId = aProjectId;
        documentId = aDocumentId;
        sourceId = aSourceId;
        targetId = aTargetId;
        color = aColor;
        sourceCoveredText = aSourceCoveredText;
        targetCoveredText = aTargetCoveredText;
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

    public String getClientName()
    {
        return clientName;
    }

    public void setClientName(String aClientName)
    {
        clientName = aClientName;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String aUserName)
    {
        userName = aUserName;
    }

    public long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(long aProjectId)
    {
        projectId = aProjectId;
    }

    public long getDocumentId()
    {
        return documentId;
    }

    public void setDocumentId(long aDocumentId)
    {
        documentId = aDocumentId;
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
