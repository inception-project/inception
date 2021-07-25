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

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class SelectRelationResponse
{
    private VID relationAddress;
    private String governorCoveredText;
    private String dependentCoveredText;
    private String flavor;
    private String relation;

    public SelectRelationResponse(VID aRelationAddress, String aGovernorCoveredText,
            String aDependentCoveredText, String aFlavor, String aRelation)
    {
        relationAddress = aRelationAddress;
        governorCoveredText = aGovernorCoveredText;
        dependentCoveredText = aDependentCoveredText;
        flavor = aFlavor;
        relation = aRelation;
    }

    public VID getRelationAddress()
    {
        return relationAddress;
    }

    public void setRelationAddress(VID aRelationAddress)
    {
        relationAddress = aRelationAddress;
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

    public String getFlavor()
    {
        return flavor;
    }

    public void setFlavor(String aFlavor)
    {
        flavor = aFlavor;
    }

    public String getRelation()
    {
        return relation;
    }

    public void setRelation(String aRelation)
    {
        relation = aRelation;
    }
}
