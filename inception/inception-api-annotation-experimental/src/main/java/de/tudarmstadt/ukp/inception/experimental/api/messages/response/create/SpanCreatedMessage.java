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
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

/**
 * Class required for Messaging between Server and Client.
 * Basis for JSON
 * SpanCreatedMessage: Message published to clients that a Span annotation has been created
 *
 * Attributes:
 * @spanId: The ID of the new Span
 * @begin: The character offset begin of the span
 * @end: The character offset end of the span
 * @color: The color of the Arc
 * @layerId: The ID of the layer the Arc belongs to
 * @features: List of AnnotationFeatures that the Span has
 **/
public class SpanCreatedMessage
{
    private VID spanId;
    private int begin;
    private int end;
    private String color;
    private long layerId;
    private List<AnnotationFeature> features;

    public SpanCreatedMessage(VID aSpanId, int aBegin, int aEnd,
                              long aLayerId, String aColor, List<AnnotationFeature> aFeatures)
    {
        spanId = aSpanId;
        begin = aBegin;
        end = aEnd;
        layerId = aLayerId;
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

    public long getLayerId()
    {
        return layerId;
    }

    public void setLayerId(long aLayerId)
    {
        layerId = aLayerId;
    }

    public List<AnnotationFeature> getFeatures()
    {
        return features;
    }

    public void setFeatures(List<AnnotationFeature> aFeatures)
    {
        features = aFeatures;
    }
}
