/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.ui.kb;

import org.apache.wicket.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.ui.kb.config.KnowledgeBaseServiceUIAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link KnowledgeBaseServiceUIAutoConfiguration#knowledgeBasePageMenuItem}.
 * </p>
 */
@Order(220)
public class KnowledgeBasePageMenuItem
    implements ProjectMenuItem
{
    private final UserDao userRepo;
    private final ProjectService projectService;
    private final KnowledgeBaseService kbService;

    @Autowired
    public KnowledgeBasePageMenuItem(UserDao aUserRepo, ProjectService aProjectService,
            KnowledgeBaseService aKbService)
    {
        userRepo = aUserRepo;
        projectService = aProjectService;
        kbService = aKbService;
    }

    @Override
    public String getPath()
    {
        return "/knowledge-base";
    }

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
    public boolean applies(Project aProject)
    {
        if (aProject == null) {
            return false;
        }

        // Not visible if the current user is not an annotator
        User user = userRepo.getCurrentUser();
        if (!(projectService.isAnnotator(aProject, user))) {
            return false;
        }

        // not visible if the current project does not have knowledge bases
        return !kbService.getKnowledgeBases(aProject).isEmpty();
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return KnowledgeBasePage.class;
    }
}
