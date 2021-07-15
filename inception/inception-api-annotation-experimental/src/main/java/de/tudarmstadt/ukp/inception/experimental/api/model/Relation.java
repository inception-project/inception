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
    private VID relationId;
    private VID vidGovernor;
    private VID vidDependent;
    private String color;
    private String dependencyType;
    private String flavor;

    public Relation(VID aRelationId, VID aVidGovernor, VID aVidDependent, String aColor,
            String aDependencyType, String aFlavor)
    {
        relationId = aRelationId;
        vidGovernor = aVidGovernor;
        vidDependent = aVidDependent;
        color = aColor;
        dependencyType = aDependencyType;
        flavor = aFlavor;
    }

    public VID getRelationId()
    {
        return relationId;
    }

    public void setRelationId(VID aRelationId)
    {
        relationId = aRelationId;
    }

    public VID getVidGovernor()
    {
        return vidGovernor;
    }

    public void setVidGovernor(VID aVidGovernor)
    {
        vidGovernor = aVidGovernor;
    }

    public VID getVidDependent()
    {
        return vidDependent;
    }

    public void setVidDependent(VID aVidDependent)
    {
        vidDependent = aVidDependent;
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
