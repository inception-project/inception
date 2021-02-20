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

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.unit.DataSize;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LoggingFilter;

public class InceptionApplicationContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext>
{
    public static final String PROFILE_PREAUTH = "auto-mode-preauth";
    public static final String PROFILE_DATABASE = "auto-mode-builtin";

    private static final String AUTH_MODE_PREAUTH = "preauth";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void initialize(ConfigurableApplicationContext aApplicationContext)
    {
        LoggingFilter.setLoggingUsername("SYSTEM");

        ConfigurableEnvironment aEnvironment = aApplicationContext.getEnvironment();

        File settings = SettingsUtil.getSettingsFile();

        // If settings were found, add them to the environment
        if (settings != null) {
            log.info("Settings: " + settings);
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
            aEnvironment.setActiveProfiles(PROFILE_PREAUTH);
            log.info("Authentication: pre-auth");
        }
        else {
            aEnvironment.setActiveProfiles(PROFILE_DATABASE);
            log.info("Authentication: database");
        }

        Runtime rt = Runtime.getRuntime();
        log.info("Max. application memory: {}MB", DataSize.ofBytes(rt.maxMemory()).toMegabytes());

    }
}
