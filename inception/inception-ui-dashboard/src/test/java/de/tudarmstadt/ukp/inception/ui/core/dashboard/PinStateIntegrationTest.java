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
package de.tudarmstadt.ukp.inception.ui.core.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.preferences.PreferencesServiceImpl;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

@DataJpaTest( //
        showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
@EnableAutoConfiguration
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model", //
        "de.tudarmstadt.ukp.inception.preferences.model" })
@Import({ //
        SecurityAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class })
public class PinStateIntegrationTest
{
    static @TempDir Path tempFolder;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry)
    {
        registry.add("repository.path", () -> tempFolder.toAbsolutePath().toString());
    }

    private PreferencesServiceImpl sut;

    private @Autowired TestEntityManager testEntityManager;
    private @Autowired ProjectService projectService;
    private @Autowired UserDao userDao;

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
    public void testThatPinStateCanBeStoredAndLoaded() throws IOException
    {
        User user = createUser();

        PinState expected = new PinState(false);
        sut.saveTraitsForUser(DashboardMenu.KEY_PINNED, user, expected);

        PinState actual = sut.loadTraitsForUser(DashboardMenu.KEY_PINNED, user);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    private User createUser()
    {
        User user = new User();
        user.setUsername("testUserPinStateUi");
        userDao.create(user);

        return user;
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        // Everything is handled by auto-config
    }
}
