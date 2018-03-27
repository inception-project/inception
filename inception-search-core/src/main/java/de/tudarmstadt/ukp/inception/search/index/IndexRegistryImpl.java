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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IndexRegistryImpl
    implements IndexRegistry, BeanPostProcessor
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, IndexFactory> beans = new HashMap<>();
    private final List<IndexFactory> sortedBeans = new ArrayList<>();
    private boolean sorted = false;

    @Override
    public Object postProcessAfterInitialization(Object aBean, String aBeanName)
        throws BeansException
    {
        // Collect data source implementations
        if (aBean instanceof IndexFactory) {
            beans.put(aBeanName, (IndexFactory) aBean);
            log.debug("Found data source factory: {}", aBeanName);
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
    public List<IndexFactory> getIndexFactories()
    {
        if (!sorted) {
            sortedBeans.addAll(beans.values());
            OrderComparator.sort(sortedBeans);
            sorted = true;
        }
        return sortedBeans;
    }

    @Override
    public IndexFactory getIndexFactory(String aId)
    {
        return beans.get(aId);
    }

    @Override
    public IndexFactory getDefaultIndexFactory()
    {
        return getIndexFactories().get(0);
    }
}
