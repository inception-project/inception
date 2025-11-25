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
package de.tudarmstadt.ukp.inception.scheduling;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;

import java.lang.invoke.MethodHandles;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.persistence.NoResultException;

public class TaskAccessImpl
    implements TaskAccess
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final UserDao userService;
    private final ProjectService projectService;

    public TaskAccessImpl(ProjectService aProjectService, UserDao aUserService)
    {
        userService = aUserService;
        projectService = aProjectService;
    }

    @Override
    public boolean canManageTasks(String aSessionOwner, String aProjectId)
    {
        LOG.trace("Permission check: canManageTasks [aSessionOwner: {}] [project: {}]",
                aSessionOwner, aProjectId);

        try {
            var user = getUser(aSessionOwner);
            var project = getProject(aProjectId);

            assertCanManageTasks(user, project);

            LOG.trace("Access granted: canManageTasks [sessionOwner: {}] [project: {}]",
                    aSessionOwner, aProjectId);
            return true;
        }
        catch (NoResultException | AccessDeniedException e) {
            LOG.trace("Access denied: prerequisites not met", e);
            // If any object does not exist, the user cannot view
            return false;
        }
    }

    @Override
    public void assertCanManageTasks(User aSessionOwner, Project aProject)
    {
        if (projectService.hasRole(aSessionOwner, aProject, MANAGER)
                || userService.isAdministrator(aSessionOwner)) {
            return;
        }

        throw new AccessDeniedException("You have no permission to manage tasks in this project");
    }

    private Project getProject(String aProjectId)
    {
        try {
            if (StringUtils.isNumeric(aProjectId)) {
                return projectService.getProject(Long.valueOf(aProjectId));
            }

            return projectService.getProjectBySlug(aProjectId);
        }
        catch (NoResultException e) {
            throw new AccessDeniedException("Project [" + aProjectId + "] does not exist");
        }
    }

    private User getUser(String aUser)
    {
        var user = userService.get(aUser);

        // Does the user exist and is enabled?
        if (user == null || !user.isEnabled()) {
            throw new AccessDeniedException(
                    "User [" + aUser + "] does not exist or is not enabled");
        }

        return user;
    }
}
