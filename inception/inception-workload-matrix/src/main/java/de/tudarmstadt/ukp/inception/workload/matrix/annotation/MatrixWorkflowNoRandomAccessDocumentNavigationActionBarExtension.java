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
package de.tudarmstadt.ukp.inception.workload.matrix.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension.MATRIX_WORKLOAD_MANAGER_EXTENSION_ID;

import java.io.Serializable;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.matrix.config.MatrixWorkloadManagerAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

/**
 * This extension disables random access to documents for non-managers if random access is disabled
 * in the matrix workload configuration.
 * 
 * <p>
 * This class is exposed as a Spring Component via
 * {@link MatrixWorkloadManagerAutoConfiguration#matrixWorkflowNoRandomAccessDocumentNavigationActionBarExtension}
 * </p>
 */
@Order(ActionBarExtension.ORDER_WORKFLOW)
public class MatrixWorkflowNoRandomAccessDocumentNavigationActionBarExtension
    implements ActionBarExtension, Serializable
{
    private static final long serialVersionUID = -8123846972605546654L;

    private final WorkloadManagementService workloadManagementService;
    private final MatrixWorkloadExtension matrixWorkloadExtension;
    private final ProjectService projectService;
    private final UserDao userService;

    @Autowired
    public MatrixWorkflowNoRandomAccessDocumentNavigationActionBarExtension(
            DocumentService aDocumentService, WorkloadManagementService aWorkloadManagementService,
            MatrixWorkloadExtension aMatrixWorkloadExtension, ProjectService aProjectService,
            UserDao aUserService)
    {
        workloadManagementService = aWorkloadManagementService;
        matrixWorkloadExtension = aMatrixWorkloadExtension;
        projectService = aProjectService;
        userService = aUserService;
    }

    @Override
    public String getRole()
    {
        return ROLE_NAVIGATOR;
    }

    @Override
    public int getPriority()
    {
        return 10;
    }

    @Override
    public boolean accepts(AnnotationPageBase aPage)
    {
        var project = aPage.getModelObject().getProject();
        if (project == null) {
            return false;
        }

        var workloadManager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project);
        if (!MATRIX_WORKLOAD_MANAGER_EXTENSION_ID.equals(workloadManager.getType())) {
            return false;
        }

        var sessionOwner = userService.getCurrentUser();
        if (projectService.hasRole(sessionOwner, project, MANAGER)) {
            return false;
        }

        return !matrixWorkloadExtension.readTraits(workloadManager).isRandomDocumentAccessAllowed();
    }

    @Override
    public Panel createActionBarItem(String aId, AnnotationPageBase aPage)
    {
        return new EmptyPanel(aId);
    }
}
