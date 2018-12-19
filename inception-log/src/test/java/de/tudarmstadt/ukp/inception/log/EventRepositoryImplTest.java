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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
public class EventRepositoryImplTest  {
    private static final String PROJECT_NAME = "Test project";
    private static final String USERNAME = "Test user";
    private static final String DETAIL_JSON = "{}";
    private static final String EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT = "RecommenderEvaluationResultEvent";

    @Autowired
    private TestEntityManager testEntityManager;

    private EventRepositoryImpl sut;
    private Project project;
    private User user;
    private LoggedEvent le;

    @Before
    public void setUp() throws Exception
    {
        sut = new EventRepositoryImpl(testEntityManager.getEntityManager());
        project = createProject(PROJECT_NAME);
        user = createUser(USERNAME);
    }

    @After
    public void tearDown() throws Exception
    {
        testEntityManager.clear();
    }
    
    @Test
    public void thatApplicationContextStarts() {
    }
    
    @Test
    public void getLoggedEvents_WithOneStoredLoggedEvent_ShouldReturnStoredLoggedEvent()
    {
        le = buildRecommenderEvaluationLoggedEvent(project, USERNAME);

        sut.create(le);
        List<LoggedEvent> loggedEvents = sut.listLoggedEvents(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5);

        assertThat(loggedEvents).as("Check that only the previously created logged event is found")
                .hasSize(1).contains(le);
    }

    @Test
    public void getLoggedEvents_WithoutLoggedEvent_ShouldReturnEmptyList()
    {
        List<LoggedEvent> loggedEvents = sut.listLoggedEvents(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }

    @Test
    public void getLoggedEvents_WithLoggedEventOfOtherUser_ShouldReturnEmptyList()
    {
        le = buildRecommenderEvaluationLoggedEvent(project, "OtherUser");

        sut.create(le);

        List<LoggedEvent> loggedEvents = sut.listLoggedEvents(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }
    
    @Test
    public void getLoggedEvents_WithLoggedEventOfOtherProject_ShouldReturnEmptyList()
    {
        Project otherProject = createProject("otherProject");
        le = buildRecommenderEvaluationLoggedEvent(otherProject, user.getUsername());

        sut.create(le);
        
        List<LoggedEvent> loggedEvents = sut.listLoggedEvents(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }
    
    @Test
    public void getLoggedEvents_WithLoggedEventOfOtherType_ShouldReturnEmptyList()
    {
        project = createProject("otherProject");
        le = buildLoggedEvent(project, user.getUsername());
        le.setEvent("OTHER_TYPE");

        sut.create(le);
        
        List<LoggedEvent> loggedEvents = sut.listLoggedEvents(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }
    
    @Test
    public void getLoggedEvents_WithLoggedEventsMoreThanGivenSize_ShouldReturnListOfGivenSize()
    {
        for (int i = 0; i < 6; i++) {
            le = buildRecommenderEvaluationLoggedEvent(project, user.getUsername());
            Date d = new Date();
            d.setHours(i);
            le.setCreated(d);
            sut.create(le);
        }
        
        List<LoggedEvent> loggedEvents = sut.listLoggedEvents(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5);

        assertThat(loggedEvents).as("Check that the number of logged events is 5").hasSize(5);
    }
    
    @Test
    public void getLoggedEvents_WithLoggedEventsCreatedAtDifferentTimes_ShouldReturnSortedList()
    {
        for (int i = 0; i < 5; i++) {
            le = buildRecommenderEvaluationLoggedEvent(project, user.getUsername());
           
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, i);
            Date date = cal.getTime();

            le.setCreated(date);
            sut.create(le);
        }
        
        List<LoggedEvent> loggedEvents = sut.listLoggedEvents(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5);
        
        
        for (int i = 1; i < loggedEvents.size(); i++) {
            Date created = loggedEvents.get(i).getCreated();
            assertThat(loggedEvents.get(i - 1).getCreated()).isAfterOrEqualsTo(created);
        }
    }
    
    // Helper
    private Project createProject(String aName)
    {
        Project project = new Project();
        project.setName(aName);
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);
        return testEntityManager.persist(project);
    }

    private LoggedEvent buildRecommenderEvaluationLoggedEvent(Project aProject, String aUsername)
    {
        LoggedEvent le = buildLoggedEvent(aProject, aUsername);

        le.setEvent(EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT); 

        return le;
    }

    private LoggedEvent buildLoggedEvent(Project aProject, String aUsername)
    {
        LoggedEvent le = new LoggedEvent();
        le.setUser(aUsername);
        le.setProject(aProject.getId());
        le.setDetails(DETAIL_JSON);
        le.setCreated(new Date());
        return le;
    }

    public User createUser(String aUsername)
    {
        User user = new User();
        user.setUsername(aUsername);
        return testEntityManager.persist(user);
    }
}
