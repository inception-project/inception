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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.page;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.inception.curation.settings.CurationManagerPrefs.KEY_CURATION_MANAGER_PREFS;
import static de.tudarmstadt.ukp.inception.curation.settings.CurationPageType.WEBANNO;
import static java.lang.String.format;

import org.apache.wicket.Page;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.IconType;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.servlet.ServletContext;
import wicket.contrib.input.events.key.KeyType;

@ConditionalOnWebApplication
@Order(200)
public class LegacyCurationPageMenuItem
    implements ProjectMenuItem
{
    private final UserDao userRepo;
    private final ProjectService projectService;
    private final ServletContext servletContext;
    private final PreferencesService preferencesService;

    public LegacyCurationPageMenuItem(UserDao aUserRepo, ProjectService aProjectService,
            ServletContext aServletContext, PreferencesService aPreferencesService)
    {
        userRepo = aUserRepo;
        projectService = aProjectService;
        servletContext = aServletContext;
        preferencesService = aPreferencesService;
    }

    @Override
    public String getPath()
    {
        return "/curate-split";
    }

    public String getUrl(Project aProject, long aDocumentId)
    {
        String p = aProject.getSlug() != null ? aProject.getSlug()
                : String.valueOf(aProject.getId());

        return format("%s/p/%s%s/%d", servletContext.getContextPath(), p, getPath(), aDocumentId);
    }

    @Override
    public IconType getIcon()
    {
        return FontAwesome5IconType.clipboard_s;
    }

    @Override
    public String getLabel()
    {
        return "Curation";
    }

    @Override
    public boolean applies(Project aProject)
    {
        if (aProject == null) {
            return false;
        }

        var prefs = preferencesService.loadDefaultTraitsForProject(KEY_CURATION_MANAGER_PREFS,
                aProject);
        if (prefs.getCurationPageType() != WEBANNO) {
            return false;
        }

        // Visible if the current user is a curator
        User user = userRepo.getCurrentUser();
        return projectService.hasRole(user, aProject, CURATOR);
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return LegacyCurationPage.class;
    }

    @Override
    public KeyType[] shortcut()
    {
        return new KeyType[] { KeyType.Alt, KeyType.c };
    }
}
