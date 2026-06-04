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
package de.tudarmstadt.ukp.inception.curation.service;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession.openNested;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils.getInternalTypeSystem;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createCas;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.config.ConstraintsServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.config.CurationDocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;

@EnableAutoConfiguration
@DataJpaTest(showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@Import({ //
        EventLoggingAutoConfiguration.class, //
        ConstraintsServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        CurationDocumentServiceAutoConfiguration.class })
class CurationDocumentServiceImplTest
{
    static @TempDir Path tempFolder;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry)
    {
        registry.add("repository.path", () -> tempFolder.toAbsolutePath().toString());
    }

    private @Autowired TestEntityManager testEntityManager;

    private @Autowired DocumentService documentService;
    private @Autowired CasStorageService casStorageService;
    private @Autowired CurationDocumentServiceImpl sut;

    private User current;
    private User beate;
    private User kevin;
    private Project testProject;
    private SourceDocument testDocument;

    @BeforeEach
    void setup() throws Exception
    {
        // create users
        current = new User("current", ROLE_USER);
        beate = new User("beate", ROLE_USER);
        kevin = new User("kevin", ROLE_USER);
        testEntityManager.persist(current);
        testEntityManager.persist(beate);
        testEntityManager.persist(kevin);

        // create project
        testProject = new Project("test-project");
        testEntityManager.persist(testProject);
        testEntityManager.persist(new ProjectPermission(testProject, "beate", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "kevin", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "beate", CURATOR));

        testDocument = new SourceDocument("doc", testProject, "text");
        testEntityManager.persist(testDocument);
    }

    @Test
    void testListCuratableSourceDocuments_legacy()
    {
        var ann = documentService.createOrUpdateAnnotationDocument(
                new AnnotationDocument(beate.getUsername(), testDocument));

        assertThat(sut.listCuratableSourceDocuments_legacy(testProject))
                .as("No curatable documents as long as no document is marked as finished")
                .isEmpty();

        documentService.setAnnotationDocumentState(ann, FINISHED);

        assertThat(sut.listCuratableSourceDocuments_legacy(testProject)) //
                .as("Finished documents become curatable") //
                .contains(testDocument);
    }

    @Test
    void listCuratableSourceDocuments_legacy_ShouldIncludeFormerAnnotatorsDocuments()
    {
        // "current" has a user account but no ANNOTATOR permission in the project (e.g. removed
        // from the project or role changed). A document with only their finished data must still
        // surface for curation so their data stays accessible.
        var ann = documentService
                .createOrUpdateAnnotationDocument(new AnnotationDocument("current", testDocument));
        documentService.setAnnotationDocumentState(ann, FINISHED);

        assertThat(sut.listCuratableSourceDocuments_legacy(testProject)) //
                .as("Documents with only former-annotator data are curatable") //
                .contains(testDocument);
    }

    @Test
    void listCuratableSourceDocuments_legacy_ShouldIncludeIgnoredDocumentsOnlyWhenTheyHaveData()
        throws Exception
    {
        var withData = new SourceDocument("withData", testProject, "text");
        var withoutData = new SourceDocument("withoutData", testProject, "text");
        testEntityManager.persist(withData);
        testEntityManager.persist(withoutData);

        // kevin opened "withData" (a CAS exists) before setting it to IGNORE.
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("kevin") //
                .forDocument(withData) //
                .withAnnotatorState(IGNORE) //
                .build());
        writeCasFor(withData, "kevin");

        // kevin set "withoutData" to IGNORE without ever opening it (no CAS).
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("kevin") //
                .forDocument(withoutData) //
                .withAnnotatorState(IGNORE) //
                .build());

        // A CAS storage session is required so the CAS-existence check can run (as in the real
        // curation flow, which always runs within such a session).
        try (var session = openNested(true)) {
            assertThat(sut.listCuratableSourceDocuments_legacy(testProject)) //
                    .as("IGNORE documents are curatable only when the annotator produced data") //
                    .containsExactly(withData);
        }
    }

    @Test
    void testListCuratableSourceDocuments_new()
    {
        assertThat(sut.listCuratableSourceDocuments_new(testProject))
                .as("No curatable documents as long as source document is not marked as finished")
                .isEmpty();

        documentService.setSourceDocumentState(testDocument, ANNOTATION_FINISHED);

        assertThat(sut.listCuratableSourceDocuments_new(testProject)) //
                .as("Source documents marked ANNOTATION_FINISHED become curatable") //
                .contains(testDocument);
    }

    @Test
    void listCuratableUsers_ShouldIncludeIgnoredDocumentsOnlyWhenTheyHaveData() throws Exception
    {
        // beate finished the document - a finished document always has a CAS, so she is curatable.
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("beate") //
                .forDocument(testDocument) //
                .withState(FINISHED) //
                .build());

        // kevin opened the document (a CAS exists) and then set it to IGNORE - his partial work is
        // still curatable.
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("kevin") //
                .forDocument(testDocument) //
                .withAnnotatorState(IGNORE) //
                .build());
        writeCasFor(testDocument, "kevin");

        // current set the document to IGNORE without ever opening it (no CAS) - there is nothing to
        // curate, so he is not curatable.
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("current") //
                .forDocument(testDocument) //
                .withAnnotatorState(IGNORE) //
                .build());

        // A CAS storage session is required so the CAS-existence check can run (as in the real
        // curation flow, which always runs within such a session).
        try (var session = openNested(true)) {
            assertThat(sut.listCuratableUsers(testDocument)) //
                    .as("IGNORE documents are only curatable when the annotator produced data") //
                    .containsExactly(beate, kevin);
        }
    }

    private void writeCasFor(SourceDocument aDocument, String aUsername) throws Exception
    {
        try (var session = openNested(true)) {
            var tsd = mergeTypeSystems(
                    asList(createTypeSystemDescription(), getInternalTypeSystem()));
            var cas = createCas(tsd);
            session.add(AnnotationSet.forUser(aUsername), EXCLUSIVE_WRITE_ACCESS, cas);
            casStorageService.writeCas(aDocument, cas, AnnotationSet.forUser(aUsername));
        }
    }

    @Test
    void listCuratableUsers_ShouldIncludeFormerAnnotatorsButNotDeletedAccounts()
    {
        // "beate" is a current annotator. "current" has a user account but no ANNOTATOR permission
        // in the project (e.g. removed from the project or role changed). "ghost" left data behind
        // but the user account was deleted entirely (no User row).
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("beate") //
                .forDocument(testDocument) //
                .withState(FINISHED) //
                .build());

        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("current") //
                .forDocument(testDocument) //
                .withState(FINISHED) //
                .build());

        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("ghost") //
                .forDocument(testDocument) //
                .withState(FINISHED) //
                .build());

        var curatableUsers = sut.listCuratableUsers(testDocument);

        assertThat(curatableUsers) //
                .as("Former annotators with data are curatable; deleted accounts are not") //
                .containsExactly(beate, current);
    }

    @Test
    void listFinishedUsers_ShouldReturnFinishedUsers()
    {
        // create finished annotation documents
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("beate") //
                .forDocument(testDocument) //
                .withState(FINISHED) //
                .build());

        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("kevin") //
                .forDocument(testDocument) //
                .withState(FINISHED) //
                .build());

        var finishedUsers = sut.listCuratableUsers(testDocument);

        assertThat(finishedUsers).containsExactly(beate, kevin);
    }

    @SpringBootConfiguration
    static class TestContext
    {
        @Bean
        DocumentImportExportService documentImportExportService(
                AnnotationSchemaService aSchemaService)
            throws Exception
        {
            var tsd = createTypeSystemDescription();
            var importService = mock(DocumentImportExportService.class);
            when(importService.importCasFromFile(any(), any(), any(), any()))
                    .thenReturn(CasCreationUtils.createCas(tsd, null, null, null));
            return importService;
        }
    }
}
