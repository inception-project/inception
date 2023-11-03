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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.activity;

import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.ProjectDashboardDashletExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@Order(10000)
@Component
public class ActivitiesDashletExtension
    implements ProjectDashboardDashletExtension
{
    private WorkloadManagementService workloadService;

    @Autowired
    public ActivitiesDashletExtension(WorkloadManagementService aWorkloadService)
    {
        workloadService = aWorkloadService;
    }

    @Override
    public String getId()
    {
        return "activities";
    }

    @Override
    public boolean accepts(Project aContext)
    {
        return workloadService.getWorkloadManagerExtension(aContext)
                .isDocumentRandomAccessAllowed(aContext);
    }

    @Override
    public ActivitiesDashlet createDashlet(String aId, IModel<Project> aModel)
    {
        return new ActivitiesDashlet(aId, aModel);
    }
}
