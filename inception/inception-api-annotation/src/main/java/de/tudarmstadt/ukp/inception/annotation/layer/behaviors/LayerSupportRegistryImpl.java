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
package de.tudarmstadt.ukp.inception.annotation.layer.behaviors;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerType;
import de.tudarmstadt.ukp.inception.support.extensionpoint.CachingContextLookupExtensionPoint_ImplBase;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#layerSupportRegistry}.
 * </p>
 */
public class LayerSupportRegistryImpl
    extends CachingContextLookupExtensionPoint_ImplBase<AnnotationLayer, LayerSupport<?, ?>>
    implements LayerSupportRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<LayerSupport<?, ?>> layerSupports;

    public LayerSupportRegistryImpl(
            @Lazy @Autowired(required = false) List<LayerSupport<?, ?>> aLayerSupports)
    {
        super(aLayerSupports, AnnotationLayer::getId);
    }

    @Override
    public void init()
    {
        super.init();

        getExtensions().forEach($ -> $.setLayerSupportRegistry(this));
    }

    @Override
    public List<LayerSupport<?, ?>> getLayerSupports()
    {
        if (layerSupports == null) {
            log.error(
                    "List of extensions was accessed on this extension point before the extension "
                            + "point was initialized!",
                    new IllegalStateException());
            return Collections.emptyList();
        }

        return layerSupports;
    }

    @Override
    public LayerSupport<?, ?> getLayerSupport(AnnotationLayer aLayer)
    {
        return findExtension(aLayer).orElseThrow(() -> new IllegalArgumentException(
                "Unsupported layer: [" + aLayer.getName() + "]"));
    }

    @Override
    public LayerSupport<?, ?> getLayerSupport(String aId)
    {
        return getLayerSupports().stream() //
                .filter(fs -> fs.getId().equals(aId)).findFirst() //
                .orElse(null);
    }

    @Override
    public LayerType getLayerType(AnnotationLayer aLayer)
    {
        if (aLayer.getType() == null) {
            return null;
        }

        // Figure out which layer support provides the given type.
        // If we can find a suitable layer support, then use it to resolve the type to a LayerType
        for (var s : getLayerSupports()) {
            var ft = s.getLayerType(aLayer);
            if (ft.isPresent()) {
                return ft.get();
            }
        }

        return null;
    }

    @Override
    public <T extends TypeAdapter, S> Optional<LayerSupport<T, S>> findExtension(
            AnnotationLayer aKey)
    {
        return super.findGenericExtension(aKey);
    }

    @Override
    public boolean isSupported(AnnotationLayer aLayer)
    {
        return findExtension(aLayer).isPresent();
    }
}
