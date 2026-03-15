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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession.openNested;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.INITIAL_SET;
import static de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils.getInternalTypeSystem;
import static java.lang.Thread.sleep;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageServiceImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageBackupProperties;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageCachePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStoragePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.driver.filesystem.FileSystemCasStorageDriver;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentStorageService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.logging.Logging;
import jakarta.persistence.EntityManager;

@Execution(CONCURRENT)
@ExtendWith(MockitoExtension.class)
public class DocumentServiceImplConcurrencyTest
{
    private static final int DOC_SIZE = 100_000;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Mock(stubOnly = true) DocumentImportExportService importExportService;
    private @Mock(stubOnly = true) ProjectService projectService;
    private @Mock(stubOnly = true) ApplicationEventPublisher applicationEventPublisher;
    private @Mock(stubOnly = true) EntityManager entityManager;

    public @TempDir File testFolder;

    private AtomicBoolean exception = new AtomicBoolean(false);
    private AtomicBoolean rwTasksCompleted = new AtomicBoolean(false);
    private AtomicInteger managedReadCounter = new AtomicInteger(0);
    private AtomicInteger unmanagedReadCounter = new AtomicInteger(0);
    private AtomicInteger deleteCounter = new AtomicInteger(0);
    private AtomicInteger deleteInitialCounter = new AtomicInteger(0);
    private AtomicInteger writeCounter = new AtomicInteger(0);

    private DocumentService sut;

    private RepositoryProperties repositoryProperties;
    private CasStorageService casStorageService;
    private DocumentStorageService docStorageService;
    private TypeSystemDescription typeSystem;

    @BeforeEach
    public void setup() throws Exception
    {
        exception.set(false);
        rwTasksCompleted.set(false);
        managedReadCounter.set(0);
        unmanagedReadCounter.set(0);
        writeCounter.set(0);
        deleteCounter.set(0);
        deleteInitialCounter.set(0);

        typeSystem = mergeTypeSystems(
                asList(createTypeSystemDescription(), getInternalTypeSystem()));

        repositoryProperties = new RepositoryPropertiesImpl();
        repositoryProperties.setPath(testFolder);
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        var driver = new FileSystemCasStorageDriver(repositoryProperties,
                new CasStorageBackupProperties(), new CasStoragePropertiesImpl());

        casStorageService = new CasStorageServiceImpl(driver, new CasStorageCachePropertiesImpl(),
                null, null);
        docStorageService = new DocumentStorageServiceImpl(repositoryProperties);

        var realSut = new DocumentServiceImpl(repositoryProperties, casStorageService,
                importExportService, projectService, applicationEventPublisher, entityManager,
                docStorageService);
        sut = Mockito.mock(DocumentServiceImpl.class, Mockito.withSettings().spiedInstance(realSut)
                .stubOnly().defaultAnswer(Answers.CALLS_REAL_METHODS));

        lenient().doAnswer(_invocation -> {
            var doc = _invocation.getArgument(0, SourceDocument.class);
            var user = _invocation.getArgument(1, AnnotationSet.class);
            return new AnnotationDocument(user.id(), doc);
        }).when(sut).getAnnotationDocument(any(), any(AnnotationSet.class));

        var cas = createCas(typeSystem);
        cas.setDocumentText("Test");
        lenient().when(importExportService.importCasFromFileNoChecks(any(File.class),
                any(SourceDocument.class), any())).thenReturn(cas);
    }

    @Test
    public void thatCreatingOrReadingInitialCasForNewDocumentCreatesNewCas() throws Exception
    {
        try (var session = CasStorageSession.open()) {
            var doc = makeSourceDocument(1l, 1l, "test");

            var cas = sut.createOrReadInitialCas(doc).getJCas();

            assertThat(cas).isNotNull();
            assertThat(cas.getDocumentText()).isEqualTo("Test");
            assertThat(casStorageService.existsCas(doc, INITIAL_SET)).isTrue();
        }
    }

    @Test
    public void thatReadingNonExistentAnnotationCasCreatesNewCas() throws Exception
    {
        try (var session = CasStorageSession.open()) {
            var sourceDocument = makeSourceDocument(1l, 1l, "test");
            var set = AnnotationSet.forTest("test");

            var cas = sut.readAnnotationCas(sourceDocument, set).getJCas();

            assertThat(cas).isNotNull();
            assertThat(cas.getDocumentText()).isEqualTo("Test");
            assertThat(casStorageService.existsCas(sourceDocument, set)).isTrue();
        }
    }

