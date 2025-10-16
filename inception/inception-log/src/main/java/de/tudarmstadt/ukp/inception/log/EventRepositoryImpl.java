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

import static java.lang.String.join;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
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
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.model.AnnotationDetails;
import de.tudarmstadt.ukp.inception.log.model.FeatureChangeDetails;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent_;
import de.tudarmstadt.ukp.inception.log.model.SummarizedLoggedEvent;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
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

    @Override
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
    public Optional<Range> getLastEditRange(SourceDocument aDocument, String aDataOwner)
    {
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(LoggedEvent.class);
        var root = cq.from(LoggedEvent.class);

        // Predicates for filtering
        var documentPredicate = cb.equal(root.get(LoggedEvent_.document), aDocument.getId());
        var dataOwnerPredicate = cb.equal(root.get(LoggedEvent_.annotator), aDataOwner);

        var eventPredicate = cb.or( //
                root.get(LoggedEvent_.event).in(asList( //
                        "ChainLinkCreatedEvent", //
                        "ChainSpanCreatedEvent", //
                        "DocumentMetadataCreatedEvent", //
                        "RelationCreatedEvent", //
                        "SpanCreatedEvent")),
                cb.equal(root.get(LoggedEvent_.event), "SpanMovedEvent"), //
                cb.equal(root.get(LoggedEvent_.event), "FeatureValueUpdatedEvent"));

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
                var details = JSONUtil.fromJsonString(FeatureChangeDetails.class, detailsString);
                return Optional.of(new Range(details.getAnnotation().getBegin(),
                        details.getAnnotation().getEnd()));
            }
            catch (IOException e) {
                // Maybe it is not a feature update event after all
            }
        }

        try {
            var details = JSONUtil.fromJsonString(AnnotationDetails.class, detailsString);
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
                "FROM  LoggedEvent", //
                "WHERE user = :user", //
                "  AND project = :project", //
                "  AND event in (:eventTypes)", //
                "ORDER BY created DESC");

        var result = entityManager.createQuery(query, LoggedEvent.class) //
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
        var query = join("\n", //
                "FROM  LoggedEvent", //
                "WHERE user = :user", //
                "ORDER BY created DESC");

        return entityManager.createQuery(query, LoggedEvent.class) //
                .setParameter("user", aUsername) //
                .setMaxResults(aMaxSize) //
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public <E extends Throwable> void forEachLoggedEvent(Project aProject,
            FailableConsumer<LoggedEvent, E> aConsumer)
    {
        // Set up data source
        var query = String.join("\n", //
                "FROM LoggedEvent WHERE ", //
                "project = :project ", //
                "ORDER BY id");
        var typedQuery = entityManager.createQuery(query, LoggedEvent.class) //
                .setParameter("project", aProject.getId());

        try (var eventStream = typedQuery.getResultStream()) {
            Streams.failableStream(eventStream).forEach(aConsumer);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SummarizedLoggedEvent> summarizeEventsBySessionOwner(String aSessionOwner,
            Project aProject, Instant aFrom, Instant aTo)
    {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Tuple.class);
        var root = query.from(LoggedEvent.class);

        query //
                .multiselect( //
                        root.get(LoggedEvent_.created), //
                        root.get(LoggedEvent_.document), //
                        root.get(LoggedEvent_.event))
                .where( //
                        cb.equal(root.get(LoggedEvent_.user), aSessionOwner), //
                        cb.equal(root.get(LoggedEvent_.project), aProject.getId()), //
                        cb.between(root.get(LoggedEvent_.created), Date.from(aFrom),
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
        var root = query.from(LoggedEvent.class);

        query //
                .multiselect( //
                        root.get(LoggedEvent_.created), //
                        root.get(LoggedEvent_.document), //
                        root.get(LoggedEvent_.event))
                .where( //
                        cb.equal(root.get(LoggedEvent_.annotator), aDataOwner), //
                        cb.equal(root.get(LoggedEvent_.project), aProject.getId()), //
                        cb.between(root.get(LoggedEvent_.created), Date.from(aFrom),
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
