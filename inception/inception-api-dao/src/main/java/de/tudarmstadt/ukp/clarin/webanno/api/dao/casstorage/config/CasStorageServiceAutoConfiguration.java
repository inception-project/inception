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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageDriver;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.FileSystemCasStorageDriver;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor;

@Configuration
@EnableConfigurationProperties({ CasStoragePropertiesImpl.class, BackupProperties.class })
public class CasStorageServiceAutoConfiguration
{
    @Bean(CasStorageService.SERVICE_NAME)
    public CasStorageService casStorageService(CasStorageDriver aDriver,
            @Autowired(required = false) CasDoctor aCasDoctor,
            @Autowired(required = false) AnnotationSchemaService aSchemaService,
            CasStorageProperties aCasStorageProperties)
    {
        return new CasStorageServiceImpl(aDriver, aCasDoctor, aSchemaService,
                aCasStorageProperties);
    }

    @Bean
    CasStorageDriver fileSystemCasStorageDriver(RepositoryProperties aRepositoryProperties,
            BackupProperties aBackupProperties)
    {
        return new FileSystemCasStorageDriver(aRepositoryProperties, aBackupProperties);
    }
}
