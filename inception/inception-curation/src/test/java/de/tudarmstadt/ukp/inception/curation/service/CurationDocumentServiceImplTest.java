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
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils.getInternalTypeSystem;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
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
import org.junit.jupiter.api.Nested;
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
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.config.CurationDocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
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
    private @Autowired DocumentAccess documentAccess;
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
    void listCuratableSourceDocuments_legacy_ShouldNotIncludeIgnoredDocuments() throws Exception
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

        // IGNORE says "I am not going to annotate this", not "this is done".
        try (var session = openNested(true)) {
            assertThat(sut.listCuratableSourceDocuments_legacy(testProject)) //
                    .as("IGNORE documents are not curatable, even with leftover data") //
                    .isEmpty();
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

    @Test
    void isDocumentCuratable_ShouldRefuseDocumentThatIsNotAnnotationFinished() throws Exception
    {
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("beate") //
                .forDocument(testDocument) //
                .withState(FINISHED) //
                .build());

        try (var session = openNested(true)) {
            assertThat(sut.isDocumentCuratable(testDocument)) //
                    .as("A document that has not reached ANNOTATION_FINISHED is not curatable") //
                    .isFalse();
        }
    }

    @Test
    void isDocumentCuratable_ShouldAcceptAnnotationFinishedDocumentWithData() throws Exception
    {
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("beate") //
                .forDocument(testDocument) //
                .withState(FINISHED) //
                .build());
        documentService.setSourceDocumentState(testDocument, ANNOTATION_FINISHED);

        try (var session = openNested(true)) {
            assertThat(sut.isDocumentCuratable(testDocument)) //
                    .as("An annotation-finished document with finished annotation data is curatable")
                    .isTrue();
        }
    }

    @Test
    void isDocumentCuratable_ShouldRefuseAnnotationFinishedDocumentWithoutAnyData() throws Exception
    {
        // The document claims annotation is complete but has no annotation document at all - e.g. a
        // project imported without users/permissions, whose documents were marked
        // annotation-finished because the empty set of annotators trivially satisfied the
        // completion rule. So there is no template to initialize the curation CAS from.
        documentService.setSourceDocumentState(testDocument, ANNOTATION_FINISHED);

        try (var session = openNested(true)) {
            assertThat(sut.isDocumentCuratable(testDocument)) //
                    .as("Without any annotation data there is no template, so curation is refused") //
                    .isFalse();
        }
    }

    @Test
    void isDocumentCuratable_ShouldRefuseAnnotationFinishedDocumentThatWasOnlyLocked()
        throws Exception
    {
        // Every annotator locked the document without ever opening it, so no CAS was written and
        // listCuratableUsers comes out empty even though annotation documents exist.
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("beate") //
                .forDocument(testDocument) //
                .withAnnotatorState(IGNORE) //
                .build());
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("kevin") //
                .forDocument(testDocument) //
                .withAnnotatorState(IGNORE) //
                .build());
        documentService.setSourceDocumentState(testDocument, ANNOTATION_FINISHED);

        try (var session = openNested(true)) {
            assertThat(sut.isDocumentCuratable(testDocument)) //
                    .as("A document that was only locked has no data to curate") //
                    .isFalse();
        }
    }

    @Test
    void isDocumentCuratable_ShouldAllowContinuingStartedCurationWithoutAnyAnnotationData()
        throws Exception
    {
        // Curation was started and afterwards all annotators were removed/reset. The curator's work
        // must remain accessible, so a document in a curation state stays curatable even though
        // there is nothing left to merge from.
        for (var state : asList(CURATION_IN_PROGRESS, CURATION_FINISHED)) {
            documentService.setSourceDocumentState(testDocument, state);

            try (var session = openNested(true)) {
                assertThat(sut.isDocumentCuratable(testDocument)) //
                        .as("Curation already in state %s may always be continued", state) //
                        .isTrue();
            }
        }
    }

    @Test
    void isDocumentCuratable_ShouldAllowContinuingWhenCurationCasExists() throws Exception
    {
        // An existing curation CAS means curation has already been started, so it may be continued
        // even though the document itself never reached ANNOTATION_FINISHED and has no annotators.
        writeCurationCasFor(testDocument);

        try (var session = openNested(true)) {
            assertThat(sut.isDocumentCuratable(testDocument)) //
                    .as("A document with an existing curation CAS may always be continued") //
                    .isTrue();
        }
    }

    @Test
    void isDocumentCuratable_ShouldUseTheStoredStateRatherThanTheGivenEntity() throws Exception
    {
        // Another curator moved the document into curation in the meantime - the caller's entity is
        // stale. As a service method this must not depend on the caller having refreshed it.
        documentService.setSourceDocumentState(testDocument, CURATION_IN_PROGRESS);
        testEntityManager.flush();

        var staleDocument = new SourceDocument("doc", testProject, "text");
        staleDocument.setId(testDocument.getId());
        staleDocument.updateState(NEW);

        try (var session = openNested(true)) {
            assertThat(sut.isDocumentCuratable(staleDocument)) //
                    .as("The stored curation state wins over the stale entity") //
                    .isTrue();
        }
    }

    @Test
    void isDocumentCuratable_ShouldRefuseWhenStoredStateIsBehindTheGivenEntity() throws Exception
    {
        // The inverse: the caller's entity claims annotation is finished, but the stored state says
        // otherwise - e.g. an annotator was put back into IN_PROGRESS. The gate must not be fooled.
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("beate") //
                .forDocument(testDocument) //
                .withState(FINISHED) //
                .build());

        var staleDocument = new SourceDocument("doc", testProject, "text");
        staleDocument.setId(testDocument.getId());
        staleDocument.updateState(ANNOTATION_FINISHED);

        try (var session = openNested(true)) {
            assertThat(sut.isDocumentCuratable(staleDocument)) //
                    .as("A stale ANNOTATION_FINISHED on the entity does not open the gate") //
                    .isFalse();
        }
    }

    @Test
    void markCurationInProgress_ShouldPutDocumentIntoCuration()
    {
        documentService.setSourceDocumentState(testDocument, ANNOTATION_FINISHED);

        sut.markCurationInProgress(testDocument);

        assertThat(testEntityManager.find(SourceDocument.class, testDocument.getId()).getState()) //
                .as("An annotation-finished document is put into curation") //
                .isEqualTo(CURATION_IN_PROGRESS);
    }

    @Test
    void markCurationInProgress_ShouldNotReopenFinishedCuration()
    {
        documentService.setSourceDocumentState(testDocument, CURATION_FINISHED);

        sut.markCurationInProgress(testDocument);

        assertThat(testEntityManager.find(SourceDocument.class, testDocument.getId()).getState()) //
                .as("A finished curation is not silently reopened") //
                .isEqualTo(CURATION_FINISHED);
    }

    @Test
    void markCurationInProgress_ShouldLeaveDocumentAlreadyInCurationUntouched()
    {
        documentService.setSourceDocumentState(testDocument, CURATION_IN_PROGRESS);
        testEntityManager.flush();

        var updatedBefore = testEntityManager.find(SourceDocument.class, testDocument.getId())
                .getUpdated();

        sut.markCurationInProgress(testDocument);
        testEntityManager.flush();

        var reloaded = testEntityManager.find(SourceDocument.class, testDocument.getId());
        assertThat(reloaded.getState()) //
                .as("The document stays in curation") //
                .isEqualTo(CURATION_IN_PROGRESS);
        assertThat(reloaded.getUpdated()) //
                .as("The user-visible update timestamp is not bumped") //
                .isEqualTo(updatedBefore);
    }

    @Test
    void markCurationInProgress_ShouldDecideBasedOnTheStoredState()
    {
        // Another curator finished curation in a different request, so the state on the entity we
        // hold predates that. The guard has to look at the database, otherwise a finished curation
        // gets reopened. The write bypasses the entity so that it keeps its pre-transition state,
        // mimicking an entity loaded before the concurrent change.
        documentService.setSourceDocumentState(testDocument, ANNOTATION_FINISHED);
        testEntityManager.flush();
        setStoredStateBypassingEntity(testDocument, CURATION_FINISHED);

        assertThat(testDocument.getState()) //
                .as("Precondition: the entity still has the pre-transition state") //
                .isEqualTo(ANNOTATION_FINISHED);

        sut.markCurationInProgress(testDocument);
        testEntityManager.flush();
        testEntityManager.clear();

        assertThat(testEntityManager.find(SourceDocument.class, testDocument.getId()).getState()) //
                .as("The stored state wins over the state on the entity") //
                .isEqualTo(CURATION_FINISHED);
    }

    /**
     * Change the stored state of a document without touching the given entity, so that the entity
     * is left holding the state from before the change - as it would be if another request had done
     * the change after this one loaded the document.
     */
    private void setStoredStateBypassingEntity(SourceDocument aDocument, SourceDocumentState aState)
    {
        testEntityManager.getEntityManager()
                .createQuery("UPDATE SourceDocument SET state = :state WHERE id = :id")
                .setParameter("state", aState) //
                .setParameter("id", aDocument.getId()) //
                .executeUpdate();
    }

    private void writeCurationCasFor(SourceDocument aDocument) throws Exception
    {
        try (var session = openNested(true)) {
            var tsd = mergeTypeSystems(
                    asList(createTypeSystemDescription(), getInternalTypeSystem()));
            var cas = createCas(tsd);
            session.add(CURATION_SET, EXCLUSIVE_WRITE_ACCESS, cas);
            casStorageService.writeCas(aDocument, cas, CURATION_SET);
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

    /**
     * The legacy strategy drops the requirement that the document itself has reached
     * annotation-finished, but it must still require annotation data to curate.
     */
    @Nested
    @DataJpaTest(showSql = false, //
            properties = { //
                    "spring.main.banner-mode=off", //
                    "curation.legacy-curatable-documents-strategy=true" })
    class LegacyCuratableDocumentsStrategy
    {
        @Test
        void isDocumentCuratable_ShouldAcceptDocumentWithFinishedAnnotationRegardlessOfDocumentState()
            throws Exception
        {
            testEntityManager.persist(AnnotationDocument.builder() //
                    .withUser("beate") //
                    .forDocument(testDocument) //
                    .withState(FINISHED) //
                    .build());

            try (var session = openNested(true)) {
                assertThat(sut.isDocumentCuratable(testDocument)) //
                        .as("Under the legacy strategy a single finished annotator is enough") //
                        .isTrue();
            }
        }

        @Test
        void isDocumentCuratable_ShouldRefuseDocumentWithoutAnyData() throws Exception
        {
            try (var session = openNested(true)) {
                assertThat(sut.isDocumentCuratable(testDocument)) //
                        .as("Even the legacy strategy needs annotation data to curate") //
                        .isFalse();
            }
        }

        @Test
        void isDocumentCuratable_ShouldRefuseDocumentWithoutAnyFinishedAnnotation() throws Exception
        {
            // Every annotator opened the document - so data exists that could serve as a template -
            // but then locked it instead of finishing it. The legacy strategy requires an annotator
            // to have *finished*, so mere IGNORE data does not open the gate.
            testEntityManager.persist(AnnotationDocument.builder() //
                    .withUser("beate") //
                    .forDocument(testDocument) //
                    .withAnnotatorState(IGNORE) //
                    .build());
            writeCasFor(testDocument, "beate");

            testEntityManager.persist(AnnotationDocument.builder() //
                    .withUser("kevin") //
                    .forDocument(testDocument) //
                    .withAnnotatorState(IGNORE) //
                    .build());
            writeCasFor(testDocument, "kevin");

            try (var session = openNested(true)) {
                assertThat(sut.isDocumentCuratable(testDocument)) //
                        .as("The legacy strategy requires a finished annotation, not just data") //
                        .isFalse();
            }
        }

        @Test
        void isDocumentCuratable_ShouldAcceptDocumentWithOneFinishedAnnotationAmongIgnored()
            throws Exception
        {
            // Only one of the annotators finished, the other locked the document. A single finished
            // annotator is all the legacy strategy asks for.
            testEntityManager.persist(AnnotationDocument.builder() //
                    .withUser("beate") //
                    .forDocument(testDocument) //
                    .withAnnotatorState(IGNORE) //
                    .build());
            writeCasFor(testDocument, "beate");

            testEntityManager.persist(AnnotationDocument.builder() //
                    .withUser("kevin") //
                    .forDocument(testDocument) //
                    .withState(FINISHED) //
                    .build());

            try (var session = openNested(true)) {
                assertThat(sut.isDocumentCuratable(testDocument)) //
                        .as("One finished annotator among locked ones is enough") //
                        .isTrue();
            }
        }

        @Test
        void markCurationInProgress_ShouldGrantTheCuratorEditAccess() throws Exception
        {
            // Under the legacy strategy a document is curatable while still being
            // ANNOTATION_IN_PROGRESS, but a curator is granted edit access only once it has reached
            // a curation state. So putting it into curation is the precondition for editing it -
            // see AnnotationPageBase2#transitionDocumentStateOnLoadDocument, which therefore must
            // not gate that transition on editability.
            testEntityManager.persist(AnnotationDocument.builder() //
                    .withUser("kevin") //
                    .forDocument(testDocument) //
                    .withState(FINISHED) //
                    .build());

            assertThat(documentAccess.canEditAnnotationDocument("beate",
                    String.valueOf(testProject.getId()), testDocument.getId(), CURATION_USER)) //
                            .as("Curator cannot edit the document before it is put into curation") //
                            .isFalse();

            sut.markCurationInProgress(testDocument);

            assertThat(documentAccess.canEditAnnotationDocument("beate",
                    String.valueOf(testProject.getId()), testDocument.getId(), CURATION_USER)) //
                            .as("Putting the document into curation grants the curator edit access") //
                            .isTrue();
        }

        @Test
        void listCuratableSourceDocuments_ShouldRequireAFinishedAnnotation() throws Exception
        {
            // The document list must apply the same rule as the open-gate above, otherwise a
            // document offered in the curation list cannot actually be opened.
            var onlyLocked = new SourceDocument("onlyLocked", testProject, "text");
            var finished = new SourceDocument("finished", testProject, "text");
            testEntityManager.persist(onlyLocked);
            testEntityManager.persist(finished);

            testEntityManager.persist(AnnotationDocument.builder() //
                    .withUser("kevin") //
                    .forDocument(onlyLocked) //
                    .withAnnotatorState(IGNORE) //
                    .build());
            writeCasFor(onlyLocked, "kevin");

            testEntityManager.persist(AnnotationDocument.builder() //
                    .withUser("kevin") //
                    .forDocument(finished) //
                    .withState(FINISHED) //
                    .build());

            try (var session = openNested(true)) {
                assertThat(sut.listCuratableSourceDocuments(testProject)) //
                        .as("Only documents with a finished annotation are offered for curation") //
                        .containsExactly(finished);
            }
        }

        @Test
        void listCuratableSourceDocuments_ShouldKeepDocumentsAlreadyInCuration() throws Exception
        {
            // Curation was started and the annotators were reset afterwards - e.g. from the
            // workload
            // management page. The workload managers no longer touch the state of a document in
            // curation, so it stays in a curation state while no annotation document is finished
            // any
            // longer. The curator must not lose access to the work already done.
            var inProgress = new SourceDocument("inProgress", testProject, "text");
            inProgress.updateState(CURATION_IN_PROGRESS);
            var curationFinished = new SourceDocument("curationFinished", testProject, "text");
            curationFinished.updateState(CURATION_FINISHED);
            testEntityManager.persist(inProgress);
            testEntityManager.persist(curationFinished);

            testEntityManager.persist(AnnotationDocument.builder() //
                    .withUser("kevin") //
                    .forDocument(inProgress) //
                    .withState(AnnotationDocumentState.NEW) //
                    .build());

            try (var session = openNested(true)) {
                assertThat(sut.listCuratableSourceDocuments(testProject)) //
                        .as("Documents already in curation remain offered for curation") //
                        .containsExactly(curationFinished, inProgress);
                assertThat(sut.isDocumentCuratable(inProgress)) //
                        .as("The list agrees with the open gate") //
                        .isTrue();
            }
        }
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
