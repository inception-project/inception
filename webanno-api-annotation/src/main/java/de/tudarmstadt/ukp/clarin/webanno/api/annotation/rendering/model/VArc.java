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

import java.util.Collections;
import java.util.Map;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class VArc
    extends VObject
{
    private VID source;
    private VID target;
    private String colorHint;
    private String labelHint;

    public VArc(AnnotationLayer aLayer, AnnotationFS aFS, String aType, FeatureStructure aSourceFS,
            FeatureStructure aTargetFS, Map<String, String> aFeatures)
    {
        this(aLayer, new VID(getAddr(aFS)), aType, new VID(getAddr(aSourceFS)),
                new VID(getAddr(aTargetFS)), null, aFeatures, null);
    }

    public VArc(AnnotationLayer aLayer, VID aVid, String aType, FeatureStructure aSourceFS,
            FeatureStructure aTargetFS, String aLabelHint, Map<String, String> aFeatures)
    {
        this(aLayer, aVid, aType, new VID(getAddr(aSourceFS)), new VID(getAddr(aTargetFS)),
                aLabelHint, aFeatures, null);
    }

    public VArc(AnnotationLayer aLayer, VID aVid, String aType, FeatureStructure aSourceFS,
            FeatureStructure aTargetFS, int aEquivalenceSet, Map<String, String> aFeatures)
    {
        super(aLayer, aVid, aType, aEquivalenceSet, aFeatures, Collections.emptyMap());
        source = new VID(getAddr(aSourceFS));
        target = new VID(getAddr(aTargetFS));
    }

    public VArc(AnnotationLayer aLayer, VID aVid, String aType, VID aSource, VID aTarget,
            String aLabelHint, Map<String, String> aFeatures, String aColor)
    {
        super(aLayer, aVid, aType, aFeatures, Collections.emptyMap());
        source = aSource;
        target = aTarget;
        labelHint = aLabelHint;
        colorHint = aColor;
    }

    public String getColorHint () {
        return colorHint;
    }
    
    public String getLabelHint()
    {
        return labelHint;
    }
    
    public VID getSource()
    {
        return source;
    }

    public void setSource(VID aSource)
    {
        source = aSource;
    }

    public VID getTarget()
    {
        return target;
    }

    public void setTarget(VID aTarget)
    {
        target = aTarget;
    }
    
    public void setColor(String aColor)
    {
        colorHint = aColor;
    }
}
