/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.workload.matrix.management;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PROJECT_TYPE_ANNOTATION;
import static de.tudarmstadt.ukp.inception.ui.core.session.SessionMetaData.CURRENT_PROJECT;
import static de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension.MATRIX_WORKLOAD_MANAGER_EXTENSION_ID;

import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.page.MonitoringPage;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@Component
@Order(300)
public class MonitoringPageMenuItem implements MenuItem
{
    private final UserDao userRepo;
    private final ProjectService projectService;
    private final WorkloadManagerExtensionPoint workloadManagerExtensionPoint;
    private final WorkloadManagementService workloadManagementService;

    @Autowired
    public MonitoringPageMenuItem(
        UserDao aUserRepo,
        ProjectService aProjectService,
        WorkloadManagementService aWorkloadManagementService,
        WorkloadManagerExtensionPoint aWorkloadManagerExtensionPoint)
    {
        userRepo = aUserRepo;
        projectService = aProjectService;
        workloadManagementService = aWorkloadManagementService;
        workloadManagerExtensionPoint = aWorkloadManagerExtensionPoint;
    }

    @Override
    public String getPath()
    {
        return "/monitoring";
    }
    
    @Override
    public String getIcon()
    {
        return "images/attribution.png";
    }
    
    @Override
    public String getLabel()
    {
        return "Monitoring";
    }
    
    /**
     * Only admins and project managers can see this page
     */
    @Override
    public boolean applies()
    {
        Project sessionProject = Session.get().getMetaData(CURRENT_PROJECT);
        if (sessionProject == null) {
            return false;
        }
        
        // The project object stored in the session is detached from the persistence context and
        // cannot be used immediately in DB interactions. Fetch a fresh copy from the DB.
        Project project = projectService.getProject(sessionProject.getId());

        // Visible if the current user is a curator or project admin
        User user = userRepo.getCurrentUser();

        return (projectService.isCurator(project, user)
            || projectService.isManager(project, user))
            && PROJECT_TYPE_ANNOTATION.equals(project.getMode())
            && MATRIX_WORKLOAD_MANAGER_EXTENSION_ID.equals(workloadManagementService.
            getOrCreateWorkloadManagerConfiguration(project).
            getType());
    }
    
    @Override
    public Class<? extends Page> getPageClass()
    {
        return MonitoringPage.class;
    }
}