    @Test
    public void testHighConcurrencySingleUser() throws Exception
    {
        var docText = repeat("This is a test.\n", DOC_SIZE);

        when(importExportService.importCasFromFileNoChecks(any(File.class),
                any(SourceDocument.class), any())).then(_invocation -> {
                    var cas = createCas(typeSystem);
                    cas.setDocumentText(docText);
                    return cas;
                });

        var doc = makeSourceDocument(2l, 2l, "doc");
        var user = AnnotationSet.forTest("annotator");

        // Primary tasks run for a certain number of iterations
        // Secondary tasks run as long as any primary task is still running
        var tasks = new ArrayList<Thread>();
        var primaryTasks = new ArrayList<Thread>();
        var secondaryTasks = new ArrayList<Thread>();

        var threadGroupCount = 4;
        var iterations = 50;
        for (var n = 0; n < threadGroupCount; n++) {
            var rw = new ExclusiveReadWriteTask(n, doc, user, iterations);
            primaryTasks.add(rw);
            tasks.add(rw);

            var ro = new SharedReadOnlyTask(n, doc, user);
            secondaryTasks.add(ro);
            tasks.add(ro);

            var un = new UnmanagedTask(n, doc, user);
            secondaryTasks.add(un);
            tasks.add(un);

            var xx = new DeleterTask(n, doc, user);
            secondaryTasks.add(xx);
            tasks.add(xx);
        }

        LOG.info("---- Starting all threads ----");
        tasks.forEach(Thread::start);

        LOG.info("---- Wait for primary threads to complete ----");
        var done = false;
        while (!done) {
            var running = primaryTasks.stream().filter(Thread::isAlive).count();
            done = running == 0l;
            sleep(1000);
            LOG.info("running {}  complete {}%  rw {}  ro {}  un {}  xx {} XX {}", running,
                    (writeCounter.get() * 100) / (threadGroupCount * iterations), writeCounter,
                    managedReadCounter, unmanagedReadCounter, deleteCounter, deleteInitialCounter);
        }

        LOG.info("---- Wait for threads secondary threads to wrap up ----");
        rwTasksCompleted.set(true);
        for (Thread thread : secondaryTasks) {
            thread.join();
        }

        LOG.info("---- Test is done ----");

        assertThat(exception).isFalse();
    }

    @Test
    public void testHighConcurrencyMultiUser() throws Exception
    {
        var docText = repeat("This is a test.\n", DOC_SIZE);

        when(importExportService.importCasFromFileNoChecks(any(File.class),
                any(SourceDocument.class), any())).then(_invocation -> {
                    CAS cas = createCas(typeSystem);
                    cas.setDocumentText(docText);
                    return cas;
                });

        var doc = makeSourceDocument(3l, 3l, "doc");
        var user = "annotator";

        // Primary tasks run for a certain number of iterations
        // Secondary tasks run as long as any primary task is still running
        var tasks = new ArrayList<Thread>();
        var primaryTasks = new ArrayList<Thread>();
        var secondaryTasks = new ArrayList<Thread>();

        var threadGroupCount = 4;
        var iterations = 50;
        var userCount = 4;
        for (var u = 0; u < userCount; u++) {
            for (var n = 0; n < threadGroupCount; n++) {
                var rw = new ExclusiveReadWriteTask(n, doc, AnnotationSet.forTest(user + n),
                        iterations);
                primaryTasks.add(rw);
                tasks.add(rw);

                var ro = new SharedReadOnlyTask(n, doc, AnnotationSet.forTest(user + n));
                secondaryTasks.add(ro);
                tasks.add(ro);

                var un = new UnmanagedTask(n, doc, AnnotationSet.forTest(user + n));
                secondaryTasks.add(un);
                tasks.add(un);

                var xx = new DeleterTask(n, doc, AnnotationSet.forTest(user + n));
                secondaryTasks.add(xx);
                tasks.add(xx);
            }
        }

        LOG.info("---- Starting all threads ----");
        tasks.forEach(Thread::start);

        LOG.info("---- Wait for primary threads to complete ----");
        var done = false;
        while (!done) {
            var running = primaryTasks.stream().filter(Thread::isAlive).count();
            done = running == 0l;
            sleep(1000);
            var freeMemory = Runtime.getRuntime().freeMemory();
            var totalMemory = Runtime.getRuntime().totalMemory();
            var maxMemory = Runtime.getRuntime().maxMemory();
            var realFree = maxMemory - totalMemory + freeMemory;
            var used = maxMemory - realFree;
            var perc = (used * 100) / maxMemory;
            LOG.info("running {}  complete {}%  rw {}  ro {}  un {}  xx {} XX {} [{}% {}/{}]",
                    running,
                    (writeCounter.get() * 100) / (userCount * threadGroupCount * iterations),
                    writeCounter, managedReadCounter, unmanagedReadCounter, deleteCounter,
                    deleteInitialCounter, perc, used, maxMemory);
        }

        LOG.info("---- Wait for threads secondary threads to wrap up ----");
        rwTasksCompleted.set(true);
        for (Thread thread : secondaryTasks) {
            thread.join();
        }

        LOG.info("---- Test is done ----");

        assertThat(exception).isFalse();
    }

