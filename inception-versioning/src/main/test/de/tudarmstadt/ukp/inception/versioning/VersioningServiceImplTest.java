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
package de.tudarmstadt.ukp.inception.versioning;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@RunWith(SpringRunner.class)
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
public class VersioningServiceImplTest
{
    private VersioningService sut;
    private @Autowired TestEntityManager testEntityManager;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired AnnotationSchemaService annotationSchemaService;
    private @Autowired ProjectService projectService;

    private Project testProject;
    private File repoDir;

    @Before
    public void setUp() throws Exception
    {
        repoDir = Files.createTempDirectory(".inception-test").toFile();

        repositoryProperties.setPath(repoDir);
        sut = new VersioningServiceImpl(repositoryProperties, annotationSchemaService);

        testProject = new Project("testProject");
    }

    @After
    public void tearDown()
    {
        testEntityManager.clear();
    }

    @Test
    public void creatingProject_ShouldCreateGitRepository() throws IOException
    {
        projectService.createProject(testProject);
    }

}
