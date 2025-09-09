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
package de.tudarmstadt.ukp.inception.pivot.page;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import wicket.contrib.input.events.key.KeyType;

@Order(300)
public class PivotTableMenuItem
    implements ProjectMenuItem
{
    private @Autowired UserDao userRepo;
    private @Autowired ProjectService projectService;
    private @Autowired AnnotationSchemaService schemaService;

    @Override
    public String getPath()
    {
        return "/explorer";
    }

    @Override
    public Component getIcon(String aId)
    {
        return new Icon(aId, FontAwesome5IconType.table_s);
    }

    @Override
    public String getLabel()
    {
        return "Explorer";
    }

    /**
     * Only admins and project managers can see this page
     */
    @Override
    public boolean applies(Project aProject)
    {
        if (schemaService.listEnabledLayers(aProject).isEmpty()) {
            return false;
        }
        
        // Visible if the current user is a curator or project admin
        var user = userRepo.getCurrentUser();
        return projectService.hasRole(user, aProject, CURATOR, MANAGER);
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return PivotTablePage.class;
    }

    @Override
    public KeyType[] shortcut()
    {
        return new KeyType[] { KeyType.Alt, KeyType.g };
    }
}
