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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public abstract class Renderer_ImplBase<T extends TypeAdapter>
    implements Renderer
{
    private final T typeAdapter;
    private final FeatureSupportRegistry featureSupportRegistry;
    private final LayerSupportRegistry layerSupportRegistry;
    
    private Map<AnnotationFeature, Object> featureTraitsCache;
    private Map<AnnotationLayer, Object> layerTraitsCache;

    public Renderer_ImplBase(T aTypeAdapter, LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        featureSupportRegistry = aFeatureSupportRegistry;
        layerSupportRegistry = aLayerSupportRegistry;
        typeAdapter = aTypeAdapter;
    }

    @Override
    public FeatureSupportRegistry getFeatureSupportRegistry()
    {
        return featureSupportRegistry;
    }
    
    public T getTypeAdapter()
    {
        return typeAdapter;
    }
    
    /**
     * Decodes the traits for the given feature and returns them if they implement the requested
     * interface. This method internally caches the decoded traits, so it can be called often.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getTraits(AnnotationFeature aFeature, Class<T> aInterface)
    {
        if (featureTraitsCache == null) {
            featureTraitsCache = new HashMap<>();
        }
        
        Object trait = featureTraitsCache.computeIfAbsent(aFeature, feature ->
               featureSupportRegistry.getFeatureSupport(feature).readTraits(feature));
        
        if (trait != null && aInterface.isAssignableFrom(trait.getClass())) {
            return Optional.of((T) trait);
        }
        
        return Optional.empty();
    }

    /**
     * Decodes the traits for the given layer and returns them if they implement the requested
     * interface. This method internally caches the decoded traits, so it can be called often.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getTraits(AnnotationLayer aLayer, Class<T> aInterface)
    {
        if (layerTraitsCache == null) {
            layerTraitsCache = new HashMap<>();
        }
        
        Object trait = layerTraitsCache.computeIfAbsent(aLayer, feature ->
               layerSupportRegistry.getLayerSupport(feature).readTraits(feature));
        
        if (trait != null && aInterface.isAssignableFrom(trait.getClass())) {
            return Optional.of((T) trait);
        }
        
        return Optional.empty();
    }
}
