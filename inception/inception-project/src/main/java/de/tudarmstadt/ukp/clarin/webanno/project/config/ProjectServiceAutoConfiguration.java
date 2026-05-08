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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.project.ProjectAccessImpl;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectPermissionExtension;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.project.exporters.ProjectPermissionsExporter;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.project.api.FeatureInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectAccess;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.api.footprint.FootprintProvider;
import de.tudarmstadt.ukp.inception.project.api.footprint.FootprintProviderRegistry;
import de.tudarmstadt.ukp.inception.project.footprint.FootprintProviderRegistryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
public class ProjectServiceAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean(ProjectService.SERVICE_NAME)
    public ProjectServiceImpl projectService(UserDao aUserRepository,
            ApplicationEventPublisher aApplicationEventPublisher,
            RepositoryProperties aRepositoryProperties,
            @Lazy @Autowired(required = false) List<ProjectInitializer> aProjectInitializerProxy,
            @Lazy @Autowired(required = false) List<FeatureInitializer> aFeatureInitializerProxy)
    {
        return new ProjectServiceImpl(aUserRepository, aApplicationEventPublisher,
                aRepositoryProperties, aProjectInitializerProxy, aFeatureInitializerProxy,
                entityManager);
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

    @Bean
    public FootprintProviderRegistry footprintProviderRegistry(
            @Lazy @Autowired(required = false) List<FootprintProvider> aExtensions)
    {
        return new FootprintProviderRegistryImpl(aExtensions);
    }

}
