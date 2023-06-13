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
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;

import org.apache.wicket.Page;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.PageRequestHandlerTracker;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.IconType;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import wicket.contrib.input.events.key.KeyType;

@Component
@Order(400)
public class ProjectPageMenuItem
    implements MenuItem
{
    private @Autowired UserDao userRepo;
    private @Autowired ProjectService projectService;

    @Override
    public String getPath()
    {
        return "/admin/project";
    }

    @Override
    public IconType getIcon()
    {
        return FontAwesome5IconType.archive_s;
    }

    @Override
    public String getLabel()
    {
        IRequestablePage currentPage = PageRequestHandlerTracker.getLastHandler(RequestCycle.get())
                .getPage();

        PageParameters pageParameters = currentPage.getPageParameters();

        if (pageParameters.get(PAGE_PARAM_PROJECT).isNull()) {
            pageParameters.add(PAGE_PARAM_PROJECT, ProjectSettingsPage.NEW_PROJECT_ID);
        }

        return new StringResourceModel("project.page.menuitem.label",
                new ProjectSettingsPage(pageParameters)).getString();

        // return new StringResourceModel("project.page.menuitem.label").getString();
    }

    /**
     * Only admins and project managers can see this page
     */
    @Override
    public boolean applies()
    {
        return projectService.managesAnyProject(userRepo.getCurrentUser());
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return ProjectSettingsPage.class;
    }

    @Override
    public KeyType[] shortcut()
    {
        return new KeyType[] { KeyType.Alt, KeyType.p };
    }
}
