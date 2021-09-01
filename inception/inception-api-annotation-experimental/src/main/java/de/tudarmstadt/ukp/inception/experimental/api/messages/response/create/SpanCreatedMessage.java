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
package de.tudarmstadt.ukp.inception.experimental.api.messages.response.create;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class SpanCreatedMessage
{
    private VID spanId;
    private int begin;
    private int end;
    private String color;
    private String type;
    private List<String> features;

    public SpanCreatedMessage(VID aSpanId, int aBegin, int aEnd,
                              String aType, String aColor, List<String> aFeatures)
    {
        spanId = aSpanId;
        begin = aBegin;
        end = aEnd;
        type = aType;
        color = aColor;
        features = aFeatures;
    }

    public VID getSpanId()
    {
        return spanId;
    }

    public void setSpanId(VID aSpanId)
    {
        spanId = aSpanId;
    }

    public int getBegin()
    {
        return begin;
    }

    public void setBegin(int aBegin)
    {
        begin = aBegin;
    }

    public int getEnd()
    {
        return end;
    }

    public void setEnd(int aEnd)
    {
        end = aEnd;
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
