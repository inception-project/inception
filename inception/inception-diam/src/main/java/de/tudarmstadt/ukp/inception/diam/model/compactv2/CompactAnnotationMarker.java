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
package de.tudarmstadt.ukp.inception.diam.model.compactv2;

import static java.util.Arrays.asList;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VAnnotationMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "type", "vids" })
public class CompactAnnotationMarker
{
    private List<VID> vids;
    private String type;

    public CompactAnnotationMarker(VID aVid, String aType)
    {
        vids = asList(aVid);
        type = aType;
    }

    public CompactAnnotationMarker(List<VID> aVid, String aType)
    {
        vids = aVid;
        type = aType;
    }

    public CompactAnnotationMarker(VAnnotationMarker aAnnotationMarker)
    {
        vids = asList(aAnnotationMarker.getVid());
        type = aAnnotationMarker.getType();
    }

    public void setVids(List<VID> aVid)
    {
        vids = aVid;
    }

    public List<VID> getVids()
    {
        return vids;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    public String getType()
    {
        return type;
    }
}
