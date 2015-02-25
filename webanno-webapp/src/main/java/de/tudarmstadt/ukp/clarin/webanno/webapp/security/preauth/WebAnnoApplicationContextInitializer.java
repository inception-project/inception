/*******************************************************************************
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.security.preauth;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;

import de.tudarmstadt.ukp.clarin.webanno.webapp.home.page.SettingsUtil;

public class WebAnnoApplicationContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext>
{
    private static final String PROFILE_PREAUTH = "auto-mode-preauth";
    private static final String PROFILE_DATABASE = "auto-mode-builtin";

    private static final String PROP_AUTH_MODE = "auth.mode";
    private static final String AUTH_MODE_PREAUTH = "preauth";

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public void initialize(ConfigurableApplicationContext aApplicationContext)
    {
        log.info("  _      __    __   ___                ");
        log.info(" | | /| / /__ / /  / _ | ___  ___  ___ ");
        log.info(" | |/ |/ / -_) _ \\/ __ |/ _ \\/ _ \\/ _ \\");
        log.info(" |__/|__/\\__/_.__/_/ |_/_//_/_//_/\\___/");
        log.info(SettingsUtil.getVersionString());
        
        ConfigurableEnvironment aEnvironment = aApplicationContext.getEnvironment();

        File settings = SettingsUtil.getSettingsFile();
        
        // If settings were found, add them to the environment
        if (settings != null) {
            log.info("Settings: " + settings);
            try {
                aEnvironment.getPropertySources().addFirst(
                        new ResourcePropertySource(new FileSystemResource(settings)));
            }
            catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        // Activate bean profile depending on authentication mode
        if (AUTH_MODE_PREAUTH.equals(aEnvironment.getProperty(PROP_AUTH_MODE))) {
            aEnvironment.setActiveProfiles(PROFILE_PREAUTH);
            log.info("Authentication: pre-auth");
        }
        else {
            aEnvironment.setActiveProfiles(PROFILE_DATABASE);
            log.info("Authentication: database");
        }
    }
}