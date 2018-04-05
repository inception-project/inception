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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;

@Component(ProjectSettingsPanelRegistryService.SERVICE_NAME)
public class ProjectSettingsPanelRegistryServiceImpl
    implements SmartLifecycle, ProjectSettingsPanelRegistryService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private boolean running = false;

    private List<ProjectSettingsPanelDecl> panels;

    private @Autowired ProjectService projectService;
    private @Autowired UserDao userRepository;
    
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
        panels = new ArrayList<>();

        // Scan project settings using annotation
        ClassPathScanningCandidateComponentProvider scanner = 
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(ProjectSettingsPanel.class));

        for (BeanDefinition bd : scanner.findCandidateComponents("de.tudarmstadt.ukp")) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Panel> panelClass = (Class<? extends Panel>) Class
                        .forName(bd.getBeanClassName());
                ProjectSettingsPanel mia = panelClass.getAnnotation(ProjectSettingsPanel.class);

                ProjectSettingsPanelDecl panel = new ProjectSettingsPanelDecl();
                panel.label = mia.label();
                panel.prio = mia.prio();
                panel.panel = panelClass;
                panels.add(panel);
               
                log.debug("Found settings panel: {} ({})", panel.label, panel.prio);

                List<Method> methods = MethodUtils.getMethodsListWithAnnotation(panelClass,
                        ProjectSettingsPanelCondition.class);
                if (!methods.isEmpty()) {
                    panel.condition = (aProject) -> {
                        try {
                            // Need to look the method up again here because methods are not
                            // serializable
                            Method m = MethodUtils.getMethodsListWithAnnotation(panelClass,
                                    ProjectSettingsPanelCondition.class).get(0);
                            return (boolean) m.invoke(null, aProject);
                        }
                        catch (Exception e) {
                            LoggerFactory.getLogger(ProjectSettingsPanelRegistryServiceImpl.class)
                                    .error("Unable to invoke settings panel condition method", e);
                            return false;
                        }
                    };
                }
                else {
                    panel.condition = (aProject) -> aProject != null;
                }
            }
            catch (ClassNotFoundException e) {
                log.error("Settings panel class [{}] not found", bd.getBeanClassName(), e);
            }
        }
        
        panels.sort(Comparator.comparingInt(a -> a.prio));
    }

    @Override
    public List<ProjectSettingsPanelDecl> getPanels()
    {
        return Collections.unmodifiableList(panels);
    }
}
