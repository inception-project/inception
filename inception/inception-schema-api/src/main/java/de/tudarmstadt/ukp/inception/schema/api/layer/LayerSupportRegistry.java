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
package de.tudarmstadt.ukp.inception.schema.api.layer;

import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.support.extensionpoint.ContextLookupExtensionPoint;

public interface LayerSupportRegistry
    extends ContextLookupExtensionPoint<AnnotationLayer, LayerSupport<?, ?>>
{
    List<LayerSupport<?, ?>> getLayerSupports();

    /**
     * Retrieves the layer support for the given layer.
     * 
     * @param aLayer
     *            the layer to get the support for.
     * @return the layer support.
     * @throws IllegalArgumentException
     *             if there is no support for the given layer.
     */
    LayerSupport<?, ?> getLayerSupport(AnnotationLayer aLayer);

    LayerSupport<?, ?> getLayerSupport(String aId);

    LayerType getLayerType(AnnotationLayer aLayer);

    /**
     * @return the types of all layers the user should be able to create. There can also be internal
     *         types reserved for built-in features. These are not returned.
     */
    default List<LayerType> getAllTypes()
    {
        List<LayerType> allTypes = new ArrayList<>();

        for (LayerSupport<?, ?> layerSupport : getLayerSupports()) {
            List<LayerType> types = layerSupport.getSupportedLayerTypes();
            types.stream().filter(l -> !l.isInternal()).forEach(allTypes::add);
        }

        allTypes.sort(comparing(LayerType::getUiName));

        return allTypes;
    }

    <T extends TypeAdapter, S> Optional<LayerSupport<T, S>> findExtension(AnnotationLayer aKey);

    boolean isSupported(AnnotationLayer aLayer);
}