    private class ExclusiveReadWriteTask
        extends Thread
    {
        private SourceDocument doc;
        private AnnotationSet user;
        private int repeat;

        public ExclusiveReadWriteTask(int n, SourceDocument aDoc, AnnotationSet aSet, int aRepeat)
        {
            super("RW" + n);
            doc = aDoc;
            user = aSet;
            repeat = aRepeat;
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
                    var cas = sut.readAnnotationCas(doc, user);
                    sleep(50);
                    sut.writeAnnotationCas(cas, doc, user);
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

        public SharedReadOnlyTask(int n, SourceDocument aDoc, AnnotationSet aSet)
        {
            super("RO" + n);
            doc = aDoc;
            set = aSet;
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            while (!(exception.get() || rwTasksCompleted.get())) {
                try (var session = openNested()) {
                    sut.readAnnotationCas(doc, set, AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
                    managedReadCounter.incrementAndGet();
                    sleep(50);
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

        public DeleterTask(int n, SourceDocument aDoc, AnnotationSet aSet)
        {
            super("XX" + n);
            doc = aDoc;
            set = aSet;
            rnd = new Random();
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            while (!(exception.get() || rwTasksCompleted.get())) {
                try (var session = openNested()) {
                    sleep(2500 + rnd.nextInt(2500));
                    if (rnd.nextInt(100) >= 75) {
                        sut.deleteAnnotationCas(doc, INITIAL_SET);
                        deleteInitialCounter.incrementAndGet();
                    }
                    sut.deleteAnnotationCas(doc, set);
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

        public UnmanagedTask(int n, SourceDocument aDoc, AnnotationSet aSet)
        {
            super("UN" + n);
            doc = aDoc;
            set = aSet;
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            while (!(exception.get() || rwTasksCompleted.get())) {
                try (var session = openNested()) {
                    sut.readAnnotationCas(doc, set, AUTO_CAS_UPGRADE, UNMANAGED_ACCESS);
                    unmanagedReadCounter.incrementAndGet();
                    sleep(50);
                }
                catch (Exception e) {
                    exception.set(true);
                    throw new RuntimeException(e);
                }
            }
        }
    };

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

    @Disabled("Flaky deadlock reproducer; for manual debugging only")
    @Test
    public void thatCrossDocumentImportCanDeadlock() throws Exception
    {
        // Two documents that will try to import each other while holding exclusive CAS access
        var docA = makeSourceDocument(10l, 10l, "docA");
        var docB = makeSourceDocument(20l, 20l, "docB");

        // Try the scenario multiple times to increase chance of reproducing the race
        var attempts = 12;
        var reproduced = false;

        for (var attempt = 1; attempt <= attempts && !reproduced; attempt++) {
            LOG.info("Deadlock reproduction attempt {}/{}", attempt, attempts);

            var ready = new CountDownLatch(2);

            // Override importer to attempt to create/read the other document's initial CAS
            when(importExportService.importCasFromFileNoChecks(any(File.class),
                    any(SourceDocument.class), any())).thenAnswer(invocation -> {
                        var requested = invocation.getArgument(1, SourceDocument.class);
                        ready.countDown();
                        // Wait until both importer threads are in the importer to maximize chance
                        // of cycle
                        ready.await();

                        // Small pause to increase overlap while holding the importer
                        try {
                            sleep(200);
                        }
                        catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        if (requested.getId() == docA.getId()) {
                            // While creating A, try to create B (will block if B is held by other
                            // thread)
                            return sut.createOrReadInitialCas(docB);
                        }
                        else {
                            return sut.createOrReadInitialCas(docA);
                        }
                    });

            var t1 = new Thread(() -> {
                try {
                    MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
                    try (var session = CasStorageSession.open()) {
                        sut.createOrReadInitialCas(docA);
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, "T1");

            var t2 = new Thread(() -> {
                try {
                    MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
                    try (var session = CasStorageSession.open()) {
                        sut.createOrReadInitialCas(docB);
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, "T2");

            t1.start();
            t2.start();

            // Wait longer for deadlock to manifest
            t1.join(10_000);
            t2.join(10_000);

            // If both threads are still alive after the join timeout, we reproduced a deadlock
            var deadlocked = t1.isAlive() && t2.isAlive();

            // Interrupt to clean up threads so test runner doesn't hang
            t1.interrupt();
            t2.interrupt();

            if (deadlocked) {
                reproduced = true;
                LOG.warn("Deadlock reproduced on attempt {}/{}", attempt, attempts);
            }

            // Reset mock for next attempt
            reset(importExportService);
            lenient().when(importExportService.importCasFromFileNoChecks(any(File.class),
                    any(SourceDocument.class), any())).thenReturn(createCas(typeSystem));
        }

        if (!reproduced) {
            // Write thread dump to temp folder for inspection
            try {
                var dumpFile = new File(testFolder, "thread-dump.txt");
                try (var pw = new PrintWriter(dumpFile, UTF_8)) {
                    pw.println("Thread dump at " + Instant.now());
                    for (var e : Thread.getAllStackTraces().entrySet()) {
                        var t = e.getKey();
                        pw.println("Thread " + t.getName() + " (id=" + t.threadId() + ") state="
                                + t.getState());
                        for (var st : e.getValue()) {
                            pw.println("\t" + st.toString());
                        }
                        pw.println();
                    }
                }
                LOG.info("Wrote thread dump to {}", dumpFile.getAbsolutePath());
            }
            catch (Exception e) {
                LOG.error("Failed to write thread dump", e);
            }
        }

        assertThat(reproduced).as("Expected deadlock reproduction in one of the attempts").isTrue();
    }
}
