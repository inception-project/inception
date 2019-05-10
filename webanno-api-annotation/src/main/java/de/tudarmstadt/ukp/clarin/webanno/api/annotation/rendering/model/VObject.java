/*
 * Copyright 2017
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
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public abstract class VObject
{
    private AnnotationLayer layer;
    private VID vid;
    private String type;
    private Map<String, String> features = new HashMap<>();
    private Map<String, String> hoverFeatures = new HashMap<>();
    private List<VLazyDetailQuery> lazyDetails = new ArrayList<>();
    private int equivalenceSet;

    public VObject(AnnotationLayer aLayer, VID aVid, String aType, Map<String, String> aFeatures, 
            Map<String, String> aHoverFeatures)
    {
        this(aLayer, aVid, aType, -1, aFeatures, aHoverFeatures);
    }

    public VObject(AnnotationLayer aLayer, VID aVid, String aType, int aEquivalenceSet,
            Map<String, String> aFeatures, Map<String, String> aHoverFeatures)
    {
        layer = aLayer;
        vid = aVid;
        type = aType;
        features = aFeatures;
        setHoverFeatures(aHoverFeatures);
        equivalenceSet = aEquivalenceSet;
    }

    public VID getVid()
    {
        return vid;
    }

    public AnnotationLayer getLayer()
    {
        return layer;
    }

    public String getType()
    {
        return type;
    }
    
    public int getEquivalenceSet()
    {
        return equivalenceSet;
    }

    public Map<String, String> getFeatures()
    {
        return features;
    }

    public Map<String, String> getHoverFeatures()
    {
        return hoverFeatures;
    }

    public void setHoverFeatures(Map<String, String> aHoverFeatures)
    {
        hoverFeatures = aHoverFeatures;
    }
    
    public void addHoverFeature(String aFeature, String aValue)
    {
        hoverFeatures.put(aFeature, aValue);
    }

    public List<VLazyDetailQuery> getLazyDetails()
    {
        return lazyDetails;
    }

    public void setLazyDetails(List<VLazyDetailQuery> aLazyDetails)
    {
        lazyDetails = aLazyDetails;
    }

    public void addLazyDetail(VLazyDetailQuery aDetail)
    {
        lazyDetails.add(aDetail);
    }
}
