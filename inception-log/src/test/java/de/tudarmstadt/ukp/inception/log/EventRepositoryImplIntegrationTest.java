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
public class EventRepositoryImplIntegrationTest  {
    private static final String PROJECT_NAME = "Test project";
    private static final String USERNAME = "Test user";
    private static final int RECOMMENDER_ID = 7;
    private static final String DETAIL_JSON = "{\"recommenderId\":" + RECOMMENDER_ID + "}";
    private static final String EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT = "RecommenderEvaluationResultEvent";
    private static final String EVENT_TYPE_AFTER_ANNO_EVENT = "AfterAnnotationUpdateEvent";

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
    public void getLoggedEventsForType_WithoutLoggedEvent_ShouldReturnEmptyList()
    {
        List<LoggedEvent> loggedEvents = sut.listLoggedEventsForEventType(project,
                user.getUsername(), EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 10);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }
    
    @Test
    public void getLoggedEventsForType_WithOneStoredLoggedEvent_ShouldReturnStoredLoggedEvent()
    {
        le = buildLoggedEvent(project, USERNAME, EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT);
        LoggedEvent otherTypeEvent = buildLoggedEvent(project, 
                USERNAME, EVENT_TYPE_AFTER_ANNO_EVENT);

        sut.create(le);
        sut.create(otherTypeEvent);
        List<LoggedEvent> loggedEvents = sut.listLoggedEventsForEventType(project,
                user.getUsername(), EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5);

        assertThat(loggedEvents).as("Check that only the previously created logged event is found")
                .hasSize(1).contains(le);
    }

    @Test
    public void getLoggedEvents_WithOneStoredLoggedEvent_ShouldReturnStoredLoggedEvent()
    {
        le = buildLoggedEvent(project, USERNAME, EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT);

        sut.create(le);
        List<LoggedEvent> loggedEvents = sut.listLoggedEventsForRecommender(project,
                user.getUsername(), EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);

        assertThat(loggedEvents).as("Check that only the previously created logged event is found")
                .hasSize(1).contains(le);
    }

    @Test
    public void getLoggedEvents_WithoutLoggedEvent_ShouldReturnEmptyList()
    {
        List<LoggedEvent> loggedEvents = sut.listLoggedEventsForRecommender(project,
                user.getUsername(), EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }

    @Test
    public void getLoggedEvents_WithLoggedEventOfOtherUser_ShouldReturnEmptyList()
    {
        le = buildLoggedEvent(project, "OtherUser", EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT);

        sut.create(le);

        List<LoggedEvent> loggedEvents = sut.listLoggedEventsForRecommender(project,
                user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }
    
    @Test
    public void getLoggedEvents_WithLoggedEventOfOtherProject_ShouldReturnEmptyList()
    {
        Project otherProject = createProject("otherProject");
        le = buildLoggedEvent(otherProject, user.getUsername(), 
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT);

        sut.create(le);

        List<LoggedEvent> loggedEvents = sut.listLoggedEventsForRecommender(project,
                user.getUsername(), EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }
    
    @Test
    public void getLoggedEvents_WithLoggedEventOfOtherType_ShouldReturnEmptyList()
    {
        le = buildLoggedEvent(project, user.getUsername(), EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT);
        le.setEvent("OTHER_TYPE");

        sut.create(le);

        List<LoggedEvent> loggedEvents = sut.listLoggedEventsForRecommender(project,
                user.getUsername(), EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }
    
    @Test
    public void getLoggedEvents_WithLoggedEventsMoreThanGivenSize_ShouldReturnListOfGivenSize()
    {
        for (int i = 0; i < 6; i++) {
            le = buildLoggedEvent(project, user.getUsername(), 
                    EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT);
            Date d = new Date();
            d.setHours(i);
            le.setCreated(d);
            sut.create(le);
        }

        List<LoggedEvent> loggedEvents = sut.listLoggedEventsForRecommender(project,
                user.getUsername(), EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);

        assertThat(loggedEvents).as("Check that the number of logged events is 5").hasSize(5);
    }
    
    @Test
    public void getLoggedEvents_WithLoggedEventsCreatedAtDifferentTimes_ShouldReturnSortedList()
    {
        for (int i = 0; i < 5; i++) {
            le = buildLoggedEvent(project, user.getUsername(),
                    EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT);
           
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, i);
            Date date = cal.getTime();

            le.setCreated(date);
            sut.create(le);
        }
        
        List<LoggedEvent> loggedEvents = sut.listLoggedEventsForRecommender(project,
                user.getUsername(), EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);
        
        assertThat(loggedEvents).as("Check that the returned list is not empty").isNotEmpty();
        
        for (int i = 1; i < 5; i++) {
            assertThat(loggedEvents.get(i - 1).getCreated())
                    .as("Check that the list of logged events is ordered by created time in descending order")
                    .isAfterOrEqualsTo(loggedEvents.get(i).getCreated());
        }
    }
    
    @Test
    public void getLoggedEvents_WithLoggedEventOfOtherRecommenderId_ShouldReturnEmptyList()
    {
        le = buildLoggedEvent(project, user.getUsername(), EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT);
        sut.create(le);
        
        int otherRecommenderId = 6;

        List<LoggedEvent> loggedEvents = sut.listLoggedEventsForRecommender(project,
                user.getUsername(), EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, otherRecommenderId);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }
    
    // Helper
    private Project createProject(String aName)
    {
        Project project = new Project();
        project.setName(aName);
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);
        return testEntityManager.persist(project);
    }

    private LoggedEvent buildLoggedEvent(Project aProject, String aUsername,
            String aEventType)
    {
        LoggedEvent le = new LoggedEvent();
        le.setUser(aUsername);
        le.setProject(aProject.getId());
        le.setDetails(DETAIL_JSON);
        le.setCreated(new Date());
        le.setEvent(aEventType);
        return le;
    }

    public User createUser(String aUsername)
    {
        User user = new User();
        user.setUsername(aUsername);
        return testEntityManager.persist(user);
    }
}
