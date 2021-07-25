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

public class UpdateRelationResponse
{
    private VID relationAddress;
    private String newDependencyType;
    private String newFlavor;
    private String color;

    public UpdateRelationResponse(VID aRelationAddress, String aNewDependencyType, String aNewFlavor, String aColor)
    {
        relationAddress = aRelationAddress;
        newDependencyType = aNewDependencyType;
        newFlavor = aNewFlavor;
        color = aColor;
    }

    public VID getRelationAddress()
    {
        return relationAddress;
    }

    public void setRelationAddress(VID aRelationAddress)
    {
        relationAddress = aRelationAddress;
    }

    public String getNewDependencyType()
    {
        return newDependencyType;
    }

    public void setNewDependencyType(String aNewDependencyType)
    {
        newDependencyType = aNewDependencyType;
    }

    public String getNewFlavor()
    {
        return newFlavor;
    }

    public void setNewFlavor(String aNewFlavor)
    {
        newFlavor = aNewFlavor;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String aColor)
    {
        color = aColor;
    }
}
