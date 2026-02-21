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

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonString;
import static java.lang.String.join;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Comparator.comparing;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.stream.Streams;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.log.api.EventRepository;
import de.tudarmstadt.ukp.inception.log.api.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.log.api.model.StateChangeDetails;
import de.tudarmstadt.ukp.inception.log.api.model.SummarizedLoggedEvent;
import de.tudarmstadt.ukp.inception.log.api.model.UserSessionStats;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.model.LoggedEventEntity;
import de.tudarmstadt.ukp.inception.log.model.LoggedEventEntity_;
import de.tudarmstadt.ukp.inception.log.model.SessionDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link EventLoggingAutoConfiguration#eventRepository}.
 * </p>
 */
public class EventRepositoryImpl
    implements EventRepository
{
    private static final int RECENT_ACTIVITY_HORIZON = 3500;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @PersistenceContext EntityManager entityManager;

    @Autowired
    public EventRepositoryImpl(EntityManager aEntityManager)
    {
        entityManager = aEntityManager;
    }

    @Transactional
    public void create(LoggedEvent... aEvents)
    {
        var start = System.currentTimeMillis();
        for (var event : aEvents) {
            LOG.trace("{}", event);
            entityManager.persist(event);
        }

        long duration = System.currentTimeMillis() - start;

        if (aEvents.length > 0 && !LOG.isTraceEnabled()) {
            LOG.debug("... {} events stored ... ({}ms)", aEvents.length, duration);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserSessionStats getAggregateSessionDuration(String aSessionOwner)
    {
        var query = join("\n", //
                "FROM LoggedEventEntity", //
                "WHERE user = :sessionOwner", //
                "  AND project = :project", //
                "  AND event = :eventType");

        // We query for project here so we can use the (project, user, event) index.
        // If there is no project, the project is -1
        var results = entityManager.createQuery(query, LoggedEventEntity.class) //
                .setParameter("sessionOwner", aSessionOwner) //
                .setParameter("project", -1) //
                .setParameter("eventType", "UserSessionEndedEvent") //
                .setMaxResults(RECENT_ACTIVITY_HORIZON) //
                .getResultList();

        var accuDuration = new AtomicLong();
        for (var result : results) {
            try {
                var details = fromJsonString(SessionDetails.class, result.getDetails());
                accuDuration.addAndGet(details.getDuration());
            }
            catch (Exception e) {
                // Skip if we cannot parse
            }
        }

        return new UserSessionStats(aSessionOwner, Duration.ofMillis(accuDuration.get()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoggedEvent> listRecentActivity(Project aProject, String aUsername,
            Collection<String> aEventTypes, int aMaxSize)
    {
        var query = join("\n", //
                "FROM  LoggedEventEntity", //
                "WHERE user = :user", //
                "  AND project = :project", //
                "  AND event in (:eventTypes)", //
                "ORDER BY created DESC");

        var result = entityManager.createQuery(query, LoggedEventEntity.class) //
                .setParameter("user", aUsername) //
                .setParameter("project", aProject.getId()) //
                .setParameter("eventTypes", aEventTypes) //
                .setMaxResults(RECENT_ACTIVITY_HORIZON) //
                .getResultList();

        var reducedResults = new ArrayList<LoggedEvent>();
        var documentsSeen = new HashSet<Pair<Long, String>>();

        var i = result.iterator();
        while (i.hasNext() && reducedResults.size() < aMaxSize) {
            LoggedEvent event = i.next();

            // Check if we already have the latest event of this doc/annotator combination
            var doc = Pair.of(event.getDocument(), event.getAnnotator());
            if (documentsSeen.contains(doc)) {
                continue;
            }

            reducedResults.add(event);
            documentsSeen.add(doc);
        }

        return reducedResults;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoggedEvent> listRecentActivity(String aUsername, int aMaxSize)
    {
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(LoggedEventEntity.class);
        var root = cq.from(LoggedEventEntity.class);

        cq.select(root)//
                .where(cb.equal(root.get(LoggedEventEntity_.user), aUsername))//
                .orderBy(cb.desc(root.get(LoggedEventEntity_.created)));

        return entityManager.createQuery(cq)//
                .setMaxResults(aMaxSize)//
                .getResultStream() //
                .map(e -> (LoggedEvent) e) //
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public <E extends Throwable> void forEachLoggedEvent(Project aProject,
            FailableConsumer<LoggedEvent, E> aConsumer)
    {
        // Set up data source
        var query = String.join("\n", //
                "FROM LoggedEventEntity WHERE ", //
                "project = :project ", //
                "ORDER BY id");
        var typedQuery = entityManager.createQuery(query, LoggedEventEntity.class) //
                .setParameter("project", aProject.getId());

        try (var eventStream = typedQuery.getResultStream()) {
            Streams.failableStream(eventStream).forEach(e -> aConsumer.accept(e));
        }
    }

    @Override
    @Transactional() // NOT read-only!
    public <E extends Throwable> void forEachLoggedEventUpdatable(Project aProject,
            FailableConsumer<LoggedEvent, E> aConsumer)
    {
        // Set up data source
        var query = String.join("\n", //
                "FROM LoggedEventEntity WHERE ", //
                "project = :project ", //
                "ORDER BY id");
        var typedQuery = entityManager.createQuery(query, LoggedEventEntity.class) //
                .setParameter("project", aProject.getId());

        try (var eventStream = typedQuery.getResultStream()) {
            Streams.failableStream(eventStream).forEach(e -> aConsumer.accept(e));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SummarizedLoggedEvent> summarizeEventsBySessionOwner(String aSessionOwner,
            Project aProject, Instant aFrom, Instant aTo)
    {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Tuple.class);
        var root = query.from(LoggedEventEntity.class);

        query //
                .multiselect( //
                        root.get(LoggedEventEntity_.created), //
                        root.get(LoggedEventEntity_.document), //
                        root.get(LoggedEventEntity_.event))
                .where( //
                        cb.equal(root.get(LoggedEventEntity_.user), aSessionOwner), //
                        cb.equal(root.get(LoggedEventEntity_.project), aProject.getId()), //
                        cb.between(root.get(LoggedEventEntity_.created), aFrom, aTo));

        var aggregator = new HashMap<SummarizedLoggedEventKey, AtomicLong>();

        entityManager.createQuery(query).getResultStream().forEach(tuple -> {
            var truncDate = tuple.get(0, Instant.class).truncatedTo(DAYS);
            var document = tuple.get(1, Long.class);
            var event = tuple.get(2, String.class);
            var key = new SummarizedLoggedEventKey(event, truncDate, document);
            aggregator.computeIfAbsent(key, $ -> new AtomicLong()).addAndGet(1);
        });

        return aggregator.entrySet().stream() //
                .map(e -> new SummarizedLoggedEvent(e.getKey().event(), e.getKey().document(),
                        e.getKey().date(), e.getValue().get())) //
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SummarizedLoggedEvent> summarizeEventsByDataOwner(String aDataOwner,
            Project aProject, Instant aFrom, Instant aTo)
    {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Tuple.class);
        var root = query.from(LoggedEventEntity.class);

        query //
                .multiselect( //
                        root.get(LoggedEventEntity_.created), //
                        root.get(LoggedEventEntity_.document), //
                        root.get(LoggedEventEntity_.event))
                .where( //
                        cb.equal(root.get(LoggedEventEntity_.annotator), aDataOwner), //
                        cb.equal(root.get(LoggedEventEntity_.project), aProject.getId()), //
                        cb.between(root.get(LoggedEventEntity_.created), aFrom, aTo));

        var aggregator = new HashMap<SummarizedLoggedEventKey, AtomicLong>();

        entityManager.createQuery(query).getResultStream().forEach(tuple -> {
            var truncDate = tuple.get(0, Instant.class).truncatedTo(DAYS);
            var document = tuple.get(1, Long.class);
            var event = tuple.get(2, String.class);
            var key = new SummarizedLoggedEventKey(event, truncDate, document);
            aggregator.computeIfAbsent(key, $ -> new AtomicLong()).addAndGet(1);
        });

        return aggregator.entrySet().stream() //
                .map(e -> new SummarizedLoggedEvent(e.getKey().event(), e.getKey().document(),
                        e.getKey().date(), e.getValue().get())) //
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentStateSnapshot> calculateHistoricalDocumentStates(Project aProject,
            Map<SourceDocumentState, Long> aCurrentStats, Instant aFrom)
    {
        var from = aFrom;
        if (from == null) {
            from = Instant.MIN;
        }

        // Initialize running counts from current state
        var counts = new HashMap<SourceDocumentState, Long>();
        for (var state : SourceDocumentState.values()) {
            counts.put(state, aCurrentStats.getOrDefault(state, 0L));
        }

        // Build result list (will be in reverse order initially)
        var result = new ArrayList<DocumentStateSnapshot>();

        // Add current state snapshot
        var now = Instant.now();
        result.add(createSnapshot(now, counts));

        // Track previous day and counts for change detection
        var previousDay = toDay(now);
        var previousCounts = new HashMap<>(counts);

        // Fetch all relevant events sorted newest-first
        var query = join("\n", //
                "FROM LoggedEventEntity WHERE ", //
                "project = :project ", //
                "AND created >= :from ", //
                "AND event IN ('AfterDocumentCreatedEvent', 'BeforeDocumentRemovedEvent', 'DocumentStateChangedEvent') ", //
                "ORDER BY created DESC");

        var events = entityManager.createQuery(query, LoggedEventEntity.class) //
                .setParameter("project", aProject.getId()) //
                .setParameter("from", from) //
                .getResultList();

        // Backwards replay
        for (var event : events) {
            var eventTime = event.getCreated();
            var eventDay = toDay(eventTime);

            // Apply inverse event logic
            var changed = applyInverseEvent(event, counts, events);

            // Check if we crossed a day boundary
            if (!eventDay.equals(previousDay)) {
                // Check if counts changed from previous snapshot
                if (changed || !countsEqual(counts, previousCounts)) {
                    result.add(createSnapshot(eventTime, counts));
                    previousDay = eventDay;
                    previousCounts = new HashMap<>(counts);
                }
            }
        }

        // Add boundary snapshot at aTo
        if (!result.isEmpty() && !from.equals(Instant.MIN)) {
            var lastSnapshot = result.get(result.size() - 1);
            if (!toDay(lastSnapshot.day()).equals(toDay(from))) {
                result.add(createSnapshot(from, counts));
            }
        }

        // Reverse to get oldest-first ordering
        result.sort(comparing(DocumentStateSnapshot::day));

        return result;
    }

    private boolean applyInverseEvent(LoggedEvent aEvent, Map<SourceDocumentState, Long> aCounts,
            List<LoggedEventEntity> aAllEvents)
    {
        var eventType = aEvent.getEvent();

        switch (eventType) {
        case "AfterDocumentCreatedEvent":
            // Document was created, so going backwards it didn't exist
            // Decrement NEW state
            decrementState(aCounts, SourceDocumentState.NEW);
            return true;

        case "BeforeDocumentRemovedEvent":
            // Document was removed, so going backwards it existed
            // Find last known state of this document
            var removedDocState = findLastKnownState(aEvent.getDocument(), aAllEvents);
            incrementState(aCounts, removedDocState);
            return true;

        case "DocumentStateChangedEvent":
            // Swap the state transition
            try {
                var details = fromJsonString(StateChangeDetails.class, aEvent.getDetails());
                var newState = SourceDocumentState.fromString(details.getState());
                var previousState = details.getPreviousState() != null
                        ? SourceDocumentState.fromString(details.getPreviousState())
                        : null;

                // Undo the transition: decrement new state, increment previous state
                decrementState(aCounts, newState);
                if (previousState != null) {
                    incrementState(aCounts, previousState);
                }
                return true;
            }
            catch (Exception e) {
                LOG.warn("Failed to parse DocumentStateChangedEvent details", e);
                return false;
            }

        default:
            return false;
        }
    }

    private SourceDocumentState findLastKnownState(long aDocumentId,
            List<LoggedEventEntity> aAllEvents)
    {
        // Search through all events for the last state change of this document
        for (var event : aAllEvents) {
            if (event.getDocument() == aDocumentId
                    && "DocumentStateChangedEvent".equals(event.getEvent())) {
                try {
                    var details = fromJsonString(StateChangeDetails.class, event.getDetails());
                    return SourceDocumentState.valueOf(details.getState());
                }
                catch (Exception e) {
                    LOG.warn("Failed to parse state for removed document", e);
                }
            }
        }

        // No state changes found, assume it was NEW when removed
        return SourceDocumentState.NEW;
    }

    private void incrementState(Map<SourceDocumentState, Long> aCounts, SourceDocumentState aState)
    {
        aCounts.put(aState, aCounts.getOrDefault(aState, 0L) + 1);
    }

    private void decrementState(Map<SourceDocumentState, Long> aCounts, SourceDocumentState aState)
    {
        aCounts.put(aState, Math.max(0L, aCounts.getOrDefault(aState, 0L) - 1));
    }

    private LocalDate toDay(Instant aInstant)
    {
        return aInstant.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private DocumentStateSnapshot createSnapshot(Instant aTime,
            Map<SourceDocumentState, Long> aCounts)
    {
        var snapshot = new LinkedHashMap<SourceDocumentState, Integer>();
        for (var state : SourceDocumentState.values()) {
            snapshot.put(state, aCounts.getOrDefault(state, 0L).intValue());
        }
        return new DocumentStateSnapshot(aTime, snapshot);
    }

    private boolean countsEqual(Map<SourceDocumentState, Long> a, Map<SourceDocumentState, Long> b)
    {
        for (var state : SourceDocumentState.values()) {
            if (!a.getOrDefault(state, 0L).equals(b.getOrDefault(state, 0L))) {
                return false;
            }
        }
        return true;
    }

    private static record SummarizedLoggedEventKey(String event, Instant date, long document) {}
}
