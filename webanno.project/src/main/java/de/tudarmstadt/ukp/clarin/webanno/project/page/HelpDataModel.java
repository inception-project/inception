/*******************************************************************************
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.tudarmstadt.ukp.clarin.webanno.project.page;

import java.io.Serializable;

/**
 * A class consisting of property field to store help contents for different buttons in
 * {@link ProjectPage}
 *
 * @author Seid Muhie Yimam
 *
 */
public class HelpDataModel
    implements Serializable
{

    private static final long serialVersionUID = 3177595634275483617L;
    private String lockToToken;
    private String crossSentence;
    private String multipleToken;
    private String allowStacking;
    private String layerName;
    private String featureName;
    private String visible;
    private String layerEnabled;
    private String featureEnabled;
    private String featureVisible;
    private String attachType;
    private String layerTypes;
    private String featureType;
    private String tagSet;


    public String getLockToToken()
    {
        return lockToToken;
    }

    public void setLockToToken(String attachToToken)
    {
        this.lockToToken = attachToToken;
    }

    public String getCrossSentence()
    {
        return crossSentence;
    }

    public void setCrossSentence(String crossSentence)
    {
        this.crossSentence = crossSentence;
    }

    public String getMultipleToken()
    {
        return multipleToken;
    }

    public void setMultipleToken(String multipleToken)
    {
        this.multipleToken = multipleToken;
    }


    public String getLayerName()
    {
        return layerName;
    }

    public void setLayerName(String layerName)
    {
        this.layerName = layerName;
    }

    public String getFeatureName()
    {
        return featureName;
    }

    public void setFeatureName(String featureName)
    {
        this.featureName = featureName;
    }

    public String getVisible()
    {
        return visible;
    }

    public void setVisible(String visible)
    {
        this.visible = visible;
    }

    public String getLayerEnabled()
    {
        return layerEnabled;
    }

    public void setLayerEnabled(String layerEnabled)
    {
        this.layerEnabled = layerEnabled;
    }

    public String getFeatureEnabled()
    {
        return featureEnabled;
    }

    public void setFeatureEnabled(String featureEnabled)
    {
        this.featureEnabled = featureEnabled;
    }

    public String getAllowStacking()
    {
        return allowStacking;
    }

    public void setAllowStacking(String allowStacking)
    {
        this.allowStacking = allowStacking;
    }

    public String getAttachType()
    {
        return attachType;
    }

    public void setAttachType(String attachType)
    {
        this.attachType = attachType;
    }

    public String getLayerTypes()
    {
        return layerTypes;
    }

    public void setLayerTypes(String layerTypes)
    {
        this.layerTypes = layerTypes;
    }

    public String getFeatureVisible()
    {
        return featureVisible;
    }

    public void setFeatureVisible(String featureVisible)
    {
        this.featureVisible = featureVisible;
    }

    public String getFeatureType()
    {
        return featureType;
    }

    public void setFeatureType(String featureType)
    {
        this.featureType = featureType;
    }

    public String getTagSet()
    {
        return tagSet;
    }

    public void setTagSet(String tagSet)
    {
        this.tagSet = tagSet;
    }


}
