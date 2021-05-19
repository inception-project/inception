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
package de.tudarmstadt.ukp.inception.sharing.config;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.sharing.InviteService;
import de.tudarmstadt.ukp.inception.sharing.InviteServiceImpl;
import de.tudarmstadt.ukp.inception.sharing.project.InviteProjectSettingsPanelFactory;
import de.tudarmstadt.ukp.inception.sharing.project.ProjectSharingMenuItem;
import de.tudarmstadt.ukp.inception.sharing.project.exporters.ProjectInviteExporter;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@Configuration
@EnableConfigurationProperties(InviteServicePropertiesImpl.class)
@ConditionalOnProperty(prefix = "sharing.invites", name = "enabled", havingValue = "true")
public class InviteServiceAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean
    public InviteService inviteService(UserDao aUserRepository, ProjectService aProjectService,
            InviteServiceProperties aInviteProperties,
            WorkloadManagementService aWorkloadManagementService)
    {
        return new InviteServiceImpl(aUserRepository, aProjectService, aInviteProperties,
                aWorkloadManagementService, entityManager);
    }

    @Bean
    public InviteProjectSettingsPanelFactory inviteProjectSettingsPanelFactory()
    {
        return new InviteProjectSettingsPanelFactory();
    }

    @Bean
    public ProjectSharingMenuItem projectSharingMenuItem()
    {
        return new ProjectSharingMenuItem();
    }

    @Bean
    @Autowired
    public ProjectInviteExporter projectInviteExporter(InviteService aInviteService)
    {
        return new ProjectInviteExporter(aInviteService);
    }
}
