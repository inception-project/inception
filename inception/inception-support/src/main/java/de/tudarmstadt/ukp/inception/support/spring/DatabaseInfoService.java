/*
# * Licensed to the Technische Universität Darmstadt under one
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
package de.tudarmstadt.ukp.inception.support.spring;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.rightPad;
import static org.apache.commons.lang3.reflect.FieldUtils.readField;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class DatabaseInfoService
    implements InitializingBean
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value(value = "${spring.jpa.properties.hibernate.dialect:#{null}}")
    private String databaseDialect;

    @Value(value = "${spring.datasource.driver-class-name:#{null}}")
    private String databaseDriver;

    @Value(value = "${spring.datasource.url:#{null}}")
    private String databaseUrl;

    @Value(value = "${spring.datasource.username:#{null}}")
    private String databaseUsername;

    private @PersistenceContext EntityManager entityManager;

    private @Autowired ConfigurableApplicationContext appContext;

    @Override
    public void afterPropertiesSet() throws Exception
    {
        List<Setting> settings = new ArrayList<>();
        settings.add(new Setting("Database URL", databaseUrl, databaseUrl));
        settings.add(new Setting("Database username", databaseUsername, databaseUsername));

        Session session = null;
        try {
            session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                DatabaseMetaData metadata = connection.getMetaData();
                settings.add(new Setting("Database driver",
                        metadata.getDriverName() + "  " + metadata.getDriverVersion(),
                        databaseDriver));
            });
        }
        catch (Exception e) {
            settings.add(new Setting("Database driver", e, databaseDriver,
                    "Please check that the required database driver is available on the classpath"));
        }

        if (session != null) {
            Dialect dialect = null;
            try {
                var sessionFactory = ((SessionFactoryImplementor) session.getSessionFactory());
                dialect = sessionFactory.getJdbcServices().getDialect();
                settings.add(new Setting("Database dialect", dialect.getClass().getName(),
                        databaseDialect));
            }
            catch (Exception e) {
                settings.add(new Setting("Database dialect", e, databaseDialect,
                        "Please check that the required database dialect available is on the classpath"));
            }

            if (dialect instanceof MySQLDialect) {
                try {
                    settings.add(new Setting("Database storage engine",
                            readField(dialect, "storageEngine", true).getClass().getName()));
                }
                catch (Exception e) {
                    // Ignore
                }
            }
        }

        int maxSettingNameLength = settings.stream().mapToInt(s -> s.name.length()).max().orElse(0);
        for (Setting setting : settings) {
            if (setting.exception != null) {
                log.error("{}: {} {}", rightPad(setting.name, maxSettingNameLength),
                        setting.exception.getMessage(), setting.status());
                if (setting.hint != null) {
                    log.error("{} {}", StringUtils.repeat(" ", maxSettingNameLength + 1),
                            setting.hint);
                }
            }
            else {
                BaseLoggers.BOOT_LOG.info("{}: [{}] {}",
                        rightPad(setting.name, maxSettingNameLength), setting.value,
                        setting.status());
            }
        }

        if (settings.stream().anyMatch(s -> s.exception != null)) {
            log.error("Could not establish access to the database. Shutting down.");
            appContext.close();
        }
    }

    private static class Setting
    {
        final String name;
        final String value;
        final String requested;
        final Exception exception;
        final String hint;

        public Setting(String aName, Exception aException, String aRequested, String aHint)
        {
            name = aName;
            value = null;
            exception = aException;
            requested = aRequested;
            hint = aHint;
        }

        public Setting(String aName, String aValue)
        {
            this(aName, aValue, null);
        }

        public Setting(String aName, String aValue, String aRequested)
        {
            super();
            name = aName;
            value = aValue;
            requested = aRequested;
            exception = null;
            hint = null;
        }

        String status()
        {
            if (isBlank(requested)) {
                return "(auto-detected)";
            }

            if (Objects.equals(value, requested)) {
                return "";
            }

            return "(not matching requested: [" + requested + "])";
        }
    }
}
