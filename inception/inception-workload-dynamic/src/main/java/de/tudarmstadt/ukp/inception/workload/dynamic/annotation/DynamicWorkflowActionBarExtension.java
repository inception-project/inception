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
package de.tudarmstadt.ukp.inception.workload.dynamic.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension.DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.workload.dynamic.config.DynamicWorkloadManagerAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DynamicWorkloadManagerAutoConfiguration#dynamicWorkflowActionBarExtension}
 * </p>
 */
public class DynamicWorkflowActionBarExtension
    implements ActionBarExtension
{
    private final WorkloadManagementService workloadManagementService;
    private final ProjectService projectService;

    @Autowired
    public DynamicWorkflowActionBarExtension(WorkloadManagementService aWorkloadManagementService,
            ProjectService aProjectService)
    {
        workloadManagementService = aWorkloadManagementService;
        projectService = aProjectService;
    }

    @Override
    public String getRole()
    {
        return WorkloadManagerExtension.WORKLOAD_ACTION_BAR_ROLE;
    }

    @Override
    public int getPriority()
    {
        return 1;
    }

    @Override
    public boolean accepts(AnnotationPageBase aPage)
    {
        // #Issue 1813 fix
        if (aPage.getModelObject().getProject() == null) {
            return false;
        }

        // Curator are excluded from the feature
        return DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID
                .equals(workloadManagementService.loadOrCreateWorkloadManagerConfiguration(
                        aPage.getModelObject().getProject()).getType())
                && !projectService.hasRole(aPage.getModelObject().getUser(),
                        aPage.getModelObject().getProject(), CURATOR);
    }

    @Override
    public Panel createActionBarItem(String aID, AnnotationPageBase aAnnotationPageBase)
    {
        return new DynamicAnnotatorWorkflowActionBarItemGroup(aID, aAnnotationPageBase);
    }
}
