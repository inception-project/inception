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
    private VID vidFirstSpan;
    private VID vidRelatedSpan;
    private String color;
    private String label;

    public Relation(VID aRelationId, VID aVidFirstSpan, VID aVidRelatedSpan, String aColor, String aLabel)
    {
        relationId = aRelationId;
        vidFirstSpan = aVidFirstSpan;
        vidRelatedSpan = aVidRelatedSpan;
        color = aColor;
        label = aLabel;
    }

    public VID getRelationId()
    {
        return relationId;
    }

    public void setRelationId(VID aRelationId)
    {
        relationId = aRelationId;
    }

    public VID getVidFirstSpan()
    {
        return vidFirstSpan;
    }

    public void setVidFirstSpan(VID aVidFirstSpan)
    {
        vidFirstSpan = aVidFirstSpan;
    }

    public VID getVidRelatedSpan()
    {
        return vidRelatedSpan;
    }

    public void setVidRelatedSpan(VID aVidRelatedSpan)
    {
        vidRelatedSpan = aVidRelatedSpan;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String aColor)
    {
        color = aColor;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String aLabel)
    {
        label = aLabel;
    }
}
