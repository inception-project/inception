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
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession.openNested;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.INITIAL_SET;
import static de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils.getInternalTypeSystem;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createCas;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasSessionException;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
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

@Execution(CONCURRENT)
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
        try (var casStorageSession = openNested(true)) {
            // Setup fixture
            var doc = makeSourceDocument(1l, 1l, "test");
            var templateCas = createCas(createTypeSystemDescription()).getJCas();
            templateCas.setDocumentText("This is a test");
            casStorageSession.add(AnnotationSet.forTest("cas"), EXCLUSIVE_WRITE_ACCESS,
                    templateCas.getCas());
            var set = AnnotationSet.forTest("test");

            sut.writeCas(doc, templateCas.getCas(), set);
            assertThat(sut.existsCas(doc, set)).isTrue();

            // Actual test
            var cas = sut.readCas(doc, set);
            assertThat(cas.getDocumentText()).isEqualTo(templateCas.getDocumentText());

            sut.deleteCas(doc, set);
            assertThat(sut.existsCas(doc, set)).isFalse();
            assertThat(casStorageSession.contains(cas)).isFalse();
        }
    }

    @Test
    public void testCasMetadataGetsCreated() throws Exception
    {
        try (var casStorageSession = openNested(true)) {
            var typeSystems = new ArrayList<TypeSystemDescription>();
            typeSystems.add(createTypeSystemDescription());
            typeSystems.add(CasMetadataUtils.getInternalTypeSystem());

            var cas = WebAnnoCasUtil.createCas(mergeTypeSystems(typeSystems)).getJCas();
            casStorageSession.add(AnnotationSet.forTest("cas"), EXCLUSIVE_WRITE_ACCESS,
                    cas.getCas());

            var doc = makeSourceDocument(2l, 2l, "test");
            var set = AnnotationSet.forTest("test");

            sut.writeCas(doc, cas.getCas(), set);

            var cas2 = sut.readCas(doc, set).getJCas();

            var cmds = new ArrayList<>(select(cas2, CASMetadata.class));
            assertThat(cmds).hasSize(1);
            assertThat(cmds.get(0).getProjectId()).isEqualTo(doc.getProject().getId());
            assertThat(cmds.get(0).getSourceDocumentId()).isEqualTo(doc.getId());
            assertThat(cmds.get(0).getLastChangedOnDisk())
                    .isEqualTo(sut.getCasTimestamp(doc, set).get());
        }
    }

    @Test
    public void testReadOrCreateCas() throws Exception
    {
        try (var casStorageSession = openNested(true)) {
            // Setup fixture
            var doc = makeSourceDocument(3l, 3l, "test");
            var set = AnnotationSet.forTest("test");
            var text = "This is a test";
            createCasFile(doc, set, text);

            // Actual test
            var cas = sut.readCas(doc, set).getJCas();
            assertThat(cas.getDocumentText()).isEqualTo(text);

            sut.deleteCas(doc, set);
            assertThat(sut.existsCas(doc, set)).isFalse();
        }
    }

    @Test
    public void testThatLayerChangeEventInvalidatesCachedCas() throws Exception
    {
        // Setup fixture
        var doc = makeSourceDocument(4l, 4l, "test");
        var set = AnnotationSet.forTest("test");
        try (var session = openNested(true)) {
            var text = "This is a test";
            createCasFile(doc, set, text);
        }

        // Actual test
        int casIdentity1;
        try (var session = openNested(true)) {
            var cas = sut.readCas(doc, set).getJCas();
            casIdentity1 = System.identityHashCode(cas);
        }

        int casIdentity2;
        try (var session = openNested(true)) {
            var cas = sut.readCas(doc, set).getJCas();
            casIdentity2 = System.identityHashCode(cas);
        }

        sut.beforeLayerConfigurationChanged(
                new LayerConfigurationChangedEvent(this, doc.getProject()));

        int casIdentity3;
        try (var session = openNested(true)) {
            var cas = sut.readCas(doc, set).getJCas();
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
        var set = AnnotationSet.forTest("test");

        try (var session = openNested(true)) {
            createCasFile(doc, set, "This is a test");
            assertThat(sut.existsCas(doc, set)).isTrue();
        }

        try (var casStorageSession = openNested(true)) {
            var mainCas = sut.readCas(doc, set, EXCLUSIVE_WRITE_ACCESS);

            var casFile = driver.getCasFile(doc, set);
            casFile.setLastModified(casFile.lastModified() + 10_000);

            var timestamp = sut.getCasTimestamp(doc, set).get();

            assertThatExceptionOfType(IOException.class)
                    .isThrownBy(() -> sut.writeCas(doc, mainCas, set))
                    .withMessageContaining("concurrent modification");

            assertThat(sut.existsCas(doc, set)).isTrue();
            assertThat(sut.getCasTimestamp(doc, set).get()).isEqualTo(timestamp);
        }
    }

    @Test
    public void testRestorationOfCasWhenSaveFails() throws Exception
    {
        try (CasStorageSession casStorageSession = openNested(true)) {
            // Setup fixture
            var doc = makeSourceDocument(6l, 6l, "test");
            var set = AnnotationSet.forTest("test");
            var casFile = driver.getCasFile(doc, set);

            long casFileSize;
            long casFileLastModified;

            try (var session = openNested(true)) {
                createCasFile(doc, set, "This is a test");
                assertThat(sut.existsCas(doc, set)).isTrue();
                casFileSize = casFile.length();
                casFileLastModified = casFile.lastModified();
            }

            var mainCas = sut.readCas(doc, set, EXCLUSIVE_WRITE_ACCESS);

            // Wrap the CAS in a proxy so that UIMA cannot serialize it
            var guardedCas = (CAS) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[] { CAS.class },
                    (proxy, method, args) -> method.invoke(mainCas, args));

            assertThatExceptionOfType(IOException.class).as(
                    "Saving fails because UIMA cannot cast the proxied CAS to something serializable")
                    .isThrownBy(() -> sut.writeCas(doc, guardedCas, set))
                    .withRootCauseInstanceOf(ClassCastException.class);

            assertThat(casFile).exists().hasSize(casFileSize);
            assertThat(sut.getCasTimestamp(doc, set).get()).isEqualTo(casFileLastModified);
            assertThat(new File(casFile.getParentFile(), set + ".ser.old")).doesNotExist();
        }
    }

    @Test
    public void testHighConcurrencyIncludingDeletion() throws Exception
    {
        CasProvider initializer = () -> {
            try {
                var cas = WebAnnoCasUtil.createCas(mergeTypeSystems(
                        asList(createTypeSystemDescription(), getInternalTypeSystem())));
                cas.setDocumentText(repeat("This is a test.\n", 100_000));
                return cas;
            }
            catch (ResourceInitializationException e) {
                throw new IOException(e);
            }
        };

        var doc = makeSourceDocument(7l, 7l, "doc");
        var set = AnnotationSet.forTest("annotator");

        // We interleave all the primary and secondary tasks into the main tasks list
        // Primary tasks run for a certain number of iterations
        // Secondary tasks run as long as any primary task is still running
        var tasks = new ArrayList<Thread>();
        var primaryTasks = new ArrayList<Thread>();
        var secondaryTasks = new ArrayList<Thread>();

        var threadGroupCount = 4;
        var iterations = 100;
        for (var n = 0; n < threadGroupCount; n++) {
            var rw = new ExclusiveReadWriteTask(n, doc, set, initializer, iterations);
            primaryTasks.add(rw);
            tasks.add(rw);

            var ro = new SharedReadOnlyTask(n, doc, set, initializer);
            secondaryTasks.add(ro);
            tasks.add(ro);

            var un = new UnmanagedTask(n, doc, set, initializer);
            secondaryTasks.add(un);
            tasks.add(un);

            var uni = new UnmanagedNonInitializingTask(n, doc, set);
            secondaryTasks.add(uni);
            tasks.add(uni);

            var xx = new DeleterTask(n, doc, set);
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
                var cas = WebAnnoCasUtil.createCas(mergeTypeSystems(
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
        var set = AnnotationSet.forTest("annotator");
        try (var session = openNested()) {
            // Make sure the CAS exists so that the threads should never be forced to call the
            // the initializer
            sut.readOrCreateCas(doc, set, FORCE_CAS_UPGRADE, initializer, EXCLUSIVE_WRITE_ACCESS);
        }

        // We interleave all the primary and secondary tasks into the main tasks list
        // Primary tasks run for a certain number of iterations
        // Secondary tasks run as long as any primary task is still running
        var tasks = new ArrayList<Thread>();
        var primaryTasks = new ArrayList<Thread>();
        var secondaryTasks = new ArrayList<Thread>();

        var threadGroupCount = 4;
        var iterations = 100;
        for (var n = 0; n < threadGroupCount; n++) {
            var rw = new ExclusiveReadWriteTask(n, doc, set, badSeed, iterations);
            primaryTasks.add(rw);
            tasks.add(rw);

            var ro = new SharedReadOnlyTask(n, doc, set, badSeed);
            secondaryTasks.add(ro);
            tasks.add(ro);

            var un = new UnmanagedTask(n, doc, set, badSeed);
            secondaryTasks.add(un);
            tasks.add(un);

            var uni = new UnmanagedNonInitializingTask(n, doc, set);
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
        private AnnotationSet set;
        private int repeat;
        private CasProvider initializer;

        public ExclusiveReadWriteTask(int n, SourceDocument aDoc, AnnotationSet aUser,
                CasProvider aInitializer, int aRepeat)
        {
            super("RW" + n);
            doc = aDoc;
            set = aUser;
            repeat = aRepeat;
            initializer = aInitializer;
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            for (var n = 0; n < repeat; n++) {
                if (exception.get()) {
                    return;
                }

                try (var session = openNested()) {
                    var cas = sut.readOrCreateCas(doc, set, FORCE_CAS_UPGRADE, initializer,
                            EXCLUSIVE_WRITE_ACCESS);
                    Thread.sleep(50);
                    var fs = cas.createAnnotation(cas.getAnnotationType(), 0, 10);
                    cas.addFsToIndexes(fs);
                    LOG.debug("CAS size: {}", cas.getAnnotationIndex().size());
                    sut.writeCas(doc, cas, set);
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
        private AnnotationSet set;
        private CasProvider initializer;

        public SharedReadOnlyTask(int n, SourceDocument aDoc, AnnotationSet aUser,
                CasProvider aInitializer)
        {
            super("RO" + n);
            doc = aDoc;
            set = aUser;
            initializer = aInitializer;
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            while (!(exception.get() || rwTasksCompleted.get())) {
                try (var session = openNested()) {
                    sut.readOrCreateCas(doc, set, AUTO_CAS_UPGRADE, initializer,
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
        private AnnotationSet set;
        private Random rnd;

        public DeleterTask(int n, SourceDocument aDoc, AnnotationSet aUser)
        {
            super("XX" + n);
            doc = aDoc;
            set = aUser;
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
                        sut.deleteCas(doc, INITIAL_SET);
                        deleteInitialCounter.incrementAndGet();
                    }
                    sut.deleteCas(doc, set);
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
        private AnnotationSet set;
        private CasProvider initializer;

        public UnmanagedTask(int n, SourceDocument aDoc, AnnotationSet aUser,
                CasProvider aInitializer)
        {
            super("UN" + n);
            doc = aDoc;
            set = aUser;
            initializer = aInitializer;
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            while (!(exception.get() || rwTasksCompleted.get())) {
                try (var session = openNested()) {
                    sut.readOrCreateCas(doc, set, AUTO_CAS_UPGRADE, initializer, UNMANAGED_ACCESS);
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
        private AnnotationSet set;

        public UnmanagedNonInitializingTask(int n, SourceDocument aDoc, AnnotationSet aUser)
        {
            super("U_" + n);
            doc = aDoc;
            set = aUser;
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            while (!(exception.get() || rwTasksCompleted.get())) {
                try (var session = openNested()) {
                    sut.readCas(doc, set, UNMANAGED_NON_INITIALIZING_ACCESS);
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

    CAS makeCas(String aText) throws IOException
    {
        try {
            var cas = createCas(createTypeSystemDescription());
            cas.setDocumentText(aText);
            return cas;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    TypeSystemDescription createTypeSystemDescription() throws ResourceInitializationException
    {
        var internalTsd = CasMetadataUtils.getInternalTypeSystem();
        var globalTsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
        return CasCreationUtils.mergeTypeSystems(asList(globalTsd, internalTsd));
    }

    private JCas createCasFile(SourceDocument aDoc, AnnotationSet aSet, String aText)
        throws CASException, CasSessionException, IOException
    {
        var casTemplate = sut.readOrCreateCas(aDoc, aSet, NO_CAS_UPGRADE, () -> makeCas(aText),
                EXCLUSIVE_WRITE_ACCESS).getJCas();
        assertThat(sut.existsCas(aDoc, aSet)).isTrue();

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
