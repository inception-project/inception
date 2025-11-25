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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.giffing.wicket.spring.boot.starter.configuration.extensions.core.csrf.CsrfAttacksPreventionProperties;

import de.tudarmstadt.ukp.inception.ui.core.config.DashboardPropertiesImpl;
import de.tudarmstadt.ukp.inception.ui.core.config.ProjectUiPropertiesImpl;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.AdminDashboardPageMenuBarItemSupport;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.dashlet.SystemStatusService;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.dashlet.SystemStatusServiceImpl;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.ProjectDashboardDashletExtension;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.ProjectDashboardDashletExtensionPoint;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.ProjectDashboardDashletExtensionPointImpl;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPageMenuBarItemSupport;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist.ProjectsOverviewPageMenuBarItemSupport;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.ProjectSettingsDashboardMenuItem;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.annotation.AnnotationPreferencesMenuItem;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.casdoctor.ProjectCasDoctorMenuItem;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.constraints.ProjectConstraintsMenuItem;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.dangerzone.ProjectDangerZoneMenuItem;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.details.ProjectDetailMenuItem;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.documents.ProjectDocumentsMenuItem;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.export.LegacyProjectExportMenuItem;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.export.ProjectExportMenuItem;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.layers.ProjectLayersMenuItem;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.users.ProjectUsersMenuItem;

@ConditionalOnWebApplication
@Configuration
@EnableConfigurationProperties({ DashboardPropertiesImpl.class, ProjectUiPropertiesImpl.class })
public class DashboardAutoConfiguration
{
    @Bean
    public ProjectSettingsDashboardMenuItem projectSettingsDashboardMenuItem()
    {
        return new ProjectSettingsDashboardMenuItem();
    }

    @Bean
    @ConditionalOnExpression("${websocket.enabled:true} and !${dashboard.legacy-export.enabled:false}")
    public ProjectExportMenuItem projectExportMenuItem()
    {
        return new ProjectExportMenuItem();
    }

    @Bean
    @ConditionalOnExpression("!${websocket.enabled:true} or ${dashboard.legacy-export.enabled:false}")
    @Deprecated
    public LegacyProjectExportMenuItem legacyProjectExportMenuItem()
    {
        return new LegacyProjectExportMenuItem();
    }

    @Bean
    public ProjectDashboardDashletExtensionPoint projectDashboardDashletExtensionPoint(
            @Lazy @Autowired(required = false) List<ProjectDashboardDashletExtension> aExtensions)
    {
        return new ProjectDashboardDashletExtensionPointImpl(aExtensions);
    }

    @Bean
    public ProjectsOverviewPageMenuBarItemSupport projectsOverviewPageMenuBarItemSupport()
    {
        return new ProjectsOverviewPageMenuBarItemSupport();
    }

    @Bean
    public ProjectDashboardPageMenuBarItemSupport projectDashboardPageMenuBarItemSupport()
    {
        return new ProjectDashboardPageMenuBarItemSupport();
    }

    @Bean
    public AdminDashboardPageMenuBarItemSupport adminDashboardPageMenuBarItemSupport()
    {
        return new AdminDashboardPageMenuBarItemSupport();
    }

    @Bean
    public SystemStatusService systemStatusService(
            CsrfAttacksPreventionProperties aCsrfAttacksPreventionProperties)
    {
        return new SystemStatusServiceImpl(aCsrfAttacksPreventionProperties);
    }

    @Bean
    public AnnotationPreferencesMenuItem annotationPreferencesMenuItem()
    {
        return new AnnotationPreferencesMenuItem();
    }

    @Bean
    public ProjectCasDoctorMenuItem projectCasDoctorMenuItem()
    {
        return new ProjectCasDoctorMenuItem();
    }

    @Bean
    public ProjectConstraintsMenuItem projectConstraintsMenuItem()
    {
        return new ProjectConstraintsMenuItem();
    }

    @Bean
    public ProjectDangerZoneMenuItem projectDangerZoneMenuItem()
    {
        return new ProjectDangerZoneMenuItem();
    }

    @Bean
    public ProjectDetailMenuItem projectDetailMenuItem()
    {
        return new ProjectDetailMenuItem();
    }

    @Bean
    public ProjectDocumentsMenuItem projectDocumentsMenuItem()
    {
        return new ProjectDocumentsMenuItem();
    }

    @Bean
    public ProjectLayersMenuItem projectLayersMenuItem()
    {
        return new ProjectLayersMenuItem();
    }

    @Bean
    public ProjectUsersMenuItem projectUsersMenuItem()
    {
        return new ProjectUsersMenuItem();
    }
}
