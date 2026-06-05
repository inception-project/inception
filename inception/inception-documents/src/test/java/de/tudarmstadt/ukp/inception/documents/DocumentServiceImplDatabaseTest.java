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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSetMarker.DEACTIVATED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSetMarker.FORMER_ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSetMarker.MISSING;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_REPOSITORY_PATH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentStorageService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.persistence.EntityManager;

/**
 * Database-backed tests for {@link DocumentServiceImpl} that only need a real
 * {@link EntityManager}. The collaborators that would pull in modules depending on this one (CAS
 * storage, import/export, schema) are mocked, so the service under test is constructed by hand
 * rather than wired by Spring. Tests that exercise CAS storage / import-export live in the
 * integration-level {@code DocumentServiceImplCasStorageTest} in the {@code inception-schema}
 * module instead.
 */
@EnableAutoConfiguration
@DataJpaTest(showSql = false, //
        properties = { "spring.main.banner-mode=off" })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
class DocumentServiceImplDatabaseTest
{
    private @Autowired TestEntityManager testEntityManager;

    private ProjectService projectService;
    private DocumentImportExportService importExportService;
    private CasStorageService casStorageService;
    private EntityManager entityManager;
    private DocumentServiceImpl sut;

    private User annotator1;
    private Project project;

    @BeforeEach
    void setup()
    {
        // The service wraps its work in ProjectService.withProjectLogger(...), which requires the
        // repository path to be present in the logging MDC.
        MDC.put(KEY_REPOSITORY_PATH, "target/test-repository");

        projectService = mock(ProjectService.class);
        importExportService = mock(DocumentImportExportService.class);
        casStorageService = mock(CasStorageService.class);
        entityManager = testEntityManager.getEntityManager();
        sut = new DocumentServiceImpl(null, casStorageService, importExportService, projectService,
                mock(ApplicationEventPublisher.class), entityManager,
                mock(DocumentStorageService.class));

        annotator1 = new User("anno1");
        testEntityManager.persist(annotator1);

        project = new Project("project");
        testEntityManager.persist(project);
        testEntityManager
                .persist(new ProjectPermission(project, annotator1.getUsername(), ANNOTATOR));
    }

    @AfterEach
    void tearDown()
    {
        MDC.remove(KEY_REPOSITORY_PATH);
    }

    @Test
    void testThatAnnotationDocumentsForNonExistingUserAreNotReturned()
    {
        var doc = sut.createSourceDocument(new SourceDocument("doc", project, "text"));

        var ann = sut.createOrUpdateAnnotationDocument(
                new AnnotationDocument(annotator1.getUsername(), doc));

        assertThat(sut.listAnnotationDocuments(doc))
                .as("As long as the user exists, the annotation document must be found")
                .containsExactly(ann);

        testEntityManager.remove(annotator1);
        testEntityManager.flush();

        assertThat(sut.listAnnotationDocuments(doc))
                .as("When the user is deleted, the document must no longer be found") //
                .isEmpty();
    }

    @Test
    void thatListAllAnnotationDocumentsIncludesFormerAnnotators()
    {
        var doc = sut.createSourceDocument(new SourceDocument("doc", project, "text"));

        var current = sut.createOrUpdateAnnotationDocument(
                new AnnotationDocument(annotator1.getUsername(), doc));
        // A former annotator: has data but holds no permission and has no user account.
        var former = sut.createOrUpdateAnnotationDocument(new AnnotationDocument("ghost", doc));

        assertThat(sut.listAnnotationDocuments(project)) //
                .as("The permission-filtered list only contains current annotators") //
                .containsExactly(current);

        assertThat(sut.listAllAnnotationDocuments(project)) //
                .as("The unfiltered list also contains former annotators") //
                .containsExactlyInAnyOrder(current, former);
    }

