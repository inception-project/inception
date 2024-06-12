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
package de.tudarmstadt.ukp.clarin.webanno.project;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.persistence.NoResultException;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectServiceAutoConfiguration#projectAccess}.
 * </p>
 */
public class ProjectAccessImpl
    implements ProjectAccess
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final UserDao userService;
    private final ProjectService projectService;

    public ProjectAccessImpl(UserDao aUserService, ProjectService aProjectService)
    {
        userService = aUserService;
        projectService = aProjectService;
    }

    @Override
    public boolean canCreateProjects()
    {
        var sessionOwner = userService.getCurrentUser();
        log.trace("Permission check: canCreateProjects [user: {}]", sessionOwner);

        if (userService.isProjectCreator(sessionOwner)) {
            return true;
        }

        if (userService.isAdministrator(sessionOwner)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean canAccessProject(String aProjectId)
    {
        return canAccessProject(userService.getCurrentUsername(), aProjectId);
    }

    public boolean canAccessProject(String aUser, String aProjectId)
    {
        log.trace("Permission check: canAccessProject [user: {}] [project: {}]", aUser, aProjectId);

        try {
            var user = getUser(aUser);
            var project = getProject(aProjectId);

            if (userService.isAdministrator(user)) {
                log.trace("Access granted: User {} can access project {} as administrator", user,
                        project);
                return true;
            }

            if (projectService.hasAnyRole(user, project)) {
                log.trace("Access granted: User {} can access project {} as project member", user,
                        project);
                return true;
            }

            return false;
        }
        catch (NoResultException | AccessDeniedException e) {
            log.trace("Access denied: prerequisites not met", e);
            // If any object does not exist, the user cannot view
            return false;
        }
    }

    @Override
    public boolean canManageProject(String aProjectId)
    {
        return canManageProject(userService.getCurrentUsername(), aProjectId);
    }

    public boolean canManageProject(String aUser, String aProjectId)
    {
        log.trace("Permission check: canManageProject [user: {}] [project: {}]", aUser, aProjectId);

        try {
            var user = getUser(aUser);
            var project = getProject(aProjectId);

            if (userService.isAdministrator(user)) {
                log.trace("Access granted: User {} can manage project {} as administrator", user,
                        project);
                return true;
            }

            if (projectService.hasRole(user, project, PermissionLevel.MANAGER)) {
                log.trace("Access granted: User {} can manage project {} as manager", user,
                        project);
                return true;
            }

            return false;
        }
        catch (NoResultException | AccessDeniedException e) {
            log.trace("Access denied: prerequisites not met", e);
            // If any object does not exist, the user cannot view
            return false;
        }
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
        User user = userService.get(aUser);

        // Does the user exist and is enabled?
        if (user == null || !user.isEnabled()) {
            throw new AccessDeniedException(
                    "User [" + aUser + "] does not exist or is not enabled");
        }

        return user;
    }
}
