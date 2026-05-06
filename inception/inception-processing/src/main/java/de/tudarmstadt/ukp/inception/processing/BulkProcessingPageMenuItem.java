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
package de.tudarmstadt.ukp.inception.processing;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static java.lang.String.format;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.servlet.ServletContext;
import wicket.contrib.input.events.key.KeyType;

@ConditionalOnWebApplication
@Order(250)
public class BulkProcessingPageMenuItem
    implements ProjectMenuItem
{
    private final UserDao userService;
    private final ProjectService projectService;
    private final ServletContext servletContext;

    public BulkProcessingPageMenuItem(UserDao aUserRepo, ProjectService aProjectService,
            ServletContext aServletContext)
    {
        userService = aUserRepo;
        projectService = aProjectService;
        servletContext = aServletContext;
    }

    @Override
    public String getPath()
    {
        return "/process";
    }

    public String getUrl(Project aProject, long aDocumentId)
    {
        var p = aProject.getSlug() != null ? aProject.getSlug() : String.valueOf(aProject.getId());

        return format("%s/p/%s%s/%d", servletContext.getContextPath(), p, getPath(), aDocumentId);
    }

    @Override
    public Component getIcon(String aId)
    {
        // return new Icon(aId, FontAwesome5IconType.robot_s);
        return new BulkProcessingSidebarIcon(aId);
    }

    @Override
    public String getLabel()
    {
        return "Process";
    }

    @Override
    public boolean applies(Project aProject)
    {
        if (aProject == null) {
            return false;
        }

        User user = userService.getCurrentUser();
        return projectService.hasRole(user, aProject, MANAGER);
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return BulkProcessingPage.class;
    }

    @Override
    public KeyType[] shortcut()
    {
        return new KeyType[] { KeyType.Alt, KeyType.p };
    }
}
