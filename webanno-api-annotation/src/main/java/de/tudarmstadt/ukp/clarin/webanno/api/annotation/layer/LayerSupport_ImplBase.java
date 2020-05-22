/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toJsonString;

import java.io.IOException;
import java.util.List;

import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public abstract class LayerSupport_ImplBase<A extends TypeAdapter, T>
    implements LayerSupport<A, T>
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private LayerSupportRegistry layerSupportRegistry;

    protected final FeatureSupportRegistry featureSupportRegistry;

    public LayerSupport_ImplBase(FeatureSupportRegistry aFeatureSupportRegistry)
    {
        featureSupportRegistry = aFeatureSupportRegistry;
    }
    
    public final void generateFeatures(TypeSystemDescription aTSD, TypeDescription aTD,
            List<AnnotationFeature> aFeatures)
    {
        for (AnnotationFeature feature : aFeatures) {
            FeatureSupport<?> fs = featureSupportRegistry.getFeatureSupport(feature);
            fs.generateFeature(aTSD, aTD, feature);
        }
    }
    
    @Override
    public void setLayerSupportRegistry(LayerSupportRegistry aLayerSupportRegistry)
    {
        if (layerSupportRegistry != null) {
            throw new IllegalStateException("LayerSupportRegistry can only be set once!");
        }
        
        layerSupportRegistry = aLayerSupportRegistry;
    }
    
    @Override
    public LayerSupportRegistry getLayerSupportRegistry()
    {
        if (layerSupportRegistry == null) {
            throw new IllegalStateException("LayerSupportRegistry not set!");
        }
        
        return layerSupportRegistry;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public T readTraits(AnnotationLayer aLayer)
    {
        T traits = null;
        try {
            traits = fromJsonString((Class<T>) createTraits().getClass(), aLayer.getTraits());
        }
        catch (IOException e) {
            log.error("Unable to read traits", e);
        }
    
        if (traits == null) {
            traits = createTraits();
        }
    
        return traits;
    }
    
    @Override
    public void writeTraits(AnnotationLayer aLayer, T aTraits)
    {
        try {
            aLayer.setTraits(toJsonString(aTraits));
        }
        catch (IOException e) {
            log.error("Unable to write traits", e);
        }
    }
}
