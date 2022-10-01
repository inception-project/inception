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

import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

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
    public boolean canManageProject(String aProjectId)
    {
        return canManageProject(userService.getCurrentUsername(), aProjectId);
    }

    public boolean canManageProject(String aUser, String aProjectId)
    {
        log.trace("Permission check: canManageProject [user: {}] [project: {}]", aUser, aProjectId);

        try {
            User user = getUser(aUser);
            Project project = getProject(aProjectId);

            return projectService.hasRole(user, project, PermissionLevel.MANAGER);
        }
        catch (NoResultException | AccessDeniedException e) {
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
