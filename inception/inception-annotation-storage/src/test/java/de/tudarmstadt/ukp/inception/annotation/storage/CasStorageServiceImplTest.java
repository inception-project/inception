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
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.NO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils.getInternalTypeSystem;
import static de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession.openNested;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasSessionException;
import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageBackupProperties;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageCachePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStoragePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.driver.filesystem.FileSystemCasStorageDriver;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.schema.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.support.logging.Logging;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class CasStorageServiceImplTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private AtomicBoolean exception = new AtomicBoolean(false);
    private AtomicBoolean rwTasksCompleted = new AtomicBoolean(false);
    private AtomicInteger managedReadCounter = new AtomicInteger(0);
    private AtomicInteger unmanagedReadCounter = new AtomicInteger(0);
    private AtomicInteger unmanagedNonInitializingReadCounter = new AtomicInteger(0);
    private AtomicInteger deleteCounter = new AtomicInteger(0);
    private AtomicInteger deleteInitialCounter = new AtomicInteger(0);
    private AtomicInteger writeCounter = new AtomicInteger(0);

    private CasStorageServiceImpl sut;
    private FileSystemCasStorageDriver driver;
    private RepositoryProperties repositoryProperties;

    @TempDir
    File testFolder;

    @BeforeEach
    public void setup() throws Exception
    {
        exception.set(false);
        rwTasksCompleted.set(false);
        managedReadCounter.set(0);
        unmanagedReadCounter.set(0);
        unmanagedNonInitializingReadCounter.set(0);
        writeCounter.set(0);
        deleteCounter.set(0);
        deleteInitialCounter.set(0);

        repositoryProperties = new RepositoryPropertiesImpl();
        repositoryProperties.setPath(testFolder);

        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        driver = new FileSystemCasStorageDriver(repositoryProperties,
                new CasStorageBackupProperties(), new CasStoragePropertiesImpl());

        sut = new CasStorageServiceImpl(driver, new CasStorageCachePropertiesImpl(), null, null);
    }

    @Test
    public void testWriteReadExistsDeleteCas() throws Exception
    {
        try (CasStorageSession casStorageSession = openNested(true)) {
            // Setup fixture
            var doc = makeSourceDocument(1l, 1l, "test");
            var templateCas = WebAnnoCasUtil.createCas(createTypeSystemDescription()).getJCas();
            templateCas.setDocumentText("This is a test");
            casStorageSession.add("cas", EXCLUSIVE_WRITE_ACCESS, templateCas.getCas());
            var user = "test";

            sut.writeCas(doc, templateCas.getCas(), user);
            assertThat(sut.existsCas(doc, user)).isTrue();

            // Actual test
            var cas = sut.readCas(doc, user);
            assertThat(cas.getDocumentText()).isEqualTo(templateCas.getDocumentText());

            sut.deleteCas(doc, user);
            assertThat(sut.existsCas(doc, user)).isFalse();
            assertThat(casStorageSession.contains(cas)).isFalse();
        }
    }

    @Test
    public void testCasMetadataGetsCreated() throws Exception
    {
        try (CasStorageSession casStorageSession = openNested(true)) {
            List<TypeSystemDescription> typeSystems = new ArrayList<>();
            typeSystems.add(createTypeSystemDescription());
            typeSystems.add(CasMetadataUtils.getInternalTypeSystem());

            var cas = WebAnnoCasUtil.createCas(mergeTypeSystems(typeSystems)).getJCas();
            casStorageSession.add("cas", EXCLUSIVE_WRITE_ACCESS, cas.getCas());

            var doc = makeSourceDocument(2l, 2l, "test");
            var user = "test";

            sut.writeCas(doc, cas.getCas(), user);

            var cas2 = sut.readCas(doc, user).getJCas();

            var cmds = new ArrayList<>(select(cas2, CASMetadata.class));
            assertThat(cmds).hasSize(1);
            assertThat(cmds.get(0).getProjectId()).isEqualTo(doc.getProject().getId());
            assertThat(cmds.get(0).getSourceDocumentId()).isEqualTo(doc.getId());
            assertThat(cmds.get(0).getLastChangedOnDisk())
                    .isEqualTo(sut.getCasTimestamp(doc, user).get());
        }
    }

    @Test
    public void testReadOrCreateCas() throws Exception
    {
        try (var casStorageSession = openNested(true)) {
            // Setup fixture
            var doc = makeSourceDocument(3l, 3l, "test");
            String user = "test";
            String text = "This is a test";
            createCasFile(doc, user, text);

            // Actual test
            var cas = sut.readCas(doc, user).getJCas();
            assertThat(cas.getDocumentText()).isEqualTo(text);

            sut.deleteCas(doc, user);
            assertThat(sut.existsCas(doc, user)).isFalse();
        }
    }

    @Test
    public void testThatLayerChangeEventInvalidatesCachedCas() throws Exception
    {
        // Setup fixture
        var doc = makeSourceDocument(4l, 4l, "test");
        var user = "test";
        try (var session = openNested(true)) {
            var text = "This is a test";
            createCasFile(doc, user, text);
        }

        // Actual test
        int casIdentity1;
        try (var session = openNested(true)) {
            var cas = sut.readCas(doc, user).getJCas();
            casIdentity1 = System.identityHashCode(cas);
        }

        int casIdentity2;
        try (var session = openNested(true)) {
            var cas = sut.readCas(doc, user).getJCas();
            casIdentity2 = System.identityHashCode(cas);
        }

        sut.beforeLayerConfigurationChanged(
                new LayerConfigurationChangedEvent(this, doc.getProject()));

        int casIdentity3;
        try (var session = openNested(true)) {
            var cas = sut.readCas(doc, user).getJCas();
            casIdentity3 = System.identityHashCode(cas);
        }

        assertThat(casIdentity1)
                .as("Getting the CAS a second time returns the same instance from memory")
                .isEqualTo(casIdentity2);
        assertThat(casIdentity1)
                .as("After a type system change event must return a different CAS instance")
                .isNotEqualTo(casIdentity3);
    }

    @Test
    public void testConcurrentAccess() throws Exception
    {
        // Setup fixture
        var doc = makeSourceDocument(5l, 5l, "test");
        var user = "test";

        try (var session = openNested(true)) {
            createCasFile(doc, user, "This is a test");
            assertThat(sut.existsCas(doc, user)).isTrue();
        }

        try (var casStorageSession = openNested(true)) {
            var mainCas = sut.readCas(doc, user, EXCLUSIVE_WRITE_ACCESS);

            var casFile = driver.getCasFile(doc, user);
            casFile.setLastModified(casFile.lastModified() + 10_000);

            var timestamp = sut.getCasTimestamp(doc, user).get();

            assertThatExceptionOfType(IOException.class)
                    .isThrownBy(() -> sut.writeCas(doc, mainCas, user))
                    .withMessageContaining("concurrent modification");

            assertThat(sut.existsCas(doc, user)).isTrue();
            assertThat(sut.getCasTimestamp(doc, user).get()).isEqualTo(timestamp);
        }
    }

    @Test
    public void testRestorationOfCasWhenSaveFails() throws Exception
    {
        try (CasStorageSession casStorageSession = openNested(true)) {
            // Setup fixture
            var doc = makeSourceDocument(6l, 6l, "test");
            var user = "test";
            var casFile = driver.getCasFile(doc, user);

            long casFileSize;
            long casFileLastModified;

            try (var session = openNested(true)) {
                createCasFile(doc, user, "This is a test");
                assertThat(sut.existsCas(doc, user)).isTrue();
                casFileSize = casFile.length();
                casFileLastModified = casFile.lastModified();
            }

            var mainCas = sut.readCas(doc, user, EXCLUSIVE_WRITE_ACCESS);

            // Wrap the CAS in a proxy so that UIMA cannot serialize it
            var guardedCas = (CAS) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[] { CAS.class },
                    (proxy, method, args) -> method.invoke(mainCas, args));

            assertThatExceptionOfType(IOException.class).as(
                    "Saving fails because UIMA cannot cast the proxied CAS to something serializable")
                    .isThrownBy(() -> sut.writeCas(doc, guardedCas, user))
                    .withRootCauseInstanceOf(ClassCastException.class);

            assertThat(casFile).exists().hasSize(casFileSize);
            assertThat(sut.getCasTimestamp(doc, user).get()).isEqualTo(casFileLastModified);
            assertThat(new File(casFile.getParentFile(), user + ".ser.old")).doesNotExist();
        }
    }

    @Test
    public void testHighConcurrencyIncludingDeletion() throws Exception
    {
        CasProvider initializer = () -> {
            try {
                CAS cas = WebAnnoCasUtil.createCas(mergeTypeSystems(
                        asList(createTypeSystemDescription(), getInternalTypeSystem())));
                cas.setDocumentText(repeat("This is a test.\n", 100_000));
                return cas;
            }
            catch (ResourceInitializationException e) {
                throw new IOException(e);
            }
        };

        var doc = makeSourceDocument(7l, 7l, "doc");
        var user = "annotator";

        // We interleave all the primary and secondary tasks into the main tasks list
        // Primary tasks run for a certain number of iterations
        // Secondary tasks run as long as any primary task is still running
        List<Thread> tasks = new ArrayList<>();
        List<Thread> primaryTasks = new ArrayList<>();
        List<Thread> secondaryTasks = new ArrayList<>();

        int threadGroupCount = 4;
        int iterations = 100;
        for (int n = 0; n < threadGroupCount; n++) {
            Thread rw = new ExclusiveReadWriteTask(n, doc, user, initializer, iterations);
            primaryTasks.add(rw);
            tasks.add(rw);

            Thread ro = new SharedReadOnlyTask(n, doc, user, initializer);
            secondaryTasks.add(ro);
            tasks.add(ro);

            Thread un = new UnmanagedTask(n, doc, user, initializer);
            secondaryTasks.add(un);
            tasks.add(un);

            Thread uni = new UnmanagedNonInitializingTask(n, doc, user);
            secondaryTasks.add(uni);
            tasks.add(uni);

            DeleterTask xx = new DeleterTask(n, doc, user);
            secondaryTasks.add(xx);
            tasks.add(xx);
        }

        LOG.info("---- Starting all threads ----");
        tasks.forEach(Thread::start);

        LOG.info("---- Waiting for primary threads to complete ----");
        var done = false;
        while (!done) {
            var running = primaryTasks.stream().filter(Thread::isAlive).count();
            done = running == 0l;
            sleep(1000);
            LOG.info("running {}  complete {}%  rw {}  ro {}  un {}  uni {}  xx {} XX {}", running,
                    (writeCounter.get() * 100) / (threadGroupCount * iterations), writeCounter,
                    managedReadCounter, unmanagedReadCounter, unmanagedNonInitializingReadCounter,
                    deleteCounter, deleteInitialCounter);
        }

        LOG.info("---- Waiting for secondary threads to wrap up ----");
        rwTasksCompleted.set(true);
        for (var thread : secondaryTasks) {
            thread.join();
        }

        LOG.info("---- Test is done ----");

        assertThat(exception).isFalse();
    }

    @Test
    public void testHighConcurrencyWithoutDeletion() throws Exception
    {
        CasProvider initializer = () -> {
            try {
                CAS cas = WebAnnoCasUtil.createCas(mergeTypeSystems(
                        asList(createTypeSystemDescription(), getInternalTypeSystem())));
                cas.setDocumentText(repeat("This is a test.\n", 100_000));
                return cas;
            }
            catch (ResourceInitializationException e) {
                throw new IOException(e);
            }
        };

        CasProvider badSeed = () -> {
            throw new IOException("This initializer should never be called!");
        };

        var doc = makeSourceDocument(8l, 8l, "doc");
        var user = "annotator";
        try (var session = openNested()) {
            // Make sure the CAS exists so that the threads should never be forced to call the
            // the initializer
            sut.readOrCreateCas(doc, user, FORCE_CAS_UPGRADE, initializer, EXCLUSIVE_WRITE_ACCESS);
        }

        // We interleave all the primary and secondary tasks into the main tasks list
        // Primary tasks run for a certain number of iterations
        // Secondary tasks run as long as any primary task is still running
        List<Thread> tasks = new ArrayList<>();
        List<Thread> primaryTasks = new ArrayList<>();
        List<Thread> secondaryTasks = new ArrayList<>();

        int threadGroupCount = 4;
        int iterations = 100;
        for (var n = 0; n < threadGroupCount; n++) {
            var rw = new ExclusiveReadWriteTask(n, doc, user, badSeed, iterations);
            primaryTasks.add(rw);
            tasks.add(rw);

            var ro = new SharedReadOnlyTask(n, doc, user, badSeed);
            secondaryTasks.add(ro);
            tasks.add(ro);

            var un = new UnmanagedTask(n, doc, user, badSeed);
            secondaryTasks.add(un);
            tasks.add(un);

            var uni = new UnmanagedNonInitializingTask(n, doc, user);
            secondaryTasks.add(uni);
            tasks.add(uni);
        }

        LOG.info("---- Starting all threads ----");
        tasks.forEach(Thread::start);

        LOG.info("---- Wait for primary threads to complete ----");
        boolean done = false;
        while (!done) {
            var running = primaryTasks.stream().filter(Thread::isAlive).count();
            done = running == 0l;
            sleep(1000);
            LOG.info("running {}  complete {}%  rw {}  ro {}  un {}  uni {}", running,
                    (writeCounter.get() * 100) / (threadGroupCount * iterations), writeCounter,
                    managedReadCounter, unmanagedReadCounter, unmanagedNonInitializingReadCounter);
        }

        LOG.info("---- Wait for threads secondary threads to wrap up ----");
        rwTasksCompleted.set(true);
        for (var thread : secondaryTasks) {
            thread.join();
        }

        LOG.info("---- Test is done ----");

        assertThat(exception).isFalse();
    }

    private class ExclusiveReadWriteTask
        extends Thread
    {
        private SourceDocument doc;
        private String user;
        private int repeat;
        private CasProvider initializer;

        public ExclusiveReadWriteTask(int n, SourceDocument aDoc, String aUser,
                CasProvider aInitializer, int aRepeat)
        {
            super("RW" + n);
            doc = aDoc;
            user = aUser;
            repeat = aRepeat;
            initializer = aInitializer;
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            for (int n = 0; n < repeat; n++) {
                if (exception.get()) {
                    return;
                }

                try (var session = openNested()) {
                    var cas = sut.readOrCreateCas(doc, user, FORCE_CAS_UPGRADE, initializer,
                            EXCLUSIVE_WRITE_ACCESS);
                    Thread.sleep(50);
                    var fs = cas.createAnnotation(cas.getAnnotationType(), 0, 10);
                    cas.addFsToIndexes(fs);
                    LOG.debug("CAS size: {}", cas.getAnnotationIndex().size());
                    sut.writeCas(doc, cas, user);
                    writeCounter.incrementAndGet();
                }
                catch (Exception e) {
                    exception.set(true);
                    throw new RuntimeException(e);
                }
            }
        }
    };

    private class SharedReadOnlyTask
        extends Thread
    {
        private SourceDocument doc;
        private String user;
        private CasProvider initializer;

        public SharedReadOnlyTask(int n, SourceDocument aDoc, String aUser,
                CasProvider aInitializer)
        {
            super("RO" + n);
            doc = aDoc;
            user = aUser;
            initializer = aInitializer;
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            while (!(exception.get() || rwTasksCompleted.get())) {
                try (var session = openNested()) {
                    sut.readOrCreateCas(doc, user, AUTO_CAS_UPGRADE, initializer,
                            SHARED_READ_ONLY_ACCESS);
                    managedReadCounter.incrementAndGet();
                    Thread.sleep(50);
                }
                catch (Exception e) {
                    exception.set(true);
                    throw new RuntimeException(e);
                }
            }
        }
    };

    private class DeleterTask
        extends Thread
    {
        private SourceDocument doc;
        private String user;
        private Random rnd;

        public DeleterTask(int n, SourceDocument aDoc, String aUser)
        {
            super("XX" + n);
            doc = aDoc;
            user = aUser;
            rnd = new Random();
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            while (!(exception.get() || rwTasksCompleted.get())) {
                try (var session = openNested()) {
                    Thread.sleep(2500 + rnd.nextInt(2500));
                    if (rnd.nextInt(100) >= 75) {
                        sut.deleteCas(doc, INITIAL_CAS_PSEUDO_USER);
                        deleteInitialCounter.incrementAndGet();
                    }
                    sut.deleteCas(doc, user);
                    deleteCounter.incrementAndGet();
                }
                catch (Exception e) {
                    exception.set(true);
                    throw new RuntimeException(e);
                }
            }
        }
    };

    private class UnmanagedTask
        extends Thread
    {
        private SourceDocument doc;
        private String user;
        private CasProvider initializer;

        public UnmanagedTask(int n, SourceDocument aDoc, String aUser, CasProvider aInitializer)
        {
            super("UN" + n);
            doc = aDoc;
            user = aUser;
            initializer = aInitializer;
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            while (!(exception.get() || rwTasksCompleted.get())) {
                try (var session = openNested()) {
                    sut.readOrCreateCas(doc, user, AUTO_CAS_UPGRADE, initializer, UNMANAGED_ACCESS);
                    unmanagedReadCounter.incrementAndGet();
                    Thread.sleep(50);
                }
                catch (Exception e) {
                    exception.set(true);
                    throw new RuntimeException(e);
                }
            }
        }
    };

    private class UnmanagedNonInitializingTask
        extends Thread
    {
        private SourceDocument doc;
        private String user;

        public UnmanagedNonInitializingTask(int n, SourceDocument aDoc, String aUser)
        {
            super("U_" + n);
            doc = aDoc;
            user = aUser;
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            while (!(exception.get() || rwTasksCompleted.get())) {
                try (CasStorageSession session = openNested()) {
                    sut.readCas(doc, user, UNMANAGED_NON_INITIALIZING_ACCESS);
                    unmanagedNonInitializingReadCounter.incrementAndGet();
                    Thread.sleep(50);
                }
                catch (FileNotFoundException e) {
                    // We ignore the FileNotFoundException, this could be happening if the deleter
                    // has just kicked in and would be perfectly normal
                }
                catch (Exception e) {
                    exception.set(true);
                    throw new RuntimeException(e);
                }
            }
        }
    };

    private CAS makeCas(String aText) throws IOException
    {
        try {
            CAS cas = WebAnnoCasUtil.createCas(mergeTypeSystems(
                    asList(createTypeSystemDescription(), getInternalTypeSystem())));
            cas.setDocumentText(aText);
            return cas;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    private JCas createCasFile(SourceDocument doc, String user, String text)
        throws CASException, CasSessionException, IOException
    {
        var casTemplate = sut.readOrCreateCas(doc, user, NO_CAS_UPGRADE, () -> makeCas(text),
                EXCLUSIVE_WRITE_ACCESS).getJCas();
        assertThat(sut.existsCas(doc, user)).isTrue();

        return casTemplate;
    }

    private SourceDocument makeSourceDocument(long aProjectId, long aDocumentId, String aDocName)
    {
        var project = new Project();
        project.setId(aProjectId);

        var doc = new SourceDocument();
        doc.setProject(project);
        doc.setId(aDocumentId);
        doc.setName(aDocName);

        return doc;
    }
}
