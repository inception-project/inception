/*

 * Copyright (C) 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.plugin.impl;

import java.nio.file.Path;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginWrapper;
import org.pf4j.spring.ExtensionsInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;

import de.tudarmstadt.ukp.inception.plugin.api.InceptionPlugin;
import de.tudarmstadt.ukp.inception.plugin.api.InceptionPluginManager;

public class InceptionPluginManagerImpl
    extends DefaultPluginManager
    implements ApplicationContextAware, InitializingBean, InceptionPluginManager
{
    private static final Logger LOG = LoggerFactory.getLogger(InceptionPluginManagerImpl.class);

    private ApplicationContext applicationContext;

    public InceptionPluginManagerImpl(Path pluginsRoot)
    {
        super(pluginsRoot);
    }

    @Override
    public void setApplicationContext(ApplicationContext aApplicationContext) throws BeansException
    {
        applicationContext = aApplicationContext;
    }

    @Override
    public ApplicationContext getApplicationContext()
    {
        return applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        loadPlugins();
        startPlugins();

        AbstractAutowireCapableBeanFactory beanFactory = (AbstractAutowireCapableBeanFactory) 
                applicationContext.getAutowireCapableBeanFactory();
        ExtensionsInjector extensionsInjector = new ExtensionsInjector(this, beanFactory);
        extensionsInjector.injectExtensions();
        
        // Add child application contexts for every plugin
        for (PluginWrapper plugin : getStartedPlugins()) {
            Class pluginClass = plugin.getPlugin().getClass();
            LOG.info("Found plugin: {}", plugin.getDescriptor().getPluginId());
            
            // Attach the plugin application context to the main application context such that it
            // can access its beans for auto-wiring
            GenericApplicationContext pluginContext = (GenericApplicationContext) 
                    ((InceptionPlugin) plugin.getPlugin()).getApplicationContext();
            pluginContext.setParent(applicationContext);
        }
    }
}
