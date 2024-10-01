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

import static de.tudarmstadt.ukp.inception.support.SettingsUtil.CFG_AUTH_MODE;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_AUTH_MODE_DATABASE;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_AUTH_MODE_EXTERNAL_PREAUTH;
import static de.tudarmstadt.ukp.inception.support.logging.BaseLoggers.BOOT_LOG;

import java.io.IOException;
import java.util.HashMap;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.unit.DataSize;

import de.tudarmstadt.ukp.inception.support.SettingsUtil;
import de.tudarmstadt.ukp.inception.support.db.InceptionHSQLDialect;
import de.tudarmstadt.ukp.inception.support.db.InceptionMySQLDialect;
import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;
import de.tudarmstadt.ukp.inception.support.logging.LoggingFilter;

public class InceptionApplicationContextInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext>
{
    private static final String AUTH_MODE_PREAUTH = "preauth";

    @Override
    public void initialize(ConfigurableApplicationContext aApplicationContext)
    {
        LoggingFilter.setLoggingUsername("SYSTEM");

        var aEnvironment = aApplicationContext.getEnvironment();

        loadSettings(aEnvironment);

        applyDatabaseSpecificOverrides(aEnvironment);

        activateAuthenticationProfiles(aEnvironment);

        Runtime rt = Runtime.getRuntime();
        BOOT_LOG.info("Max. application memory: {}MB",
                DataSize.ofBytes(rt.maxMemory()).toMegabytes());
    }

    private void loadSettings(ConfigurableEnvironment aEnvironment)
    {
        var settings = SettingsUtil.getSettingsFileLocation();

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
    }

    private void applyDatabaseSpecificOverrides(ConfigurableEnvironment aEnvironment)
    {
        var datasourceUrl = aEnvironment.getProperty("spring.datasource.url");
        if (datasourceUrl == null) {
            return;
        }

        if (datasourceUrl.startsWith("jdbc:hsqldb:")) {
            var overrides = new HashMap<String, Object>();
            overrides.put("spring.jpa.properties.hibernate.dialect",
                    InceptionHSQLDialect.class.getName());
            // Existing HSQLDBs will have all-caps table and column names - if we quote, it will
            // break loading these.
            overrides.put("spring.jpa.properties.hibernate.globally_quoted_identifiers", false);
            aEnvironment.getPropertySources()
                    .addFirst(new MapPropertySource("HSQLDB-Overrides", overrides));
        }

        if (datasourceUrl.startsWith("jdbc:postgresql:")) {
            var overrides = new HashMap<String, Object>();
            // PostgreSQL does not like unquoted mixed-case columns - but we have those, e.g.
            // the `curationSettings_users` table.
            overrides.put("spring.jpa.properties.hibernate.globally_quoted_identifiers", true);
            aEnvironment.getPropertySources()
                    .addFirst(new MapPropertySource("PostgreSQL-Overrides", overrides));
        }

        if (datasourceUrl.startsWith("jdbc:sqlserver:")) {
            var overrides = new HashMap<String, Object>();
            // We use certain column names like `user` that are reserved words in MS SQL Server
            overrides.put("spring.jpa.properties.hibernate.globally_quoted_identifiers", true);
            aEnvironment.getPropertySources()
                    .addFirst(new MapPropertySource("MS-SQL-Server-Overrides", overrides));
        }

        if (datasourceUrl.startsWith("jdbc:mysql:")) {
            var overrides = new HashMap<String, Object>();
            // We use certain column names like `rank` that are reserved words in MySQL
            overrides.put("spring.jpa.properties.hibernate.globally_quoted_identifiers", true);
            overrides.put("spring.jpa.properties.hibernate.dialect",
                    InceptionMySQLDialect.class.getName());
            aEnvironment.getPropertySources()
                    .addFirst(new MapPropertySource("MySQL-Overrides", overrides));
        }
    }

    private void activateAuthenticationProfiles(ConfigurableEnvironment aEnvironment)
    {
        // Activate bean profile depending on authentication mode
        if (AUTH_MODE_PREAUTH.equals(aEnvironment.getProperty(CFG_AUTH_MODE))) {
            aEnvironment.addActiveProfile(PROFILE_AUTH_MODE_EXTERNAL_PREAUTH);
            BOOT_LOG.info("Authentication: pre-auth");
        }
        else {
            aEnvironment.addActiveProfile(PROFILE_AUTH_MODE_DATABASE);
            BOOT_LOG.info("Authentication: database");
        }
    }
}
