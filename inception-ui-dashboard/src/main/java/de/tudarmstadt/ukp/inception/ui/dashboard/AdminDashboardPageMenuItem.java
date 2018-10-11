/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.dashboard;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Properties;

import org.apache.wicket.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;

@Component
public class AdminDashboardPageMenuItem implements MenuItem
{
    private @Autowired UserDao userRepo;
    private @Autowired ProjectService projectService;
    private @Autowired ApplicationContext applicationContext;
    
    @Override
    public String getPath()
    {
        return "/admin";
    }
    
    @Override
    public String getIcon()
    {
        return "images/books.png";
    }
    
    @Override
    public String getLabel()
    {
        return "Administration";
    }
    
    @Override
    public boolean applies()
    {
        User user = userRepo.getCurrentUser();
        
        // Project managers need access to the admin area to manage projects
        if (SecurityUtil.projectSettingsEnabeled(projectService, user)) {
            return true;
        }
        
        // Admins need access to the admin area to manage projects 
        if (SecurityUtil.isSuperAdmin(projectService, user)) {
            return true;
        }

        // If users are allowed to access their profile information, the also need to access the
        // admin area. Note: access to the users own profile should be handled differently.
        List<String> activeProfiles = asList(
                applicationContext.getEnvironment().getActiveProfiles());
        Properties settings = SettingsUtil.getSettings();
        return !activeProfiles.contains("auto-mode-preauth") && "true"
                        .equals(settings.getProperty(SettingsUtil.CFG_USER_ALLOW_PROFILE_ACCESS));
    }
    
    @Override
    public Class<? extends Page> getPageClass()
    {
        return AdminDashboardPage.class;
    }
}
