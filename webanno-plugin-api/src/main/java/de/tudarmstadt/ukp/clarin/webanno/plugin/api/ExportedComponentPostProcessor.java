/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.plugin.api;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class ExportedComponentPostProcessor
    implements BeanPostProcessor
{
    private ConfigurableListableBeanFactory beanFactory;

    public ExportedComponentPostProcessor(ConfigurableListableBeanFactory aBeanFactory)
    {
        beanFactory = aBeanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)
        throws BeansException
    {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        ConfigurableListableBeanFactory parentBeanFactory = getParentBeanFactory();

        if (parentBeanFactory == null) {
            return bean;
        }

        BeanDefinition bd;
        try {
            bd = beanFactory.getBeanDefinition(beanName);
        }
        catch (NoSuchBeanDefinitionException exception) {
            return bean;
        }

        if (bd instanceof AnnotatedBeanDefinition) {
            AnnotatedBeanDefinition metadata = (AnnotatedBeanDefinition) bd;

            if (metadata.getMetadata().isAnnotated(ExportedComponent.class.getName())) {
                parentBeanFactory.registerSingleton(beanName, bean);
            }
        }

        return bean;
    }

    private ConfigurableListableBeanFactory getParentBeanFactory()
    {
        BeanFactory parent = beanFactory.getParentBeanFactory();

        return (parent instanceof ConfigurableListableBeanFactory)
                ? (ConfigurableListableBeanFactory) parent
                : null;
    }
}
