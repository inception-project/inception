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

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.NEW;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.config.ConstraintsServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.api.model.StateChangeDetails;
import de.tudarmstadt.ukp.inception.log.api.model.SummarizedLoggedEvent;
import de.tudarmstadt.ukp.inception.log.model.LoggedEventEntity;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import jakarta.persistence.EntityManager;

@EnableAutoConfiguration
@DataJpaTest(showSql = false, //
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
        SecurityAutoConfiguration.class })
public class EventRepositoryImplIntegrationTest
{
    private static final String PROJECT_NAME = "Test project";
    private static final String USERNAME = "Test user";
    private static final int RECOMMENDER_ID = 7;
    private static final String DETAIL_JSON = "{\"recommenderId\":" + RECOMMENDER_ID + "}";
    private static final String EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT = "RecommenderEvaluationResultEvent";
    private static final String SPAN_CREATED_EVENT = "SpanCreatedEvent";

    private @Autowired TestEntityManager testEntityManager;

    private @Autowired EventRepositoryImpl sut;
    private Project project;
    private User user;
    private LoggedEventEntity le;

    @BeforeEach
    public void setUp() throws Exception
    {
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
    void testSummarizeEventsBySessionOwner()
    {
        for (var i = 0; i < 5; i++) {
            le = buildLoggedEvent(project, user.getUsername(), SPAN_CREATED_EVENT, new Date(), -1,
                    DETAIL_JSON);

            var cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, i + 3);
            le.setCreated(cal.getTime().toInstant());
            sut.create(le);
        }

        var today = LocalDate.now();
        var beginOfDay = today.atTime(LocalTime.MIN).atZone(ZoneId.of("UTC")).toInstant();
        var endOfDay = today.atTime(LocalTime.MAX).atZone(ZoneId.of("UTC")).toInstant();

        var summarizedEvents = sut.summarizeEventsBySessionOwner(USERNAME, project, beginOfDay,
                endOfDay);

        assertThat(summarizedEvents).as("Check that the returned list is not empty").isNotEmpty();

        assertThat(summarizedEvents)
                .extracting(SummarizedLoggedEvent::getEvent, SummarizedLoggedEvent::getCount)
                .containsExactly(tuple("SpanCreatedEvent", 5L));
    }

    @Test
    void testCalculateHistoricalDocumentStates() throws Exception
    {
        var now = Instant.now();
        var threeDaysAgo = now.minus(3, DAYS);

        // Create events: document created, state changed, document removed
        // Day -3: Document created (NEW)
        var createEvent = buildLoggedEvent(project, user.getUsername(), "AfterDocumentCreatedEvent",
                Date.from(threeDaysAgo), 100L, "");
        sut.create(createEvent);

        // Day -2: State changed to ANNOTATION_FINISHED
        var twoDaysAgo = now.minus(2, DAYS);
        var stateDetails = new StateChangeDetails();
        stateDetails.setState(ANNOTATION_FINISHED.toString());
        stateDetails.setPreviousState(NEW.toString());
        var stateChangeEvent = buildLoggedEvent(project, user.getUsername(),
                "DocumentStateChangedEvent", Date.from(twoDaysAgo), 100L,
                JSONUtil.toJsonString(stateDetails));
        sut.create(stateChangeEvent);

        // Day -1: Document removed
        var oneDayAgo = now.minus(1, DAYS);
        var removeEvent = buildLoggedEvent(project, user.getUsername(),
                "BeforeDocumentRemovedEvent", Date.from(oneDayAgo), 100L, "");
        sut.create(removeEvent);

        // Current state: no documents
        var currentStats = new HashMap<SourceDocumentState, Long>();
        for (var state : SourceDocumentState.values()) {
            currentStats.put(state, 0L);
        }

        // Calculate backwards from current state
        var fourDaysAgo = now.minus(4, DAYS);
        var snapshots = sut.calculateHistoricalDocumentStates(project, currentStats, fourDaysAgo);

        assertThat(snapshots).isNotEmpty();

        // Verify we have snapshots within the time range
        assertThat(snapshots.get(0).day()).isAfterOrEqualTo(fourDaysAgo).isBefore(now);
        assertThat(snapshots.get(snapshots.size() - 1).day()).isBefore(now.plus(1, SECONDS));

        // Verify document counts change over time
        var hasNonZeroCounts = snapshots.stream()
                .anyMatch(s -> s.counts().values().stream().anyMatch(count -> count > 0));
        assertThat(hasNonZeroCounts)
                .as("At least one snapshot should have non-zero document counts").isTrue();
    }

