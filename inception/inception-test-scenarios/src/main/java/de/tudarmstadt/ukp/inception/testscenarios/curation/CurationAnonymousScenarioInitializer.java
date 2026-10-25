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

import static de.tudarmstadt.ukp.inception.testscenarios.accounts.ScenarioAccount.CURATOR;

import java.util.Optional;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.testscenarios.accounts.ScenarioAccountService;
import de.tudarmstadt.ukp.inception.testscenarios.config.InceptionTestScenariosAutoConfiguration;

/**
 * The curation-ready project with anonymous curation enabled, plus the curator-only scenario
 * account.
 * <p>
 * Annotator names are only anonymized for a curator who is not a manager, and the project creator
 * is always a manager - so the curation pages have to be checked as {@code scenario-curator}.
 * <p>
 * <b>This class is exposed as a Spring Component via
 * {@link InceptionTestScenariosAutoConfiguration#curationAnonymousScenarioInitializer}.</b>
 */
@Order(9000)
public class CurationAnonymousScenarioInitializer
    extends CurationScenarioInitializer_ImplBase
{
    private final ScenarioAccountService scenarioAccountService;

    public CurationAnonymousScenarioInitializer(DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserService,
            ScenarioAccountService aScenarioAccountService)
    {
        super(aDocumentService, aProjectService, aUserService, aScenarioAccountService);
        scenarioAccountService = aScenarioAccountService;
    }

    @Override
    public String getName()
    {
        return "TEST: curation-ready, anonymous curation";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("One document finished by two annotators, with anonymous curation "
                + "enabled and a curator who is not a manager.");
    }

    @Override
    protected String getProjectNameSuffix()
    {
        return "curation anonymous";
    }

    @Override
    protected Optional<String> describeVariant()
    {
        return Optional.of("""
                Anonymous curation is enabled. Log in as **scenario-curator** (password `admin`), \
                who is a curator but not a manager, to see the annotators anonymized.""");
    }

    @Override
    protected void configureVariant(Project aProject)
    {
        aProject.setAnonymousCuration(true);
        scenarioAccountService.join(aProject, CURATOR);
    }
}