    @Test
    void thatCreateOrGetAnnotationDocumentsWithAnnotationSetsCreatesDocuments()
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
    void thatCreateOrGetAnnotationDocumentsForMultipleDocumentsCreatesDocuments()
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
    void thatListAnnotationDocumentsProjectAndSetWorks()
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
    void thatListAllDocumentsProjectAndSetReturnsNullForMissingDocs()
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
    void thatExistsAnnotationDocumentWithSetWorks()
    {
        var doc = sut.createSourceDocument(new SourceDocument("docx", project, "text"));
        var set = AnnotationSet.forUser(annotator1);

        assertThat(sut.existsAnnotationDocument(doc, set)).isFalse();
        sut.createOrGetAnnotationDocument(doc, set);
        assertThat(sut.existsAnnotationDocument(doc, set)).isTrue();
    }

    @Test
    void thatExistsSourceDocumentWorks()
    {
        var doc1 = sut.createSourceDocument(new SourceDocument("s1", project, "text"));
        var doc2 = sut.createSourceDocument(new SourceDocument("s2", project, "text"));

        assertThat(sut.existsSourceDocument(project)).isTrue();
        assertThat(sut.existsSourceDocument(project, doc1.getName())).isTrue();
        assertThat(sut.existsSourceDocument(project, doc2.getName())).isTrue();
        assertThat(sut.existsSourceDocument(project, "not-found.txt")).isFalse();
    }

    @Test
    void thatListFinishedAnnotationDocumentsAndExistsWorks()
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
    void thatListSupportedSourceDocumentsFiltersUnsupported()
    {
        when(importExportService.getFormatById("text"))
                .thenReturn(Optional.of(mock(FormatSupport.class)));

        var d1 = sut.createSourceDocument(new SourceDocument("f1.txt", project, "text"));
        var d2 = sut.createSourceDocument(new SourceDocument("f2.txt", project, "unsupported"));

        var res = sut.listSupportedSourceDocuments(project);
        assertThat(res).contains(d1);
        assertThat(res).doesNotContain(d2);
    }

