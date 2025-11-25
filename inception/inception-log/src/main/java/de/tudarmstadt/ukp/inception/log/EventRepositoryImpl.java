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
import static java.util.Arrays.asList;
import static java.util.Optional.empty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.stream.Streams;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.log.api.EventRepository;
import de.tudarmstadt.ukp.inception.log.api.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.log.api.model.SummarizedLoggedEvent;
import de.tudarmstadt.ukp.inception.log.api.model.UserSessionStats;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.model.AnnotationDetails;
import de.tudarmstadt.ukp.inception.log.model.FeatureChangeDetails;
import de.tudarmstadt.ukp.inception.log.model.LoggedEventEntity;
import de.tudarmstadt.ukp.inception.log.model.LoggedEventEntity_;
import de.tudarmstadt.ukp.inception.log.model.SessionDetails;
import de.tudarmstadt.ukp.inception.support.uima.Range;
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
    public Optional<Range> getLastEditRange(SourceDocument aDocument, String aDataOwner)
    {
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(LoggedEventEntity.class);
        var root = cq.from(LoggedEventEntity.class);

        // Predicates for filtering
        var documentPredicate = cb.equal(root.get(LoggedEventEntity_.document), aDocument.getId());
        var dataOwnerPredicate = cb.equal(root.get(LoggedEventEntity_.annotator), aDataOwner);

        var eventPredicate = cb.or( //
                root.get(LoggedEventEntity_.event).in(asList( //
                        "ChainLinkCreatedEvent", //
                        "ChainSpanCreatedEvent", //
                        "RelationCreatedEvent", //
                        "SpanCreatedEvent")),
                cb.equal(root.get(LoggedEventEntity_.event), "SpanMovedEvent"), //
                cb.equal(root.get(LoggedEventEntity_.event), "FeatureValueUpdatedEvent"));

        // Combine all together with AND
        cq.select(root).where(cb.and(documentPredicate, dataOwnerPredicate, eventPredicate))
                .orderBy(cb.desc(root.get("created"))); // Most recent first

        // Build query and limit to 1 result
        var query = entityManager.createQuery(cq).setMaxResults(1);

        var latestEvent = query.getResultStream().findFirst().orElse(null);
        if (latestEvent == null) {
            return empty();
        }

        // Extract range from details
        var detailsString = latestEvent.getDetails();
        if ("FeatureValueUpdatedEvent".equals(latestEvent.getEvent())) {
            try {
                var details = fromJsonString(FeatureChangeDetails.class, detailsString);
                if (details.getAnnotation().getBegin() < 0
                        || details.getAnnotation().getEnd() < 0) {
                    return empty();
                }

                return Optional.of(new Range(details.getAnnotation().getBegin(),
                        details.getAnnotation().getEnd()));
            }
            catch (IOException e) {
                // Maybe it is not a feature update event after all
            }
        }

        try {
            var details = fromJsonString(AnnotationDetails.class, detailsString);
            if (details.getBegin() < 0 || details.getEnd() < 0) {
                return empty();
            }
            return Optional.of(new Range(details.getBegin(), details.getEnd()));

        }
        catch (IOException e) {
            // Maybe it is not an annotation event after all
        }

        return empty();
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
                        cb.between(root.get(LoggedEventEntity_.created), Date.from(aFrom),
                                Date.from(aTo)));

        var aggregator = new HashMap<SummarizedLoggedEventKey, AtomicLong>();

        entityManager.createQuery(query).getResultStream().forEach(tuple -> {
            var truncDate = tuple.get(0, Date.class).toInstant().truncatedTo(DAYS);
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
                        cb.between(root.get(LoggedEventEntity_.created), Date.from(aFrom),
                                Date.from(aTo)));

        var aggregator = new HashMap<SummarizedLoggedEventKey, AtomicLong>();

        entityManager.createQuery(query).getResultStream().forEach(tuple -> {
            var truncDate = tuple.get(0, Date.class).toInstant().truncatedTo(DAYS);
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

    private static record SummarizedLoggedEventKey(String event, Instant date, long document) {}
}
