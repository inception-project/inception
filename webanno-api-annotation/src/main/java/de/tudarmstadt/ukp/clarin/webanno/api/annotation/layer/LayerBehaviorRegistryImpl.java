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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.LayerBehavior;

@Component
public class LayerBehaviorRegistryImpl
    implements LayerBehaviorRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<LayerBehavior> layerBehaviorsProxy;
    
    private List<LayerBehavior> layerBehaviors;
    
    public LayerBehaviorRegistryImpl(
            @Lazy @Autowired(required = false) List<LayerBehavior> aLayerSupports)
    {
        layerBehaviorsProxy = aLayerSupports;
    }
    
    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }
    
    public void init()
    {
        List<LayerBehavior> lsp = new ArrayList<>();

        if (layerBehaviorsProxy != null) {
            lsp.addAll(layerBehaviorsProxy);
            AnnotationAwareOrderComparator.sort(lsp);
        
            for (LayerBehavior fs : lsp) {
                log.info("Found layer behavior: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }
        
        layerBehaviors = Collections.unmodifiableList(lsp);
    }
    
    @Override
    public List<LayerBehavior> getLayerBehaviors()
    {
        return layerBehaviors;
    }

    @Override
    public <T> List<T> getLayerBehaviors(LayerSupport<?> aLayerSupport, Class<? extends T> aAPI)
    {
        return getLayerBehaviors().stream()
                .filter(b -> b.accepts(aLayerSupport) && aAPI.isAssignableFrom(b.getClass()))
                .map(b -> aAPI.cast(b))
                .collect(Collectors.toList());
    }
}
