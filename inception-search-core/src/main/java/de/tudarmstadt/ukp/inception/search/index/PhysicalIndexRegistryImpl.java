/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.search.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

@Component
public class PhysicalIndexRegistryImpl
    implements PhysicalIndexRegistry, BeanPostProcessor
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<PhysicalIndexFactory> extensionsProxy;

    private List<PhysicalIndexFactory> extensions;

    public PhysicalIndexRegistryImpl(
            @Lazy @Autowired(required = false) List<PhysicalIndexFactory> aExtensions)
    {
        extensionsProxy = aExtensions;
    }
    
    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }
    
    /* package private */ void init()
    {
        List<PhysicalIndexFactory> exts = new ArrayList<>();

        if (extensionsProxy != null) {
            exts.addAll(extensionsProxy);
            AnnotationAwareOrderComparator.sort(exts);
        
            for (PhysicalIndexFactory fs : exts) {
                log.info("Found index extension: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }
        
        extensions = Collections.unmodifiableList(exts);
    }
    
    @Override
    public List<PhysicalIndexFactory> getIndexFactories()
    {
        return extensions;
    }

    @Override
    public PhysicalIndexFactory getIndexFactory(String aId)
    {
        if (aId == null) {
            return null;
        }
        else {
            return extensions.stream().filter(f -> aId.equals(f.getBeanName())).findFirst()
                    .orElse(null);
        }
    }

    @Override
    public PhysicalIndexFactory getDefaultIndexFactory()
    {
        return getIndexFactories().get(0);
    }
}
