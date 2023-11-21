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
package de.tudarmstadt.ukp.inception.app.config;

import static de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil.CFG_AUTH_MODE;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.BaseLoggers.BOOT_LOG;

import java.io.File;
import java.io.IOException;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.unit.DataSize;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.BaseLoggers;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LoggingFilter;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService;

public class InceptionApplicationContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext>
{
    private static final String AUTH_MODE_PREAUTH = "preauth";

    @Override
    public void initialize(ConfigurableApplicationContext aApplicationContext)
    {
        LoggingFilter.setLoggingUsername("SYSTEM");

        ConfigurableEnvironment aEnvironment = aApplicationContext.getEnvironment();

        File settings = SettingsUtil.getSettingsFileLocation();

        BaseLoggers.BOOT_LOG.info("Settings: {} {}", settings,
                settings.exists() ? "(file exists)" : "(file does not exist)");

        // If settings were found, add them to the environment
        if (settings.exists()) {
            try {
                aEnvironment.getPropertySources()
                        .addFirst(new ResourcePropertySource(new FileSystemResource(settings)));
            }
            catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        // Activate bean profile depending on authentication mode
        if (AUTH_MODE_PREAUTH.equals(aEnvironment.getProperty(CFG_AUTH_MODE))) {
            aEnvironment.addActiveProfile(DeploymentModeService.PROFILE_AUTH_MODE_EXTERNAL_PREAUTH);
            BOOT_LOG.info("Authentication: pre-auth");
        }
        else {
            aEnvironment.addActiveProfile(DeploymentModeService.PROFILE_AUTH_MODE_DATABASE);
            BOOT_LOG.info("Authentication: database");
        }

        Runtime rt = Runtime.getRuntime();
        BOOT_LOG.info("Max. application memory: {}MB",
                DataSize.ofBytes(rt.maxMemory()).toMegabytes());
    }
}
