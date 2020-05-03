/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CasStorageSession
    implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory
            .getLogger(MethodHandles.lookup().lookupClass());

    private static final ThreadLocal<CasStorageSession> storageSession = ThreadLocal
            .withInitial(() -> null);

    private boolean closed = false;
    private StackTraceElement[] creatorStack;

    private final Map<Long, Map<String, SessionManagedCas>> managedCases = new LinkedHashMap<>();

    /**
     * @return a new session.
     * 
     * @throws IllegalStateException
     *             if a session already exists for the current thread.
     */
    public static CasStorageSession open()
    {
        if (storageSession.get() != null) {
            throw new IllegalStateException("CAS storage session for thread ["
                    + Thread.currentThread().getName() + "] already initialized");
        }

        CasStorageSession session = new CasStorageSession();
        storageSession.set(session);

        session.creatorStack = new Exception().getStackTrace();

        LOGGER.trace("CAS storage session [{}] opened", session.hashCode());

        return session;
    }

    /**
     * @return the current session. Returns {@code null} if there is no current session.
     */
    public static CasStorageSession get()
    {
        CasStorageSession session = storageSession.get();

        if (session == null) {
            LOGGER.trace("No CAS storage session available");
        }

        return session;
    }

    /**
     * Closes this session.
     * 
     * @throws IllegalStateException
     *             if the current session is not associated with the current thread.
     */
    @Override
    public void close() throws Exception
    {
        if (storageSession.get() != this) {
            throw new IllegalStateException("CAS storage session on thread ["
                    + Thread.currentThread().getName() + "] is not the current session");
        }

        closed = true;

        storageSession.set(null);

        if (LOGGER.isTraceEnabled()) {
            if (managedCases.isEmpty()) {
                LOGGER.trace("CAS storage session [{}] closed - was empty", hashCode());
            }
            else {
                LOGGER.trace("CAS storage session [{}] closed", hashCode());
                LOGGER.trace("CAS storage session contained the following managed CASes:");
                managedCases.values()
                        .forEach(casByUser -> casByUser.values()
                                .forEach(managedCas -> LOGGER.trace("- {}", managedCas)));
            }
        }
    }

    /**
     * @return if the current session has already been closed.
     */
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * Register the given CAS into the session.
     * 
     * @param aDocument
     *            the document for which the CAS was retrieved.
     * @param aUser
     *            the user owning the CAS.
     * @param aMode
     *            the access mode.
     * @param aCas
     *            the CAS itself.
     */
    public void add(SourceDocument aDocument, String aUser, CasAccessMode aMode, CAS aCas)
    {
        Map<String, SessionManagedCas> casByUser = managedCases
                .computeIfAbsent(aDocument.getId(), key -> new LinkedHashMap<>());

        SessionManagedCas managedCas = new SessionManagedCas(aDocument.getId(), aUser, aMode, aCas);
        casByUser.put(aUser, managedCas);
        
        LOGGER.trace("Added CAS to storage session [{}]: ", hashCode(), managedCas);
    }
}
