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
package de.tudarmstadt.ukp.inception.ui.kb;

import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.inception.app.session.SessionMetaData;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;

@Component
@Order(220)
public class KnowledgeBasePageMenuItem implements MenuItem
{
    private @Autowired UserDao userRepo;
    private @Autowired ProjectService projectService;
    private @Autowired KnowledgeBaseService kbService;
    
    @Override
    public String getIcon()
    {
        return "images/books.png";
    }
    
    @Override
    public String getLabel()
    {
        return "Knowledge Base";
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

        // Not visible if the current user is not an annotator
        User user = userRepo.getCurrentUser();
        if (!(SecurityUtil.isAnnotator(project, projectService, user)
                && WebAnnoConst.PROJECT_TYPE_ANNOTATION.equals(project.getMode()))) {
            return false;
        }
        
        // not visible if the current project does not have knowledge bases
        return !kbService.getKnowledgeBases(project).isEmpty();
    }
    
    @Override
    public Class<? extends Page> getPageClass()
    {
        return KnowledgeBasePage.class;
    }
}
