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
package de.tudarmstadt.ukp.inception.testscenarios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.testscenarios.curation.CurationMixedEditabilityScenarioInitializer;
import de.tudarmstadt.ukp.inception.testscenarios.curation.CurationScenarioInitializer;
import de.tudarmstadt.ukp.inception.testscenarios.curation.CurationSentenceLayerScenarioInitializer;
import de.tudarmstadt.ukp.inception.testscenarios.editorplacement.EditorPlacementScenarioInitializer;
import de.tudarmstadt.ukp.inception.testscenarios.recommender.RecommenderScenarioInitializer;

/**
 * Project initializers that exist purely to set up reproducible test scenarios.
 * <p>
 * These are gated at build time rather than at runtime: the module is compiled by every build - and
 * hence cannot rot - but the web application only depends on it under the {@code test-scenarios}
 * Maven profile. In a shipped application these classes are not on the classpath at all, so there
 * is no property that could switch them on by accident.
 */
@Configuration
public class InceptionTestScenariosAutoConfiguration
{
    @Bean
    public CurationScenarioInitializer curationScenarioInitializer(DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserService)
    {
        return new CurationScenarioInitializer(aDocumentService, aProjectService, aUserService);
    }

    @Bean
    public CurationMixedEditabilityScenarioInitializer curationMixedEditabilityScenarioInitializer(
            DocumentService aDocumentService, ProjectService aProjectService, UserDao aUserService)
    {
        return new CurationMixedEditabilityScenarioInitializer(aDocumentService, aProjectService,
                aUserService);
    }

    @Bean
    public CurationSentenceLayerScenarioInitializer curationSentenceLayerScenarioInitializer(
            DocumentService aDocumentService, ProjectService aProjectService, UserDao aUserService,
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new CurationSentenceLayerScenarioInitializer(aDocumentService, aProjectService,
                aUserService, aAnnotationSchemaService);
    }

    @Bean
    public RecommenderScenarioInitializer recommenderScenarioInitializer(
            DocumentService aDocumentService, ProjectService aProjectService, UserDao aUserService)
    {
        return new RecommenderScenarioInitializer(aDocumentService, aProjectService, aUserService);
    }

    @Bean
    public EditorPlacementScenarioInitializer editorPlacementScenarioInitializer(
            DocumentService aDocumentService, ProjectService aProjectService, UserDao aUserService)
    {
        return new EditorPlacementScenarioInitializer(aDocumentService, aProjectService,
                aUserService);
    }
}
