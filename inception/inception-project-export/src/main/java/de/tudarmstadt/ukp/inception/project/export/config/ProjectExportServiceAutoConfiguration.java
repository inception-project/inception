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
package de.tudarmstadt.ukp.inception.project.export.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportServiceImpl;

@Configuration
@AutoConfigureAfter(name = {
        "de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration" })
@ConditionalOnBean(ProjectService.class)
public class ProjectExportServiceAutoConfiguration
{
    @Bean
    public ProjectExportService projectExportService(ApplicationContext aApplicationContext,
            @Lazy @Autowired(required = false) List<ProjectExporter> aExporters,
            ProjectService aProjectService)
    {
        return new ProjectExportServiceImpl(aApplicationContext, aExporters, aProjectService);
    }
}
