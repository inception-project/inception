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
package de.tudarmstadt.ukp.clarin.webanno.brat.render.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "begin", "end" })
public class TextMarker
    implements Marker
{
    private @JsonIgnore String type;
    private int begin;
    private int end;

    public TextMarker(String aType, int aBegin, int aEnd)
    {
        type = aType;
        begin = aBegin;
        end = aEnd;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    @Override
    public String getType()
    {
        return type;
    }
}
