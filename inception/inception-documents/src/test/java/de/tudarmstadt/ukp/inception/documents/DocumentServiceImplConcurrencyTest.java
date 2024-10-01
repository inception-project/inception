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
import static de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils.getInternalTypeSystem;
import static de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession.openNested;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageServiceImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageBackupProperties;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageCachePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStoragePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.driver.CasStorageDriver;
import de.tudarmstadt.ukp.inception.annotation.storage.driver.filesystem.FileSystemCasStorageDriver;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentStorageService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.logging.Logging;
import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceImplConcurrencyTest
{
    private static final int DOC_SIZE = 100_000;

    private Logger log = LoggerFactory.getLogger(getClass());

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

        repositoryProperties = new RepositoryPropertiesImpl();
        repositoryProperties.setPath(testFolder);
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        CasStorageDriver driver = new FileSystemCasStorageDriver(repositoryProperties,
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
            SourceDocument doc = _invocation.getArgument(0, SourceDocument.class);
            String user = _invocation.getArgument(1, String.class);
            return new AnnotationDocument(user, doc);
        }).when(sut).getAnnotationDocument(any(), any(String.class));

        lenient()
                .when(importExportService.importCasFromFileNoChecks(any(File.class),
                        any(SourceDocument.class), any()))
                .thenReturn(CasFactory.createText("Test"));
    }

    @Test
    public void thatCreatingOrReadingInitialCasForNewDocumentCreatesNewCas() throws Exception
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            var doc = makeSourceDocument(1l, 1l, "test");

            var cas = sut.createOrReadInitialCas(doc).getJCas();

            assertThat(cas).isNotNull();
            assertThat(cas.getDocumentText()).isEqualTo("Test");
            assertThat(casStorageService.existsCas(doc, INITIAL_CAS_PSEUDO_USER)).isTrue();
        }
    }

    @Test
    public void thatReadingNonExistentAnnotationCasCreatesNewCas() throws Exception
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            SourceDocument sourceDocument = makeSourceDocument(1l, 1l, "test");
            User user = makeUser();

            JCas cas = sut.readAnnotationCas(sourceDocument, user.getUsername()).getJCas();

            assertThat(cas).isNotNull();
            assertThat(cas.getDocumentText()).isEqualTo("Test");
            assertThat(casStorageService.existsCas(sourceDocument, user.getUsername())).isTrue();
        }
    }

    @Test
    public void testHighConcurrencySingleUser() throws Exception
    {
        var docText = repeat("This is a test.\n", DOC_SIZE);
        var typeSystem = mergeTypeSystems(
                asList(createTypeSystemDescription(), getInternalTypeSystem()));

        when(importExportService.importCasFromFileNoChecks(any(File.class),
                any(SourceDocument.class), any())).then(_invocation -> {
                    CAS cas = createCas(typeSystem);
                    cas.setDocumentText(docText);
                    return cas;
                });

        SourceDocument doc = makeSourceDocument(2l, 2l, "doc");
        String user = "annotator";

        // Primary tasks run for a certain number of iterations
        // Secondary tasks run as long as any primary task is still running
        List<Thread> tasks = new ArrayList<>();
        List<Thread> primaryTasks = new ArrayList<>();
        List<Thread> secondaryTasks = new ArrayList<>();

        int threadGroupCount = 4;
        int iterations = 50;
        for (int n = 0; n < threadGroupCount; n++) {
            Thread rw = new ExclusiveReadWriteTask(n, doc, user, iterations);
            primaryTasks.add(rw);
            tasks.add(rw);

            Thread ro = new SharedReadOnlyTask(n, doc, user);
            secondaryTasks.add(ro);
            tasks.add(ro);

            Thread un = new UnmanagedTask(n, doc, user);
            secondaryTasks.add(un);
            tasks.add(un);

            DeleterTask xx = new DeleterTask(n, doc, user);
            secondaryTasks.add(xx);
            tasks.add(xx);
        }

        log.info("---- Starting all threads ----");
        tasks.forEach(Thread::start);

        log.info("---- Wait for primary threads to complete ----");
        boolean done = false;
        while (!done) {
            long running = primaryTasks.stream().filter(Thread::isAlive).count();
            done = running == 0l;
            sleep(1000);
            log.info("running {}  complete {}%  rw {}  ro {}  un {}  xx {} XX {}", running,
                    (writeCounter.get() * 100) / (threadGroupCount * iterations), writeCounter,
                    managedReadCounter, unmanagedReadCounter, deleteCounter, deleteInitialCounter);
        }

        log.info("---- Wait for threads secondary threads to wrap up ----");
        rwTasksCompleted.set(true);
        for (Thread thread : secondaryTasks) {
            thread.join();
        }

        log.info("---- Test is done ----");

        assertThat(exception).isFalse();
    }

    @Test
    public void testHighConcurrencyMultiUser() throws Exception
    {
        var docText = repeat("This is a test.\n", DOC_SIZE);
        var typeSystem = mergeTypeSystems(
                asList(createTypeSystemDescription(), getInternalTypeSystem()));

        when(importExportService.importCasFromFileNoChecks(any(File.class),
                any(SourceDocument.class), any())).then(_invocation -> {
                    CAS cas = createCas(typeSystem);
                    cas.setDocumentText(docText);
                    return cas;
                });

        SourceDocument doc = makeSourceDocument(3l, 3l, "doc");
        String user = "annotator";

        // Primary tasks run for a certain number of iterations
        // Secondary tasks run as long as any primary task is still running
        List<Thread> tasks = new ArrayList<>();
        List<Thread> primaryTasks = new ArrayList<>();
        List<Thread> secondaryTasks = new ArrayList<>();

        int threadGroupCount = 4;
        int iterations = 50;
        int userCount = 4;
        for (int u = 0; u < userCount; u++) {
            for (int n = 0; n < threadGroupCount; n++) {
                Thread rw = new ExclusiveReadWriteTask(n, doc, user + n, iterations);
                primaryTasks.add(rw);
                tasks.add(rw);

                Thread ro = new SharedReadOnlyTask(n, doc, user + n);
                secondaryTasks.add(ro);
                tasks.add(ro);

                Thread un = new UnmanagedTask(n, doc, user + n);
                secondaryTasks.add(un);
                tasks.add(un);

                DeleterTask xx = new DeleterTask(n, doc, user + n);
                secondaryTasks.add(xx);
                tasks.add(xx);
            }
        }

        log.info("---- Starting all threads ----");
        tasks.forEach(Thread::start);

        log.info("---- Wait for primary threads to complete ----");
        boolean done = false;
        while (!done) {
            long running = primaryTasks.stream().filter(Thread::isAlive).count();
            done = running == 0l;
            sleep(1000);
            long freeMemory = Runtime.getRuntime().freeMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();
            long maxMemory = Runtime.getRuntime().maxMemory();
            long realFree = maxMemory - totalMemory + freeMemory;
            long used = maxMemory - realFree;
            long perc = (used * 100) / maxMemory;
            log.info("running {}  complete {}%  rw {}  ro {}  un {}  xx {} XX {} [{}% {}/{}]",
                    running,
                    (writeCounter.get() * 100) / (userCount * threadGroupCount * iterations),
                    writeCounter, managedReadCounter, unmanagedReadCounter, deleteCounter,
                    deleteInitialCounter, perc, used, maxMemory);
        }

        log.info("---- Wait for threads secondary threads to wrap up ----");
        rwTasksCompleted.set(true);
        for (Thread thread : secondaryTasks) {
            thread.join();
        }

        log.info("---- Test is done ----");

        assertThat(exception).isFalse();
    }

    private class ExclusiveReadWriteTask
        extends Thread
    {
        private SourceDocument doc;
        private String user;
        private int repeat;

        public ExclusiveReadWriteTask(int n, SourceDocument aDoc, String aUser, int aRepeat)
        {
            super("RW" + n);
            doc = aDoc;
            user = aUser;
            repeat = aRepeat;
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
                    var cas = sut.readAnnotationCas(doc, user);
                    Thread.sleep(50);
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
        private String user;

        public SharedReadOnlyTask(int n, SourceDocument aDoc, String aUser)
        {
            super("RO" + n);
            doc = aDoc;
            user = aUser;
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            while (!(exception.get() || rwTasksCompleted.get())) {
                try (CasStorageSession session = openNested()) {
                    sut.readAnnotationCas(doc, user, AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
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
                try (CasStorageSession session = openNested()) {
                    Thread.sleep(2500 + rnd.nextInt(2500));
                    if (rnd.nextInt(100) >= 75) {
                        sut.deleteAnnotationCas(doc, INITIAL_CAS_PSEUDO_USER);
                        deleteInitialCounter.incrementAndGet();
                    }
                    sut.deleteAnnotationCas(doc, user);
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

        public UnmanagedTask(int n, SourceDocument aDoc, String aUser)
        {
            super("UN" + n);
            doc = aDoc;
            user = aUser;
        }

        @Override
        public void run()
        {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            while (!(exception.get() || rwTasksCompleted.get())) {
                try (CasStorageSession session = openNested()) {
                    sut.readAnnotationCas(doc, user, AUTO_CAS_UPGRADE, UNMANAGED_ACCESS);
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

    private SourceDocument makeSourceDocument(long aProjectId, long aDocumentId, String aDocName)
    {
        Project project = new Project();
        project.setId(aProjectId);

        SourceDocument doc = new SourceDocument();
        doc.setProject(project);
        doc.setId(aDocumentId);
        doc.setName(aDocName);

        return doc;
    }

    private User makeUser()
    {
        User user = new User();
        user.setUsername("Test user");
        return user;
    }
}
