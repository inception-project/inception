/*
 * Copyright 2018
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
import java.util.Optional;

import org.springframework.beans.factory.BeanNameAware;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public interface LayerSupport<T extends TypeAdapter>
    extends BeanNameAware
{
    Object getId();

    /**
     * Checks whether the given layer is provided by the current layer support.
     * 
     * @param aLayer
     *            a layer definition.
     * @return whether the given layer is provided by the current layer support.
     */
    boolean accepts(AnnotationLayer aLayer);

    /**
     * Get the layer type for the given annotation layer. If the current layer support does
     * not provide any layer type for the given layer, an empty value is returned. As we
     * usually use {@link LayerType} objects in layer type selection lists, this method is
     * helpful in obtaining the selected value of such a list from the {@link AnnotationLayer}
     * object being edited.
     * 
     * @param aLayer
     *            a layer definition.
     * @return the corresponding layer type.
     */
    default Optional<LayerType> getLayerType(AnnotationLayer aLayer)
    {
        return getSupportedLayerTypes().stream()
                .filter(t -> t.getName().equals(aLayer.getType())).findFirst();
    }
    
    List<LayerType> getSupportedLayerTypes();

    /**
     * Create an adapter for the given annotation layer.
     * 
     * @param aLayer
     *            the annotation layer.
     * @return the adapter.
     */
    T createAdapter(AnnotationLayer aLayer);
}
