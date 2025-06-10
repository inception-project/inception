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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_NON_INITIALIZING_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.NO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.annotation.storage.CasStorageServiceImpl.RepairAndUpgradeFlags.ISOLATED_SESSION;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.withProjectLogger;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getRealCas;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.transferCasOwnershipToCurrentThread;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.newSetFromMap;
import static java.util.Collections.synchronizedSet;
import static org.apache.commons.lang3.ArrayUtils.contains;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.Validate;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultEvictionPolicy;
import org.apache.commons.pool2.impl.EvictionConfig;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.ConcurrentReferenceHashMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageServiceAction;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageServiceLoader;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.ConcurentCasModificationException;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasHolder;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasKey;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasSessionException;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.SessionManagedCas;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorException;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageCacheProperties;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.storage.driver.CasStorageDriver;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CasStorageServiceAutoConfiguration#casStorageService}.
 * </p>
 */
public class CasStorageServiceImpl
    implements CasStorageService
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CasDoctor casDoctor;
    private final AnnotationSchemaService schemaService;
    private final CasStorageCacheProperties casStorageProperties;

    private final int snapshotInterval = 1000;
    private final int warningThreshold = 50;
    private final AtomicLong lastExclusiveAccessPoolSnapshotUpdate = new AtomicLong();
    private final AtomicInteger lastExclusiveAccessPoolSnapshotSize = new AtomicInteger();

    private final GenericKeyedObjectPool<CasKey, CasHolder> exclusiveAccessPool;
    private final Set<CasHolder> exclusiveAccessHolders = synchronizedSet(
            newSetFromMap(new WeakHashMap<>()));
    private final Cache<CasKey, CasHolder> sharedAccessCache;

    private final CasStorageDriver driver;

    public static enum RepairAndUpgradeFlags
    {
        /**
         * Open an isolated mini-session here to permit repairing and upgrading without affecting
         * the actual session.
         */
        ISOLATED_SESSION;
    }

    /**
     * @param aDriver
     *            a storage driver implementation
     * @param aCasStorageProperties
     *            storage configuration properties
     * @param aCasDoctor
     *            (optional) if present, CAS validation can take place
     * @param aSchemaService
     *            (optional) if present, CAS upgrades can be performed
     */
    @Autowired
    public CasStorageServiceImpl(CasStorageDriver aDriver,
            CasStorageCacheProperties aCasStorageProperties,
            @Autowired(required = false) CasDoctor aCasDoctor,
            @Autowired(required = false) AnnotationSchemaService aSchemaService)
    {
        driver = aDriver;
        casDoctor = aCasDoctor;
        schemaService = aSchemaService;
        casStorageProperties = aCasStorageProperties;

        var config = new GenericKeyedObjectPoolConfig<CasHolder>();
        config.setEvictionPolicy(new LoggingDefaultEvictionPolicy());
        // Since we want the pool to control exclusive access to a particular CAS, we only ever
        // must have one instance per key (the key uniquely identifies the CAS)
        config.setMaxTotalPerKey(1);
        config.setMaxIdlePerKey(1);
        // Setting this to 0 because we do not want any CAS to stick around in memory indefinitely
        config.setMinIdlePerKey(0);
        // Run an evictor thread periodically
        config.setTimeBetweenEvictionRuns(casStorageProperties.getIdleCasEvictionDelay());
        // Allow the evictor to drop idle CASes from pool after a short time. This should avoid that
        // CASes that are used regularly are dropped from the pool too quickly.
        config.setMinEvictableIdleDuration(casStorageProperties.getMinIdleCasTime());
        // Allow the evictor to drop all idle CASes on every eviction run
        config.setNumTestsPerEvictionRun(-1);
        // Allow viewing the pool in JMX
        config.setJmxEnabled(true);
        config.setJmxNameBase(getClass().getPackage().getName() + ":type="
                + getClass().getSimpleName() + ",name=");
        config.setJmxNamePrefix("exclusiveCasAccessPool");
        // Check if the CAS needs to be replaced every time we do a significant action
        config.setTestOnReturn(true);
        config.setTestOnBorrow(true);
        config.setTestWhileIdle(true);
        // Max. time we wait for a CAS to become available before giving up with an error
        config.setMaxWait(casStorageProperties.getCasBorrowWaitTimeout());
        // We do not have to set maxTotal because the default is already to have no limit (-1)
        exclusiveAccessPool = new GenericKeyedObjectPool<>(new PooledCasHolderFactory(), config);

        sharedAccessCache = Caffeine.newBuilder() //
                .scheduler(Scheduler.systemScheduler()) //
                .expireAfterAccess(casStorageProperties.getIdleCasEvictionDelay()) //
                .maximumSize(casStorageProperties.getSharedCasCacheSize()) //
                .recordStats() //
                .evictionListener((key, value, cause) -> {
                    LOG.debug("Marked CAS for eviction from shared-access pool: {} [{}]", value,
                            cause);
                }) //
                .build();

        if (casDoctor == null) {
            LOG.info("CAS doctor not available - unable to check/repair CASes");
        }

        BaseLoggers.BOOT_LOG.info("CAS cache size: {} instances",
                casStorageProperties.getSharedCasCacheSize());
    }

    public long getSharedAccessCacheSize()
    {
        return sharedAccessCache.estimatedSize();
    }

    public CacheStats getSharedAccessCacheStats()
    {
        return sharedAccessCache.stats();
    }

    @Override
    public void writeCas(SourceDocument aDocument, CAS aCas, String aUserName)
        throws IOException, CasSessionException
    {
        try (var logCtx = withProjectLogger(aDocument.getProject())) {
            var session = CasStorageSession.get();

            // If the CAS is in the session, then it must be there in a mode where writing is
            // permitted
            // ... we do this for the moment so we keep a door open for "detecting" if somebody may
            // have broken their promise to not make any modifications to a CAS that was loaded in
            // read-only mode.
            // ... however, if the CAS has been obtained bypassing the session and caches, then no
            // such
            // promise has been made. So we can then try to get exclusive access and save it.
            if (session.contains(aCas)) {
                if (!session.isWritingPermitted(aCas)) {
                    throw new IOException("Session does not permit the CAS for user [" + aUserName
                            + "] on document [" + aDocument.getName() + "](" + aDocument.getId()
                            + ") in project [" + aDocument.getProject().getName() + "]("
                            + aDocument.getProject().getId() + ") to be written");
                }

                // When overriding a stored CAS using an different CAS, the new CAS must be
                // unmanaged or must have been added to the session using a "special purpose". This
                // is to avoid having one CAS being accessible view two different username/docId
                // pairs.
                Optional<SessionManagedCas> mCas = session.getManagedState(aDocument.getId(),
                        aUserName);
                if (mCas.isPresent() && mCas.get().getCas() != aCas) {
                    throw new IOException("Cannot override managed CAS [" + aUserName
                            + "] on document [" + aDocument.getName() + "](" + aDocument.getId()
                            + ") in project [" + aDocument.getProject().getName() + "]("
                            + aDocument.getProject().getId()
                            + ") with another managed CAS for user [" + mCas.get().getUserId()
                            + "] on document [" + mCas.get().getSourceDocumentId() + "]");
                }

                realWriteCas(aDocument, aUserName, aCas);
            }
            else {
                try (var access = new WithExclusiveAccess(aDocument, aUserName)) {
                    realWriteCas(aDocument, aUserName, aCas);

                    // If the CAS which was written does not match the CAS in the session for the
                    // given document/user, then we replace the CAS in the session with the new CAS.
                    // This happens for example when a document is reset. In this case, the CAS in
                    // storage is overwritten with an unmanaged copy of the initial CAS which then
                    // becomes the new CAS for the given document/user.
                    // It is possible that the CAS is not set in the exclusive access, in that case
                    // we use the exclusive access just to reserve access to the username/docID
                    // pair. This could e.g. happen when saving an unmanaged CAS under a new
                    // username/docId pair.
                    if (access.isCasSet() && access.getCas() != aCas) {
                        access.setCas(aCas);
                    }
                }
                catch (IOException e) {
                    throw e;
                }
                catch (Exception e) {
                    throw new IOException(e);
                }
            }

            // Drop the CAS from the shared CAS it gets re-loaded on the next access - no effect if
            // the CAS is not present in the shared cache
            sharedAccessCache.invalidate(new CasKey(aDocument, aUserName));

            session.getManagedState(aCas).ifPresent(SessionManagedCas::incrementWriteCount);
        }
    }

    @Override
    public CAS readCas(SourceDocument aDocument, String aUsername)
        throws IOException, CasSessionException
    {
        return readOrCreateCas(aDocument, aUsername, NO_CAS_UPGRADE, null, EXCLUSIVE_WRITE_ACCESS);
    }

    @Override
    public CAS readCas(SourceDocument aDocument, String aUsername, CasAccessMode aAccessMode)
        throws IOException, CasSessionException
    {
        return readOrCreateCas(aDocument, aUsername,
                SHARED_READ_ONLY_ACCESS.equals(aAccessMode) ? AUTO_CAS_UPGRADE : NO_CAS_UPGRADE,
                null, aAccessMode);
    }

    @Override
    public CAS readOrCreateCas(SourceDocument aDocument, String aUsername,
            CasUpgradeMode aUpgradeMode, CasProvider aSupplier, CasAccessMode aAccessMode)
        throws IOException, CasSessionException
    {

        try (var logCtx = withProjectLogger(aDocument.getProject())) {
            CasStorageSession session = CasStorageSession.get();

            LOG.debug(
                    "CAS storage session [{}]: reading annotations for [{}]@{} in {} with {} using {}",
                    session.hashCode(), aUsername, aDocument, aDocument.getProject(), aAccessMode,
                    aUpgradeMode);

            // If the CAS is already present in the current session and the access mode is
            // compatible with the requested access mode, then we can return it immediately
            // THOUGHT: As it is written now - if the access mode already recorded in the session
            // is insufficient, the access mode is upgraded because we simply continue after this
            // IF-clause. I am not entirely sure this is valid.
            // Case 1) CAS was added during the current session - the holder in the session is
            // replaced with an exclusive access CAS and when the session is closed, it is released.
            // Case 2) CAS was added during a parent session - the new exclusive access holder is
            // added to the current session and released as the current session is closed. The
            // parent session then still has the previously obtained read-only CAS - which at this
            // point might be stale if the CAS was changed during the exclusive access period
            Optional<SessionManagedCas> mCas = session.getManagedState(aDocument.getId(),
                    aUsername);
            if (mCas.isPresent() && mCas.get().getMode().alsoPermits(aAccessMode)) {
                LOG.debug(
                        "CAS storage session [{}]: session already contains CAS [{}] for [{}]@{} with mode {}",
                        session.hashCode(), mCas.get().getCas().hashCode(), aUsername, aDocument,
                        mCas.get().getMode());
                return mCas.get().getCas();
            }

            // If the CAS is not yet in the session, then we must get hold of it somehow...
            CasHolder casHolder;

            // If exclusive access is requested, then we check the CAS out of the exclusive access
            // pool
            if (EXCLUSIVE_WRITE_ACCESS.equals(aAccessMode)) {
                CasKey key = null;
                CasHolder holder = null;
                try {
                    LOG.trace("CAS storage session [{}]: trying to borrow CAS [{}]@{}",
                            session.hashCode(), aUsername, aDocument);

                    key = new CasKey(aDocument, aUsername);
                    holder = borrowCas(key);

                    // If the CAS has not been loaded into the exclusive access pool, then we need
                    // to load it
                    if (!holder.isCasSet()) {
                        CasKey finalKey = key;
                        CasHolder finalHolder = holder;

                        CAS cas;
                        // Make sure the system knows that the session has legitimate access to the
                        // CAS being loaded so that it won't lock itself up trying to acquire the
                        // exclusive lock in CAS in readOrCreateUnmanagedCas
                        try (CasStorageSession loaderSession = CasStorageSession.openNested(true)) {
                            SessionManagedCas mLoaderCas = loaderSession.add(aDocument.getId(),
                                    aUsername, EXCLUSIVE_WRITE_ACCESS, holder);
                            // Do not try to release the CAS when the loader session closes because
                            // in fact we won't even have set the CAS in the holder by then
                            mLoaderCas.setReleaseOnClose(false);

                            cas = readOrCreateUnmanagedCas(aDocument, aUsername, aSupplier,
                                    aUpgradeMode, aAccessMode);
                        }

                        holder.setCas(cas);

                        // Hook up releasing of the CAS when CAS.release() is called via the
                        // CasStorageSession
                        ((CASImpl) getRealCas(cas))
                                .setOwner(_cas -> returnBorrowedCas(_cas, finalKey, finalHolder));

                        LOG.debug(
                                "CAS storage session [{}]: borrowed CAS [{}] for [{}]@{} loaded from storage",
                                session.hashCode(), holder.getCasHashCode(), aUsername, aDocument);
                    }
                    else {
                        LOG.debug(
                                "CAS storage session [{}]: borrowed CAS [{}] for [{}]@{} was already in memory",
                                session.hashCode(), holder.getCasHashCode(), aUsername, aDocument);

                        transferCasOwnershipToCurrentThread(holder.getCas());

                        repairAndUpgradeCasIfRequired(aDocument, aUsername, holder.getCas(),
                                aUpgradeMode, ISOLATED_SESSION);
                    }

                    casHolder = holder;
                }
                catch (Exception e) {
                    // If there was an exception, we need to return the CAS to the pool
                    if (key != null && holder != null) {
                        LOG.trace(
                                "CAS storage session [{}]: returning borrowed CAS [{}] for [{}]@{} after failure to load CAS",
                                session.hashCode(), holder.getCasHashCode(), aUsername, aDocument);
                        try {
                            exclusiveAccessPool.returnObject(key, holder);
                            logExclusiveAccessHolders();
                        }
                        catch (Exception e1) {
                            LOG.error("Unable to return CAS to exclusive access pool", e1);
                        }
                    }
                    casHolder = new CasHolder(key, e);
                }
            }
            // else if shared read access is requested, then we try fetching it from the shared
            // cache
            else if (SHARED_READ_ONLY_ACCESS.equals(aAccessMode)) {
                if (!AUTO_CAS_UPGRADE.equals(aUpgradeMode)) {
                    throw new IllegalArgumentException(
                            "When requesting a shared read-only CAS, the " + "access mode must be "
                                    + AUTO_CAS_UPGRADE);
                }

                // Ensure that the CAS is not being re-written and temporarily unavailable while we
                // check for its existence
                try (var access = new WithExclusiveAccess(aDocument, aUsername)) {
                    // Since we promise to only read the CAS, we don't have to worry about it being
                    // locked to a particular thread...
                    casHolder = sharedAccessCache
                            .get(new CasKey(aDocument, aUsername),
                                    (key) -> CasHolder.of(key,
                                            () -> getRealCas(readOrCreateUnmanagedCas(aDocument,
                                                    aUsername, aSupplier, aUpgradeMode,
                                                    aAccessMode))));
                    var size = getSharedAccessCacheSize();
                    var max = casStorageProperties.getSharedCasCacheSize();
                    if (size > (max * 0.9)) {
                        LOG.warn("Shared access CAS cache is >= 90% full: {} / {}", size, max);
                    }
                }
            }
            // else if the special bypass mode is requested, then we fetch directly from disk
            else if (UNMANAGED_ACCESS.equals(aAccessMode)) {
                // Ensure that the CAS is not being re-written and temporarily unavailable while we
                // check for its existence
                try (var access = new WithExclusiveAccess(aDocument, aUsername)) {
                    casHolder = CasHolder.of(new CasKey(aDocument, aUsername),
                            () -> readOrCreateUnmanagedCas(aDocument, aUsername, aSupplier,
                                    aUpgradeMode, aAccessMode));
                }
            }
            // else if the special bypass mode is requested, then we fetch directly from disk
            else if (UNMANAGED_NON_INITIALIZING_ACCESS.equals(aAccessMode)) {
                // Ensure that the CAS is not being re-written and temporarily unavailable while we
                // check for its existence
                try (var access = new WithExclusiveAccess(aDocument, aUsername)) {
                    casHolder = CasHolder.of(new CasKey(aDocument, aUsername),
                            () -> driver.readCas(aDocument, aUsername));
                }
            }
            else {
                throw new IllegalArgumentException("Unknown CAS access mode [" + aAccessMode + "]");
            }

            // If there was a problem retrieving the CAS, then we throw an exception
            if (casHolder.getException() != null) {
                if (casHolder.getException() instanceof IOException) {
                    throw (IOException) casHolder.getException();
                }

                throw new IOException(casHolder.getException());
            }

            var cas = casHolder.getCas();

            if (aAccessMode.isSessionManaged()) {
                session.add(aDocument.getId(), aUsername, aAccessMode, cas).incrementReadCount();
            }

            return cas;
        }
    }

    private CasHolder borrowCas(CasKey aKey)
    {
        try {
            var holder = exclusiveAccessPool.borrowObject(aKey);
            // Add the holder to the set of known holder. Because this set it using weak
            // references, and because we use the set only to inform holders when they become
            // invalid we do never have to explicitly remove the holder from the set
            exclusiveAccessHolders.add(holder);
            LOG.trace("Added to exclusiveAccessHolders: {}", holder);

            if (currentTimeMillis()
                    - lastExclusiveAccessPoolSnapshotUpdate.get() > snapshotInterval) {
                lastExclusiveAccessPoolSnapshotUpdate.set(System.currentTimeMillis());
                var currentSize = exclusiveAccessPool.getNumActive()
                        + exclusiveAccessPool.getNumIdle();
                var lastSize = lastExclusiveAccessPoolSnapshotSize.getAndSet(currentSize);

                if (currentSize > (lastSize + warningThreshold)) {
                    LOG.warn(
                            "Exclusive CAS access pool sizes increased strongly in the last {}ms: {} -> {}",
                            snapshotInterval, lastSize, currentSize);
                }
            }

            logExclusiveAccessHolders();
            return holder;
        }
        catch (Exception e) {
            throw new CasSessionException("Unable to borrow CAS", e);
        }
    }

    /**
     * Returns a borrowed CAS to the exclusive access pool. This method is not called directly when
     * a CAS needs to be returned. Rather, it is registered as a "CAS owner" in CAS instances such
     * that it is called when {@link CAS#release()} is called.
     */
    private void returnBorrowedCas(AbstractCas cas, CasKey aKey, CasHolder aHolder)
    {
        try {
            LOG.trace("Returning borrowed CAS [{}] for [{}]@[{}]({})", cas.hashCode(),
                    aKey.getUserId(), aKey.getDocumentName(), aKey.getDocumentId());
            exclusiveAccessPool.returnObject(aKey, aHolder);
            logExclusiveAccessHolders();
        }
        catch (Exception e) {
            LOG.error("Unable to return CAS [{}] for [{}]@[{}]({}) to exclusive access pool",
                    cas.hashCode(), aKey.getUserId(), aKey.getDocumentName(), aKey.getDocumentId(),
                    e);
        }
    }

    private void repairAndUpgradeCasIfRequired(SourceDocument aDocument, String aUsername, CAS aCas,
            CasUpgradeMode aUpgradeMode, RepairAndUpgradeFlags... aFlags)
        throws IOException
    {
        try (var session = CasStorageSession.openNested(contains(aFlags, ISOLATED_SESSION))) {
            session.add(aDocument.getId(), aUsername, EXCLUSIVE_WRITE_ACCESS, aCas);

            try {
                analyzeAndRepair(aDocument, aUsername, aCas);

                if (schemaService != null) {
                    try {
                        schemaService.upgradeCas(aCas, aDocument, aUsername, aUpgradeMode);
                    }
                    catch (UIMAException e) {
                        throw new IOException(e);
                    }
                }
            }
            finally {
                // We do not want the CAS to be released by this nested session
                session.remove(aCas);
            }
        }
    }

    /**
     * Fetches the CAS for the given user/document combination either from the storage or by using
     * the given {@link CasProvider} if it does not yet exist in the storage.
     * 
     * @param aDocument
     *            a document.
     * @param aUsername
     *            a user.
     * @param aSupplier
     *            a supplier to be used if the CAS does not yet exist in the storage.
     * @param aUpgradeMode
     *            whether to upgrade the CAS.
     * @return the CAS.
     * @throws IOException
     *             if the CAS could not be obtained.
     */
    private CAS readOrCreateUnmanagedCas(SourceDocument aDocument, String aUsername,
            CasProvider aSupplier, CasUpgradeMode aUpgradeMode, CasAccessMode aAccessMode)
        throws IOException
    {
        var start = currentTimeMillis();

        CAS cas;
        String source;

        if (LOG.isTraceEnabled()) {
            if (CasStorageSession.exists()) {
                var session = CasStorageSession.get();
                LOG.trace("CAS storage session [{}]: loading CAS [{}]@{} [{}]", session.hashCode(),
                        aUsername, aDocument, aUpgradeMode);
            }
            else {
                LOG.trace("Loading CAS [{}]@{} [{}]", aUsername, aDocument, aUpgradeMode);
            }
        }

        // If the CAS exists on disk already, load it from there
        if (driver.existsCas(aDocument, aUsername)) {
            source = "disk";
            cas = driver.readCas(aDocument, aUsername);
            repairAndUpgradeCasIfRequired(aDocument, aUsername, cas, aUpgradeMode,
                    ISOLATED_SESSION);

            addOrUpdateCasMetadata(aDocument, aUsername, cas);
        }
        // If the CAS does NOT exist on disk, try obtaining it through the given CAS provider
        else if (aSupplier != null) {
            source = "importer";
            cas = aSupplier.get();
            repairAndUpgradeCasIfRequired(aDocument, aUsername, cas, aUpgradeMode);

            if (aAccessMode == EXCLUSIVE_WRITE_ACCESS) {
                realWriteCas(aDocument, aUsername, cas);

                addOrUpdateCasMetadata(aDocument, aUsername, cas);
            }
        }
        // If no CAS provider is given, fail
        else {
            throw new FileNotFoundException("CAS file for [" + aDocument.getId() + "," + aUsername
                    + "] does not exist and no initializer is specified.");
        }

        var duration = currentTimeMillis() - start;

        if (LOG.isDebugEnabled()) {
            if (CasStorageSession.exists()) {
                var session = CasStorageSession.get();
                LOG.debug(
                        "CAS storage session [{}]: loaded CAS [{}] for [{}]@{} from {} in {}ms [{}]",
                        session.hashCode(), cas.hashCode(), aUsername, aDocument, source, duration,
                        aUpgradeMode);
            }
            else {
                LOG.debug("Loaded CAS [{}] for [{}]@{} from {} in {}ms [{}]", cas.hashCode(),
                        aUsername, aDocument, source, duration, aUpgradeMode);
            }
        }

        return cas;
    }

    private void addOrUpdateCasMetadata(SourceDocument aDocument, String aUsername, CAS cas)
        throws IOException
    {
        CasMetadataUtils.addOrUpdateCasMetadata(cas, driver.getCasMetadata(aDocument, aUsername)
                .orElseThrow(() -> new IOException(
                        "Unable to obtain last modified data for annotation document [" + aDocument
                                + "] of user [" + aUsername + "]"))
                .getTimestamp(), aDocument, aUsername);
    }

    @Override
    public boolean deleteCas(SourceDocument aDocument, String aUsername)
        throws IOException, CasSessionException
    {
        try (var logCtx = withProjectLogger(aDocument.getProject());
                var access = new WithExclusiveAccess(aDocument, aUsername)) {
            var fileWasDeleted = driver.deleteCas(aDocument, aUsername);

            // Drop the CAS from the shared CAS it doesn't ghost around. Also set the deleted flag
            // in the holder in case anybody might still be holding on to the holder and needs to
            // know that CAS was deleted.
            var key = new CasKey(aDocument, aUsername);
            var sharedCasHolder = sharedAccessCache.getIfPresent(key);
            if (sharedCasHolder != null) {
                sharedCasHolder.setDeleted(true);
            }
            sharedAccessCache.invalidate(key);

            // Drop the CAS from the exclusive access pool. This is done my marking it as deleted
            // and then releasing it (returning it to the pool). Upon return, the deleted flag
            // causes the CAS to be invalidated and dropped from the pool.
            exclusiveAccessHolders.forEach(h -> {
                // Must use the forEach here because stream() is not synchronized!
                if (Objects.equals(h.getKey(), key)) {
                    h.setDeleted(true);
                }
            });
            access.release();

            // Drop the CAS from the current session
            // This must happen after the call to access.release() because access.release() tries
            // to fetch the CAS from the session if WithExclusiveAccess did not borrow it itself
            CasStorageSession.get().remove(aDocument.getId(), aUsername);

            return fileWasDeleted;
        }
    }

    @Override
    public void analyzeAndRepair(SourceDocument aDocument, String aDataOwner, CAS aCas)
    {
        var project = aDocument.getProject();

        try (var logCtx = withProjectLogger(project)) {
            if (casDoctor == null) {
                return;
            }

            // Check if repairs are active - if this is the case, we only need to run the repairs
            // because the repairs do an analysis as a pre- and post-condition.
            if (casDoctor.isRepairsActive()) {
                try {
                    casDoctor.repair(aDocument, aDataOwner, aCas);
                }
                catch (Exception e) {
                    throw new DataRetrievalFailureException(
                            "Error repairing CAS of user [" + aDataOwner + "] for document "
                                    + aDocument + " in project " + aDocument.getProject(),
                            e);
                }
            }
            // If the repairs are not active, then we run the analysis explicitly
            else {
                analyze(aDocument, aDataOwner, aCas);
            }
        }
    }

    /**
     * Runs {@link CasDoctor} in analysis mode on the given CAS.
     * <p>
     * <b>Note:</b> {@link CasDoctor} is an optional service. If no {@link CasDoctor} implementation
     * is available, this method returns without doing anything.
     * 
     * @param aDocument
     *            the document
     * @param aDataOwner
     *            the user owning the CAS (used for logging)
     * @param aCas
     *            the CAS object
     */
    private void analyze(SourceDocument aDocument, String aDataOwner, CAS aCas)
    {
        if (casDoctor == null) {
            return;
        }

        var project = aDocument.getProject();

        try {
            casDoctor.analyze(aDocument, aDataOwner, aCas);
        }
        catch (CasDoctorException e) {
            var detailMsg = new StringBuilder();
            detailMsg.append("CAS Doctor found problems for user [").append(aDataOwner)
                    .append("] in document ").append(aDocument).append(" in project ")
                    .append(project).append("\n");
            e.getDetails().forEach(
                    m -> detailMsg.append(String.format("- [%s] %s%n", m.level, m.message)));

            throw new DataRetrievalFailureException(detailMsg.toString());
        }
        catch (Exception e) {
            throw new DataRetrievalFailureException("Error analyzing CAS of user [" + aDataOwner
                    + "] in document " + aDocument + " in project " + project, e);
        }
    }

    @Override
    public void exportCas(SourceDocument aDocument, String aDataOwner, OutputStream aStream)
        throws IOException
    {
        // Ensure that the CAS is not being re-written and temporarily unavailable while we export
        // it, then add this info to a mini-session to ensure that write-access is known
        try (var session = CasStorageSession.openNested(true)) {
            try (var access = new WithExclusiveAccess(aDocument, aDataOwner)) {
                session.add(aDocument.getId(), aDataOwner, EXCLUSIVE_WRITE_ACCESS,
                        access.getHolder());

                driver.exportCas(aDocument, aDataOwner, aStream);
            }
            finally {
                session.remove(aDocument.getId(), aDataOwner);
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void importCas(SourceDocument aDocument, String aDataOwner, InputStream aStream)
        throws IOException
    {
        // Ensure that the CAS is not being re-written and temporarily unavailable while we export
        // it, then add this info to a mini-session to ensure that write-access is known
        try (var session = CasStorageSession.openNested(true)) {
            try (var access = new WithExclusiveAccess(aDocument, aDataOwner)) {
                session.add(aDocument.getId(), aDataOwner, EXCLUSIVE_WRITE_ACCESS,
                        access.getHolder());

                driver.importCas(aDocument, aDataOwner, aStream);
            }
            finally {
                session.remove(aDocument.getId(), aDataOwner);
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void upgradeCas(SourceDocument aDocument, String aDataOwner) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notBlank(aDataOwner, "Data owner must be specified");

        forceActionOnCas(aDocument, aDataOwner, //
                (doc, user) -> driver.readCas(doc, user),
                (doc, user, cas) -> schemaService.upgradeCas(cas, doc, user), //
                true);
    }

    @Override
    public void forceActionOnCas(SourceDocument aDocument, String aDataOwner,
            CasStorageServiceLoader aLoader, CasStorageServiceAction aAction, boolean aSave)
        throws IOException
    {
        // Ensure that the CAS is not being re-written and temporarily unavailable while we check
        // upgrade it, then add this info to a mini-session to ensure that write-access is known
        try (var session = CasStorageSession.openNested(true)) {
            try (var access = new WithExclusiveAccess(aDocument, aDataOwner)) {
                session.add(aDocument.getId(), aDataOwner, EXCLUSIVE_WRITE_ACCESS,
                        access.getHolder());

                var cas = aLoader.load(aDocument, aDataOwner);
                access.setCas(cas);

                aAction.apply(aDocument, aDataOwner, cas);

                if (aSave) {
                    realWriteCas(aDocument, aDataOwner, cas);
                }
            }
            finally {
                session.remove(aDocument.getId(), aDataOwner);
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean existsCas(SourceDocument aDocument, String aUser) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notBlank(aUser, "User must be specified");

        // Ensure that the CAS is not being re-written and temporarily unavailable while we check
        // for its existence
        try (var access = new WithExclusiveAccess(aDocument, aUser)) {
            return driver.existsCas(aDocument, aUser);
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public Optional<Long> getCasFileSize(SourceDocument aDocument, String aUser) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notBlank(aUser, "User must be specified");

        // Ensure that the CAS is not being re-written and temporarily unavailable while we check
        // for its existence
        try (var access = new WithExclusiveAccess(aDocument, aUser)) {
            return driver.getCasFileSize(aDocument, aUser);
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    private class WithExclusiveAccess
        implements AutoCloseable
    {
        private final CasKey key;
        private CasHolder holder;
        private String documentName;
        private long documentId;
        private String username;

        public WithExclusiveAccess(SourceDocument aDocument, String aUser)
            throws CasSessionException
        {
            key = new CasKey(aDocument, aUser);
            documentName = aDocument.getName();
            documentId = aDocument.getId();
            username = aUser;

            var session = CasStorageSession.get();

            if (!session.hasExclusiveAccess(aDocument, aUser)) {
                LOG.trace("CAS storage session [{}]: trying to briefly borrow CAS [{}]@[{}]({})",
                        session.hashCode(), aUser, aDocument.getName(), aDocument.getId());

                holder = borrowCas(key);

                LOG.trace("CAS storage session [{}]: briefly borrowed CAS [{}]@[{}]({})",
                        session.hashCode(), aUser, aDocument.getName(), aDocument.getId());

                if (holder.isCasSet()) {
                    transferCasOwnershipToCurrentThread(holder.getCas());
                }
            }
            else {
                holder = null;
            }
        }

        public CasKey getKey()
        {
            return key;
        }

        public boolean isCasSet()
        {
            if (holder != null) {
                return holder.isCasSet();
            }

            return CasStorageSession.get().getManagedState(documentId, username)
                    .orElseThrow(() -> new IllegalStateException("This should not happen. If "
                            + "the no holder is set, then the CAS must already be part of the "
                            + "session."))
                    .isCasSet();
        }

        public void setCas(CAS aCas)
        {
            if (holder != null) {
                // Unset the release hook for the old CAS
                if (holder.isCasSet()) {
                    ((CASImpl) getRealCas(getCas())).setOwner(null);
                }

                // Set the release hook for the new CAS
                ((CASImpl) getRealCas(aCas))
                        .setOwner(_cas -> returnBorrowedCas(_cas, getKey(), holder));

                holder.setCas(aCas);
            }
            else {
                CasStorageSession.get().getManagedState(documentId, username)
                        .orElseThrow(() -> new IllegalStateException("This should not happen. If "
                                + "the no holder is set, then the CAS must already be part of the "
                                + "session."))
                        .setCas(aCas);
            }
        }

        public CAS getCas()
        {
            return holder != null ? holder.getCas()
                    : CasStorageSession.get().getManagedState(documentId, username).orElseThrow(
                            () -> new IllegalStateException("This should not happen. If "
                                    + "the no holder is set, then the CAS must already be part of the "
                                    + "session."))
                            .getCas();
        }

        public CasHolder getHolder()
        {
            return holder;
        }

        /**
         * Releases the CAS prior to closing the exclusive access context. This is used if the CAS
         * must be released irrespective of whether it was borrowed by {@link WithExclusiveAccess}
         * or exclusive access already existed before. Such is the case specifically when the CAS is
         * deleted.
         * <p>
         * <b>NOTE:</b> If this is used, it should be the last action in the
         * {@link WithExclusiveAccess} context because as a side effect, it sets clears the internal
         * CAS holder.
         */
        public void release()
        {
            if (holder != null) {
                exclusiveAccessPool.returnObject(key, holder);
                holder = null;
                logExclusiveAccessHolders();
            }
            else {
                getCas().release();
            }
        }

        @Override
        public void close()
        {
            if (holder != null) {
                LOG.trace("Returning briefly borrowed CAS [{}]@[{}]({})", username, documentName,
                        documentId);
                exclusiveAccessPool.returnObject(key, holder);
                logExclusiveAccessHolders();
            }
        }
    }

    @Override
    public Optional<Long> getCasTimestamp(SourceDocument aDocument, String aUser) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notBlank(aUser, "User must be specified");

        // Ensure that the CAS is not being re-written and temporarily unavailable while we check
        // for its timestamp
        try (var access = new WithExclusiveAccess(aDocument, aUser)) {
            return driver.getCasMetadata(aDocument, aUser).map(CasStorageMetadata::getTimestamp);
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public Optional<Long> verifyCasTimestamp(SourceDocument aDocument, String aUser,
            long aExpectedTimeStamp, String aContextAction)
        throws IOException, ConcurentCasModificationException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notBlank(aUser, "User must be specified");

        // Ensure that the CAS is not being re-written and temporarily unavailable while we check
        // for its timestamp
        try (var access = new WithExclusiveAccess(aDocument, aUser)) {
            return driver.verifyCasTimestamp(aDocument, aUser, aExpectedTimeStamp, aContextAction);
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * When using the Spring {@link ConcurrentReferenceHashMap} for the
     * {@link #exclusiveAccessHolders}, we had some trouble that CASHolders disappeared from the set
     * even though they had not yet been garbage collected (i.e. still referenced from the
     * {@link #exclusiveAccessPool}. To fix this, we switch to a simple synchronized
     * {@link WeakHashMap} turned into a set. We keep the debug/logging code around for a little
     * more to facilitate debugging this again if need be.
     */
    private void logExclusiveAccessHolders()
    {
        if (LOG.isTraceEnabled()) {
            if (exclusiveAccessHolders.isEmpty()) {
                LOG.trace("exclusiveAccessHolders: empty!");
            }
            else {
                LOG.trace("exclusiveAccessHolders: {}", exclusiveAccessHolders);
            }
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void beforeLayerConfigurationChanged(LayerConfigurationChangedEvent aEvent)
    {
        // Tell the known CAS holders for the given project that their type system is outdated
        // so they can be refreshed when next returned or borrowed
        logExclusiveAccessHolders();

        exclusiveAccessHolders.forEach(h -> {
            // Must use the forEach here because stream() is not synchronized!
            if (Objects.equals(h.getKey().getProjectId(), aEvent.getProject().getId())) {
                h.setTypeSystemOutdated(true);
            }
        });

        logExclusiveAccessHolders();

        // Drop all cached CASes from the updated project from the cache so the CASes get loaded
        // with an updated type system on next access
        sharedAccessCache.asMap().keySet()
                .removeIf(key -> Objects.equals(key.getProjectId(), aEvent.getProject().getId()));
    }

    private void realWriteCas(SourceDocument aDocument, String aUserName, CAS aCas)
        throws IOException
    {
        analyze(aDocument, aUserName, aCas);

        if (CasStorageSession.exists()) {
            var session = CasStorageSession.get();
            LOG.debug("CAS storage session [{}]: writing annotations for [{}]@{} in {}",
                    session.hashCode(), aUserName, aDocument, aDocument.getProject());

        }
        else {
            LOG.debug("Writing annotations for [{}]@{} in {}", aUserName, aDocument,
                    aDocument.getProject());
        }

        driver.writeCas(aDocument, aUserName, aCas);
    }

    public static class LoggingDefaultEvictionPolicy
        extends DefaultEvictionPolicy<CasHolder>
    {
        @Override
        public boolean evict(EvictionConfig aConfig, PooledObject<CasHolder> aUnderTest,
                int aIdleCount)
        {
            var result = super.evict(aConfig, aUnderTest, aIdleCount);
            if (result) {
                LOG.debug("Marked CAS for eviction from exclusive-access pool: {}",
                        aUnderTest.getObject());
            }
            return result;
        }
    }
}
