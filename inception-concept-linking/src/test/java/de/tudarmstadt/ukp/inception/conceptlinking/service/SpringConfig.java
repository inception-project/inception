/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.conceptlinking.service;

import org.mockito.Mockito;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.inception.kb.exporter.KnowledgeBaseExporter;

@SpringBootConfiguration
@ComponentScan(
        excludeFilters = {
            // We do now text exporting here and the exporter depends on the annotation schema
            // service which is otherwise not needed. So we exclude this component here.
            @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                    KnowledgeBaseExporter.class
            })
        },
        basePackages = {
            "de.tudarmstadt.ukp.clarin.webanno.api",
            "de.tudarmstadt.ukp.clarin.webanno.security",
            "de.tudarmstadt.ukp.inception"
        })
@EntityScan(
        basePackages = {
            "de.tudarmstadt.ukp.inception.kb.model",
            "de.tudarmstadt.ukp.clarin.webanno.model"
})
@EnableAutoConfiguration
public class SpringConfig {
//    @Bean(name = "formats")
//    public Properties getFileFormats()
//    {
//        return new Properties();
//    }
//
//    @Bean
//    @Primary
//    public DocumentService documentService()
//    {
//        return Mockito.mock(DocumentService.class);
//    }

    @Bean
    @Primary
    public ProjectService projectService()
    {
        return Mockito.mock(ProjectService.class);
    }
}
