/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Component;

@Component
public class AnnotationSidebarRegistryImpl
    implements AnnotationSidebarRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired List<AnnotationSidebarFactory> factories;

    @PostConstruct
    private void init()
    {
        OrderComparator.sort(factories);
        
        for (AnnotationSidebarFactory asf : factories) {
            log.info("Found annotation sidebar factory: {}",
                    ClassUtils.getAbbreviatedName(asf.getClass(), 20));
        }
        
        factories = Collections.unmodifiableList(factories);
    }
    
    @Override
    public List<AnnotationSidebarFactory> getSidebarFactories()
    {
        return factories;
    }
    
    @Override
    public AnnotationSidebarFactory getSidebarFactory(String aId)
    {
        if (aId == null) {
            return null;
        }
        else {
            return factories.stream().filter(f -> aId.equals(f.getBeanName())).findFirst()
                    .orElse(null);
        }
    }
    
    @Override
    public AnnotationSidebarFactory getDefaultSidebarFactory()
    {
        return getSidebarFactories().get(0);
    }
}
