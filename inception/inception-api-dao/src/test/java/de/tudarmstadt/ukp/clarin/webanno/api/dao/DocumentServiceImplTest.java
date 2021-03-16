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
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.dao.CasMetadataUtils.getInternalTypeSystem;
import static de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession.openNested;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@RunWith(MockitoJUnitRunner.class)
public class DocumentServiceImplTest
{
    private Logger log = LoggerFactory.getLogger(getClass());

    private @Mock ImportExportService importExportService;
    private @Mock ProjectService projectService;
    private @Mock ApplicationEventPublisher applicationEventPublisher;
    private @Mock EntityManager entityManager;

    public @Rule TemporaryFolder testFolder = new TemporaryFolder();

    private AtomicBoolean exception = new AtomicBoolean(false);
    private AtomicBoolean rwTasksCompleted = new AtomicBoolean(false);
    private AtomicInteger managedReadCounter = new AtomicInteger(0);
    private AtomicInteger unmanagedReadCounter = new AtomicInteger(0);
    private AtomicInteger deleteCounter = new AtomicInteger(0);
    private AtomicInteger deleteInitialCounter = new AtomicInteger(0);
    private AtomicInteger writeCounter = new AtomicInteger(0);

    private DocumentService sut;

    private BackupProperties backupProperties;
    private RepositoryProperties repositoryProperties;
    private CasStorageService storageService;

    @Before
    public void setup() throws Exception
    {
        exception.set(false);
        rwTasksCompleted.set(false);
        managedReadCounter.set(0);
        unmanagedReadCounter.set(0);
        writeCounter.set(0);
        deleteCounter.set(0);
        deleteInitialCounter.set(0);

        backupProperties = new BackupProperties();

        repositoryProperties = new RepositoryProperties();
        repositoryProperties.setPath(testFolder.newFolder());

        storageService = new CasStorageServiceImpl(null, null, repositoryProperties,
                backupProperties);

        sut = spy(new DocumentServiceImpl(repositoryProperties, storageService, importExportService,
                projectService, applicationEventPublisher, entityManager));

        doAnswer(_invocation -> {
            SourceDocument doc = _invocation.getArgument(0, SourceDocument.class);
            String user = _invocation.getArgument(1, String.class);
            return new AnnotationDocument(doc.getName(), doc.getProject(), user, doc);
        }).when(sut).getAnnotationDocument(any(), any(String.class));

        when(importExportService.importCasFromFile(any(File.class), any(Project.class), any(),
                any())).thenReturn(CasFactory.createText("Test"));
    }

    @Test
    public void thatCreatingOrReadingInitialCasForNewDocumentCreatesNewCas() throws Exception
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            SourceDocument doc = makeSourceDocument(1l, 1l, "test");

            JCas cas = sut.createOrReadInitialCas(doc).getJCas();

            assertThat(cas).isNotNull();
            assertThat(cas.getDocumentText()).isEqualTo("Test");
            assertThat(storageService.getCasFile(doc, INITIAL_CAS_PSEUDO_USER)).exists();
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
            assertThat(storageService.getCasFile(sourceDocument, user.getUsername())).exists();
        }
    }

    @Test
    public void testHighConcurrencySingleUser() throws Exception
    {
        when(importExportService.importCasFromFile(any(File.class), any(Project.class), any(),
                any())).then(_invocation -> {
                    CAS cas = createCas(mergeTypeSystems(
                            asList(createTypeSystemDescription(), getInternalTypeSystem())));
                    cas.setDocumentText(repeat("This is a test.\n", 100_000));
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
        int iterations = 100;
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
        when(importExportService.importCasFromFile(any(File.class), any(Project.class), any(),
                any())).then(_invocation -> {
                    CAS cas = createCas(mergeTypeSystems(
                            asList(createTypeSystemDescription(), getInternalTypeSystem())));
                    cas.setDocumentText(repeat("This is a test.\n", 100_000));
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
        int iterations = 100;
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
            log.info("running {}  complete {}%  rw {}  ro {}  un {}  xx {} XX {}", running,
                    (writeCounter.get() * 100) / (userCount * threadGroupCount * iterations),
                    writeCounter, managedReadCounter, unmanagedReadCounter, deleteCounter,
                    deleteInitialCounter);
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
            for (int n = 0; n < repeat; n++) {
                if (exception.get()) {
                    return;
                }

                try (CasStorageSession session = openNested()) {
                    CAS cas = sut.readAnnotationCas(doc, user);
                    Thread.sleep(50);
                    sut.writeAnnotationCas(cas, doc, user, false);
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
