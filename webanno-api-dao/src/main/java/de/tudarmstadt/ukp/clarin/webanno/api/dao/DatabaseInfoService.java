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

    @Value(value = "${database.dialect}")
    private String databaseDialect;

    @Value(value = "${database.driver}")
    private String databaseDriver;

    @Value(value = "${database.url}")
    private String databaseUrl;

    @Value(value = "${database.username}")
    private String databaseUsername;

    @Override
    public void afterPropertiesSet()
    {
        log.info("Database dialect: " + databaseDialect);
        log.info("Database driver: " + databaseDriver);
        log.info("Database URL: " + databaseUrl);
        log.info("Database username: " + databaseUsername);
    }
}
