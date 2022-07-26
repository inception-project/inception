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
package de.tudarmstadt.ukp.inception.rendering.vmodel;

import static de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil.getAddr;

import java.util.Map;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class VArc
    extends VObject
{
    private static final long serialVersionUID = 3930613022560194891L;

    private VID source;
    private VID target;

    public VArc(AnnotationLayer aLayer, AnnotationFS aFS, String aType, FeatureStructure aSourceFS,
            FeatureStructure aTargetFS, String aLabelHint)
    {
        this(aLayer, new VID(getAddr(aFS)), aType, new VID(getAddr(aSourceFS)),
                new VID(getAddr(aTargetFS)), aLabelHint, null, null);
    }

    public VArc(AnnotationLayer aLayer, AnnotationFS aFS, String aType, FeatureStructure aSourceFS,
            FeatureStructure aTargetFS, Map<String, String> aFeatures)
    {
        this(aLayer, new VID(getAddr(aFS)), aType, new VID(getAddr(aSourceFS)),
                new VID(getAddr(aTargetFS)), null, aFeatures, null);
    }

    public VArc(AnnotationLayer aLayer, VID aVid, String aType, FeatureStructure aSourceFS,
            FeatureStructure aTargetFS, String aLabelHint)
    {
        this(aLayer, aVid, aType, new VID(getAddr(aSourceFS)), new VID(getAddr(aTargetFS)),
                aLabelHint, null, null);
    }

    public VArc(AnnotationLayer aLayer, VID aVid, String aType, FeatureStructure aSourceFS,
            FeatureStructure aTargetFS, String aLabelHint, Map<String, String> aFeatures)
    {
        this(aLayer, aVid, aType, new VID(getAddr(aSourceFS)), new VID(getAddr(aTargetFS)),
                aLabelHint, aFeatures, null);
    }

    public VArc(AnnotationLayer aLayer, VID aVid, String aType, FeatureStructure aSourceFS,
            FeatureStructure aTargetFS, int aEquivalenceSet, String aLabel)
    {
        super(aLayer, aVid, aType, aEquivalenceSet, null);
        setLabelHint(aLabel);
        source = new VID(getAddr(aSourceFS));
        target = new VID(getAddr(aTargetFS));
    }

    public VArc(AnnotationLayer aLayer, VID aVid, String aType, FeatureStructure aSourceFS,
            FeatureStructure aTargetFS, int aEquivalenceSet, Map<String, String> aFeatures)
    {
        super(aLayer, aVid, aType, aEquivalenceSet, aFeatures);
        source = new VID(getAddr(aSourceFS));
        target = new VID(getAddr(aTargetFS));
    }

    public VArc(AnnotationLayer aLayer, VID aVid, String aType, VID aSource, VID aTarget,
            String aLabel, String aColor)
    {
        this(aLayer, aVid, aType, aSource, aTarget, aLabel, null, aColor);
    }

    public VArc(AnnotationLayer aLayer, VID aVid, String aType, VID aSource, VID aTarget,
            String aLabel, Map<String, String> aFeatures, String aColor)
    {
        super(aLayer, aVid, aType, aFeatures);
        setColorHint(aColor);
        setLabelHint(aLabel);
        source = aSource;
        target = aTarget;
    }

    public void setSource(VID aSource)
    {
        source = aSource;
    }

    public VID getSource()
    {
        return source;
    }

    public void setTarget(VID aTarget)
    {
        target = aTarget;
    }

    public VID getTarget()
    {
        return target;
    }
}
