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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

import java.util.List;

public class AnnotationJSONObject
    extends JSONOutput
{

    private int begin;
    private int end;
    private int annotationId;
    private List<Long> coveredTokens;
    private long layerId;
    private List<FeatureRef<Object>> features;

    public int getBegin()
    {
        return begin;
    }

    public void setBegin(int begin)
    {
        this.begin = begin;
    }

    public int getEnd()
    {
        return end;
    }

    public void setEnd(int end)
    {
        this.end = end;
    }

    public int getAnnotationId()
    {
        return annotationId;
    }

    public void setAnnotationId(int annotationId)
    {
        this.annotationId = annotationId;
    }

    public List<Long> getCoveredTokens()
    {
        return coveredTokens;
    }

    public void setCoveredTokens(List<Long> coveredTokens)
    {
        this.coveredTokens = coveredTokens;
    }

    public long getLayerId()
    {
        return layerId;
    }

    public void setLayerId(long layerId)
    {
        this.layerId = layerId;
    }

    public List<FeatureRef<Object>> getFeatures()
    {
        return features;
    }

    public void setFeatures(List<FeatureRef<Object>> features)
    {
        this.features = features;
    }

}
