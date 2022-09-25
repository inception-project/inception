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
package de.tudarmstadt.ukp.clarin.webanno.project.config;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectAccess;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectAccessImpl;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectPermissionExtension;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.project.exporters.ProjectPermissionsExporter;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;

@Configuration
public class ProjectServiceAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean(ProjectService.SERVICE_NAME)
    public ProjectServiceImpl projectService(UserDao aUserRepository,
            ApplicationEventPublisher aApplicationEventPublisher,
            RepositoryProperties aRepositoryProperties,
            @Lazy @Autowired(required = false) List<ProjectInitializer> aInitializerProxy)
    {
        return new ProjectServiceImpl(aUserRepository, aApplicationEventPublisher,
                aRepositoryProperties, aInitializerProxy, entityManager);
    }

    @Bean
    public ProjectPermissionExtension projectPermissionExtension(UserDao aUserService,
            ProjectService aProjectService)
    {
        return new ProjectPermissionExtension(aUserService, aProjectService);
    }

    @Bean
    public ProjectPermissionsExporter projectPermissionsExporter(ProjectService aProjectService,
            UserDao aUserService)
    {
        return new ProjectPermissionsExporter(aProjectService, aUserService);
    }

    @Bean
    public ProjectAccess projectAccess(UserDao aUserService, ProjectService aProjectService)
    {
        return new ProjectAccessImpl(aUserService, aProjectService);
    }

}