    @Test
    void thatUpdateStateUpdatedDirectlyWorks()
    {
        var doc = sut.createSourceDocument(new SourceDocument("ust.txt", project, "text"));
        var set = AnnotationSet.forUser(annotator1);
        var ann = sut.createOrGetAnnotationDocument(doc, set);

        var d = new Date(1620000000000L);
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
    void thatExplicitUserActionsSetAnnotatorState()
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
    void thatListDataOwnersUnionsCurrentAndFormerAnnotators()
    {
        // anno1 (from setup) plus a second current annotator without any data yet
        var anno2 = new User("anno2");
        testEntityManager.persist(anno2);
        when(projectService.listUsersWithRoleInProject(project, ANNOTATOR))
                .thenReturn(asList(annotator1, anno2));

        var doc = sut.createSourceDocument(new SourceDocument("doc", project, "text"));

        // anno1 is a current annotator that has produced data
        var anno1Doc = sut.createOrUpdateAnnotationDocument(
                new AnnotationDocument(annotator1.getUsername(), doc));
        sut.setAnnotationDocumentState(anno1Doc, AnnotationDocumentState.IN_PROGRESS);

        // a former annotator: left data behind but no longer holds the annotator permission
        var carol = User.builder().withUsername("carol").withUiName("carol-display").build();
        testEntityManager.persist(carol);
        var carolDoc = sut
                .createOrUpdateAnnotationDocument(new AnnotationDocument(carol.getUsername(), doc));
        sut.setAnnotationDocumentState(carolDoc, AnnotationDocumentState.FINISHED);

        // a former annotator whose account has been deleted entirely
        var ghostDoc = sut.createOrUpdateAnnotationDocument(new AnnotationDocument("ghost", doc));
        sut.setAnnotationDocumentState(ghostDoc, AnnotationDocumentState.IN_PROGRESS);

        // the curation and initial CAS pseudo users must not show up as data owners
        sut.createOrUpdateAnnotationDocument(new AnnotationDocument(CURATION_USER, doc));
        sut.createOrUpdateAnnotationDocument(new AnnotationDocument(INITIAL_CAS_PSEUDO_USER, doc));

        assertThat(sut.listDataOwners(project)) //
                .extracting(AnnotationSet::id, AnnotationSet::displayName) //
                .containsExactly( //
                        tuple("anno1", "anno1"), // current annotator with data
                        tuple("anno2", "anno2"), // current annotator without data
                        tuple("carol", "carol-display (former annotator)"), // former, account kept
                        tuple("ghost", "ghost (missing!)")); // former, account deleted
    }

    @Test
    void thatListDataOwnersIgnoresFormerAnnotatorsWithoutData()
    {
        when(projectService.listUsersWithRoleInProject(project, ANNOTATOR))
                .thenReturn(asList(annotator1));

        var doc = sut.createSourceDocument(new SourceDocument("doc", project, "text"));

        // A former annotator that was only ever assigned the document (state NEW) but never
        // produced any annotation data must not be reported as a data owner.
        var dave = User.builder().withUsername("dave").withUiName("dave-display").build();
        testEntityManager.persist(dave);
        sut.createOrUpdateAnnotationDocument(new AnnotationDocument(dave.getUsername(), doc));

        assertThat(sut.listDataOwners(project)) //
                .extracting(AnnotationSet::id) //
                .containsExactly("anno1"); // only the current annotator; dave is omitted
    }

    @Test
    void thatListDataOwnersIgnoresFormerAnnotatorWithLockedButEmptyDocument()
    {
        when(projectService.listUsersWithRoleInProject(project, ANNOTATOR))
                .thenReturn(asList(annotator1));

        var doc = sut.createSourceDocument(new SourceDocument("doc", project, "text"));

        // A former annotator whose only document was locked before they ever worked on it: the
        // document is in IGNORE state but no CAS exists, so they are not a data owner.
        var erin = User.builder().withUsername("erin").withUiName("erin-display").build();
        testEntityManager.persist(erin);
        var erinDoc = sut.createOrUpdateAnnotationDocument(new AnnotationDocument("erin", doc));
        sut.setAnnotationDocumentState(erinDoc, AnnotationDocumentState.IGNORE);

        // The mock CasStorageService reports no CAS by default.
        assertThat(sut.listDataOwners(project)) //
                .extracting(AnnotationSet::id) //
                .containsExactly("anno1"); // erin is omitted - locked but no data
    }

    @Test
    void thatListDataOwnersIncludesFormerAnnotatorWithLockedDocumentThatHasData() throws Exception
    {
        when(projectService.listUsersWithRoleInProject(project, ANNOTATOR))
                .thenReturn(asList(annotator1));
        // The former annotator did produce data before the document was locked.
        when(casStorageService.existsCas(any(), any())).thenReturn(true);

        var doc = sut.createSourceDocument(new SourceDocument("doc", project, "text"));

        var frank = User.builder().withUsername("frank").withUiName("frank-display").build();
        testEntityManager.persist(frank);
        var frankDoc = sut.createOrUpdateAnnotationDocument(new AnnotationDocument("frank", doc));
        sut.setAnnotationDocumentState(frankDoc, AnnotationDocumentState.IGNORE);

        assertThat(sut.listDataOwners(project)) //
                .extracting(AnnotationSet::id, AnnotationSet::displayName) //
                .containsExactly( //
                        tuple("anno1", "anno1"), //
                        tuple("frank", "frank-display (former annotator)"));
    }

    @Test
    void thatGetDataOwnerResolvesCurrentAnnotatorWithoutMarker()
    {
        when(projectService.hasRole(annotator1, project, ANNOTATOR)).thenReturn(true);

        var set = sut.getDataOwner(project, annotator1.getUsername());

        assertThat(set.id()).isEqualTo("anno1");
        assertThat(set.marker()).isNull();
    }

    @Test
    void thatGetDataOwnerResolvesFormerAnnotatorWithFormerMarker()
    {
        var carol = User.builder().withUsername("carol").withUiName("carol-display").build();
        testEntityManager.persist(carol);
        when(projectService.hasRole(carol, project, ANNOTATOR)).thenReturn(false);

        var set = sut.getDataOwner(project, "carol");

        assertThat(set.id()).isEqualTo("carol");
        assertThat(set.displayName()).isEqualTo("carol-display (former annotator)");
        assertThat(set.marker()).isEqualTo(FORMER_ANNOTATOR);
    }

    @Test
    void thatGetDataOwnerResolvesMissingAccountWithMissingMarker()
    {
        // No user account with this name exists.
        var set = sut.getDataOwner(project, "ghost");

        assertThat(set.id()).isEqualTo("ghost");
        assertThat(set.displayName()).isEqualTo("ghost (missing!)");
        assertThat(set.marker()).isEqualTo(MISSING);
    }

    @Test
    void thatListDataOwnersFlagsDisabledCurrentAnnotatorAsDeactivated()
    {
        // A disabled account that still holds the annotator permission stays a current data owner
        // but is flagged as deactivated rather than former.
        var disabled = User.builder().withUsername("anno2").withUiName("anno2-display")
                .withEnabled(false).build();
        testEntityManager.persist(disabled);
        when(projectService.listUsersWithRoleInProject(project, ANNOTATOR))
                .thenReturn(asList(annotator1, disabled));

        assertThat(sut.listDataOwners(project)) //
                .extracting(AnnotationSet::id, AnnotationSet::marker) //
                .containsExactlyInAnyOrder( //
                        tuple("anno1", null), // enabled current annotator
                        tuple("anno2", DEACTIVATED)); // disabled current annotator
    }

    @Test
    void thatGetDataOwnerFlagsDisabledCurrentAnnotatorAsDeactivated()
    {
        var disabled = User.builder().withUsername("dora").withUiName("dora-display")
                .withEnabled(false).build();
        testEntityManager.persist(disabled);
        when(projectService.hasRole(disabled, project, ANNOTATOR)).thenReturn(true);

        var set = sut.getDataOwner(project, "dora");

        assertThat(set.id()).isEqualTo("dora");
        assertThat(set.displayName()).isEqualTo("dora-display (deactivated)");
        assertThat(set.marker()).isEqualTo(DEACTIVATED);
    }

    @Test
    void thatGetDataOwnersResolvesAllMarkersInOneCall()
    {
        var disabled = User.builder().withUsername("dora").withUiName("dora-display")
                .withEnabled(false).build();
        testEntityManager.persist(disabled);
        var carol = User.builder().withUsername("carol").withUiName("carol-display").build();
        testEntityManager.persist(carol);

        // anno1 (enabled) and dora (disabled) currently hold the annotator permission; carol and
        // the non-existent ghost do not.
        when(projectService.listUsersWithRoleInProject(project, ANNOTATOR))
                .thenReturn(asList(annotator1, disabled));

        var resolved = sut.getDataOwners(project, asList("anno1", "dora", "carol", "ghost"));

        assertThat(resolved.keySet()) //
                .as("Every requested data owner is resolved, preserving the requested order") //
                .containsExactly("anno1", "dora", "carol", "ghost");
        assertThat(resolved.get("anno1").marker()).isNull();
        assertThat(resolved.get("dora").marker()).isEqualTo(DEACTIVATED);
        assertThat(resolved.get("carol").marker()).isEqualTo(FORMER_ANNOTATOR);
        assertThat(resolved.get("carol").displayName())
                .isEqualTo("carol-display (former annotator)");
        assertThat(resolved.get("ghost").marker()).isEqualTo(MISSING);
    }

    @SpringBootConfiguration
    static class TestContext
    {
    }
}
