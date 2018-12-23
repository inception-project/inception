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

import java.util.List;
import java.util.Properties;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;

/**
 * This class contains Utility methods that can be used in Project settings
 */
public class SecurityUtil
{
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
    
    /**
     * @deprecated Use {@link UserDao#getRoles(User)}
     */
    @Deprecated
    public static Set<String> getRoles(UserDao aUserRepository, User aUser)
    {
        return aUserRepository.getRoles(aUser);
    }
    
    /**
     * @deprecated Use {@link UserDao#isAdministrator(User)}
     */
    @Deprecated
    public static boolean isSuperAdmin(UserDao aUserRepository, User aUser)
    {
        return aUserRepository.isAdministrator(aUser);
    }

    /**
     * @deprecated Use {@link UserDao#isAdministrator(User)}
     */
    @Deprecated
    public static boolean isSuperAdmin(ProjectService aProjectRepository, User aUser)
    {
        return ApplicationContextProvider.getApplicationContext().getBean(UserDao.class)
                .isAdministrator(aUser);
    }

    /**
     * @deprecated Use {@link UserDao#isProjectCreator(User)}
     */
    @Deprecated
    public static boolean isProjectCreator(UserDao aUserRepository, User aUser)
    {
        return aUserRepository.isProjectCreator(aUser);
    }

    /**
     * @deprecated Use {@link ProjectService#isManager(Project, User)}
     */
    @Deprecated
    public static boolean isProjectAdmin(Project aProject, ProjectService aProjectRepository,
            User aUser)
    {
        return aProjectRepository.isManager(aProject, aUser);
    }

    /**
     * @deprecated Use {@link ProjectService#isCurator(Project, User)}
     */
    @Deprecated
    public static boolean isCurator(Project aProject, ProjectService aProjectRepository, User aUser)
    {
        return aProjectRepository.isCurator(aProject, aUser);
    }

    /**
     * @deprecated Use {@link ProjectService#isAnnotator(Project, User)}
     */
    @Deprecated
    public static boolean isAnnotator(Project aProject, ProjectService aProjectRepository,
            User aUser)
    {
        return aProjectRepository.isAnnotator(aProject, aUser);
    }
    
    /**
     * @deprecated Use {@link ProjectService#isAdmin(Project, User)}
     */
    @Deprecated
    public static boolean isAdmin(Project aProject, ProjectService aProjectRepository, User aUser)
    {
        return aProjectRepository.isAdmin(aProject, aUser);
    }
    
    /**
     * @deprecated Use {@link ProjectService#managesAnyProject(User)}
     */
    @Deprecated
    public static boolean projectSettingsEnabeled(ProjectService repository, User user)
    {
        return repository.managesAnyProject(user);
    }

    public static boolean curationEnabeled(ProjectService repository, User user)
    {
        for (Project project : repository.listProjects()) {
            if (repository.isCurator(project, user)) {
                return true;
            }
        }
        
        return false;
    }

    public static boolean annotationEnabeled(ProjectService aRepository, User aUser, String aMode)
    {
        for (Project project : aRepository.listProjects()) {
            if (aRepository.isAnnotator(project, aUser)
                    && aMode.equals(project.getMode())) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean monitoringEnabeled(ProjectService repository, User user)
    {
        for (Project project : repository.listProjects()) {
            if (repository.isCurator(project, user)
                    || repository.isManager(project, user)) {
                return true;
            }
        }
        
        return false;
    }

}
