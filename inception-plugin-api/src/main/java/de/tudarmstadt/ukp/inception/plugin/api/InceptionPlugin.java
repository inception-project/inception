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
package de.tudarmstadt.ukp.inception.plugin.api;

import java.util.Set;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;

public abstract class InceptionPlugin
    extends Plugin
{
    private ApplicationContext applicationContext;

    public InceptionPlugin(PluginWrapper wrapper)
    {
        super(wrapper);
    }

    public final ApplicationContext getApplicationContext()
    {
        if (applicationContext == null) {
            applicationContext = createApplicationContext();
        }

        return applicationContext;
    }

    @Override
    public void stop()
    {
        // close applicationContext
        if ((applicationContext != null)
                && (applicationContext instanceof ConfigurableApplicationContext)) {
            ((ConfigurableApplicationContext) applicationContext).close();
        }
    }
    
    protected ApplicationContext createApplicationContext()
    {
        InceptionPlugin springPlugin = (InceptionPlugin) getWrapper().getPlugin();
        
        // Create an application context for this plugin using Spring annotated classes starting
        // with the plugin class
        AnnotationConfigApplicationContext pluginContext = new AnnotationConfigApplicationContext();
        pluginContext
                .setResourceLoader(new DefaultResourceLoader(getWrapper().getPluginClassLoader()));
        pluginContext.registerBean(ExportedBeanPostProcessor.class);
        pluginContext.register(springPlugin.getSources().stream().toArray(Class[]::new));

        // Attach the plugin application context to the main application context such that it can
        // access its beans for auto-wiring
        ApplicationContext parent = ((InceptionPluginManager) getWrapper().getPluginManager())
                .getApplicationContext();
        pluginContext.setParent(parent);

        // Initialize the context
        pluginContext.refresh();
        
        
        return pluginContext;
    }

    /**
     * Returns a set of classes which are used to initialize the Spring application context for this
     * plugin. These classes are not automatically exported to the main application context. In
     * order to be exported, they must carry the {@link ExportedBean annotation}.
     */
    public abstract Set<Class<?>> getSources();
}
