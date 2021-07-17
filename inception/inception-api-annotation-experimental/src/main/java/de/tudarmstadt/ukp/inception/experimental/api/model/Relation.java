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
package de.tudarmstadt.ukp.inception.experimental.api.model;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class Relation
{
    private VID id;
    private VID governorId;
    private VID dependentId;
    private String governorCoveredText;
    private String dependentCoveredText;
    private String color;
    private String dependencyType;
    private String flavor;

    public Relation(VID aId, VID aGovernorId, VID aDependentId, String aColor,
            String aDependencyType, String aFlavor, String aGovernorCoveredText,
            String aDependentCoveredText)
    {
        id = aId;
        governorId = aGovernorId;
        dependentId = aDependentId;
        color = aColor;
        dependencyType = aDependencyType;
        flavor = aFlavor;
        governorCoveredText = aGovernorCoveredText;
        dependentCoveredText = aDependentCoveredText;
    }

    public VID getId()
    {
        return id;
    }

    public void setId(VID aId)
    {
        id = aId;
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

    public String getDependencyType()
    {
        return dependencyType;
    }

    public void setDependencyType(String aDependencyType)
    {
        dependencyType = aDependencyType;
    }

    public String getFlavor()
    {
        return flavor;
    }

    public void setFlavor(String aFlavor)
    {
        flavor = aFlavor;
    }
}