    @Test
    void listRecentActivity_limitAndOrdering()
    {
        var base = Instant.now();
        var events = new ArrayList<LoggedEventEntity>();

        for (int i = 0; i < 5; i++) {
            var e = buildLoggedEvent(project, user.getUsername(), SPAN_CREATED_EVENT,
                    Date.from(base.plus(i, SECONDS)), i, "");
            sut.create(e);
            events.add(e);
        }

        var recent = sut.listRecentActivity(user.getUsername(), 3);

        assertThat(recent).hasSize(3);
        assertThat(recent).containsExactly(events.get(4), events.get(3), events.get(2));
    }

    @Test
    void summarizeEvents_multipleTypesAndOtherUsersExcluded()
    {
        var now = Instant.now();

        // Events for the target user
        for (int i = 0; i < 3; i++) {
            var e = buildLoggedEvent(project, user.getUsername(), SPAN_CREATED_EVENT,
                    Date.from(now.plus(i, SECONDS)), -1, "");
            sut.create(e);
        }

        for (int i = 0; i < 2; i++) {
            var e = buildLoggedEvent(project, user.getUsername(),
                    EVENT_TYPE_RECOMMENDER_EVALUATION_EVENT, Date.from(now.plus(10 + i, SECONDS)),
                    -1, DETAIL_JSON);
            sut.create(e);
        }

        // Events for another user should not show up in the summary for USERNAME
        var other = createUser("other");
        for (int i = 0; i < 4; i++) {
            var e = buildLoggedEvent(project, other.getUsername(), SPAN_CREATED_EVENT,
                    Date.from(now.minus(10 + i, SECONDS)), -1, "");
            sut.create(e);
        }

        var begin = now.minus(1, DAYS);
        var end = now.plus(1, DAYS);

        var summary = sut.summarizeEventsBySessionOwner(USERNAME, project, begin, end);

        assertThat(summary)
                .extracting(SummarizedLoggedEvent::getEvent, SummarizedLoggedEvent::getCount)
                .containsExactlyInAnyOrder(tuple("SpanCreatedEvent", 3L),
                        tuple("RecommenderEvaluationResultEvent", 2L));
    }

    @Test
    void summarizeEvents_ignoresOtherProjects()
    {
        var now = Instant.now();

        var otherProject = createProject("Other project");

        // Events in project A
        for (int i = 0; i < 2; i++) {
            var e = buildLoggedEvent(project, user.getUsername(), SPAN_CREATED_EVENT,
                    Date.from(now.plus(i, SECONDS)), -1, "");
            sut.create(e);
        }

        // Events in project B (same user)
        for (int i = 0; i < 5; i++) {
            var e = buildLoggedEvent(otherProject, user.getUsername(), SPAN_CREATED_EVENT,
                    Date.from(now.plus(10 + i, SECONDS)), -1, "");
            sut.create(e);
        }

        var begin = now.minus(1, DAYS);
        var end = now.plus(1, DAYS);

        var summaryForA = sut.summarizeEventsBySessionOwner(USERNAME, project, begin, end);
        var summaryForB = sut.summarizeEventsBySessionOwner(USERNAME, otherProject, begin, end);

        assertThat(summaryForA)
                .extracting(SummarizedLoggedEvent::getEvent, SummarizedLoggedEvent::getCount)
                .containsExactly(tuple("SpanCreatedEvent", 2L));

        assertThat(summaryForB)
                .extracting(SummarizedLoggedEvent::getEvent, SummarizedLoggedEvent::getCount)
                .containsExactly(tuple("SpanCreatedEvent", 5L));
    }

