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
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.NEW;
import static java.util.Map.entry;
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
import java.util.Map;

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
    void calculateHistoricalDocumentStates_nullFrom_doesNotOverflowTimestamp()
    {
        // Regression test: previously a null `aFrom` was substituted with Instant.MIN,
        // which overflows when Hibernate binds it as a JDBC Timestamp, producing a 500.
        var currentStats = new HashMap<SourceDocumentState, Long>();
        for (var state : SourceDocumentState.values()) {
            currentStats.put(state, 0L);
        }

        assertThat(sut.calculateHistoricalDocumentStates(project, currentStats, null)) //
                .isNotNull();
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

    @Test
    void calculateHistoricalDocumentStates_weighted_appliesPerDocWeightOnEveryReplayedEvent()
        throws Exception
    {
        var now = Instant.now();
        var threeDaysAgo = now.minus(3, DAYS);
        var twoDaysAgo = now.minus(2, DAYS);

        // Doc 500 was created (NEW) 3 days ago and transitioned to ANNOTATION_FINISHED 2 days ago.
        // It still exists today as ANNOTATION_FINISHED with weight 7.
        sut.create(buildLoggedEvent(project, user.getUsername(), "AfterDocumentCreatedEvent",
                Date.from(threeDaysAgo), 500L, ""));

        var details = new StateChangeDetails();
        details.setState(ANNOTATION_FINISHED.toString());
        details.setPreviousState(NEW.toString());
        sut.create(buildLoggedEvent(project, user.getUsername(), "DocumentStateChangedEvent",
                Date.from(twoDaysAgo), 500L, JSONUtil.toJsonString(details)));

        var currentStats = zeroStats();
        currentStats.put(ANNOTATION_FINISHED, 7L);

        var weights = Map.of(500L, 7L);
        var fourDaysAgo = now.minus(4, DAYS);

        var snapshots = sut.calculateHistoricalDocumentStates(project, currentStats, weights,
                fourDaysAgo);

        // Backwards replay (then sorted oldest-first) should produce:
        // ~4 days ago: empty (boundary)
        // ~3 days ago: empty (after undoing the creation, doc no longer existed)
        // ~2 days ago: NEW=7 (after undoing the state change)
        // now: ANNOTATION_FINISHED=7
        // Every adjustment must use the doc's weight (7), not 1.
        assertThat(snapshots).isNotEmpty();
        assertThat(snapshots.get(0).counts()).allSatisfy((s, c) -> assertThat(c).isZero());

        var newest = snapshots.get(snapshots.size() - 1);
        assertThat(newest.counts().get(ANNOTATION_FINISHED)).isEqualTo(7L);

        // At least one intermediate snapshot must reflect NEW=7 (undone state change).
        assertThat(snapshots).anySatisfy(s -> assertThat(s.counts().get(NEW)).isEqualTo(7L));

        // Every snapshot's totals must be 0 or 7 — only one doc exists in this scenario and its
        // weight is 7, so any non-zero total proves the weight (not 1) was applied.
        for (var s : snapshots) {
            var total = s.counts().values().stream().mapToLong(Long::longValue).sum();
            assertThat(total).isIn(0L, 7L);
        }
    }

    @Test
    void calculateHistoricalDocumentStates_weighted_treatsMissingWeightAsZero() throws Exception
    {
        var now = Instant.now();
        var threeDaysAgo = now.minus(3, DAYS);
        var twoDaysAgo = now.minus(2, DAYS);

        // Doc 600: created → finished, but absent from the weight map — must be invisible.
        sut.create(buildLoggedEvent(project, user.getUsername(), "AfterDocumentCreatedEvent",
                Date.from(threeDaysAgo), 600L, ""));
        var details = new StateChangeDetails();
        details.setState(ANNOTATION_FINISHED.toString());
        details.setPreviousState(NEW.toString());
        sut.create(buildLoggedEvent(project, user.getUsername(), "DocumentStateChangedEvent",
                Date.from(twoDaysAgo), 600L, JSONUtil.toJsonString(details)));

        // Current state already excludes doc 600 (weight=0 ⇒ contributes nothing).
        var currentStats = zeroStats();
        // Weight map empty — equivalent to "no doc in scope". A weight=0 entry would behave the
        // same; verifying via the absent-key path.
        var weights = Map.<Long, Long> of();
        var fourDaysAgo = now.minus(4, DAYS);

        var snapshots = sut.calculateHistoricalDocumentStates(project, currentStats, weights,
                fourDaysAgo);

        // Every snapshot must be all-zero because every event for doc 600 is a no-op under the
        // empty weight map.
        assertThat(snapshots).isNotEmpty();
        for (var s : snapshots) {
            assertThat(s.counts().values()).allSatisfy(c -> assertThat(c).isZero());
        }
    }

    @Test
    void calculateHistoricalDocumentStates_weighted_combinesDifferentDocWeightsIndependently()
        throws Exception
    {
        var now = Instant.now();
        var threeDaysAgo = now.minus(3, DAYS);
        var twoDaysAgo = now.minus(2, DAYS);

        // Doc 700 weight 3, transitions NEW → ANNOTATION_IN_PROGRESS 3 days ago.
        // Doc 701 weight 11, transitions NEW → ANNOTATION_FINISHED 2 days ago.
        // Both still exist today.
        var d1 = new StateChangeDetails();
        d1.setState(ANNOTATION_IN_PROGRESS.toString());
        d1.setPreviousState(NEW.toString());
        sut.create(buildLoggedEvent(project, user.getUsername(), "DocumentStateChangedEvent",
                Date.from(threeDaysAgo), 700L, JSONUtil.toJsonString(d1)));

        var d2 = new StateChangeDetails();
        d2.setState(ANNOTATION_FINISHED.toString());
        d2.setPreviousState(NEW.toString());
        sut.create(buildLoggedEvent(project, user.getUsername(), "DocumentStateChangedEvent",
                Date.from(twoDaysAgo), 701L, JSONUtil.toJsonString(d2)));

        // Today's totals: doc 700 (weight 3) is IN_PROGRESS, doc 701 (weight 11) is FINISHED.
        var currentStats = zeroStats();
        currentStats.put(ANNOTATION_IN_PROGRESS, 3L);
        currentStats.put(ANNOTATION_FINISHED, 11L);

        var weights = Map.ofEntries(entry(700L, 3L), entry(701L, 11L));
        var fourDaysAgo = now.minus(4, DAYS);

        var snapshots = sut.calculateHistoricalDocumentStates(project, currentStats, weights,
                fourDaysAgo);

        assertThat(snapshots).isNotEmpty();

        // Newest snapshot must match current state exactly — each doc weighted by its own value.
        var newest = snapshots.get(snapshots.size() - 1);
        assertThat(newest.counts().get(ANNOTATION_IN_PROGRESS)).isEqualTo(3L);
        assertThat(newest.counts().get(ANNOTATION_FINISHED)).isEqualTo(11L);

        // Going back past doc 701's state change (~2 days ago) but not past doc 700's
        // (~3 days ago): doc 701's weight returns to NEW (11), doc 700 remains IN_PROGRESS (3).
        // Total weight always sums to 14 — weights are conserved on state-change events because
        // each adjusts both source and target buckets by the same per-doc weight.
        for (var s : snapshots) {
            var total = s.counts().values().stream().mapToLong(Long::longValue).sum();
            assertThat(total).isEqualTo(14);
        }
    }

    private HashMap<SourceDocumentState, Long> zeroStats()
    {
        var stats = new HashMap<SourceDocumentState, Long>();
        for (var state : SourceDocumentState.values()) {
            stats.put(state, 0L);
        }
        return stats;
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
