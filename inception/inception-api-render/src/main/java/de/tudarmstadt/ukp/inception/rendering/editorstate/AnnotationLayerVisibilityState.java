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
package de.tudarmstadt.ukp.inception.rendering.editorstate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.PreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringStrategyType;
import de.tudarmstadt.ukp.inception.rendering.coloring.ReadonlyColoringStrategy;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotationLayerVisibilityState
    implements PreferenceValue
{
    private static final long serialVersionUID = -4171872631523263892L;

    public static final PreferenceKey<AnnotationLayerVisibilityState> KEY_LAYERS_STATE = new PreferenceKey<>(
            AnnotationLayerVisibilityState.class, "annotation/layers");

    private Map<Long, ColoringStrategyType> layerColoringStrategy = new HashMap<>();

    private ReadonlyColoringStrategy readonlyLayerColoringStrategy = ReadonlyColoringStrategy.LEGACY;

    private Set<Long> hiddenLayers = new HashSet<>();

    private Set<Long> hiddenFeatures = new HashSet<>();

    private Map<Long, Set<String>> hiddenFeatureValues = new HashMap<>();

    public Map<Long, ColoringStrategyType> getLayerColoringStrategy()
    {
        return layerColoringStrategy;
    }

    public void setLayerColoringStrategy(Map<Long, ColoringStrategyType> aLayerColoringStrategy)
    {
        layerColoringStrategy = aLayerColoringStrategy;
    }

    public ReadonlyColoringStrategy getReadonlyLayerColoringStrategy()
    {
        return readonlyLayerColoringStrategy;
    }

    public void setReadonlyLayerColoringStrategy(
            ReadonlyColoringStrategy aReadonlyLayerColoringStrategy)
    {
        readonlyLayerColoringStrategy = aReadonlyLayerColoringStrategy;
    }

    public Set<Long> getHiddenLayers()
    {
        return hiddenLayers;
    }

    public void setHiddenLayers(Set<Long> aHiddenLayers)
    {
        hiddenLayers = aHiddenLayers;
    }

    public Set<Long> getHiddenFeatures()
    {
        return hiddenFeatures;
    }

    public void setHiddenFeatures(Set<Long> aHiddenFeatures)
    {
        hiddenFeatures = aHiddenFeatures;
    }

    public Map<Long, Set<String>> getHiddenFeatureValues()
    {
        return hiddenFeatureValues;
    }

    public void setHiddenFeatureValues(Map<Long, Set<String>> aHiddenFeatureValues)
    {
        hiddenFeatureValues = aHiddenFeatureValues;
    }
}
