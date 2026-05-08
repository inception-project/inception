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

import java.util.Collections;
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

    private VArc(Builder builder)
    {
        super(builder.layer, builder.vid, builder.equivalenceSet, builder.features);
        setPlaceholder(builder.placeholder);
        setLabelHint(builder.label);
        setColorHint(builder.color);
        source = builder.source;
        target = builder.target;
    }

    public VArc(AnnotationLayer aLayer, VID aVid, VID aSource, VID aTarget, String aLabel,
            Map<String, String> aFeatures, String aColor)
    {
        super(aLayer, aVid, aFeatures);
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

    @Override
    public String toString()
    {
        return "VArc [" + getVid() + "]";
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private AnnotationLayer layer;
        private VID vid;
        private int equivalenceSet = -1;
        private Map<String, String> features = Collections.emptyMap();
        private VID source;
        private VID target;
        private String label;
        private String color;
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

        public Builder withSource(FeatureStructure aSource)
        {
            source = VID.of(aSource);
            return this;
        }

        public Builder withSource(VID aSource)
        {
            source = aSource;
            return this;
        }

        public Builder withTarget(FeatureStructure aTarget)
        {
            target = VID.of(aTarget);
            return this;
        }

        public Builder withTarget(VID aTarget)
        {
            target = aTarget;
            return this;
        }

        public Builder withLabel(String aLabel)
        {
            label = aLabel;
            return this;
        }

        public Builder withColor(String aColor)
        {
            color = aColor;
            return this;
        }

        public Builder placeholder()
        {
            placeholder = true;
            return this;
        }

        public VArc build()
        {
            return new VArc(this);
        }
    }
}
