/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.support.spring;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Configuration;

@Configuration("HibernateHSQLSchemaValidationFixConfig")
public class HibernateHSQLSchemaValidationFixConfig
{
    private @Autowired DataSourceProperties dataSource;
    private @Autowired JpaProperties jpa;
    
    @PostConstruct
    public void apply()
    {
        // If we run Hibernate in validation mode on HSQLDB, then Hibernate does not
        // seem to be able that it needs to retrieve table metadata from the default
        // "PUBLIC" schema. Thus, we set the JPA default schema explicitly here when
        // using HSQLDB.
        if ("org.hsqldb.jdbc.JDBCDriver".equals(dataSource.getDriverClassName())) {
            jpa.getProperties().put("hibernate.default_schema", "PUBLIC");
        }
    }
    
    /**
     * Additional configuration to ensure that {@link EntityManagerFactory} beans
     * depend-on this fix.
     */
    @Configuration
    protected static class LiquibaseJpaDependencyConfiguration
            extends EntityManagerFactoryDependsOnPostProcessor {

        public LiquibaseJpaDependencyConfiguration() {
            super("HibernateHSQLSchemaValidationFixConfig");
        }
    }
}
