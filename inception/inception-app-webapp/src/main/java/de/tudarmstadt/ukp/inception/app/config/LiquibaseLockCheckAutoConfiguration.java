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

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

import de.tudarmstadt.ukp.inception.support.db.LiquibaseLockManager;
import de.tudarmstadt.ukp.inception.support.db.LockRemovedException;
import de.tudarmstadt.ukp.inception.support.db.NotLockedException;
import liquibase.exception.LockException;
import liquibase.integration.spring.SpringLiquibase;

@AutoConfigureBefore(LiquibaseAutoConfiguration.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
public class LiquibaseLockCheckAutoConfiguration
{
    @Bean
    public LiquibaseLockManager liquibaseLockCheck(DataSource aDataSource)
        throws LockException, NotLockedException, LockRemovedException
    {
        return new LiquibaseLockManager(aDataSource);
    }

    @Bean
    public BeanFactoryPostProcessor ensureLiquibaseLockCheckRunsBeforeLiquibase()
    {
        return new AbstractDependsOnBeanFactoryPostProcessor(SpringLiquibase.class,
                LiquibaseLockManager.class)
        {
            // No content
        };
    }
}
