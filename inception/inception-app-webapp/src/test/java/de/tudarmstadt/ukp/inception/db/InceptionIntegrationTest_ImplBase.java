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
package de.tudarmstadt.ukp.inception.db;

import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_APPLICATION_MODE;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_AUTH_MODE_DATABASE;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_INTERNAL_SERVER;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_PRODUCTION_MODE;
import static java.util.Arrays.asList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.webanno.StandardProjectInitializer;
import de.tudarmstadt.ukp.inception.INCEpTION;
import de.tudarmstadt.ukp.inception.app.config.InceptionApplicationContextInitializer;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.logging.Logging;

@ActiveProfiles({ //
        PROFILE_APPLICATION_MODE, //
        PROFILE_AUTH_MODE_DATABASE, //
        PROFILE_INTERNAL_SERVER, //
        PROFILE_PRODUCTION_MODE })
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { INCEpTION.class })
@ContextConfiguration(initializers = { InceptionApplicationContextInitializer.class })
public abstract class InceptionIntegrationTest_ImplBase
{
    @Autowired
    ProjectService projectService;

    @Autowired
    StandardProjectInitializer standardProjectInitializer;

    @Autowired
    RepositoryProperties repositoryProperties;

    @BeforeEach
    void setupClass()
    {
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
    }

    @AfterEach
    void tearDownClass()
    {
        MDC.clear();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry)
    {
        registry.add("running.from.commandline", () -> "true");
    }

    @Test
    void testContextStartsUpSuccessfully() throws Exception
    {
        // Nothing
    }

    @Test
    void testCreatingAndRemovingProject() throws Exception
    {
        var project = Project.builder().withName("test").withSlug("test").build();
        projectService.createProject(project);

        var request = ProjectInitializationRequest.builder() //
                .withProject(project) //
                .withIncludeSampleData(true) //
                .build();

        projectService.initializeProject(request, asList(standardProjectInitializer));

        projectService.removeProject(project);
    }
}
