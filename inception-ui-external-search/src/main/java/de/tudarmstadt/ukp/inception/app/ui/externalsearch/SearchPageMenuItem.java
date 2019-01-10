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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch;

import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.ui.core.session.SessionMetaData;

@Order(50)
public class SearchPageMenuItem
    implements MenuItem
{
    private @Autowired ExternalSearchService externalSearchService;
    private @Autowired ProjectService projectService;
    
    @Override
    public String getPath()
    {
        return "/search";
    }

    @Override
    public String getIcon()
    {
        return "images/magnifier.png";
    }

    @Override
    public String getLabel()
    {
        return "Search";
    }

    @Override
    public boolean applies()
    {
        Project sessionProject = Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT);
        if (sessionProject == null) {
            return false;
        }

        // The project object stored in the session is detached from the persistence context and
        // cannot be used immediately in DB interactions. Fetch a fresh copy from the DB.
        Project project = projectService.getProject(sessionProject.getId());

        return !externalSearchService.listDocumentRepositories(project).isEmpty();
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return SearchPage.class;
    }
}
