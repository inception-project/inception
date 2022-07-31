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
package de.tudarmstadt.ukp.inception.diam.model.compact;

import static java.util.Arrays.asList;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VTextMarker;
import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "type", "offsets" })
public class CompactTextMarker
{
    private List<CompactRange> offsets;
    private String type;

    public CompactTextMarker(CompactRange aVid, String aType)
    {
        offsets = asList(aVid);
        type = aType;
    }

    public CompactTextMarker(List<CompactRange> aVid, String aType)
    {
        offsets = aVid;
        type = aType;
    }

    public CompactTextMarker(VTextMarker aTextMarker)
    {
        var range = aTextMarker.getRange();
        offsets = asList(new CompactRange(range.getBegin(), range.getEnd()));
        type = aTextMarker.getType();
    }

    public void setOffsets(List<CompactRange> aVid)
    {
        offsets = aVid;
    }

    public List<CompactRange> getVid()
    {
        return offsets;
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
