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

import java.io.Serializable;
import java.util.Optional;

import org.springframework.security.core.Authentication;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.PermissionExtension;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.persistence.NoResultException;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectServiceAutoConfiguration#projectPermissionExtension}.
 * </p>
 */
public class ProjectPermissionExtension
    implements PermissionExtension<Project, String>
{
    public static final String OBJ_PROJECT = "Project";

    private static final String ANY = "ANY";

    private final ProjectService projectService;
    private final UserDao userService;

    public ProjectPermissionExtension(UserDao aUserService, ProjectService aProjectService)
    {
        projectService = aProjectService;
        userService = aUserService;
    }

    @Override
    public String getId()
    {
        return "ProjectPermissionExtension";
    }

    @Override
    public boolean accepts(Object aContext)
    {
        return aContext instanceof Project || OBJ_PROJECT.equals(aContext);
    }

    @Override
    public boolean hasPermission(Authentication aAuthentication, Project aTargetDomainObject,
            String aPermission)
    {
        Project project = (Project) aTargetDomainObject;
        User user = userService.get(aAuthentication.getName());

        if (ANY.equals(aPermission)) {
            return projectService.hasAnyRole(user, project);
        }

        return projectService.hasRole(user, project, PermissionLevel.valueOf((String) aPermission));
    }

    @Override
    public boolean hasPermission(Authentication aAuthentication, Serializable aTargetId,
            String aTargetType, String aPermission)
    {
        switch (aTargetType) {
        case OBJ_PROJECT: {
            return getProject((String) aTargetId)
                    .map($ -> hasPermission(aAuthentication, $, aPermission)).get();
        }
        default:
            return false;
        }
    }

    private Optional<Project> getProject(String aTargetId)
    {
        try {
            try {
                return Optional.of(projectService.getProject(Long.parseLong(aTargetId)));
            }
            catch (NumberFormatException e) {
                // Ignore lookup by ID and try lookup by slug instead.
            }

            return Optional.of(projectService.getProjectBySlug(aTargetId));
        }
        catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
