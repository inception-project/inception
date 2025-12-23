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
package de.tudarmstadt.ukp.inception.documents.service;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.FileSystemUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.constraints.config.ConstraintsServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;

@EnableAutoConfiguration
@DataJpaTest(showSql = false, //
        properties = { //
                "spring.main.banner-mode=off", //
                "repository.path=" + DocumentServiceImplDatabaseTest.TEST_OUTPUT_FOLDER })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@Import({ //
        EventLoggingAutoConfiguration.class, //
        ConstraintsServiceAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class })
public class DocumentServiceImplDatabaseTest
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/DocumentServiceImplDatabaseTest";

    private @Autowired ProjectService projectService;
    private @Autowired UserDao userRepository;
    private @Autowired DocumentService sut;
    private @Autowired jakarta.persistence.EntityManager entityManager;

    private User annotator1;
    private Project project;

    @BeforeAll
    public static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @BeforeEach
    public void setup() throws Exception
    {
        annotator1 = userRepository.create(new User("anno1"));

        project = projectService.createProject(new Project("project"));
        projectService.assignRole(project, annotator1, ANNOTATOR);
    }

    @Test
    public void testThatAnnotationDocumentsForNonExistingUserAreNotReturned() throws Exception
    {
        var doc = sut.createSourceDocument(new SourceDocument("doc", project, "text"));

        var ann = sut.createOrUpdateAnnotationDocument(
                new AnnotationDocument(annotator1.getUsername(), doc));

        assertThat(sut.listAnnotationDocuments(doc))
                .as("As long as the user exists, the annotation document must be found")
                .containsExactly(ann);

        userRepository.delete(annotator1);

        assertThat(sut.listAnnotationDocuments(doc))
                .as("When the user is deleted, the document must no longer be found") //
                .isEmpty();
    }

    @Test
    public void thatCreateOrGetAnnotationDocumentsWithAnnotationSetsCreatesDocuments()
        throws Exception
    {
        var doc = sut.createSourceDocument(new SourceDocument("doc1", project, "text"));

        var set1 = AnnotationSet.forUser(annotator1);
        var set2 = AnnotationSet.forTest("other");

        var annDocs = sut.createOrGetAnnotationDocuments(doc, asList(set1, set2));

        assertThat(annDocs).hasSize(2);
        assertThat(sut.getAnnotationDocument(doc, set1)).isNotNull();
        assertThat(sut.getAnnotationDocument(doc, set2)).isNotNull();
    }

    @Test
    public void thatCreateOrGetAnnotationDocumentsForMultipleDocumentsCreatesDocuments()
        throws Exception
    {
        var doc1 = sut.createSourceDocument(new SourceDocument("doc1.txt", project, "text"));
        var doc2 = sut.createSourceDocument(new SourceDocument("doc2.txt", project, "text"));

        var set = AnnotationSet.forUser(annotator1);

        var annDocs = sut.createOrGetAnnotationDocuments(asList(doc1, doc2), set);

        assertThat(annDocs).hasSize(2);
        assertThat(sut.getAnnotationDocument(doc1, set)).isNotNull();
        assertThat(sut.getAnnotationDocument(doc2, set)).isNotNull();
    }

    @Test
    public void thatListAnnotationDocumentsProjectAndSetWorks() throws Exception
    {
        var doc1 = sut.createSourceDocument(new SourceDocument("doc1", project, "text"));
        var doc2 = sut.createSourceDocument(new SourceDocument("doc2", project, "text"));

        var set = AnnotationSet.forUser(annotator1);

        sut.createOrGetAnnotationDocument(doc1, set);
        sut.createOrGetAnnotationDocument(doc2, set);

        var docs = sut.listAnnotationDocuments(project, set);
        assertThat(docs).isNotEmpty();
        assertThat(docs).allMatch(a -> a.getAnnotationSet().equals(set));
    }

    @Test
    public void thatListAllDocumentsProjectAndSetReturnsNullForMissingDocs() throws Exception
    {
        var doc1 = sut.createSourceDocument(new SourceDocument("doc1", project, "text"));
        var doc2 = sut.createSourceDocument(new SourceDocument("doc2", project, "text"));

        var set = AnnotationSet.forUser(annotator1);

        var ann = sut.createOrGetAnnotationDocument(doc1, set);

        var map = sut.listAllDocuments(project, set);
        assertThat(map).containsEntry(doc1, ann);
        assertThat(map).containsKey(doc2);
        assertThat(map.get(doc2)).isNull();
    }

    @Test
    public void thatExistsAnnotationDocumentWithSetWorks() throws Exception
    {
        var doc = sut.createSourceDocument(new SourceDocument("docx", project, "text"));
        var set = AnnotationSet.forUser(annotator1);

        assertThat(sut.existsAnnotationDocument(doc, set)).isFalse();
        sut.createOrGetAnnotationDocument(doc, set);
        assertThat(sut.existsAnnotationDocument(doc, set)).isTrue();
    }

    @Test
    public void thatExistsSourceDocumentWorks() throws Exception
    {
        var doc1 = sut.createSourceDocument(new SourceDocument("s1", project, "text"));
        var doc2 = sut.createSourceDocument(new SourceDocument("s2", project, "text"));

        assertThat(sut.existsSourceDocument(project)).isTrue();
        assertThat(sut.existsSourceDocument(project, doc1.getName())).isTrue();
        assertThat(sut.existsSourceDocument(project, doc2.getName())).isTrue();
        assertThat(sut.existsSourceDocument(project, "not-found.txt")).isFalse();
    }

    @Test
    public void thatRenameSourceDocumentRenamesFileAndAnnDocs() throws Exception
    {
        var doc = sut
                .createSourceDocument(new SourceDocument("old.txt", project, TextFormatSupport.ID));
        try (var session = CasStorageSession.open()) {
            sut.uploadSourceDocument(toInputStream("abc", UTF_8), doc);
        }

        var ann = sut.createOrGetAnnotationDocument(doc, AnnotationSet.forUser(annotator1));
        assertThat(ann.getDocument().getName()).isEqualTo("old.txt");

        sut.renameSourceDocument(doc, "newname.txt");

        // document stored with new name
        var docFromDb = sut.getSourceDocument(project, "newname.txt");
        assertThat(docFromDb).isNotNull();
        assertThat(docFromDb.getName()).isEqualTo("newname.txt");

        // annotation document name also updated
        var annFound = sut.getAnnotationDocument(docFromDb, AnnotationSet.forUser(annotator1));
        assertThat(annFound).isNotNull();
        assertThat(annFound.getDocument().getName()).isEqualTo("newname.txt");
    }

    @Test
    public void thatExportSourceDocumentsCreatesZipWithEntries() throws Exception
    {
        var d1 = sut.createSourceDocument(new SourceDocument("a1.txt", project, "text"));
        var d2 = sut.createSourceDocument(new SourceDocument("a2.txt", project, "text"));
        try (var session = CasStorageSession.open()) {
            sut.uploadSourceDocument(toInputStream("1", UTF_8), d1);
            sut.uploadSourceDocument(toInputStream("2", UTF_8), d2);
        }

        var baos = new java.io.ByteArrayOutputStream();
        sut.exportSourceDocuments(baos, asList(d1, d2));

        var zip = new java.util.zip.ZipInputStream(
                new java.io.ByteArrayInputStream(baos.toByteArray()));
        var names = new java.util.ArrayList<String>();
        java.util.zip.ZipEntry e;
        while ((e = zip.getNextEntry()) != null) {
            names.add(e.getName());
            zip.closeEntry();
        }
        assertThat(names).containsExactlyInAnyOrder("a1.txt", "a2.txt");
    }

    @Test
    public void thatInitialCasAndSizeExist() throws Exception
    {
        try (var session = CasStorageSession.open()) {
            var doc = sut.createSourceDocument(
                    new SourceDocument("init.txt", project, TextFormatSupport.ID));
            var cas = sut.createOrReadInitialCas(doc);
            assertThat(cas).isNotNull();
            assertThat(sut.existsInitialCas(doc)).isTrue();
            assertThat(sut.getInitialCasFileSize(doc)).isPresent();
        }
    }

    @Test
    public void thatDeleteAnnotationCasDeletesCasFile() throws Exception
    {
        try (var session = CasStorageSession.open()) {
            var doc = sut.createSourceDocument(
                    new SourceDocument("delcas.txt", project, TextFormatSupport.ID));
            var set = AnnotationSet.forUser(annotator1);
            var ann = sut.createOrGetAnnotationDocument(doc, set);
            assertThat(ann).isNotNull();
            assertThat(ann.getDocument().getName()).isEqualTo("delcas.txt");
            var cas = sut.readAnnotationCas(doc, set);
            assertThat(cas).isNotNull();
            assertThat(sut.existsCas(doc, set)).isTrue();

            sut.deleteAnnotationCas(doc, set);
            assertThat(sut.existsCas(doc, set)).isFalse();
        }
    }

    @Test
    public void thatReadAllCasesSharedNoUpgradeReturnsCasesForMultipleUsers() throws Exception
    {
        try (var session = CasStorageSession.open()) {

            var other = userRepository.create(new User("anno2"));
            projectService.assignRole(project, other, ANNOTATOR);

            var doc = sut.createSourceDocument(
                    new SourceDocument("multi.txt", project, TextFormatSupport.ID));
            var set1 = AnnotationSet.forUser(annotator1);
            var set2 = AnnotationSet.forUser(other);
            sut.createOrGetAnnotationDocument(doc, set1);
            sut.createOrGetAnnotationDocument(doc, set2);

            var cas = sut.readAnnotationCas(doc, set1);
            sut.writeAnnotationCas(cas, doc, set1);
            var cas2 = sut.readAnnotationCas(doc, set2);
            sut.writeAnnotationCas(cas2, doc, set2);

            var map = sut.readAllCasesSharedNoUpgrade(doc, annotator1.getUsername(),
                    other.getUsername());
            assertThat(map).hasSize(2);
            assertThat(map).containsKeys(annotator1.getUsername(), other.getUsername());
        }
    }

    @Test
    public void thatAnnotationCasTimestampAndVerifyWorks() throws Exception
    {
        try (var session = CasStorageSession.open()) {

            var doc = sut.createSourceDocument(
                    new SourceDocument("t1.txt", project, TextFormatSupport.ID));
            var s = AnnotationSet.forUser(annotator1);
            sut.createOrGetAnnotationDocument(doc, s);
            var cas = sut.readAnnotationCas(doc, s);
            sut.writeAnnotationCas(cas, doc, s);

            var stamp = sut.getAnnotationCasTimestamp(doc, s);
            assertThat(stamp).isPresent();
            var verify = sut.verifyAnnotationCasTimestamp(doc, s, stamp.get(), "test");
            assertThat(verify).isPresent();
        }
    }

    @Test
    public void thatListFinishedAnnotationDocumentsAndExistsWorks() throws Exception
    {
        var doc = sut.createSourceDocument(new SourceDocument("fin.txt", project, "text"));
        var set = AnnotationSet.forUser(annotator1);
        var ann = sut.createOrGetAnnotationDocument(doc, set);

        sut.setAnnotationDocumentState(ann, AnnotationDocumentState.FINISHED);

        assertThat(sut.existsFinishedAnnotation(doc)).isTrue();
        assertThat(sut.listFinishedAnnotationDocuments(doc)).isNotEmpty();
        assertThat(sut.existsFinishedAnnotation(project)).isTrue();
        assertThat(sut.listFinishedAnnotationDocuments(project)).isNotEmpty();
    }

    @Test
    public void thatListSupportedSourceDocumentsFiltersUnsupported() throws Exception
    {
        var d1 = sut
                .createSourceDocument(new SourceDocument("f1.txt", project, TextFormatSupport.ID));
        var d2 = sut.createSourceDocument(new SourceDocument("f2.txt", project, "unsupported"));

        var res = sut.listSupportedSourceDocuments(project);
        assertThat(res).contains(d1);
        assertThat(res).doesNotContain(d2);
    }

    @Test
    public void thatRemoveSourceDocumentRemovesDocAndAnnDocs() throws Exception
    {
        try (var session = CasStorageSession.open()) {
            var doc = sut.createSourceDocument(
                    new SourceDocument("rem.txt", project, TextFormatSupport.ID));
            var set = AnnotationSet.forUser(annotator1);
            var ann = sut.createOrGetAnnotationDocument(doc, set);
            sut.uploadSourceDocument(toInputStream("abc", UTF_8), doc);
            sut.removeSourceDocument(doc);

            assertThat(sut.listSourceDocuments(project)).doesNotContain(doc);
            assertThat(sut.listAllAnnotationDocuments(doc)).isEmpty();
        }
    }

    @Test
    public void thatUpdateStateUpdatedDirectlyWorks() throws Exception
    {
        var doc = sut
                .createSourceDocument(new SourceDocument("ust.txt", project, TextFormatSupport.ID));
        var set = AnnotationSet.forUser(annotator1);
        var ann = sut.createOrGetAnnotationDocument(doc, set);

        var d = new java.util.Date(1620000000000L);
        sut.updateAnnotationDocumentStateUpdatedDirectly(ann.getId(), d);
        // Flush pending changes, evict the persistence context and re-fetch to ensure we
        // read the value updated directly in the DB
        entityManager.flush();
        entityManager.clear();
        var annReloaded = sut.getAnnotationDocument(doc, set);
        assertThat(annReloaded.getStateUpdated().getTime()).isEqualTo(d.getTime());

        var src = sut.getSourceDocument(project, "ust.txt");
        sut.updateSourceDocumentStateUpdatedDirectly(src.getId(), d);
        // Flush pending changes, evict the persistence context and re-fetch to ensure we
        // read the value updated directly in the DB
        entityManager.flush();
        entityManager.clear();
        var srcReloaded = sut.getSourceDocument(project, "ust.txt");
        assertThat(srcReloaded.getStateUpdated().getTime()).isEqualTo(d.getTime());
    }

    @Test
    public void thatExplicitUserActionsSetAnnotatorState()
    {
        var doc = sut.createSourceDocument(new SourceDocument("doc", project, "text"));

        var ann = sut.createOrUpdateAnnotationDocument(
                new AnnotationDocument(annotator1.getUsername(), doc));

        sut.setAnnotationDocumentState(ann, AnnotationDocumentState.IGNORE);
        assertThat(ann.getState()) //
                .as("Implicit actions update the effective state") //
                .isEqualTo(AnnotationDocumentState.IGNORE);
        assertThat(ann.getAnnotatorState())
                .as("Implicit actions cause the annotator state not to be set") //
                .isNull();

        sut.setAnnotationDocumentState(ann, AnnotationDocumentState.FINISHED,
                EXPLICIT_ANNOTATOR_USER_ACTION);
        assertThat(ann.getState()) //
                .as("Explicit user actions update the effective state") //
                .isEqualTo(AnnotationDocumentState.FINISHED);
        assertThat(ann.getAnnotatorState())
                .as("Explicit user actions cause the annotator state to be set")
                .isEqualTo(AnnotationDocumentState.FINISHED);

        sut.setAnnotationDocumentState(ann, AnnotationDocumentState.NEW);
        assertThat(ann.getAnnotatorState()) //
                .as("Resetting the document clears the annotator state") //
                .isNull();
        assertThat(ann.getState()) //
                .as("Implicit actions update the effective state") //
                .isEqualTo(AnnotationDocumentState.NEW);

        sut.setAnnotationDocumentState(ann, AnnotationDocumentState.IN_PROGRESS,
                EXPLICIT_ANNOTATOR_USER_ACTION);
        sut.setAnnotationDocumentState(ann, AnnotationDocumentState.FINISHED);
        assertThat(ann.getAnnotatorState())
                .as("Implicit actions cause the annotator state not to be changed") //
                .isEqualTo(AnnotationDocumentState.IN_PROGRESS);
        assertThat(ann.getState()) //
                .as("Implicit actions update the effective state") //
                .isEqualTo(AnnotationDocumentState.FINISHED);

        sut.setAnnotationDocumentState(ann, AnnotationDocumentState.FINISHED,
                EXPLICIT_ANNOTATOR_USER_ACTION);
        sut.setAnnotationDocumentState(ann, AnnotationDocumentState.IN_PROGRESS);
        assertThat(ann.getAnnotatorState())
                .as("Manager send document back to annotation updates annotator state") //
                .isEqualTo(AnnotationDocumentState.IN_PROGRESS);
        assertThat(ann.getState()) //
                .as("Manager send document back to annotation updates effective state") //
                .isEqualTo(AnnotationDocumentState.IN_PROGRESS);

        sut.setAnnotationDocumentState(ann, AnnotationDocumentState.IN_PROGRESS,
                EXPLICIT_ANNOTATOR_USER_ACTION);
        sut.setAnnotationDocumentState(ann, AnnotationDocumentState.IGNORE);
        assertThat(ann.getAnnotatorState())
                .as("Manager locking document does not change the annotator state") //
                .isEqualTo(AnnotationDocumentState.IN_PROGRESS);
        assertThat(ann.getState()) //
                .as("Manager locking document updates effective state") //
                .isEqualTo(AnnotationDocumentState.IGNORE);
    }

    @Test
    public void thatResettingADocumentSetsAlsoResetsTheStates() throws Exception
    {
        var doc = sut
                .createSourceDocument(new SourceDocument("doc.txt", project, TextFormatSupport.ID));

        var ann = sut.createOrUpdateAnnotationDocument(
                new AnnotationDocument(annotator1.getUsername(), doc));

        try (var session = CasStorageSession.open()) {
            sut.uploadSourceDocument(toInputStream("This is a test.", UTF_8), doc);
        }

        sut.setAnnotationDocumentState(ann, AnnotationDocumentState.IN_PROGRESS,
                EXPLICIT_ANNOTATOR_USER_ACTION);
        sut.setAnnotationDocumentState(ann, AnnotationDocumentState.IGNORE);

        try (var session = CasStorageSession.open()) {
            sut.resetAnnotationCas(doc, annotator1);
        }

        assertThat(ann.getState()).as("Resetting CAS sets effective state to NEW") //
                .isEqualTo(AnnotationDocumentState.NEW);
        assertThat(ann.getAnnotatorState()) //
                .as("Resetting CAS clears the annotator state") //
                .isNull();
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        @Bean
        DocumentImportExportService documentImportExportService(
                AnnotationSchemaService aSchemaService)
            throws Exception
        {
            var internalTsd = CasMetadataUtils.getInternalTypeSystem();
            var globalTsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
            var tsd = CasCreationUtils.mergeTypeSystems(asList(globalTsd, internalTsd));
            var importService = mock(DocumentImportExportService.class);
            when(importService.importCasFromFileNoChecks(any(), any(), any()))
                    .thenReturn(CasCreationUtils.createCas(tsd, null, null, null));
            var formatSupport = mock(FormatSupport.class);
            when(importService.getFormatById(TextFormatSupport.ID))
                    .thenReturn(java.util.Optional.of(formatSupport));
            return importService;
        }
    }
}
