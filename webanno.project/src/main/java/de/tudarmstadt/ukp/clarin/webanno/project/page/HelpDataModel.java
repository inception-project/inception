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
    private String layerProperty;
    private String layerTechnicalProperty;
    private String layerBehavior;
    private String featureDetail;
    public String getLayerProperty()
    {
        return layerProperty;
    }
    public void setLayerProperty(String layerProperty)
    {
        this.layerProperty = layerProperty;
    }
    public String getLayerTechnicalProperty()
    {
        return layerTechnicalProperty;
    }
    public void setLayerTechnicalProperty(String layerTechnicalProperty)
    {
        this.layerTechnicalProperty = layerTechnicalProperty;
    }
    public String getLayerBehavior()
    {
        return layerBehavior;
    }
    public void setLayerBehavior(String layerBehavior)
    {
        this.layerBehavior = layerBehavior;
    }
    public String getFeatureDetail()
    {
        return featureDetail;
    }
    public void setFeatureDetail(String featureDetail)
    {
        this.featureDetail = featureDetail;
    }

}
