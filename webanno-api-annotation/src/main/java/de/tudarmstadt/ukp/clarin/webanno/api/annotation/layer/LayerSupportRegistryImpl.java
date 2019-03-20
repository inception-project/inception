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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

@Component
public class LayerSupportRegistryImpl
    implements LayerSupportRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<LayerSupport> layerSupportsProxy;
    
    private List<LayerSupport> layerSupports;
    
    private final Map<Long, LayerSupport<?>> supportCache = new HashMap<>();

    public LayerSupportRegistryImpl(
            @Lazy @Autowired(required = false) List<LayerSupport> aLayerSupports)
    {
        layerSupportsProxy = aLayerSupports;
    }
    
    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }
    
    public void init()
    {
        List<LayerSupport> lsp = new ArrayList<>();

        if (layerSupportsProxy != null) {
            lsp.addAll(layerSupportsProxy);
            AnnotationAwareOrderComparator.sort(lsp);
        
            for (LayerSupport<?> fs : lsp) {
                log.info("Found layer support: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }
        
        layerSupports = Collections.unmodifiableList(lsp);
    }
    
    @Override
    public List<LayerSupport> getLayerSupports()
    {
        return layerSupports;
    }
    
    @Override
    public LayerSupport getLayerSupport(AnnotationLayer aLayer)
    {
        // This method is called often during rendering, so we try to make it fast by caching
        // the supports by feature. Since the set of annotation features is relatively stable,
        // this should not be a memory leak - even if we don't remove entries if annotation
        // features would be deleted from the DB.
        LayerSupport support = null;
        
        if (aLayer.getId() != null) {
            support = supportCache.get(aLayer.getId());
        }
        
        if (support == null) {
            for (LayerSupport<?> s : getLayerSupports()) {
                if (s.accepts(aLayer)) {
                    support = s;
                    if (aLayer.getId() != null) {
                        // Store feature in the cache, but only when it has an ID, i.e. it has
                        // actually been saved.
                        supportCache.put(aLayer.getId(), s);
                    }
                    break;
                }
            }
        }
        
        if (support == null) {
            throw new IllegalArgumentException("Unsupported layer: [" + aLayer.getName() + "]");
        }
        
        return support;
    }
    
    @Override
    public LayerSupport getLayerSupport(String aId)
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
        for (LayerSupport s : getLayerSupports()) {
            Optional<LayerType> ft = s.getLayerType(aLayer);
            if (ft.isPresent()) {
                featureType = ft.get();
                break;
            }
        }
        return featureType;
    }
}
