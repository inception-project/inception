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
package de.tudarmstadt.ukp.inception.workload.matrix.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtensionImpl;
import de.tudarmstadt.ukp.inception.workload.matrix.annotation.MatrixWorkflowActionBarExtension;
import de.tudarmstadt.ukp.inception.workload.matrix.annotation.MatrixWorkflowNoRandomAccessDocumentNavigationActionBarExtension;
import de.tudarmstadt.ukp.inception.workload.matrix.event.MatrixWorkloadStateWatcher;
import de.tudarmstadt.ukp.inception.workload.matrix.management.MatrixWorkloadManagementPageMenuItem;
import de.tudarmstadt.ukp.inception.workload.matrix.service.MatrixWorkloadService;
import de.tudarmstadt.ukp.inception.workload.matrix.service.MatrixWorkloadServiceImpl;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@Configuration
@ConditionalOnProperty(prefix = "workload.matrix", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MatrixWorkloadManagerAutoConfiguration
{
    @Bean
    public MatrixWorkloadExtension matrixWorkloadExtension(
            WorkloadManagementService aWorkloadManagementService, DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserRepository)
    {
        return new MatrixWorkloadExtensionImpl(aWorkloadManagementService, aDocumentService,
                aProjectService, aUserRepository);
    }

    @Bean
    public MatrixWorkloadStateWatcher matrixWorkloadStateWatcher(
            SchedulingService aSchedulingService)
    {
        return new MatrixWorkloadStateWatcher(aSchedulingService);
    }

    @Bean
    public MatrixWorkflowActionBarExtension matrixWorkflowActionBarExtension()
    {
        return new MatrixWorkflowActionBarExtension();
    }

    @Bean
    public MatrixWorkflowNoRandomAccessDocumentNavigationActionBarExtension //
            matrixWorkflowNoRandomAccessDocumentNavigationActionBarExtension(
                    DocumentService aDocumentService,
                    WorkloadManagementService aWorkloadManagementService,
                    MatrixWorkloadExtension aMatrixWorkloadExtension,
                    ProjectService aProjectService, UserDao aUserService)
    {
        return new MatrixWorkflowNoRandomAccessDocumentNavigationActionBarExtension(
                aDocumentService, aWorkloadManagementService, aMatrixWorkloadExtension,
                aProjectService, aUserService);
    }

    @Bean
    public MatrixWorkloadService matrixWorkloadService(DocumentService aDocumentService,
            ProjectService aProjectService)
    {
        return new MatrixWorkloadServiceImpl(aDocumentService, aProjectService);
    }

    @Bean
    public MatrixWorkloadManagementPageMenuItem matrixWorkloadManagementPageMenuItem(
            UserDao aUserRepo, ProjectService aProjectService,
            WorkloadManagementService aWorkloadManagementService)
    {
        return new MatrixWorkloadManagementPageMenuItem(aUserRepo, aProjectService,
                aWorkloadManagementService);
    }
}
