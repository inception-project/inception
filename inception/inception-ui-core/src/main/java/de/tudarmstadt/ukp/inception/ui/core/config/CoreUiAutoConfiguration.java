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
package de.tudarmstadt.ukp.inception.ui.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.footer.VersionFooterItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.footer.WarningsFooterItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.users.ManageUsersPageMenuItem;
import de.tudarmstadt.ukp.inception.ui.core.about.AboutFooterItem;
import de.tudarmstadt.ukp.inception.ui.core.darkmode.DarkModeMenuBarItemSupport;
import de.tudarmstadt.ukp.inception.ui.core.darkmode.DarkModePropertiesImpl;
import de.tudarmstadt.ukp.inception.ui.core.log.LogPageMenuItem;
import de.tudarmstadt.ukp.inception.ui.core.menubar.HelpMenuBarItemSupport;

@Configuration
@EnableConfigurationProperties({ DarkModePropertiesImpl.class, ErrorPagePropertiesImpl.class })
public class CoreUiAutoConfiguration
{
    @ConditionalOnMissingBean(value = VersionFooterItem.class)
    @Bean
    public VersionFooterItem versionFooterItem()
    {
        return new VersionFooterItem();
    }

    @Bean
    public WarningsFooterItem warningsFooterItem()
    {
        return new WarningsFooterItem();
    }

    @Bean
    public AboutFooterItem aboutFooterItem()
    {
        return new AboutFooterItem();
    }

    @Bean
    public HelpMenuBarItemSupport helpMenuBarItemSupport()
    {
        return new HelpMenuBarItemSupport();
    }

    @ConditionalOnProperty(prefix = "ui.dark-mode", name = "enabled", havingValue = "true", matchIfMissing = false)
    @Bean
    public DarkModeMenuBarItemSupport darkModeMenuBarItemSupport()
    {
        return new DarkModeMenuBarItemSupport();
    }

    @Bean
    public LogPageMenuItem logPageMenuItem()
    {
        return new LogPageMenuItem();
    }

    @Bean
    public ManageUsersPageMenuItem manageUsersPageMenuItem()
    {
        return new ManageUsersPageMenuItem();
    }
}
