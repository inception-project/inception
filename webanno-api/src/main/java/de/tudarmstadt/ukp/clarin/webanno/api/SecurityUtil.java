/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;

/**
 * This class contains Utility methods that can be used in Project settings
 */
public class SecurityUtil
{
    private static final Logger LOG = LoggerFactory.getLogger(SecurityUtil.class);

    public static boolean isProfileSelfServiceAllowed()
    {
        // If users are allowed to access their profile information, the also need to access the
        // admin area. Note: access to the users own profile should be handled differently.
        List<String> activeProfiles = asList(ApplicationContextProvider.getApplicationContext()
                .getEnvironment().getActiveProfiles());
        Properties settings = SettingsUtil.getSettings();
        return !activeProfiles.contains("auto-mode-preauth") && "true"
                        .equals(settings.getProperty(SettingsUtil.CFG_USER_ALLOW_PROFILE_ACCESS));
    }
    
    public static Set<String> getRoles(ProjectService aProjectRepository, User aUser)
    {
        // When looking up roles for the user who is currently logged in, then we look in the
        // security context - otherwise we ask the database.
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Set<String> roles = new HashSet<>();
        if (aUser.getUsername().equals(username)) {
            for (GrantedAuthority ga : SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities()) {
                roles.add(ga.getAuthority());
            }
        }
        else {
            for (Authority a : aProjectRepository.listAuthorities(aUser)) {
                roles.add(a.getAuthority());
            }
        }
        return roles;
    }
    
    /**
     * IS user super Admin
     * 
     * @param aProjectRepository the repository service.
     * @param aUser the user.
     * @return if the user is a global admin.
     */
    public static boolean isSuperAdmin(ProjectService aProjectRepository, User aUser)
    {
        boolean roleAdmin = false;
        for (String role : getRoles(aProjectRepository, aUser)) {
            if (Role.ROLE_ADMIN.name().equals(role)) {
                roleAdmin = true;
                break;
            }
        }
        return roleAdmin;
    }

    /**
     * IS project creator
     * 
     * @param aProjectRepository the repository service.
     * @param aUser the user.
     * @return if the user is a project creator
     */
    public static boolean isProjectCreator(ProjectService aProjectRepository, User aUser)
    {
        boolean roleAdmin = false;
        for (String role : getRoles(aProjectRepository, aUser)) {
            if (Role.ROLE_PROJECT_CREATOR.name().equals(role)) {
                roleAdmin = true;
                break;
            }
        }
        return roleAdmin;
    }

    /**
     * Determine if the User is allowed to update a project
     *
     * @param aProject the project
     * @param aProjectRepository the repository service.
     * @param aUser the user.
     * @return if the user may update a project.
     */
    public static boolean isProjectAdmin(Project aProject, ProjectService aProjectRepository,
            User aUser)
    {
        boolean projectAdmin = false;
        try {
            List<ProjectPermission> permissionLevels = aProjectRepository
                    .listProjectPermissionLevel(aUser, aProject);
            for (ProjectPermission permissionLevel : permissionLevels) {
                if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                        PermissionLevel.ADMIN.getName())) {
                    projectAdmin = true;
                    break;
                }
            }
        }
        catch (NoResultException ex) {
            LOG.info("No permision is given to this user " + ex);
        }

        return projectAdmin;
    }

    /**
     * Determine if the User is a curator or not
     *
     * @param aProject the project.
     * @param aProjectRepository the respository service.
     * @param aUser the user.
     * @return if the user is a curator.
     */
    public static boolean isCurator(Project aProject, ProjectService aProjectRepository,
            User aUser)
    {
        boolean curator = false;
        try {
            List<ProjectPermission> permissionLevels = aProjectRepository
                    .listProjectPermissionLevel(aUser, aProject);
            for (ProjectPermission permissionLevel : permissionLevels) {
                if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                        PermissionLevel.CURATOR.getName())) {
                    curator = true;
                    break;
                }
            }
        }
        catch (NoResultException ex) {
            LOG.info("No permision is given to this user " + ex);
        }

        return curator;
    }

    /**
     * Determine if the User is member of a project
     *
     * @param aProject the project.
     * @param aProjectRepository the repository service.
     * @param aUser the user.
     * @return if the user is a member.
     */
    public static boolean isAnnotator(Project aProject, ProjectService aProjectRepository,
            User aUser)
    {
        boolean user = false;
        try {
            List<ProjectPermission> permissionLevels = aProjectRepository
                    .listProjectPermissionLevel(aUser, aProject);
            for (ProjectPermission permissionLevel : permissionLevels) {
                if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                        PermissionLevel.USER.getName())) {
                    user = true;
                    break;
                }
            }
        }

        catch (NoResultException ex) {
            LOG.info("No permision is given to this user " + ex);
        }

        return user;
    }
    
    /**
     * Determine if the User is an admin of a project
     *
     * @param aProject the project.
     * @param aProjectRepository the repository service.
     * @param aUser the user.
     * @return if the user is an admin.
     */
    public static boolean isAdmin(Project aProject, ProjectService aProjectRepository,
            User aUser)
    {
        boolean user = false;
        try {
            List<ProjectPermission> permissionLevels = aProjectRepository
                    .listProjectPermissionLevel(aUser, aProject);
            for (ProjectPermission permissionLevel : permissionLevels) {
                if (StringUtils.equalsIgnoreCase(permissionLevel.getLevel().getName(),
                        PermissionLevel.ADMIN.getName())) {
                    user = true;
                    break;
                }
            }
        }

        catch (NoResultException ex) {
            LOG.info("No permision is given to this user " + ex);
        }

        return user;
    }
    
    public static boolean projectSettingsEnabeled(ProjectService repository, User user)
    {
        if (SecurityUtil.isSuperAdmin(repository, user)) {
            return true;
        }

        if (SecurityUtil.isProjectCreator(repository, user)) {
            return true;
        }

        for (Project project : repository.listProjects()) {
            if (SecurityUtil.isProjectAdmin(project, repository, user)) {
                return true;
            }
        }
        
        return false;
    }

    public static boolean curationEnabeled(ProjectService repository, User user)
    {
        for (Project project : repository.listProjects()) {
            if (SecurityUtil.isCurator(project, repository, user)) {
                return true;
            }
        }
        
        return false;
    }

    public static boolean annotationEnabeled(ProjectService aRepository, User aUser, String aMode)
    {
        for (Project project : aRepository.listProjects()) {
            if (SecurityUtil.isAnnotator(project, aRepository, aUser)
                    && aMode.equals(project.getMode())) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean monitoringEnabeled(ProjectService repository, User user)
    {
        for (Project project : repository.listProjects()) {
            if (SecurityUtil.isCurator(project, repository, user)
                    || SecurityUtil.isProjectAdmin(project, repository, user)) {
                return true;
            }
        }
        
        return false;
    }

}
