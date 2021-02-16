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
package de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.page;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;

import org.apache.wicket.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;

@Component
@Order(300)
public class AgreementPageMenuItem
    implements ProjectMenuItem
{
    private @Autowired UserDao userRepo;
    private @Autowired ProjectService projectService;

    @Override
    public String getPath()
    {
        return "/agreement";
    }

    @Override
    public String getIcon()
    {
        return "images/statistics.png";
    }

    @Override
    public String getLabel()
    {
        return "Agreement";
    }

    /**
     * Only admins and project managers can see this page
     */
    @Override
    public boolean applies(Project aProject)
    {
        // Show agreement menu item only if we have at least 2 annotators or we cannot calculate
        // pairwise agreement
        if (projectService.listProjectUsersWithPermissions(aProject, ANNOTATOR).size() < 2) {
            return false;
        }

        // Visible if the current user is a curator or project admin
        User user = userRepo.getCurrentUser();
        return (projectService.isCurator(aProject, user)
                || projectService.isManager(aProject, user));
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return AgreementPage.class;
    }
}
