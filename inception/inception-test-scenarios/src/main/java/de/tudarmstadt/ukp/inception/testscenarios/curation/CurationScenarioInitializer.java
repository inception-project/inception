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
package de.tudarmstadt.ukp.inception.testscenarios.curation;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.testscenarios.config.InceptionTestScenariosAutoConfiguration;

/**
 * The plain curation scenario: named entities only, no editable Sentence layer.
 * <p>
 * The curation editors therefore take their <em>sentence</em>-oriented branch, which is the default
 * configuration and the one most projects run.
 * <p>
 * <b>This class is exposed as a Spring Component via
 * {@link InceptionTestScenariosAutoConfiguration#curationScenarioInitializer}.</b>
 */
@Order(9000)
public class CurationScenarioInitializer
    extends CurationScenarioInitializer_ImplBase
{
    public CurationScenarioInitializer(DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserService)
    {
        super(aDocumentService, aProjectService, aUserService);
    }

    @Override
    public String getName()
    {
        return "TEST: curation-ready, two annotators";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("One document finished by two annotators with partly disagreeing named "
                + "entities. For exercising the curation pages.");
    }

    @Override
    protected String getProjectNameSuffix()
    {
        return "curation";
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(NamedEntityLayerInitializer.class);
    }
}
