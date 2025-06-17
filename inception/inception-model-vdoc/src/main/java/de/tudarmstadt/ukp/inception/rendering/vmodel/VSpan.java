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

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collections;
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

    private VSpan(Builder builder)
    {
        super(builder.layer, builder.vid, builder.equivalenceSet, builder.features);
        setPlaceholder(builder.placeholder);
        setLabelHint(builder.label);
        ranges = builder.ranges;
    }

    public VSpan(AnnotationLayer aLayer, AnnotationFS aFS, VRange aOffsets,
            Map<String, String> aFeatures)
    {
        this(aLayer, VID.of(aFS), asList(aOffsets), aFeatures, null);
    }

    public VSpan(AnnotationLayer aLayer, AnnotationFS aFS, VRange aOffsets, int aEquivalenceClass,
            String aLabelHint)
    {
        super(aLayer, VID.of(aFS), aEquivalenceClass, null);
        setLabelHint(aLabelHint);
        ranges = asList(aOffsets);
    }

    public VSpan(AnnotationLayer aLayer, VID aVid, VRange aOffsets, Map<String, String> aFeatures)
    {
        this(aLayer, aVid, asList(aOffsets), aFeatures, null);
    }

    public VSpan(AnnotationLayer aLayer, VID aVid, VRange aOffsets, Map<String, String> aFeatures,
            String color)
    {
        this(aLayer, aVid, asList(aOffsets), aFeatures, color);
    }

    /**
     * @deprecated Unused - to be removed without replacement
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public VSpan(AnnotationLayer aLayer, AnnotationFS aFS, List<VRange> aOffsets,
            Map<String, String> aFeatures)
    {
        this(aLayer, VID.of(aFS), aOffsets, aFeatures, null);
    }

    /**
     * @deprecated Unused - to be removed without replacement
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public VSpan(AnnotationLayer aLayer, AnnotationFS aFS, List<VRange> aOffsets,
            int aEquivalenceClass, Map<String, String> aFeatures)
    {
        this(aLayer, VID.of(aFS), aOffsets, aFeatures, null);
    }

    public VSpan(AnnotationLayer aLayer, VID aVid, List<VRange> aOffsets,
            Map<String, String> aFeatures, String aColor)
    {
        super(aLayer, aVid, aFeatures);
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

    public static Builder builder()
    {
        return new Builder();
    }

    @Override
    public String toString()
    {
        return "VSpan [" + getVid() + "]";
    }

    public static final class Builder
    {
        private AnnotationLayer layer;
        private VID vid;
        private int equivalenceSet = -1;
        private Map<String, String> features = Collections.emptyMap();
        private List<VRange> ranges;
        private String label;
        private boolean placeholder;

        private Builder()
        {
        }

        public Builder forAnnotation(AnnotationFS aAnnotation)
        {
            withVid(VID.of(aAnnotation));
            return this;
        }

        public Builder withLayer(AnnotationLayer aLayer)
        {
            layer = aLayer;
            return this;
        }

        public Builder withVid(VID aVid)
        {
            vid = aVid;
            return this;
        }

        public Builder withEquivalenceSet(int aEquivalenceSet)
        {
            equivalenceSet = aEquivalenceSet;
            return this;
        }

        public Builder withFeatures(Map<String, String> aFeatures)
        {
            features = aFeatures;
            return this;
        }

        public Builder withRange(VRange aRange)
        {
            ranges = asList(aRange);
            return this;
        }

        public Builder withLabel(String aLabel)
        {
            label = aLabel;
            return this;
        }

        public Builder placeholder()
        {
            placeholder = true;
            return this;
        }

        public VSpan build()
        {
            return new VSpan(this);
        }
    }
}
