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
package de.tudarmstadt.ukp.inception.log;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringConfig.class)
@Transactional
@DataJpaTest
public class EventRepositoryImplTest
{
    private static final String PROJECT_NAME = "Test project";
    private static final String USERNAME = "Test user";
    private static final String DETAIL_JSON = "{}";

    @Autowired
    private TestEntityManager testEntityManager;

    private EventRepositoryImpl sut;
    private Project project;
    private User user;
    private LoggedEvent le;

    @BeforeClass
    public static void setUpOnce()
    {
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
    }

    @Before
    public void setUp() throws Exception
    {
        sut = new EventRepositoryImpl(testEntityManager.getEntityManager());
        project = createProject(PROJECT_NAME);
        user = createUser(USERNAME);
        le = buildLoggedEvent(project, USERNAME);
    }

    @After
    public void tearDown() throws Exception
    {
        testEntityManager.clear();
    }

    // Helper
    private Project createProject(String aName)
    {
        Project project = new Project();
        project.setName(aName);
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);
        return testEntityManager.persist(project);
    }

    private LoggedEvent buildLoggedEvent(Project aProject, String aUsername)
    {
        LoggedEvent le = new LoggedEvent();
        le.setUser(aUsername);
        le.setProject(aProject.getId());
        le.setEvent("RecommenderEvaluationResultEvent");
        le.setCreated(new Date());
        le.setDetails(DETAIL_JSON);

        return le;
    }

    public User createUser(String aUsername)
    {
        User user = new User();
        user.setUsername(aUsername);
        user.setCreated(new Date());
        user.setEnabled(true);
        return testEntityManager.persist(user);
    }
}
