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

import java.util.ArrayList;

public class AnnotationLayerJSONObject
    extends JSONOutput
{
    private long layerId;
    private String uiName;
    private String name;
    private String type;
    private String description;
    private boolean readonly;
    private boolean crossSentence;
    private boolean allowStacking;
    private boolean multipleTokens;
    private boolean partialTokenCovering;
    private ArrayList<FeatureInfo> features;

    public long getLayerId()
    {
        return layerId;
    }

    public void setLayerId(long layerId)
    {
        this.layerId = layerId;
    }

    public String getUiName()
    {
        return uiName;
    }

    public void setUiName(String uiName)
    {
        this.uiName = uiName;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public boolean isReadonly()
    {
        return readonly;
    }

    public void setReadonly(boolean readonly)
    {
        this.readonly = readonly;
    }

    public boolean isCrossSentence()
    {
        return crossSentence;
    }

    public void setCrossSentence(boolean crossSentence)
    {
        this.crossSentence = crossSentence;
    }

    public boolean isAllowStacking()
    {
        return allowStacking;
    }

    public void setAllowStacking(boolean allowStacking)
    {
        this.allowStacking = allowStacking;
    }

    public boolean isMultipleTokens()
    {
        return multipleTokens;
    }

    public void setMultipleTokens(boolean multipleTokens)
    {
        this.multipleTokens = multipleTokens;
    }

    public boolean isPartialTokenCovering()
    {
        return partialTokenCovering;
    }

    public void setPartialTokenCovering(boolean partialTokenCovering)
    {
        this.partialTokenCovering = partialTokenCovering;
    }

    public ArrayList<FeatureInfo> getFeatures()
    {
        return features;
    }

    public void setFeatures(ArrayList<FeatureInfo> features)
    {
        this.features = features;
    }

}
