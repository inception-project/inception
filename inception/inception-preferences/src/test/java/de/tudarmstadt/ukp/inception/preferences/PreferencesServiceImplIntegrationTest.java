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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.util.FileSystemUtils;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

@DataJpaTest( //
        showSql = false, //
        excludeAutoConfiguration = LiquibaseAutoConfiguration.class, //
        properties = { //
                "spring.main.banner-mode=off", //
                "repository.path=" + PreferencesServiceImplIntegrationTest.TEST_OUTPUT_FOLDER })
@EnableAutoConfiguration
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model", //
        "de.tudarmstadt.ukp.inception.preferences.model" })
@Import({ //
        SecurityAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class })
public class PreferencesServiceImplIntegrationTest
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/PreferencesServiceImplIntegrationTest";

    private static final PreferenceKey<TestTraits> KEY = new PreferenceKey<>(TestTraits.class,
            "test.traits");

    private PreferencesServiceImpl sut;

    private @Autowired TestEntityManager testEntityManager;
    private @Autowired ProjectService projectService;
    private @Autowired UserDao userDao;

    @BeforeAll
    public static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @BeforeEach
    public void setUp()
    {
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

        TestTraits actualTraits = sut.loadTraitsForUser(KEY, user);

        assertThat(actualTraits).usingRecursiveComparison().isEqualTo(expectedTraits);
    }

    @Test
    public void testThatTraitsForUserHaveDefaultIfNotFound()
    {
        User user = createUser();
        TestTraits expectedTraits = new TestTraits();

        TestTraits actualTraits = sut.loadTraitsForUser(KEY, user);

        assertThat(actualTraits).usingRecursiveComparison().isEqualTo(expectedTraits);
    }

    @Test
    public void testThatTraitsForUserCanBeUpserted()
    {
        User user = createUser();
        TestTraits expectedTraits = buildTestTraits();
        sut.saveTraitsForUser(KEY, user, expectedTraits);
        expectedTraits.setTestString("anotherTestValue");

        sut.saveTraitsForUser(KEY, user, expectedTraits);
        TestTraits actualTraits = sut.loadTraitsForUser(KEY, user);

        assertThat(actualTraits).usingRecursiveComparison().isEqualTo(expectedTraits);
    }

    @Test
    public void testThatTraitsForUserAndProjectCanBeStoredAndLoaded() throws Exception
    {
        User user = createUser();
        Project project = createProject();
        TestTraits expectedTraits = buildTestTraits();

        sut.saveTraitsForUserAndProject(KEY, user, project, expectedTraits);

        TestTraits actualTraits = sut.loadTraitsForUserAndProject(KEY, user, project);

        assertThat(actualTraits).usingRecursiveComparison().isEqualTo(expectedTraits);
    }

    @Test
    public void testThatTraitsForUserAndProjectHaveDefaultIfNotFound() throws IOException
    {
        User user = createUser();
        Project project = createProject();
        TestTraits expectedTraits = new TestTraits();

        TestTraits actualTraits = sut.loadTraitsForUserAndProject(KEY, user, project);

        assertThat(actualTraits).usingRecursiveComparison().isEqualTo(expectedTraits);
    }

    @Test
    public void testThatTraitsForUserAndProjectCanBeUpserted() throws IOException
    {
        User user = createUser();
        Project project = createProject();
        TestTraits expectedTraits = buildTestTraits();
        sut.saveTraitsForUserAndProject(KEY, user, project, expectedTraits);
        expectedTraits.setTestString("anotherTestValue");

        sut.saveTraitsForUserAndProject(KEY, user, project, expectedTraits);
        TestTraits actualTraits = sut.loadTraitsForUserAndProject(KEY, user, project);

        assertThat(actualTraits).usingRecursiveComparison().isEqualTo(expectedTraits);
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
    public static class TestContext
    {
        // Everything is handled by auto-config
    }
}
