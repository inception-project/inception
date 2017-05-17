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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

@Component
public class AnnotationEditorExtensionRegistryImpl
    implements AnnotationEditorExtensionRegistry, SmartLifecycle
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private boolean running = false;
    
    private List<Class<? extends AnnotationEditorExtension>> editorExtensions;

    public AnnotationEditorExtensionRegistryImpl()
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean isRunning()
    {
        return running;
    }

    @Override
    public void start()
    {
        running = true;
        scan();
    }

    @Override
    public void stop()
    {
        running = false;
    }

    @Override
    public int getPhase()
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup()
    {
        return true;
    }

    @Override
    public void stop(Runnable aCallback)
    {
        stop();
        aCallback.run();
    }

    private void scan()
    {
        editorExtensions = new ArrayList<>();

        // Scan menu items from page class annotations
        ClassPathScanningCandidateComponentProvider scanner = 
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(AnnotationEditorExtension.class));

        for (BeanDefinition bd : scanner.findCandidateComponents("de.tudarmstadt.ukp")) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends AnnotationEditorExtension> editorClass = (Class<? extends AnnotationEditorExtension>) Class
                        .forName(bd.getBeanClassName());

                log.debug("Found annotation editor: {}", bd.getBeanClassName());

                editorExtensions.add(editorClass);
            }
            catch (ClassNotFoundException e) {
                log.error("Menu item class [{}] not found", bd.getBeanClassName(), e);
            }
        }
    }

    @Override
    public List<Class<? extends AnnotationEditorExtension>> getEditorsExtension()
    {
        return Collections.unmodifiableList(editorExtensions);
    }

    @Override
    public AnnotationEditorExtension getExtension(String aName)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
