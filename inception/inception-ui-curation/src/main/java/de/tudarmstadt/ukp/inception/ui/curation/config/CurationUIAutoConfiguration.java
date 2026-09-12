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
package de.tudarmstadt.ukp.inception.ui.curation.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.ui.curation.editor.SplitCurationEditorFactory;
import de.tudarmstadt.ukp.inception.ui.curation.page.CurationPageMenuItem;
import de.tudarmstadt.ukp.inception.ui.curation.page.SplitCurationPageMenuItem;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.overview.CurationUnitOverviewSidebarFactory;
import jakarta.servlet.ServletContext;

@ConditionalOnWebApplication
@Configuration
public class CurationUIAutoConfiguration
{
    @Bean
    public CurationPageMenuItem curationPageMenuItem(UserDao aUserRepo,
            ProjectService aProjectService, ServletContext aServletContext,
            PreferencesService aPreferencesService,
            SplitCurationPageMenuItem aSplitCurationPageMenuItem)
    {
        return new CurationPageMenuItem(aUserRepo, aProjectService, aServletContext,
                aPreferencesService, aSplitCurationPageMenuItem);
    }

    /**
     * The bean name has to be {@link SplitCurationEditorFactory#ID} - editor factories are looked
     * up by bean name, and the split-curation page pins this one by that id.
     */
    @Bean(name = SplitCurationEditorFactory.ID)
    public SplitCurationEditorFactory splitCurationEditorFactory()
    {
        return new SplitCurationEditorFactory();
    }

    @Bean
    public CurationUnitOverviewSidebarFactory curationUnitOverviewSidebarFactory()
    {
        return new CurationUnitOverviewSidebarFactory();
    }
}
