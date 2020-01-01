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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

/**
 * Represents an annotation on a span layer.
 */
public class VSpan
    extends VObject
{
    private List<VRange> ranges = new ArrayList<>();
    private String colorHint;
    
    public VSpan(AnnotationLayer aLayer, AnnotationFS aFS, String aType, VRange aOffsets,
            Map<String, String> aFeatures, Map<String, String> aHoverFeatures)
    {
        this(aLayer, new VID(getAddr(aFS)), aType, asList(aOffsets), aFeatures, 
                aHoverFeatures, null);
    }
    
    public VSpan(AnnotationLayer aLayer, AnnotationFS aFS, String aType, VRange aOffsets,
            int aEquivalenceClass, Map<String, String> aFeatures, 
            Map<String, String> aHoverFeatures)
    {
        super(aLayer, new VID(getAddr(aFS)), aType, aEquivalenceClass, aFeatures, aHoverFeatures);
        ranges = asList(aOffsets);
    }

    public VSpan(AnnotationLayer aLayer, VID aVid, String aType, VRange aOffsets,
            Map<String, String> aFeatures, Map<String, String> aHoverFeatures)
    {
        this(aLayer, aVid, aType, asList(aOffsets), aFeatures, aHoverFeatures, null);
    }
    
    public VSpan(AnnotationLayer aLayer, VID aVid, String aType, VRange aOffsets,
            Map<String, String> aFeatures, Map<String, String> aHoverFeatures, String color)
    {
        this(aLayer, aVid, aType, asList(aOffsets), aFeatures, aHoverFeatures, color);
    }

    public VSpan(AnnotationLayer aLayer, AnnotationFS aFS, String aType, List<VRange> aOffsets,
            Map<String, String> aFeatures, Map<String, String> aHoverFeatures)
    {
        this(aLayer, new VID(getAddr(aFS)), aType, aOffsets, aFeatures, aHoverFeatures, null);
    }

    public VSpan(AnnotationLayer aLayer, AnnotationFS aFS, String aType, List<VRange> aOffsets,
            int aEquivalenceClass, Map<String, String> aFeatures, 
            Map<String, String> aHoverFeatures)
    {
        this(aLayer, new VID(getAddr(aFS)), aType, aOffsets, aFeatures, aHoverFeatures, null);
    }

    public VSpan(AnnotationLayer aLayer, VID aVid, String aType, List<VRange> aOffsets,
            Map<String, String> aFeatures, Map<String, String> aHoverFeatures, String color)
    {
        super(aLayer, aVid, aType, aFeatures, aHoverFeatures);
        ranges = aOffsets;
        colorHint = color;
    }
    
    public String getColorHint () {
        return colorHint;
    }

    public List<VRange> getOffsets()
    {
        return ranges;
    }

    public List<VRange> getRanges()
    {
        return ranges;
    }

    public void setColor(String aColor)
    {
        colorHint = aColor;
    }
}
