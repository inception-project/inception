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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
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

import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationSchemaServiceAutoConfiguration#layerBehaviorRegistry}.
 * </p>
 */
public class LayerBehaviorRegistryImpl
    implements LayerBehaviorRegistry
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

            for (var fs : lsp) {
                LOG.debug("Found layer behavior: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }

        BaseLoggers.BOOT_LOG.info("Found [{}] layer behaviors", lsp.size());

        layerBehaviors = unmodifiableList(lsp);
    }

    @Override
    public List<LayerBehavior> getLayerBehaviors()
    {
        return layerBehaviors;
    }

    @Override
    public <T> List<T> getLayerBehaviors(LayerSupport<?, ?> aLayerSupport, Class<? extends T> aAPI)
    {
        return getLayerBehaviors().stream()
                .filter(b -> b.accepts(aLayerSupport) && aAPI.isAssignableFrom(b.getClass()))
                .map(b -> aAPI.cast(b)).collect(Collectors.toList());
    }
}
