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
package de.tudarmstadt.ukp.inception.documents;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSession;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class AnnotationSessionService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public long openSession(SourceDocument aDocument, String aUser)
    {
        var session = new AnnotationSession(aDocument, aUser, aDocument.getProject(), Instant.now());
        entityManager.persist(session);
        return session.getId();
    }

    @Transactional
    public void addActiveTime(long aSessionId, long aDeltaMs)
    {
        var session = entityManager.find(AnnotationSession.class, aSessionId);
        if (session != null) {
            session.addActiveTime(aDeltaMs);
            entityManager.merge(session);
        }
    }

    @Transactional
    public void closeSession(long aSessionId)
    {
        var session = entityManager.find(AnnotationSession.class, aSessionId);
        if (session != null && session.getClosedAt() == null) {
            session.setClosedAt(Instant.now());
            entityManager.merge(session);
        }
    }

    @EventListener
    @Transactional
    public void onAnnotationEvent(AnnotationEvent aEvent)
    {
        try {
            var sessions = entityManager
                    .createQuery("FROM AnnotationSession WHERE document = :doc AND user = :user "
                            + "AND closedAt IS NULL ORDER BY openedAt DESC",
                            AnnotationSession.class)
                    .setParameter("doc", aEvent.getDocument())
                    .setParameter("user", aEvent.getDocumentOwner())
                    .setMaxResults(1)
                    .getResultList();
            if (!sessions.isEmpty()) {
                sessions.get(0).incrementChangesCount();
                entityManager.merge(sessions.get(0));
            }
        }
        catch (Exception e) {
            LOG.error("Failed to record annotation event for session", e);
        }
    }

    public List<AnnotationSession> listSessions(SourceDocument aDocument, String aUser)
    {
        return entityManager
                .createQuery("FROM AnnotationSession WHERE document = :doc AND user = :user "
                        + "ORDER BY openedAt DESC",
                        AnnotationSession.class)
                .setParameter("doc", aDocument)
                .setParameter("user", aUser)
                .getResultList();
    }
}
