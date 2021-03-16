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
import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.NO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_NON_INITIALIZING_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.dao.CasMetadataUtils.getInternalTypeSystem;
import static de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession.openNested;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasSessionException;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CasStorageServiceImplTest
{
    private Logger log = LoggerFactory.getLogger(getClass());

    private AtomicBoolean exception = new AtomicBoolean(false);
    private AtomicBoolean rwTasksCompleted = new AtomicBoolean(false);
    private AtomicInteger managedReadCounter = new AtomicInteger(0);
    private AtomicInteger unmanagedReadCounter = new AtomicInteger(0);
    private AtomicInteger unmanagedNonInitializingReadCounter = new AtomicInteger(0);
    private AtomicInteger deleteCounter = new AtomicInteger(0);
    private AtomicInteger deleteInitialCounter = new AtomicInteger(0);
    private AtomicInteger writeCounter = new AtomicInteger(0);

    private CasStorageServiceImpl sut;
    private BackupProperties backupProperties;
    private RepositoryProperties repositoryProperties;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
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

        backupProperties = new BackupProperties();

        repositoryProperties = new RepositoryProperties();
        repositoryProperties.setPath(testFolder.newFolder());

        sut = new CasStorageServiceImpl(null, null, repositoryProperties, backupProperties);
    }

    @Test
    public void testWriteReadExistsDeleteCas() throws Exception
    {
        try (CasStorageSession casStorageSession = openNested(true)) {
            // Setup fixture
            SourceDocument doc = makeSourceDocument(1l, 1l, "test");
            JCas templateCas = JCasFactory.createText("This is a test");
            casStorageSession.add("cas", EXCLUSIVE_WRITE_ACCESS, templateCas.getCas());
            String user = "test";

            sut.writeCas(doc, templateCas.getCas(), user);
            assertThat(sut.getCasFile(doc, user)).exists();
            assertThat(sut.existsCas(doc, user)).isTrue();

            // Actual test
            CAS cas = sut.readCas(doc, user);
            assertThat(cas.getDocumentText()).isEqualTo(templateCas.getDocumentText());

            sut.deleteCas(doc, user);
            assertThat(sut.getCasFile(doc, user)).doesNotExist();
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

            JCas cas = JCasFactory.createJCas(mergeTypeSystems(typeSystems));
            casStorageSession.add("cas", EXCLUSIVE_WRITE_ACCESS, cas.getCas());

            SourceDocument doc = makeSourceDocument(2l, 2l, "test");
            String user = "test";

            sut.writeCas(doc, cas.getCas(), user);

            JCas cas2 = sut.readCas(doc, user).getJCas();

            List<CASMetadata> cmds = new ArrayList<>(select(cas2, CASMetadata.class));
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
        try (CasStorageSession casStorageSession = openNested(true)) {
            // Setup fixture
            SourceDocument doc = makeSourceDocument(3l, 3l, "test");
            String user = "test";
            String text = "This is a test";
            createCasFile(doc, user, text);

            // Actual test
            JCas cas = sut.readCas(doc, user).getJCas();
            assertThat(cas.getDocumentText()).isEqualTo(text);

            sut.deleteCas(doc, user);
            assertThat(sut.getCasFile(doc, user)).doesNotExist();
            assertThat(sut.existsCas(doc, user)).isFalse();
        }
    }

    @Test
    public void testThatLayerChangeEventInvalidatesCachedCas() throws Exception
    {
        // Setup fixture
        SourceDocument doc = makeSourceDocument(4l, 4l, "test");
        String user = "test";
        try (CasStorageSession session = openNested(true)) {
            String text = "This is a test";
            createCasFile(doc, user, text);
        }

        // Actual test
        int casIdentity1;
        try (CasStorageSession session = openNested(true)) {
            JCas cas = sut.readCas(doc, user).getJCas();
            casIdentity1 = System.identityHashCode(cas);
        }

        int casIdentity2;
        try (CasStorageSession session = openNested(true)) {
            JCas cas = sut.readCas(doc, user).getJCas();
            casIdentity2 = System.identityHashCode(cas);
        }

        sut.beforeLayerConfigurationChanged(
                new LayerConfigurationChangedEvent(this, doc.getProject()));

        int casIdentity3;
        try (CasStorageSession session = openNested(true)) {
            JCas cas = sut.readCas(doc, user).getJCas();
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
        SourceDocument doc = makeSourceDocument(5l, 5l, "test");
        String user = "test";
        File casFile = sut.getCasFile(doc, user);

        try (CasStorageSession session = openNested(true)) {
            createCasFile(doc, user, "This is a test");
            assertThat(casFile).exists();
        }

        try (CasStorageSession casStorageSession = openNested(true)) {
            CAS mainCas = sut.readCas(doc, user, EXCLUSIVE_WRITE_ACCESS);

            casFile.setLastModified(casFile.lastModified() + 10_000);
            long timestamp = casFile.lastModified();

            assertThatExceptionOfType(IOException.class)
                    .isThrownBy(() -> sut.writeCas(doc, mainCas, user))
                    .withMessageContaining("concurrent modification");

            assertThat(casFile).exists();
            assertThat(casFile.lastModified()).isEqualTo(timestamp);
        }
    }

    @Test
    public void testRestorationOfCasWhenSaveFails() throws Exception
    {
        try (CasStorageSession casStorageSession = openNested(true)) {
            // Setup fixture
            SourceDocument doc = makeSourceDocument(6l, 6l, "test");
            String user = "test";
            File casFile = sut.getCasFile(doc, user);

            long casFileSize;
            long casFileLastModified;

            try (CasStorageSession session = openNested(true)) {
                createCasFile(doc, user, "This is a test");
                assertThat(casFile).exists();
                casFileSize = casFile.length();
                casFileLastModified = casFile.lastModified();
            }

            CAS mainCas = sut.readCas(doc, user, EXCLUSIVE_WRITE_ACCESS);

            // Wrap the CAS in a proxy so that UIMA cannot serialize it
            CAS guardedCas = (CAS) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[] { CAS.class },
                    (proxy, method, args) -> method.invoke(mainCas, args));

            assertThatExceptionOfType(IOException.class).as(
                    "Saving fails because UIMA cannot cast the proxied CAS to something serializable")
                    .isThrownBy(() -> sut.writeCas(doc, guardedCas, user))
                    .withRootCauseInstanceOf(ClassCastException.class);

            assertThat(casFile).exists().hasSize(casFileSize);
            assertThat(casFile.lastModified()).isEqualTo(casFileLastModified);
            assertThat(new File(casFile.getParentFile(), user + ".ser.old")).doesNotExist();
        }
    }

    @Test
    public void testHighConcurrencyIncludingDeletion() throws Exception
    {
        CasProvider initializer = () -> {
            try {
                CAS cas = createCas(mergeTypeSystems(
                        asList(createTypeSystemDescription(), getInternalTypeSystem())));
                cas.setDocumentText(repeat("This is a test.\n", 100_000));
                return cas;
            }
            catch (ResourceInitializationException e) {
                throw new IOException(e);
            }
        };

        SourceDocument doc = makeSourceDocument(7l, 7l, "doc");
        String user = "annotator";

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

        log.info("---- Starting all threads ----");
        tasks.forEach(Thread::start);

        log.info("---- Wait for primary threads to complete ----");
        boolean done = false;
        while (!done) {
            long running = primaryTasks.stream().filter(Thread::isAlive).count();
            done = running == 0l;
            sleep(1000);
            log.info("running {}  complete {}%  rw {}  ro {}  un {}  uni {}  xx {} XX {}", running,
                    (writeCounter.get() * 100) / (threadGroupCount * iterations), writeCounter,
                    managedReadCounter, unmanagedReadCounter, unmanagedNonInitializingReadCounter,
                    deleteCounter, deleteInitialCounter);
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
    public void testHighConcurrencyWithoutDeletion() throws Exception
    {
        CasProvider initializer = () -> {
            try {
                CAS cas = createCas(mergeTypeSystems(
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

        SourceDocument doc = makeSourceDocument(8l, 8l, "doc");
        String user = "annotator";
        try (CasStorageSession session = openNested()) {
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
        for (int n = 0; n < threadGroupCount; n++) {
            ExclusiveReadWriteTask rw = new ExclusiveReadWriteTask(n, doc, user, badSeed,
                    iterations);
            primaryTasks.add(rw);
            tasks.add(rw);

            Thread ro = new SharedReadOnlyTask(n, doc, user, badSeed);
            secondaryTasks.add(ro);
            tasks.add(ro);

            Thread un = new UnmanagedTask(n, doc, user, badSeed);
            secondaryTasks.add(un);
            tasks.add(un);

            Thread uni = new UnmanagedNonInitializingTask(n, doc, user);
            secondaryTasks.add(uni);
            tasks.add(uni);
        }

        log.info("---- Starting all threads ----");
        tasks.forEach(Thread::start);

        log.info("---- Wait for primary threads to complete ----");
        boolean done = false;
        while (!done) {
            long running = primaryTasks.stream().filter(Thread::isAlive).count();
            done = running == 0l;
            sleep(1000);
            log.info("running {}  complete {}%  rw {}  ro {}  un {}  uni {}", running,
                    (writeCounter.get() * 100) / (threadGroupCount * iterations), writeCounter,
                    managedReadCounter, unmanagedReadCounter, unmanagedNonInitializingReadCounter);
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
            for (int n = 0; n < repeat; n++) {
                if (exception.get()) {
                    return;
                }

                try (CasStorageSession session = openNested()) {
                    CAS cas = sut.readOrCreateCas(doc, user, FORCE_CAS_UPGRADE, initializer,
                            EXCLUSIVE_WRITE_ACCESS);
                    Thread.sleep(50);
                    AnnotationFS fs = cas.createAnnotation(cas.getAnnotationType(), 0, 10);
                    cas.addFsToIndexes(fs);
                    log.info("CAS size: {}", cas.getAnnotationIndex().size());
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
            while (!(exception.get() || rwTasksCompleted.get())) {
                try (CasStorageSession session = openNested()) {
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
            while (!(exception.get() || rwTasksCompleted.get())) {
                try (CasStorageSession session = openNested()) {
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
            while (!(exception.get() || rwTasksCompleted.get())) {
                try (CasStorageSession session = openNested()) {
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
            while (!(exception.get() || rwTasksCompleted.get())) {
                try (CasStorageSession session = openNested()) {
                    sut.readCas(doc, user, UNMANAGED_NON_INITIALIZING_ACCESS);
                    unmanagedNonInitializingReadCounter.incrementAndGet();
                    Thread.sleep(50);
                }
                catch (FileNotFoundException e) {
                    // We ignore the FileNotFoundException, this could be hapening if the deleter
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
            CAS cas = CasFactory.createCas(mergeTypeSystems(
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
        JCas casTemplate = sut.readOrCreateCas(doc, user, NO_CAS_UPGRADE, () -> makeCas(text),
                EXCLUSIVE_WRITE_ACCESS).getJCas();
        assertThat(sut.getCasFile(doc, user)).exists();
        assertThat(sut.existsCas(doc, user)).isTrue();

        return casTemplate;
    }

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
}
