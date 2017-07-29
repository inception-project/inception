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
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentLifecycleAware;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentLifecycleAwareRegistry;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DocumentLifecycleAwareRegistryImpl
    implements DocumentLifecycleAwareRegistry, BeanPostProcessor
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<DocumentLifecycleAware> beans = new ArrayList<>();
    private boolean sorted = false;
    
    @Override
    public Object postProcessAfterInitialization(Object aBean, String aBeanName)
        throws BeansException
    {
        // Collect the beans that need to be notified about the document lifecycle
        if (aBean instanceof DocumentLifecycleAware) {
            beans.add((DocumentLifecycleAware) aBean);
            log.debug("Found document lifecycle aware bean: {}", aBeanName);
        }
        
        return aBean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object aBean, String aBeanName)
        throws BeansException
    {
        return aBean;
    }

    @Override
    public List<DocumentLifecycleAware> getBeans()
    {
        if (!sorted) {
            OrderComparator.sort(beans);
            sorted = true;
        }
        return beans;
    }
}
