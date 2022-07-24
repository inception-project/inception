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
package de.tudarmstadt.ukp.inception.annotation.storage;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasSessionException;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.WriteAccessNotPermittedException;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CasStorageSession
    implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory
            .getLogger(MethodHandles.lookup().lookupClass());

    private static final long SPECIAL_PURPOSE = -1;

    private static final ThreadLocal<CasStorageSession> activeSession = ThreadLocal
            .withInitial(() -> null);

    private CasStorageSession previousSession;
    private boolean isolated = false;
    private boolean closed = false;
    private StackTraceElement[] creatorStack;

    private final Map<Long, Map<String, SessionManagedCas>> managedCases = new LinkedHashMap<>();

    private int maxManagedCases = 0;

    /**
     * Open a new session. If a session already exists, this method throws an exception.
     * 
     * @return a new session.
     * 
     * @throws CasSessionException
     *             if a session already exists for the current thread.
     */
    public static CasStorageSession open() throws CasSessionException
    {
        if (activeSession.get() != null) {
            throw new CasSessionException("CAS storage session for thread ["
                    + Thread.currentThread().getName() + "] already initialized");
        }

        CasStorageSession session = new CasStorageSession();
        activeSession.set(session);

        session.creatorStack = new Exception().getStackTrace();

        LOGGER.trace("CAS storage session [{}]: opened root", session.hashCode());

        return session;
    }

    /**
     * Open a new nested non-isolated session. If a session already exists, the new session will
     * replace the previous session. When the new session is closed, the previous session is
     * restored.
     * <p>
     * This method is separate from {@link #open()} to make it easier to detect when sessions are
     * accidentally created. {@link #openNested()} is meant to be used for short throw-away sessions
     * which can e.g. be used to permit upgrading a CAS.
     * 
     * @return a new session.
     */
    public static CasStorageSession openNested()
    {
        return openNested(false);
    }

    /**
     * Open a new session. If a session already exists, the new session will replace the previous
     * session. When the new session is closed, the previous session is restored.
     * <p>
     * This method is separate from {@link #open()} to make it easier to detect when sessions are
     * accidentally created. {@link #openNested()} is meant to be used for short throw-away sessions
     * which can e.g. be used to permit upgrading a CAS.
     * 
     * @param aIsolated
     *            whether methods such as {@link #getManagedState(CAS)} are allowed to access the
     *            previous (parent) session or not.
     * @return a new session.
     */
    public static CasStorageSession openNested(boolean aIsolated)
    {
        CasStorageSession session = new CasStorageSession();
        session.previousSession = activeSession.get();
        session.isolated = aIsolated;
        activeSession.set(session);

        session.creatorStack = new Exception().getStackTrace();

        LOGGER.trace("CAS storage session [{}]: opened nested (isolated: {}, previous: {})",
                session.hashCode(), aIsolated,
                session.previousSession != null ? session.previousSession.hashCode() : "none");

        return session;
    }

    /**
     * @return the current session. Returns {@code null} if there is no current session.
     * @throws CasSessionException
     *             if no session is available.
     */
    public static CasStorageSession get() throws CasSessionException
    {
        CasStorageSession session = activeSession.get();

        if (session == null) {
            throw new CasSessionException("No CAS storage session available");
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
    public void close() throws CasSessionException
    {
        if (activeSession.get() != this) {
            throw new CasSessionException("CAS storage session on thread ["
                    + Thread.currentThread().getName() + "] is not the current session");
        }

        closed = true;

        // For nested sessions, the previous session is set. For root sessions (non-nested), the
        // previous session will be null, this thus clearing the active session.
        activeSession.set(previousSession);

        LOGGER.trace("CAS storage session [{}]: closing...", hashCode());

        managedCases.values().forEach(casByUser -> casByUser.values().forEach(managedCas -> {
            if (managedCas.isReleaseOnClose()) {
                LOGGER.trace("CAS storage session [{}]: releasing {}", hashCode(), managedCas);
                managedCas.getCas().release();
            }
        }));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("CAS storage session [{}]: closed (max. CASes during lifetime: {})",
                    hashCode(), maxManagedCases);
        }
        else if (maxManagedCases > 0) {
            LOGGER.debug("CAS storage session [{}]: closed (max. CASes during lifetime: {})",
                    hashCode(), maxManagedCases);
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
     * Register the given CAS for a special purpose into the session.
     * 
     * @param aSpecialPurpose
     *            the unique purpose identifier - unique with respect to the current session.
     * @param aMode
     *            the access mode.
     * @param aCas
     *            the CAS itself.
     * @return the managed CAS state.
     */
    public SessionManagedCas add(String aSpecialPurpose, CasAccessMode aMode, CAS aCas)
    {
        Validate.notNull(aSpecialPurpose, "The purpose cannot be null");
        Validate.notNull(aMode, "The access mode cannot be null");
        Validate.notNull(aCas, "The CAS cannot be null");

        SessionManagedCas managedCas = new SessionManagedCas(SPECIAL_PURPOSE, aSpecialPurpose,
                aMode, aCas);

        Map<String, SessionManagedCas> casByUser = managedCases.computeIfAbsent(SPECIAL_PURPOSE,
                key -> new LinkedHashMap<>());
        casByUser.put(aSpecialPurpose, managedCas);

        maxManagedCases = Math.max(maxManagedCases, managedCases.size());

        LOGGER.trace("CAS storage session [{}]: added {}", hashCode(), managedCas);

        return managedCas;
    }

    public void remove(CAS aCas)
    {
        if (aCas == null) {
            return;
        }

        managedCases.values().stream().forEach(
                casByUser -> casByUser.values().removeIf(metadata -> metadata.getCas() == aCas));
    }

    /**
     * Removed managed CAS from session for given document and username
     */
    public void remove(Long aDocumentId, String aUsername)
    {
        Map<String, SessionManagedCas> casByUser = managedCases.get(aDocumentId);

        if (casByUser == null) {
            return;
        }

        casByUser.remove(aUsername);
    }

    /**
     * Register the given CAS into the session.
     * 
     * @param aDocumentId
     *            the document ID for which the CAS was retrieved.
     * @param aUser
     *            the user owning the CAS.
     * @param aMode
     *            the access mode.
     * @param aCas
     *            the CAS itself.
     * @return the managed CAS state.
     */
    public SessionManagedCas add(Long aDocumentId, String aUser, CasAccessMode aMode, CAS aCas)
    {
        Validate.notNull(aDocumentId, "The document ID cannot be null");
        Validate.isTrue(aDocumentId >= 0, "The document ID cannot be negative");
        Validate.notNull(aUser, "The username cannot be null");
        Validate.notNull(aMode, "The access mode cannot be null");
        Validate.notNull(aCas, "The CAS cannot be null");

        SessionManagedCas managedCas = new SessionManagedCas(aDocumentId, aUser, aMode, aCas);

        add(managedCas);

        return managedCas;
    }

    /**
     * Register the given CAS into the session.
     * 
     * @param aDocumentId
     *            the document ID for which the CAS was retrieved.
     * @param aUser
     *            the user owning the CAS.
     * @param aMode
     *            the access mode.
     * @param aCasHolder
     *            the CAS holder.
     * @return the managed CAS state.
     */
    public SessionManagedCas add(Long aDocumentId, String aUser, CasAccessMode aMode,
            CasHolder aCasHolder)
    {
        Validate.notNull(aDocumentId, "The document ID cannot be null");
        Validate.isTrue(aDocumentId >= 0, "The document ID cannot be negative");
        Validate.notNull(aUser, "The username cannot be null");
        Validate.notNull(aMode, "The access mode cannot be null");
        Validate.notNull(aCasHolder, "The CAS holder cannot be null");

        SessionManagedCas managedCas = new SessionManagedCas(aDocumentId, aUser, aMode, aCasHolder);

        add(managedCas);

        return managedCas;
    }

    private void add(SessionManagedCas aMCas)
    {
        Map<String, SessionManagedCas> casByUser = managedCases
                .computeIfAbsent(aMCas.getSourceDocumentId(), key -> new LinkedHashMap<>());
        SessionManagedCas oldMCas = casByUser.put(aMCas.getUserId(), aMCas);

        if (oldMCas == null) {
            LOGGER.trace("CAS storage session [{}]: added {}", hashCode(), aMCas);
        }
        else {
            LOGGER.trace("CAS storage session [{}]: replaced {} with {}", hashCode(), oldMCas,
                    aMCas);
        }
    }

    public boolean contains(CAS aCas)
    {
        return getManagedState(aCas).isPresent();
    }

    public List<StackTraceElement> getCreatorStack()
    {
        return unmodifiableList(asList(creatorStack));
    }

    /**
     * Returns the managed state of the CAS for the given CAS (if any).
     * 
     * @param aCas
     *            a CAS.
     * @return the managed CAS state.
     */
    public Optional<SessionManagedCas> getManagedState(CAS aCas)
    {
        Validate.notNull(aCas, "The CAS cannot be null");

        Optional<SessionManagedCas> result = managedCases.values().stream()
                .flatMap(casByUser -> casByUser.values().stream()
                        .filter(metadata -> metadata.getCas() == aCas))
                .findFirst();

        if (!result.isPresent() && !isolated && previousSession != null) {
            return previousSession.getManagedState(aCas);
        }

        return result;
    }

    /**
     * Returns the managed state of the CAS for the given document/user combination (if any).
     * 
     * @param aDocumentId
     *            document ID.
     * @param aUsername
     *            user name.
     * @return the managed CAS state.
     */
    public Optional<SessionManagedCas> getManagedState(Long aDocumentId, String aUsername)
    {
        Validate.notNull(aDocumentId, "The document ID cannot be null");
        Validate.notNull(aUsername, "The username cannot be null");

        Optional<SessionManagedCas> result = Optional.ofNullable(managedCases.get(aDocumentId))
                .map(casByUser -> casByUser.get(aUsername));

        if (!result.isPresent() && !isolated && previousSession != null) {
            return previousSession.getManagedState(aDocumentId, aUsername);
        }

        return result;
    }

    /**
     * Checks if writing the CAS is permitted. This is the case if the CAS is in the session and if
     * it has the {@link CasAccessMode#EXCLUSIVE_WRITE_ACCESS}. Any CAS used in the system must have
     * been obtained through the {@link CasStorageService} and must be in a session.
     * 
     * @param aDocument
     *            document.
     * @param aUsername
     *            user name.
     */
    public boolean hasExclusiveAccess(SourceDocument aDocument, String aUsername)
    {
        return getManagedState(aDocument.getId(), aUsername)
                .map(SessionManagedCas::isWritingPermitted).orElse(false);
    }

    /**
     * Checks if writing the CAS is permitted. This is the case if the CAS is in the session and if
     * it has the {@link CasAccessMode#EXCLUSIVE_WRITE_ACCESS}. Any CAS used in the system must have
     * been obtained through the {@link CasStorageService} and must be in a session.
     * 
     * @param aCas
     *            a CAS.
     */
    public boolean isWritingPermitted(CAS aCas)
    {
        return getManagedState(aCas).map(SessionManagedCas::isWritingPermitted).orElse(false);
    }

    /**
     * Checks if writing the CAS is permitted. If writing is not permitted, a
     * {@link WriteAccessNotPermittedException} is thrown.
     * 
     * @param aCas
     *            a CAS.
     * @throws WriteAccessNotPermittedException
     *             if writing is not permitted.
     */
    public void assertWritingPermitted(CAS aCas) throws WriteAccessNotPermittedException
    {
        Optional<SessionManagedCas> mCas = getManagedState(aCas);
        if (!mCas.isPresent()) {
            // CasMetadataUtils.getSourceDocumentName(aCas)
            String docId = WebAnnoCasUtil.getDocumentId(aCas);
            String docTitle = WebAnnoCasUtil.getDocumentTitle(aCas);
            throw new WriteAccessNotPermittedException(
                    "CAS [" + docTitle + "](" + docId + ") not found in current sesssion");
        }

        if (!mCas.map(SessionManagedCas::isWritingPermitted).orElse(false)) {
            throw new WriteAccessNotPermittedException("Write access to CAS for user ["
                    + mCas.get().getUserId() + "] for document [" + mCas.get().getSourceDocumentId()
                    + "] is not permitted in the current sesssion");
        }
    }
}
