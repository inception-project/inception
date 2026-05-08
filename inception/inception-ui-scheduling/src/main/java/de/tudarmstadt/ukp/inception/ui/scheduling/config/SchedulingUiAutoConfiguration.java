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
package de.tudarmstadt.ukp.inception.ui.scheduling.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.project.api.ProjectAccess;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.controller.SchedulerController;
import de.tudarmstadt.ukp.inception.ui.scheduling.TaskMonitorFooterItem;
import de.tudarmstadt.ukp.inception.ui.scheduling.controller.SchedulerControllerImpl;

@ConditionalOnWebApplication
@Configuration
public class SchedulingUiAutoConfiguration
{
    @ConditionalOnExpression("${websocket.enabled:true}")
    @Bean
    public TaskMonitorFooterItem taskMonitorFooterItem()
    {
        return new TaskMonitorFooterItem();
    }

    @Bean
    SchedulerController schedulerController(SchedulingService aSchedulingService, UserDao aUserDao,
            ProjectAccess aProjectAccess)
    {
        return new SchedulerControllerImpl(aSchedulingService, aUserDao, aProjectAccess);
    }
}
