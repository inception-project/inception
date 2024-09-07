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
package de.tudarmstadt.ukp.inception.log;

import static java.util.Calendar.HOUR_OF_DAY;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.config.ConstraintsServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.log.model.SummarizedLoggedEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;

@EnableAutoConfiguration
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class, showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno" })
@Import({ //
        ConstraintsServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class })
public class EventRepositoryImplIntegrationTest
{
    private static final String PROJECT_NAME = "Test project";
    private static final String USERNAME = "Test user";
    private static final int RECOMMENDER_ID = 7;
    private static final String DETAIL_JSON = "{\"recommenderId\":" + RECOMMENDER_ID + "}";
    private static final String EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT = "RecommenderEvaluationResultEvent";
    private static final String EVENT_TYPE_AFTER_ANNO_EVENT = "AfterAnnotationUpdateEvent";
    private static final String SPAN_CREATED_EVENT = "SpanCreatedEvent";

    private @Autowired TestEntityManager testEntityManager;

    private EventRepositoryImpl sut;
    private Project project;
    private User user;
    private LoggedEvent le;

    @BeforeEach
    public void setUp() throws Exception
    {
        sut = new EventRepositoryImpl(testEntityManager.getEntityManager());
        project = createProject(PROJECT_NAME);
        user = createUser(USERNAME);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        testEntityManager.clear();
    }

    @Test
    public void thatApplicationContextStarts()
    {
    }

    @Test
    public void getLoggedEventsForDoc_WithoutLoggedEvent_ShouldReturnEmptyList()
    {
        var loggedEvents = sut.listUniqueLoggedEventsForDoc(project, user.getUsername(),
                new String[] { EVENT_TYPE_AFTER_ANNO_EVENT }, 10);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }

    @Test
    public void getLoggedEventsForDoc_WithStoredLoggedEvent_ShouldReturnStoredLoggedEvent()
        throws ParseException
    {
        var df = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        le = buildLoggedEvent(project, USERNAME, EVENT_TYPE_AFTER_ANNO_EVENT,
                df.parse("19-04-03 10:00:00"), 1, DETAIL_JSON);
        var excludeTypeEvent = buildLoggedEvent(project, USERNAME,
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, df.parse("19-04-03 11:00:00"), 1,
                DETAIL_JSON);
        var includeTypeEvent = buildLoggedEvent(project, USERNAME, SPAN_CREATED_EVENT,
                df.parse("19-04-03 07:00:00"), 1, DETAIL_JSON);
        var le2 = buildLoggedEvent(project, USERNAME, EVENT_TYPE_AFTER_ANNO_EVENT,
                df.parse("19-04-03 9:00:00"), 1, DETAIL_JSON);
        var le3 = buildLoggedEvent(project, USERNAME, EVENT_TYPE_AFTER_ANNO_EVENT,
                df.parse("19-04-03 8:00:00"), 2, DETAIL_JSON);

        sut.create(le);
        sut.create(includeTypeEvent);
        sut.create(excludeTypeEvent);
        sut.create(le2);
        sut.create(le3);
        var loggedEvents = sut.listUniqueLoggedEventsForDoc(project, user.getUsername(),
                new String[] { EVENT_TYPE_AFTER_ANNO_EVENT, SPAN_CREATED_EVENT }, 5);

        assertThat(loggedEvents).as("Check that last created logged events are found").hasSize(2)
                .contains(le, le3);
    }

    @Test
    public void getLoggedEvents_WithOneStoredLoggedEvent_ShouldReturnStoredLoggedEvent()
    {
        le = buildLoggedEvent(project, USERNAME, EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT,
                new Date(), -1, DETAIL_JSON);

        sut.create(le);
        var loggedEvents = sut.listLoggedEventsForRecommender(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);

        assertThat(loggedEvents).as("Check that only the previously created logged event is found")
                .hasSize(1).contains(le);
    }

