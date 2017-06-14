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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FeatureSupportRegistryImpl
    implements FeatureSupportRegistry, BeanPostProcessor
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, FeatureSupport> beans = new HashMap<>();
    private final List<FeatureSupport> sortedBeans = new ArrayList<>();
    private boolean sorted = false;

    @Override
    public Object postProcessAfterInitialization(Object aBean, String aBeanName)
        throws BeansException
    {
        // Collect annotation editor extensions
        if (aBean instanceof FeatureSupport) {
            beans.put(aBeanName, (FeatureSupport) aBean);
            log.debug("Found feature support: {}", aBeanName);
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
    public List<FeatureSupport> getFeatureSupports()
    {
        if (!sorted) {
            sortedBeans.addAll(beans.values());
            sortedBeans.sort((a, b) -> {
                int phaseA = (a instanceof Ordered) ? ((Ordered) a).getOrder() : 0;
                int phaseB = (b instanceof Ordered) ? ((Ordered) b).getOrder() : 0;
                return phaseB - phaseA;
            });
            sorted = true;
        }
        return sortedBeans;
    }
    
    @Override
    public FeatureSupport getFeatureSupport(AnnotationFeature aFeature)
    {
        for (FeatureSupport support : getFeatureSupports()) {
            if (support.accepts(aFeature)) {
                return support;
            }
        }
        
        throw new IllegalArgumentException("Unsupported feature: [" + aFeature.getName() + "]");
    }
}
