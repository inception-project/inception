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

import java.util.List;

import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public abstract class LayerSupport_ImplBase<A extends TypeAdapter, T>
    implements LayerSupport<A, T>
{
    protected final FeatureSupportRegistry featureSupportRegistry;
    
    private LayerSupportRegistry layerSupportRegistry;
    
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
    
    public void setLayerSupportRegistry(LayerSupportRegistry aLayerSupportRegistry)
    {
        if (layerSupportRegistry != null) {
            throw new IllegalStateException("LayerSupportRegistry can only be set once!");
        }
        
        layerSupportRegistry = aLayerSupportRegistry;
    }
    
    public LayerSupportRegistry getLayerSupportRegistry()
    {
        if (layerSupportRegistry == null) {
            throw new IllegalStateException("LayerSupportRegistry not set!");
        }
        
        return layerSupportRegistry;
    }
}