    @Test
    public void getLoggedEvents_WithoutLoggedEvent_ShouldReturnEmptyList()
    {
        var loggedEvents = sut.listLoggedEventsForRecommender(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }

    @Test
    public void getLoggedEvents_WithLoggedEventOfOtherUser_ShouldReturnEmptyList()
    {
        le = buildLoggedEvent(project, "OtherUser", EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT,
                new Date(), -1, DETAIL_JSON);

        sut.create(le);

        var loggedEvents = sut.listLoggedEventsForRecommender(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }

    @Test
    public void getLoggedEvents_WithLoggedEventOfOtherProject_ShouldReturnEmptyList()
    {
        var otherProject = createProject("otherProject");
        le = buildLoggedEvent(otherProject, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, new Date(), -1, DETAIL_JSON);

        sut.create(le);

        var loggedEvents = sut.listLoggedEventsForRecommender(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }

    @Test
    public void getLoggedEvents_WithLoggedEventOfOtherType_ShouldReturnEmptyList()
    {
        le = buildLoggedEvent(project, user.getUsername(), EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT,
                new Date(), -1, DETAIL_JSON);
        le.setEvent("OTHER_TYPE");

        sut.create(le);

        var loggedEvents = sut.listLoggedEventsForRecommender(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }

    @Test
    public void getLoggedEvents_WithLoggedEventsMoreThanGivenSize_ShouldReturnListOfGivenSize()
    {
        for (var i = 0; i < 6; i++) {
            le = buildLoggedEvent(project, user.getUsername(),
                    EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, new Date(), -1, DETAIL_JSON);
            var cal = Calendar.getInstance();
            cal.set(HOUR_OF_DAY, i);
            le.setCreated(cal.getTime());
            sut.create(le);
        }

        var loggedEvents = sut.listLoggedEventsForRecommender(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);

        assertThat(loggedEvents).as("Check that the number of logged events is 5").hasSize(5);
    }

    @Test
    public void getLoggedEvents_WithLoggedEventsCreatedAtDifferentTimes_ShouldReturnSortedList()
    {
        for (var i = 0; i < 5; i++) {
            le = buildLoggedEvent(project, user.getUsername(),
                    EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, new Date(), -1, DETAIL_JSON);

            var cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, i);
            le.setCreated(cal.getTime());
            sut.create(le);
        }

        var loggedEvents = sut.listLoggedEventsForRecommender(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, RECOMMENDER_ID);

        assertThat(loggedEvents).as("Check that the returned list is not empty").isNotEmpty();

        for (var i = 1; i < 5; i++) {
            assertThat(loggedEvents.get(i - 1).getCreated()).as(
                    "Check that the list of logged events is ordered by created time in descending order")
                    .isAfterOrEqualTo(loggedEvents.get(i).getCreated());
        }
    }

    @Test
    public void getLoggedEvents_WithLoggedEventOfOtherRecommenderId_ShouldReturnEmptyList()
    {
        le = buildLoggedEvent(project, user.getUsername(), EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT,
                new Date(), -1, DETAIL_JSON);
        sut.create(le);

        var otherRecommenderId = 6;

        var loggedEvents = sut.listLoggedEventsForRecommender(project, user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, 5, otherRecommenderId);

        assertThat(loggedEvents).as("Check that no logged event is found").isEmpty();
    }

    @Test
    public void getFilteredRecentLoggedEvents_ShouldReturnEvent()
    {
        var evalEvent = buildLoggedEvent(project, "!" + user.getUsername(),
                EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, new Date(), -1, DETAIL_JSON);
        var spanEvent = buildLoggedEvent(project, user.getUsername(), SPAN_CREATED_EVENT,
                new Date(), -1, "");
        sut.create(evalEvent);
        sut.create(spanEvent);

        var loggedEvents = sut.listRecentActivity(user.getUsername(), 3);

        assertThat(loggedEvents).hasSize(1);
        assertThat(loggedEvents).contains(spanEvent);
    }

    @Test
    void summarizeEvents()
    {
        for (var i = 0; i < 5; i++) {
            le = buildLoggedEvent(project, user.getUsername(), SPAN_CREATED_EVENT, new Date(), -1,
                    DETAIL_JSON);

            var cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, i + 3);
            le.setCreated(cal.getTime());
            sut.create(le);
        }

        var today = LocalDate.now();
        var beginOfDay = today.atTime(LocalTime.MIN).atZone(ZoneId.of("UTC")).toInstant();
        var endOfDay = today.atTime(LocalTime.MAX).atZone(ZoneId.of("UTC")).toInstant();

        var summarizedEvents = sut.summarizeEvents(USERNAME, project, beginOfDay, endOfDay);

        assertThat(summarizedEvents).as("Check that the returned list is not empty").isNotEmpty();

        assertThat(summarizedEvents)
                .extracting(SummarizedLoggedEvent::getEvent, SummarizedLoggedEvent::getCount)
                .containsExactly(tuple("SpanCreatedEvent", 5L));
    }

    // Helper
    private Project createProject(String aName)
    {
        Project p = new Project();
        p.setName(aName);
        return testEntityManager.persist(p);
    }

    private LoggedEvent buildLoggedEvent(Project aProject, String aUsername, String aEventType,
            Date aDate, long aDocId, String aDetails)
    {
        var loggedEvent = new LoggedEvent();
        loggedEvent.setUser(aUsername);
        loggedEvent.setProject(aProject.getId());
        loggedEvent.setDetails(aDetails);
        loggedEvent.setCreated(aDate);
        loggedEvent.setEvent(aEventType);
        loggedEvent.setDocument(aDocId);
        return loggedEvent;
    }

    public User createUser(String aUsername)
    {
        return testEntityManager.persist(new User(aUsername));
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        @Bean
        DocumentImportExportService documentImportExportService(
                AnnotationSchemaService aSchemaService)
            throws Exception
        {
            var tsd = createTypeSystemDescription();
            var importService = mock(DocumentImportExportService.class);
            when(importService.importCasFromFile(any(), any(), any(), any()))
                    .thenReturn(CasCreationUtils.createCas(tsd, null, null, null));
            return importService;
        }
    }
}
