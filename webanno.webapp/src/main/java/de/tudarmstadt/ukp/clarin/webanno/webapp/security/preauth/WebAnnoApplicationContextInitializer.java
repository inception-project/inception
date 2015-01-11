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

public class WebAnnoApplicationContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext>
{
    private static final String BEAN_PROFILE_PREAUTH = "preAuth";
    private static final String BEAN_PROFILE_DATABASE = "normal";
    private static final String AUTH = "auth";
    private static final String USER_HOME_WEBANNO_SETTING_PROPERTIES = "/.webanno/settings.properties";
    private static final String USER_HOME_PROPERTY = "user.home";
    private static final String WEBANNO_HOME_PROPERTY = "webanno.home";
    private static final String WEBANNO_HOME_SETTING_PROPERTIES = "/settings.properties";
    private static final String SETTING_PROPERTIES_PREAUTH = "preauth";

    private final Log log = LogFactory.getLog(getClass());

    @Override
    public void initialize(ConfigurableApplicationContext aApplicationContext)
    {
        ConfigurableEnvironment aEnvironment = aApplicationContext.getEnvironment();
        boolean useDefaultBeanProfile = true;

        try {
            File webannoSettingsViaProp = new File(
                    System.getProperty(WEBANNO_HOME_PROPERTY, "null")
                            + WEBANNO_HOME_SETTING_PROPERTIES);

            File webannoSettingsUserHome = new File(System.getProperty(USER_HOME_PROPERTY)
                    + USER_HOME_WEBANNO_SETTING_PROPERTIES);

            // check if settings.properties file exists in webanno.home directory
            if (System.getProperty(WEBANNO_HOME_PROPERTY) != null
                    && webannoSettingsViaProp.exists()) {
                log.info("Settings: " + webannoSettingsViaProp);
                
                aEnvironment.getPropertySources().addFirst(
                        new ResourcePropertySource(new FileSystemResource(webannoSettingsViaProp)));

                if (aEnvironment.containsProperty(AUTH)
                        && aEnvironment.getProperty(AUTH).equals(SETTING_PROPERTIES_PREAUTH)) {
                    useDefaultBeanProfile = false;
                }
            }
            // check if settings.properties file exists in user.home directory
            else if (System.getProperty(USER_HOME_PROPERTY) != null
                    && webannoSettingsUserHome.exists()) {
                log.info("Settings: " + webannoSettingsUserHome);
                
                aEnvironment.getPropertySources()
                        .addFirst(
                                new ResourcePropertySource(new FileSystemResource(
                                        webannoSettingsUserHome)));

                if (aEnvironment.containsProperty(AUTH)
                        && aEnvironment.getProperty(AUTH).equals(SETTING_PROPERTIES_PREAUTH)) {
                    useDefaultBeanProfile = false;
                }
            }

            // activating bean profile based on settings.properties file in user.home or
            // webanno.home if file does not exist in any directory then default database
            // authentication will be used
            if (useDefaultBeanProfile) {
                aEnvironment.setActiveProfiles(BEAN_PROFILE_DATABASE);
                log.info("Authentication: database");
            }
            else {
                aEnvironment.setActiveProfiles(BEAN_PROFILE_PREAUTH);
                log.info("Authentication: pre-auth");
            }

            // aApplicationContext.refresh();
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
