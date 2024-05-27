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
package de.tudarmstadt.ukp.inception.export.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.diag.ChecksRegistry;
import de.tudarmstadt.ukp.clarin.webanno.diag.RepairsRegistry;
import de.tudarmstadt.ukp.inception.export.DocumentImportExportServiceImpl;
import de.tudarmstadt.ukp.inception.export.exporters.ProjectLogExporter;
import de.tudarmstadt.ukp.inception.export.exporters.ProjectMetaInfExporter;
import de.tudarmstadt.ukp.inception.export.exporters.ProjectSettingsExporter;
import de.tudarmstadt.ukp.inception.io.xmi.XmiFormatSupport;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@Configuration
// @EnableConfigurationProperties({ DocumentImportExportServicePropertiesImpl.class })
public class DocumentImportExportServiceAutoConfiguration
{
    @Bean
    public DocumentImportExportService documentImportExportService(
            @Lazy @Autowired(required = false) List<FormatSupport> aFormats,
            CasStorageService aCasStorageService, AnnotationSchemaService aAnnotationService,
            DocumentImportExportServiceProperties aServiceProperties,
            ChecksRegistry aChecksRegistry, RepairsRegistry aRepairsRegistry,
            XmiFormatSupport fallbackFormat)
    {
        return new DocumentImportExportServiceImpl(aFormats, aCasStorageService, aAnnotationService,
                aServiceProperties, aChecksRegistry, aRepairsRegistry, fallbackFormat);
    }

    @Bean
    public DocumentImportExportServiceProperties documentImportExportServiceProperties()
    {
        return new DocumentImportExportServicePropertiesImpl();
    }

    @Bean
    public ProjectSettingsExporter projectSettingsExporter(ProjectService aProjectService)
    {
        return new ProjectSettingsExporter(aProjectService);
    }

    @Bean
    public ProjectLogExporter projectLogExporter(ProjectService aProjectService)
    {
        return new ProjectLogExporter(aProjectService);
    }

    @Bean
    public ProjectMetaInfExporter projectMetaInfExporter(ProjectService aProjectService)
    {
        return new ProjectMetaInfExporter(aProjectService);
    }
}
