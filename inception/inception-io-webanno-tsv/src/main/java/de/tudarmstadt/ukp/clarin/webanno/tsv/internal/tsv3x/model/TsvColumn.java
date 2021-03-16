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
package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;

public class TsvColumn
{
    // The index is currently only set when parsing a schema to deserialize a TSV file. During
    // serialization, it is not really needed and always set to -1.
    public final int index;
    public final Type uimaType;
    public final Feature uimaFeature;
    public final LayerType layerType;
    public final FeatureType featureType;

    private Type targetTypeHint;

    public TsvColumn(Type aUimaType, LayerType aLayerType)
    {
        this(aUimaType, aLayerType, (Feature) null, FeatureType.PLACEHOLDER);
    }

    public TsvColumn(Type aUimaType, LayerType aLayerType, String aUimaFeatureName,
            FeatureType aFeatureType)
    {
        this(aUimaType, aLayerType, aUimaType.getFeatureByBaseName(aUimaFeatureName), aFeatureType);
    }

    public TsvColumn(Type aUimaType, LayerType aLayerType, Feature aUimaFeature,
            FeatureType aFeatureType)
    {
        this(-1, aUimaType, aLayerType, aUimaFeature, aFeatureType);
    }

    public TsvColumn(int aIndex, Type aUimaType, LayerType aLayerType)
    {
        this(aIndex, aUimaType, aLayerType, (Feature) null, FeatureType.PLACEHOLDER);
    }

    public TsvColumn(int aIndex, Type aUimaType, LayerType aLayerType, String aUimaFeatureName,
            FeatureType aFeatureType)
    {
        this(aIndex, aUimaType, aLayerType, aUimaType.getFeatureByBaseName(aUimaFeatureName),
                aFeatureType);
    }

    public TsvColumn(int aIndex, Type aUimaType, LayerType aLayerType, Feature aUimaFeature,
            FeatureType aFeatureType)
    {
        index = aIndex;
        uimaType = aUimaType;
        layerType = aLayerType;
        uimaFeature = aUimaFeature;
        featureType = aFeatureType;
    }

    public void setTargetTypeHint(Type aTargetTypeHint)
    {
        targetTypeHint = aTargetTypeHint;
    }

    public Type getTargetTypeHint()
    {
        return targetTypeHint;
    }

    public int getIndex()
    {
        return index;
    }

    public Type getUimaType()
    {
        return uimaType;
    }

    public Feature getUimaFeature()
    {
        return uimaFeature;
    }

    public LayerType getLayerType()
    {
        return layerType;
    }

    public FeatureType getFeatureType()
    {
        return featureType;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("TsvColumn [uimaType=");
        builder.append(uimaType);
        builder.append(", layerType=");
        builder.append(layerType);
        builder.append(", uimaFeature=");
        builder.append(uimaFeature);
        builder.append(", featureType=");
        builder.append(featureType);
        builder.append("]");
        return builder.toString();
    }
}
