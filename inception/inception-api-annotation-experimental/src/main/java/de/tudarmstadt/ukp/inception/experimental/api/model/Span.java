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

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

/**
 * Support Class representing an Span annotation
 *
 * Attributes:
 * @id: The ID of the span
 * @begin: The character offset begin of the Span
 * @end: The character offset end of the Span
 * @layerId: The ID of the layer the Span belongs to
 * @features: List of annotation features of the Span
 * @color: Color of the Span
 **/
public class Span
{
    private VID id;
    private int begin;
    private int end;
    private long layerId;
    private List<AnnotationFeature> features;
    private String color;

    public Span(VID aId, int aBegin, int aEnd, long aLayerId, String aColor,
            List<AnnotationFeature> aFeatures)
    {
        id = aId;
        begin = aBegin;
        end = aEnd;
        layerId = aLayerId;
        color = aColor;
        features = aFeatures;
    }

    public VID getId()
    {
        return id;
    }

    public void setId(VID aId)
    {
        id = aId;
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

    public long getLayerId()
    {
        return layerId;
    }

    public void setLayerId(long aLayerId)
    {
        layerId = aLayerId;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String aColor)
    {
        color = aColor;
    }

    public List<AnnotationFeature> getFeatures()
    {
        return features;
    }

    public void setFeature(List<AnnotationFeature> aFeatures)
    {
        features = aFeatures;
    }
}
