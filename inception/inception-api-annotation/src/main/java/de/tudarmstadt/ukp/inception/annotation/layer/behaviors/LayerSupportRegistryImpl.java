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

import static java.util.Collections.unmodifiableList;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.layer.LayerType;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#layerSupportRegistry}.
 * </p>
 */
public class LayerSupportRegistryImpl
    implements LayerSupportRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<LayerSupport<?, ?>> layerSupportsProxy;

    private List<LayerSupport<?, ?>> layerSupports;

    private final LoadingCache<AnnotationLayer, LayerSupport<?, ?>> supportCache;

    public LayerSupportRegistryImpl(
            @Lazy @Autowired(required = false) List<LayerSupport<?, ?>> aLayerSupports)
    {
        layerSupportsProxy = aLayerSupports;

        supportCache = Caffeine.newBuilder() //
                .expireAfterAccess(Duration.ofHours(1)) //
                .build(this::findLayerSupport);
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    public void init()
    {
        List<LayerSupport<?, ?>> lsp = new ArrayList<>();

        if (layerSupportsProxy != null) {
            lsp.addAll(layerSupportsProxy);
            AnnotationAwareOrderComparator.sort(lsp);

            for (LayerSupport<?, ?> fs : lsp) {
                log.debug("Found layer support: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
                fs.setLayerSupportRegistry(this);
            }
        }

        log.info("Found [{}] layer supports", lsp.size());

        layerSupports = unmodifiableList(lsp);
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

    private LayerSupport<?, ?> findLayerSupport(AnnotationLayer aLayer)
    {
        for (LayerSupport<?, ?> s : getLayerSupports()) {
            if (s.accepts(aLayer)) {
                return s;
            }
        }
        return null;
    }

    @Override
    public LayerSupport<?, ?> getLayerSupport(AnnotationLayer aLayer)
    {
        // This method is called often during rendering, so we try to make it fast by caching
        // the supports by layer. Since the set of layers is relatively stable, this should not be a
        // memory leak - even if we don't remove entries if layers would be deleted from the DB.
        LayerSupport<?, ?> support = null;

        // Look for the layer in cache, but only when it has an ID, i.e. it has actually been saved.
        if (aLayer.getId() != null) {
            support = supportCache.get(aLayer);
        }
        else {
            support = findLayerSupport(aLayer);
        }

        if (support == null) {
            throw new IllegalArgumentException("Unsupported layer: [" + aLayer.getName() + "]");
        }

        return support;
    }

    @Override
    public LayerSupport<?, ?> getLayerSupport(String aId)
    {
        return getLayerSupports().stream().filter(fs -> fs.getId().equals(aId)).findFirst()
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
        LayerType featureType = null;
        for (LayerSupport<?, ?> s : getLayerSupports()) {
            Optional<LayerType> ft = s.getLayerType(aLayer);
            if (ft.isPresent()) {
                featureType = ft.get();
                break;
            }
        }
        return featureType;
    }
}
