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
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

/**
 * Represents an annotation on a span layer.
 */
public class VSpan
    extends VObject
{
    private static final long serialVersionUID = -1610831873656531589L;

    private final List<VRange> ranges;

    public VSpan(AnnotationLayer aLayer, AnnotationFS aFS, String aType, VRange aOffsets,
            Map<String, String> aFeatures)
    {
        this(aLayer, new VID(getAddr(aFS)), aType, asList(aOffsets), aFeatures, null);
    }

    public VSpan(AnnotationLayer aLayer, AnnotationFS aFS, String aType, VRange aOffsets,
            int aEquivalenceClass, String aLabelHint)
    {
        super(aLayer, new VID(getAddr(aFS)), aType, aEquivalenceClass, null);
        setLabelHint(aLabelHint);
        ranges = asList(aOffsets);
    }

    public VSpan(AnnotationLayer aLayer, AnnotationFS aFS, String aType, VRange aOffsets,
            int aEquivalenceClass, Map<String, String> aFeatures)
    {
        super(aLayer, new VID(getAddr(aFS)), aType, aEquivalenceClass, aFeatures);
        ranges = asList(aOffsets);
    }

    public VSpan(AnnotationLayer aLayer, VID aVid, String aType, VRange aOffsets,
            Map<String, String> aFeatures)
    {
        this(aLayer, aVid, aType, asList(aOffsets), aFeatures, null);
    }

    public VSpan(AnnotationLayer aLayer, VID aVid, String aType, VRange aOffsets,
            Map<String, String> aFeatures, String color)
    {
        this(aLayer, aVid, aType, asList(aOffsets), aFeatures, color);
    }

    public VSpan(AnnotationLayer aLayer, AnnotationFS aFS, String aType, List<VRange> aOffsets,
            Map<String, String> aFeatures)
    {
        this(aLayer, new VID(getAddr(aFS)), aType, aOffsets, aFeatures, null);
    }

    public VSpan(AnnotationLayer aLayer, AnnotationFS aFS, String aType, List<VRange> aOffsets,
            int aEquivalenceClass, Map<String, String> aFeatures)
    {
        this(aLayer, new VID(getAddr(aFS)), aType, aOffsets, aFeatures, null);
    }

    public VSpan(AnnotationLayer aLayer, VID aVid, String aType, List<VRange> aOffsets,
            Map<String, String> aFeatures, String aColor)
    {
        super(aLayer, aVid, aType, aFeatures);
        setColorHint(aColor);
        ranges = aOffsets != null ? aOffsets : new ArrayList<>();
    }

    public List<VRange> getOffsets()
    {
        return ranges;
    }

    public List<VRange> getRanges()
    {
        return ranges;
    }
}
