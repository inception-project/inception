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
package de.tudarmstadt.ukp.inception.preferences;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
public class PreferencesServiceImplIntegrationTest
{
    private static final Key<TestTraits> KEY = new Key<>(TestTraits.class, "test.traits");

    private PreferencesServiceImpl sut;
    
    private @Autowired TestEntityManager testEntityManager;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired ProjectService projectService;
    private @Autowired UserDao userDao;

    public @TempDir File repoDir;

    @BeforeEach
    public void setUp()
    {
        repositoryProperties.setPath(repoDir);
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
        
        sut = new PreferencesServiceImpl(testEntityManager.getEntityManager());
    }

    @AfterEach
    public void tearDown()
    {
        testEntityManager.clear();
    }

    @Test
    public void testThatApplicationContextStarts()
    {
    }

    @Test
    public void testThatTraitsForUserCanBeStoredAndLoaded()
    {
        User user = createUser();
        TestTraits expectedTraits = buildTestTraits();

        sut.saveTraitsForUser(KEY, user, expectedTraits);

        TestTraits actualTraits = sut.loadTraitsForUser(KEY, user).get();

        assertThat(actualTraits).usingRecursiveComparison().isEqualTo(actualTraits);
    }

    @Test
    public void testThatTraitsForUserAndProjectCanBeStoredAndLoaded() throws Exception
    {
        User user = createUser();
        Project project = createProject();
        TestTraits expectedTraits = buildTestTraits();

        sut.saveTraitsForUserAndProject(KEY, user, project, expectedTraits);

        TestTraits actualTraits = sut.loadTraitsForUserAndProject(KEY, user, project).get();

        assertThat(actualTraits).usingRecursiveComparison().isEqualTo(actualTraits);
    }

    private TestTraits buildTestTraits()
    {
        TestTraits traits = new TestTraits();
        traits.setTestBoolean(true);
        traits.setTestString("I am a test string");
        traits.setTestList(List.of("Foo", "Bar", "Baz"));

        return traits;
    }

    private User createUser()
    {
        User user = new User();
        user.setUsername("testUser");
        userDao.create(user);

        return user;
    }

    private Project createProject() throws IOException
    {
        Project project = new Project();
        project.setName("testProject");
        projectService.createProject(project);
        projectService.initializeProject(project);
        return project;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = { "de.tudarmstadt.ukp.clarin.webanno.model",
            "de.tudarmstadt.ukp.clarin.webanno.security.model",
            "de.tudarmstadt.ukp.inception.preferences.model" })
    public static class TestContext
    {
        private @Autowired ApplicationEventPublisher applicationEventPublisher;

        @Bean
        public RepositoryProperties repositoryProperties()
        {
            return new RepositoryProperties();
        }

        @Bean
        public UserDao userRepository()
        {
            return new UserDaoImpl();
        }

        @Bean
        public ProjectService projectService(UserDao aUserDao,
                RepositoryProperties aRepositoryProperties,
                @Lazy @Autowired(required = false) List<ProjectInitializer> aInitializerProxy)
        {
            return new ProjectServiceImpl(aUserDao, applicationEventPublisher,
                    aRepositoryProperties, aInitializerProxy);
        }
    }

}
