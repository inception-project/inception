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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.ProjectMenuItem;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.config.ExternalSearchUIAutoConfiguration;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import wicket.contrib.input.events.key.KeyType;

/**
 * Menu item for the external search page.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ExternalSearchUIAutoConfiguration#searchPageMenuItem()}.
 * </p>
 */
@Order(50)
public class SearchPageMenuItem
    implements ProjectMenuItem
{
    private @Autowired ExternalSearchService externalSearchService;

    @Override
    public String getPath()
    {
        return "/search";
    }

    @Override
    public Component getIcon(String aId)
    {
        return new Icon(aId, FontAwesome5IconType.database_s);
    }

    @Override
    public String getLabel()
    {
        return "Search";
    }

    @Override
    public boolean applies(Project aProject)
    {
        return externalSearchService.existsEnabledDocumentRepository(aProject);
    }

    @Override
    public Class<? extends Page> getPageClass()
    {
        return SearchPage.class;
    }

    @Override
    public KeyType[] shortcut()
    {
        return new KeyType[] { KeyType.Alt, KeyType.f };
    }
}
