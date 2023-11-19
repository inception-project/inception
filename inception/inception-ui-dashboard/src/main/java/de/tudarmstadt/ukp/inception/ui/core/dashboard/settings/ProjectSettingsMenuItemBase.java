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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.settings;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import wicket.contrib.input.events.key.KeyType;

public abstract class ProjectSettingsMenuItemBase
    implements ProjectMenuItem
{
    private @Autowired UserDao userRepo;
    private @Autowired ProjectService projectService;

    @Override
    public boolean applies(Project aProject)
    {
        return userRepo.isCurrentUserAdmin()
                || projectService.hasRole(userRepo.getCurrentUser(), aProject, MANAGER);
    }

    @Override
    public KeyType[] shortcut()
    {
        return null;
    }
}
