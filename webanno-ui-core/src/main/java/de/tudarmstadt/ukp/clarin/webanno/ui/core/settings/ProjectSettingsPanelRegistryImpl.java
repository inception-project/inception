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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

@Component(ProjectSettingsPanelRegistry.SERVICE_NAME)
public class ProjectSettingsPanelRegistryImpl
    implements ProjectSettingsPanelRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<ProjectSettingsPanelFactory> extensionsProxy;

    private List<ProjectSettingsPanelFactory> extensions;

    public ProjectSettingsPanelRegistryImpl(
            @Lazy @Autowired(required = false) List<ProjectSettingsPanelFactory> aExtensions)
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
        List<ProjectSettingsPanelFactory> exts = new ArrayList<>();

        if (extensionsProxy != null) {
            exts.addAll(extensionsProxy);
            AnnotationAwareOrderComparator.sort(exts);
        
            for (ProjectSettingsPanelFactory fs : exts) {
                log.info("Found project setting panel: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }
        
        extensions = Collections.unmodifiableList(exts);
    }    

    @Override
    public ProjectSettingsPanelFactory getPanel(String aPath)
    {
        return getPanels().stream().filter(psp -> aPath.equals(psp.getPath())).findFirst().get();
    }
    
    @Override
    public List<ProjectSettingsPanelFactory> getPanels()
    {
        return extensions;
    }
}
