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
package de.tudarmstadt.ukp.clarin.webanno.api;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Properties;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
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
        return !activeProfiles.contains("auto-mode-preauth")
                && "true".equals(settings.getProperty(SettingsUtil.CFG_USER_ALLOW_PROFILE_ACCESS));
    }

    public static boolean curationEnabeled(ProjectService repository, User aUser)
    {
        if (aUser == null) {
            return false;
        }

        for (Project project : repository.listProjects()) {
            if (repository.isCurator(project, aUser)) {
                return true;
            }
        }

        return false;
    }

    public static boolean annotationEnabeled(ProjectService aRepository, User aUser)
    {
        if (aUser == null) {
            return false;
        }

        for (Project project : aRepository.listProjects()) {
            if (aRepository.isAnnotator(project, aUser)) {
                return true;
            }
        }

        return false;
    }
}
