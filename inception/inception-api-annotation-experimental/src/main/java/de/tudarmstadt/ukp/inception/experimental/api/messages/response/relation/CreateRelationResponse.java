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
package de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class CreateRelationResponse
{
    private VID relationAddress;
    private String clientName;
    private String userName;
    private long projectId;
    private long documentId;
    private VID governorId;
    private VID dependentId;
    private String color;
    private String governorCoveredText;
    private String dependentCoveredText;
    private String type;
    private List<String> features;

    public CreateRelationResponse(VID aRelationAddress, String aClientName, String aUserName,
            long aProjectId, long aDocumentId, VID aGovernorId, VID aDependentId, String aColor,
            String aGovernorCoveredText, String aDependentCoveredText, String aType, List<String> aFeatures)
    {
        relationAddress = aRelationAddress;
        clientName = aClientName;
        userName = aUserName;
        projectId = aProjectId;
        documentId = aDocumentId;
        governorId = aGovernorId;
        dependentId = aDependentId;
        color = aColor;
        governorCoveredText = aGovernorCoveredText;
        dependentCoveredText = aDependentCoveredText;
        type = aType;
        features = aFeatures;
    }

    public VID getRelationAddress()
    {
        return relationAddress;
    }

    public void setRelationAddress(VID aRelationAddress)
    {
        relationAddress = aRelationAddress;
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

    public VID getGovernorId()
    {
        return governorId;
    }

    public void setGovernorId(VID aGovernorId)
    {
        governorId = aGovernorId;
    }

    public VID getDependentId()
    {
        return dependentId;
    }

    public void setDependentId(VID aDependentId)
    {
        dependentId = aDependentId;
    }

    public String getGovernorCoveredText()
    {
        return governorCoveredText;
    }

    public void setGovernorCoveredText(String aGovernorCoveredText)
    {
        governorCoveredText = aGovernorCoveredText;
    }

    public String getDependentCoveredText()
    {
        return dependentCoveredText;
    }

    public void setDependentCoveredText(String aDependentCoveredText)
    {
        dependentCoveredText = aDependentCoveredText;
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

    public List<String> getFeatures()
    {
        return features;
    }

    public void setFeatures(List<String> aFeatures)
    {
        features = aFeatures;
    }
}