    @Test
    void calculateHistoricalDocumentStates_projectIsolation() throws Exception
    {
        var now = Instant.now();
        var threeDaysAgo = now.minus(3, DAYS);

        var otherProject = createProject("Other project 2");

        // project A: create a document and then a state change
        var createA = buildLoggedEvent(project, user.getUsername(), "AfterDocumentCreatedEvent",
                Date.from(threeDaysAgo), 200L, "");
        sut.create(createA);

        var stateDetailsA = new StateChangeDetails();
        stateDetailsA.setState(ANNOTATION_FINISHED.toString());
        stateDetailsA.setPreviousState(NEW.toString());
        var stateChangeA = buildLoggedEvent(project, user.getUsername(),
                "DocumentStateChangedEvent", Date.from(threeDaysAgo.plusSeconds(10)), 200L,
                JSONUtil.toJsonString(stateDetailsA));
        sut.create(stateChangeA);

        // project B: create a document with same id and a state change
        var createB = buildLoggedEvent(otherProject, user.getUsername(),
                "AfterDocumentCreatedEvent", Date.from(threeDaysAgo), 200L, "");
        sut.create(createB);

        var stateDetailsB = new StateChangeDetails();
        stateDetailsB.setState(ANNOTATION_FINISHED.toString());
        stateDetailsB.setPreviousState(NEW.toString());
        var stateChangeB = buildLoggedEvent(otherProject, user.getUsername(),
                "DocumentStateChangedEvent", Date.from(threeDaysAgo.plusSeconds(10)), 200L,
                JSONUtil.toJsonString(stateDetailsB));
        sut.create(stateChangeB);

        var currentStats = new HashMap<SourceDocumentState, Long>();
        for (var state : SourceDocumentState.values()) {
            currentStats.put(state, 0L);
        }

        var fourDaysAgo = now.minus(4, DAYS);

        var snapsA = sut.calculateHistoricalDocumentStates(project, currentStats, fourDaysAgo);
        var snapsB = sut.calculateHistoricalDocumentStates(otherProject, currentStats, fourDaysAgo);

        // Both projects should have at least one non-zero snapshot for their own created doc
        var hasNonZeroA = snapsA.stream()
                .anyMatch(s -> s.counts().values().stream().anyMatch(c -> c > 0));
        var hasNonZeroB = snapsB.stream()
                .anyMatch(s -> s.counts().values().stream().anyMatch(c -> c > 0));

        assertThat(hasNonZeroA).isTrue();
        assertThat(hasNonZeroB).isTrue();
    }

    // Helper
    private Project createProject(String aName)
    {
        Project p = new Project();
        p.setName(aName);
        return testEntityManager.persist(p);
    }

    private LoggedEventEntity buildLoggedEvent(Project aProject, String aUsername,
            String aEventType, Date aDate, long aDocId, String aDetails)
    {
        var loggedEvent = new LoggedEventEntity();
        loggedEvent.setUser(aUsername);
        loggedEvent.setProject(aProject.getId());
        loggedEvent.setDetails(aDetails);
        loggedEvent.setCreated(aDate.toInstant());
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
        EventRepositoryImpl EventRepositoryImpl(EntityManager aEntityManager)
        {
            return new EventRepositoryImpl(aEntityManager);
        }

        @Bean
        DocumentImportExportService documentImportExportService() throws Exception
        {
            var tsd = createTypeSystemDescription();
            var importService = mock(DocumentImportExportService.class);
            when(importService.importCasFromFile(any(), any(), any(), any()))
                    .thenReturn(CasCreationUtils.createCas(tsd, null, null, null));
            return importService;
        }
    }
}
