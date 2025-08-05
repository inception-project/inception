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
package de.tudarmstadt.ukp.inception.workload.dynamic.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.session.SessionRegistry;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtensionImpl;
import de.tudarmstadt.ukp.inception.workload.dynamic.annotation.DynamicWorkflowActionBarExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.annotation.DynamicWorkflowDocumentNavigationActionBarExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.event.DynamicWorkloadStateWatcher;
import de.tudarmstadt.ukp.inception.workload.dynamic.management.DynamicWorkloadManagementPageMenuItem;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.WorkflowExtensionPointImpl;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types.DefaultWorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types.RandomizedWorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@Configuration
@ConditionalOnProperty(prefix = "workload.dynamic", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DynamicWorkloadManagerAutoConfiguration
{
    @Bean
    public DynamicWorkloadExtension dynamicWorkloadExtension(DocumentService documentService,
            WorkloadManagementService aWorkloadManagementService,
            WorkflowExtensionPoint aWorkflowExtensionPoint, ProjectService aProjectService,
            UserDao aUserRepository, SessionRegistry aSessionRegistry)
    {
        return new DynamicWorkloadExtensionImpl(aWorkloadManagementService, aWorkflowExtensionPoint,
                documentService, aProjectService, aUserRepository, aSessionRegistry);
    }

    @Bean
    public WorkflowExtensionPoint workflowExtensionPoint(
            @Lazy @Autowired(required = false) List<WorkflowExtension> aWorkflowExtension)
    {
        return new WorkflowExtensionPointImpl(aWorkflowExtension);
    }

    @Bean
    public DefaultWorkflowExtension defaultWorkflowExtension()
    {
        return new DefaultWorkflowExtension();
    }

    @Bean
    public RandomizedWorkflowExtension randomizedWorkflowExtension()
    {
        return new RandomizedWorkflowExtension();
    }

    @Bean
    public DynamicWorkloadStateWatcher dynamicWorkloadStateWatcher(
            SchedulingService aSchedulingService)
    {
        return new DynamicWorkloadStateWatcher(aSchedulingService);
    }

    @Bean
    public DynamicWorkflowActionBarExtension dynamicWorkflowActionBarExtension(
            WorkloadManagementService aWorkloadManagementService, ProjectService aProjectService,
            UserDao aUserService)
    {
        return new DynamicWorkflowActionBarExtension(aWorkloadManagementService, aProjectService,
                aUserService);
    }

    @Bean
    public DynamicWorkflowDocumentNavigationActionBarExtension dynamicWorkflowDocumentNavigationActionBarExtension(
            DocumentService aDocumentService, WorkloadManagementService aWorkloadManagementService,
            DynamicWorkloadExtension aDynamicWorkloadExtension, ProjectService aProjectService,
            UserDao aUserService)
    {
        return new DynamicWorkflowDocumentNavigationActionBarExtension(aDocumentService,
                aWorkloadManagementService, aDynamicWorkloadExtension, aProjectService,
                aUserService);
    }

    @ConditionalOnWebApplication
    @Bean
    public DynamicWorkloadManagementPageMenuItem dynamicWorkloadManagementPageMenuItem(
            UserDao aUserRepo, ProjectService aProjectService,
            WorkloadManagementService aWorkloadManagementService)
    {
        return new DynamicWorkloadManagementPageMenuItem(aUserRepo, aProjectService,
                aWorkloadManagementService);
    }
}
