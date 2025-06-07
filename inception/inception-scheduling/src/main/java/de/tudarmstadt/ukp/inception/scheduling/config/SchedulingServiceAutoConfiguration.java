/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.scheduling.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.session.SessionRegistry;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingServiceImpl;
import de.tudarmstadt.ukp.inception.scheduling.TaskAccess;
import de.tudarmstadt.ukp.inception.scheduling.TaskAccessImpl;

@Configuration
@EnableConfigurationProperties({ SchedulingProperties.class })
public class SchedulingServiceAutoConfiguration
{
    @Bean
    public SchedulingService schedulingService(ApplicationContext aApplicationContext,
            SchedulingProperties aConfig, SessionRegistry aSessionRegistry)
    {
        return new SchedulingServiceImpl(aApplicationContext, aConfig, aSessionRegistry);
    }

    @Bean
    public TaskAccess taskAccess(ProjectService aProjectService, UserDao aUserService)
    {
        return new TaskAccessImpl(aProjectService, aUserService);
    }
}
