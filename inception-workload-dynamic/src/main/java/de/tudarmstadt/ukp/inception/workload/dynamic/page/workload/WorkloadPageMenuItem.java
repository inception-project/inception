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
package de.tudarmstadt.ukp.inception.workload.dynamic.page.workload;


import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.inception.ui.core.session.SessionMetaData;
import de.tudarmstadt.ukp.inception.workload.dynamic.config.DynamicWorkloadManagerAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.dynamic.manager.WorkloadAndWorkflowService;

/**
 * Menu item to access the dynamic workload management page.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DynamicWorkloadManagerAutoConfiguration#workloadPageMenuItem}.
 * </p>
 */
@Order(500)
public class WorkloadPageMenuItem implements MenuItem
{
    private final UserDao userRepo;
    private final ProjectService projectService;
    private final WorkloadAndWorkflowService workloadAndWorkflowService;

    @Autowired
    public WorkloadPageMenuItem(UserDao aUserRepo, ProjectService aProjectService,
                                WorkloadAndWorkflowService aWorkloadAndWorkflowService)
    {
        userRepo = aUserRepo;
        projectService = aProjectService;
        workloadAndWorkflowService = aWorkloadAndWorkflowService;
    }

    @Override
    public String getPath()
    {
        return "/workload";
    }

    @Override
    public String getIcon()
    {
        return "images/attribution.png";
    }

    @Override
    public String getLabel()
    {
        return "Workload";
    }

    /**
     * Only admins and project managers can see this page
     */
    @Override
    public boolean applies()
    {
        Project sessionProject = Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT);
        if (sessionProject == null) {
            return false;
        }

        // The project object stored in the session is detached from the persistence context and
        // cannot be used immediately in DB interactions. Fetch a fresh copy from the DB.
        Project project = projectService.getProject(sessionProject.getId());

        // Visible if the current user is a curator or project admin
        User user = userRepo.getCurrentUser();

        System.out.println("---------------WORKLOAD MENU ITEM----------");
        System.out.println(workloadAndWorkflowService.getWorkloadManager(sessionProject));
        return (projectService.isCurator(project, user)
            || projectService.isProjectAdmin(project, user))
            && WebAnnoConst.PROJECT_TYPE_ANNOTATION.equals(project.getMode())
            && !workloadAndWorkflowService.getWorkloadManager(project).equals("Default monitoring page");
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return DynamicWorkloadManagementPage.class;
    }
}
