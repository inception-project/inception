/*
# * Copyright 2018
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
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.reflect.FieldUtils.readField;

import java.sql.DatabaseMetaData;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    @Override
    public void afterPropertiesSet() throws Exception
    {
        Session session = entityManager.unwrap(Session.class);
        SessionFactoryImplementor sessionFactory = ((SessionFactoryImplementor) session
                .getSessionFactory());
        Dialect dialect = sessionFactory.getJdbcServices().getDialect();

        log.info("Database URL            : [{}]", databaseUrl);
        log.info("Database username       : [{}]", databaseUsername);

        if (isBlank(databaseDriver)) {
            session.doWork(connection -> {
                DatabaseMetaData metadata = connection.getMetaData();
                log.info("Database driver         : [{} {}] (auto-detected)",
                        metadata.getDriverName(), metadata.getDriverVersion());
            });
        }
        else {
            log.info("Database driver         : [{}] (explicitly set)", databaseDriver);
        }

        if (isBlank(databaseDialect)) {
            log.info("Database dialect        : [{}] (auto-detected)", dialect);
        }
        else if (dialect.getClass().getName().equals(databaseDialect)) {
            log.info("Database dialect        : [{}] (explicitly set)", dialect);
        }
        else {
            log.warn("Database dialect        : [{}] (not matching requested: {})", dialect,
                    databaseDialect);
        }

        if (dialect instanceof MySQLDialect) {
            try {
                log.info("Database storage engine : [{}]",
                        readField(dialect, "storageEngine", true).getClass().getName());
            }
            catch (Exception e) {
                // Ignore
            }
        }
    }
}
