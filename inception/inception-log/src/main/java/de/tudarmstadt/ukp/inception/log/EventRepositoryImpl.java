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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link EventLoggingAutoConfiguration#eventRepository}.
 * </p>
 */
public class EventRepositoryImpl
    implements EventRepository
{
    private final Logger log = LoggerFactory.getLogger(getClass());

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
        long start = System.currentTimeMillis();
        for (LoggedEvent event : aEvents) {
            log.trace("{}", event);
            entityManager.persist(event);
        }
        long duration = System.currentTimeMillis() - start;

        if (aEvents.length > 0 && !log.isTraceEnabled()) {
            log.debug("... {} events stored ... ({}ms)", aEvents.length, duration);
        }
    }

    @Override
    @Transactional
    public List<LoggedEvent> listLoggedEventsForRecommender(Project aProject, String aUsername,
            String aEventType, int aMaxSize, long aRecommenderId)
    {
        String detailStr = "%\"recommenderId\":" + aRecommenderId + "%";

        return listLoggedEventsForDetail(aProject, aUsername, aEventType, aMaxSize, detailStr);
    }

    @Override
    @Transactional
    public List<LoggedEvent> listLoggedEventsForDetail(Project aProject, String aUsername,
            String aEventType, int aMaxSize, String aDetail)
    {
        String query = String.join("\n", //
                "FROM LoggedEvent WHERE ", //
                "user=:user AND ", //
                "project = :project AND ", //
                "event = :event AND ", //
                "details LIKE :details ", //
                "ORDER BY created DESC");

        return entityManager.createQuery(query, LoggedEvent.class) //
                .setParameter("user", aUsername) //
                .setParameter("project", aProject.getId()) //
                .setParameter("event", aEventType) //
                .setParameter("details", aDetail) //
                .setMaxResults(aMaxSize).getResultList();
    }

    @Override
    @Transactional
    public List<LoggedEvent> listUniqueLoggedEventsForDoc(Project aProject, String aUsername,
            String[] aEventTypes, int aMaxSize)
    {
        String query = String.join("\n", //
                "FROM LoggedEvent WHERE", //
                "id IN", //
                // select one event when time-stamps are the same per document
                "   (SELECT max(id)", //
                "   FROM LoggedEvent WHERE", //
                "   user=:user AND", //
                "   project=:project AND", //
                "   event in (:eventTypes)", //
                "   AND created in", //
                // select last created events per document
                "       (SELECT max(created) ", //
                "       FROM LoggedEvent WHERE", //
                "       user=:user AND", //
                "       project=:project AND", //
                "       event in (:eventTypes)", //
                "       GROUP BY document)", //
                "   GROUP BY document)", //
                "ORDER BY created DESC");

        TypedQuery<LoggedEvent> typedQuery = entityManager.createQuery(query, LoggedEvent.class) //
                .setParameter("user", aUsername) //
                .setParameter("project", aProject.getId()) //
                .setParameter("eventTypes", Arrays.asList(aEventTypes)); //
        return typedQuery.setMaxResults(aMaxSize).getResultList();
    }

    @Override
    @Transactional
    public List<LoggedEvent> listRecentActivity(Project aProject, String aUsername,
            Collection<String> aEventTypes, int aMaxSize)
    {
        String query = join("\n", //
                "FROM  LoggedEvent", //
                "WHERE user = :user", //
                "  AND project = :project", //
                "  AND event in (:eventTypes)", //
                "ORDER BY created DESC");

        List<LoggedEvent> result = entityManager.createQuery(query, LoggedEvent.class) //
                .setParameter("user", aUsername) //
                .setParameter("project", aProject.getId()) //
                .setParameter("eventTypes", aEventTypes) //
                .setMaxResults(3500) //
                .getResultList();

        List<LoggedEvent> reducedResults = new ArrayList<>();
        Set<Pair<Long, String>> documentsSeen = new HashSet<>();

        Iterator<LoggedEvent> i = result.iterator();
        while (i.hasNext() && reducedResults.size() < aMaxSize) {
            LoggedEvent event = i.next();

            // Check if we already have the latest event of this doc/annotator combination
            Pair<Long, String> doc = Pair.of(event.getDocument(), event.getAnnotator());
            if (documentsSeen.contains(doc)) {
                continue;
            }

            reducedResults.add(event);
            documentsSeen.add(doc);
        }

        return reducedResults;
    }
    
    @Override
    @Transactional
    public List<LoggedEvent> listFilteredRecentActivity(Collection<String> aEventTypes, int aMaxSize)
    {
        String query = join("\n", //
                "FROM  LoggedEvent", //
                "WHERE event not in (:eventTypes)", //
                "ORDER BY created DESC");

        List<LoggedEvent> result = new ArrayList<>();
        try {
            result = entityManager.createQuery(query, LoggedEvent.class) //
                    .setParameter("eventTypes", aEventTypes) //
                    .setMaxResults(aMaxSize) //
                    .getResultList();
        }
        catch (NoResultException e) {
            log.debug(e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public void forEachLoggedEvent(Project aProject, Consumer<LoggedEvent> aConsumer)
    {
        // Set up data source
        String query = String.join("\n", //
                "FROM LoggedEvent WHERE ", //
                "project = :project ", //
                "ORDER BY id");
        TypedQuery<LoggedEvent> typedQuery = entityManager.createQuery(query, LoggedEvent.class) //
                .setParameter("project", aProject.getId());

        try (Stream<LoggedEvent> eventStream = typedQuery.getResultStream()) {
            eventStream.forEach(aConsumer);
        }
    }
}
