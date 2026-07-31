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
package de.tudarmstadt.ukp.inception.workload.matrix;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.workload.matrix.Fixtures.importTestSourceDocumentAndAddNamedEntity;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.config.ConstraintsServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
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
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagementAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.matrix.config.MatrixWorkloadManagerAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@EnableAutoConfiguration
@DataJpaTest(showSql = false, //
        properties = { //
                "spring.main.banner-mode=off", //
                "workload.matrix.enabled=true" })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno" })
@Import({ //
        EventLoggingAutoConfiguration.class, //
        ConstraintsServiceAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        SchedulingServiceAutoConfiguration.class, //
        WorkloadManagementAutoConfiguration.class, //
        MatrixWorkloadManagerAutoConfiguration.class })
class MatrixWorkloadExtensionImplTest
{
    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired UserDao userService;
    private @Autowired WorkloadManagementService workloadManagementService;
    private @Autowired MatrixWorkloadExtension matrixWorkloadExtension;

    private User annotator1;
    private User annotator2;
    private User curator;
    private User manager;
    private Project project;
    private SourceDocument sourceDocument;
    private AnnotationDocument annotationDocument1;
    private AnnotationDocument annotationDocument2;

    static @TempDir Path tempFolder;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry)
    {
        registry.add("repository.path", () -> tempFolder.toAbsolutePath().toString());
    }

    @BeforeEach
    void setup() throws Exception
    {
        annotator1 = userService.create(new User("anno1"));
        annotator2 = userService.create(new User("anno2"));
        curator = userService.create(new User("curator"));
        manager = userService.create(new User("manager"));

        project = projectService.createProject(new Project("test"));

        projectService.assignRole(project, annotator1, ANNOTATOR);
        projectService.assignRole(project, annotator2, ANNOTATOR);
        projectService.assignRole(project, curator, CURATOR);
        projectService.assignRole(project, manager, MANAGER);

        sourceDocument = documentService
                .createSourceDocument(new SourceDocument("doc.txt", project, TextFormatSupport.ID));
        annotationDocument1 = documentService.createOrUpdateAnnotationDocument(
                new AnnotationDocument(annotator1.getUsername(), sourceDocument));
        annotationDocument2 = documentService.createOrUpdateAnnotationDocument(
                new AnnotationDocument(annotator2.getUsername(), sourceDocument));

        importTestSourceDocumentAndAddNamedEntity(documentService, annotationDocument1);
        importTestSourceDocumentAndAddNamedEntity(documentService, annotationDocument2);

        var workloadManager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project);
        workloadManager.setType(MatrixWorkloadExtension.MATRIX_WORKLOAD_MANAGER_EXTENSION_ID);
    }

    @Test
    void thatRecalculatingStateDoesNotFallBackBehindCuration() throws Exception
    {
        documentService.setSourceDocumentState(sourceDocument,
                SourceDocumentState.CURATION_IN_PROGRESS);
        documentService.setAnnotationDocumentState(annotationDocument1,
                AnnotationDocumentState.NEW);
        documentService.setAnnotationDocumentState(annotationDocument2,
                AnnotationDocumentState.IN_PROGRESS);

        matrixWorkloadExtension.recalculate(project);

        sourceDocument = documentService.getSourceDocument(project.getId(), sourceDocument.getId());

        assertThat(sourceDocument.getState()).isEqualTo(SourceDocumentState.CURATION_IN_PROGRESS);
    }

    @Test
    void thatAllAnnotatorsFinishedSetsDocumentToAnnotationFinished() throws Exception
    {
        documentService.setSourceDocumentState(sourceDocument,
                SourceDocumentState.ANNOTATION_IN_PROGRESS);
        documentService.setAnnotationDocumentState(annotationDocument1,
                AnnotationDocumentState.FINISHED);
        documentService.setAnnotationDocumentState(annotationDocument2,
                AnnotationDocumentState.FINISHED);

        matrixWorkloadExtension.recalculate(project);

        sourceDocument = documentService.getSourceDocument(project.getId(), sourceDocument.getId());

        assertThat(sourceDocument.getState()).isEqualTo(SourceDocumentState.ANNOTATION_FINISHED);
    }

    @Test
    void thatSomeAnnotatorsNotFinishedSetsDocumentToAnnotationInProgress() throws Exception
    {
        documentService.setSourceDocumentState(sourceDocument,
                SourceDocumentState.ANNOTATION_FINISHED);
        documentService.setAnnotationDocumentState(annotationDocument1,
                AnnotationDocumentState.IN_PROGRESS);
        documentService.setAnnotationDocumentState(annotationDocument2,
                AnnotationDocumentState.FINISHED);

        matrixWorkloadExtension.recalculate(project);

        sourceDocument = documentService.getSourceDocument(project.getId(), sourceDocument.getId());

        assertThat(sourceDocument.getState()).isEqualTo(SourceDocumentState.ANNOTATION_IN_PROGRESS);
    }

    @Test
    void thatNoAnnotatorsStartedSetsDocumentToNew() throws Exception
    {
        documentService.setSourceDocumentState(sourceDocument,
                SourceDocumentState.ANNOTATION_FINISHED);
        documentService.setAnnotationDocumentState(annotationDocument1,
                AnnotationDocumentState.NEW);
        documentService.setAnnotationDocumentState(annotationDocument2,
                AnnotationDocumentState.NEW);

        matrixWorkloadExtension.recalculate(project);

        sourceDocument = documentService.getSourceDocument(project.getId(), sourceDocument.getId());

        assertThat(sourceDocument.getState()).isEqualTo(SourceDocumentState.NEW);
    }

    /**
     * The counts satisfy the "everybody is accounted for" threshold, but nobody finished the
     * document, so there is nothing to curate - hence NEW rather than ANNOTATION_FINISHED or
     * {@link SourceDocumentState#ANNOTATION_IN_PROGRESS}.
     * 
     * @see MatrixWorkloadExtensionImpl#setSourceDocumentStateBasedOnStats
     */
    @Test
    void thatAllAnnotatorsLockingSetsDocumentToNew() throws Exception
    {
        documentService.setSourceDocumentState(sourceDocument,
                SourceDocumentState.ANNOTATION_IN_PROGRESS);
        documentService.setAnnotationDocumentState(annotationDocument1,
                AnnotationDocumentState.IGNORE);
        documentService.setAnnotationDocumentState(annotationDocument2,
                AnnotationDocumentState.IGNORE);

        matrixWorkloadExtension.recalculate(project);

        sourceDocument = documentService.getSourceDocument(project.getId(), sourceDocument.getId());

        assertThat(sourceDocument.getState()) //
                .as("A document that every annotator merely locked holds nothing, so it is NEW") //
                .isEqualTo(SourceDocumentState.NEW);
        assertStateAlignedWithReadiness(sourceDocument);
    }

    @Test
    void thatProjectWithoutAnnotatorsDoesNotSetDocumentToAnnotationFinished() throws Exception
    {
        projectService.revokeRole(project, annotator1, ANNOTATOR);
        projectService.revokeRole(project, annotator2, ANNOTATOR);
        documentService.setSourceDocumentState(sourceDocument,
                SourceDocumentState.ANNOTATION_IN_PROGRESS);
        documentService.setAnnotationDocumentState(annotationDocument1,
                AnnotationDocumentState.NEW);
        documentService.setAnnotationDocumentState(annotationDocument2,
                AnnotationDocumentState.NEW);

        matrixWorkloadExtension.recalculate(project);

        sourceDocument = documentService.getSourceDocument(project.getId(), sourceDocument.getId());

        assertThat(sourceDocument.getState()) //
                .as("Without any annotators, no document is annotation-finished") //
                .isNotEqualTo(SourceDocumentState.ANNOTATION_FINISHED);
        assertStateAlignedWithReadiness(sourceDocument);
    }

    /**
     * The alignment invariant: {@link MatrixWorkloadExtensionImpl#recalculate} must set the
     * document to {@link SourceDocumentState#ANNOTATION_FINISHED} exactly when the workload manager
     * also reports it as ready for curation. If these two drift apart, a document can claim that
     * annotation is complete while curation refuses it - or vice versa.
     */
    @ParameterizedTest(name = "anno1={0}, anno2={1}")
    @CsvSource({ //
            "NEW, NEW", //
            "NEW, IN_PROGRESS", //
            "NEW, FINISHED", //
            "NEW, IGNORE", //
            "IN_PROGRESS, IN_PROGRESS", //
            "IN_PROGRESS, FINISHED", //
            "IN_PROGRESS, IGNORE", //
            "FINISHED, FINISHED", //
            "FINISHED, IGNORE", //
            "IGNORE, IGNORE" })
    void thatDocumentStateIsAlignedWithCurationReadiness(AnnotationDocumentState aState1,
            AnnotationDocumentState aState2)
        throws Exception
    {
        documentService.setSourceDocumentState(sourceDocument,
                SourceDocumentState.ANNOTATION_IN_PROGRESS);
        documentService.setAnnotationDocumentState(annotationDocument1, aState1);
        documentService.setAnnotationDocumentState(annotationDocument2, aState2);

        matrixWorkloadExtension.recalculate(project);

        sourceDocument = documentService.getSourceDocument(project.getId(), sourceDocument.getId());

        assertStateAlignedWithReadiness(sourceDocument);
    }

    /**
     * The "reset curation" actions on the matrix workload management page clear the curation state
     * and then recalculate. The intermediate {@link SourceDocumentState#ANNOTATION_IN_PROGRESS}
     * they write is not the intended outcome; it only unlocks the document from curation so that
     * {@code recalculate} stops skipping it (see {@code MatrixWorkloadExtensionImpl#isInCuration}).
     * The state the document actually ends up in must be derived from the annotation documents -
     * see {@link MatrixCurationReadiness}.
     */
    @ParameterizedTest(name = "scope={0}, anno1={1}, anno2={2} -> {3}")
    @CsvSource({ //
            "PROJECT, NEW, NEW, NEW", //
            "PROJECT, IGNORE, IGNORE, NEW", //
            "PROJECT, NEW, IGNORE, NEW", //
            "PROJECT, NEW, IN_PROGRESS, ANNOTATION_IN_PROGRESS", //
            "PROJECT, IN_PROGRESS, IGNORE, ANNOTATION_IN_PROGRESS", //
            "PROJECT, IN_PROGRESS, FINISHED, ANNOTATION_IN_PROGRESS", //
            "PROJECT, FINISHED, FINISHED, ANNOTATION_FINISHED", //
            "PROJECT, FINISHED, IGNORE, ANNOTATION_FINISHED", //
            "DOCUMENT, NEW, NEW, NEW", //
            "DOCUMENT, IGNORE, IGNORE, NEW", //
            "DOCUMENT, NEW, IGNORE, NEW", //
            "DOCUMENT, NEW, IN_PROGRESS, ANNOTATION_IN_PROGRESS", //
            "DOCUMENT, IN_PROGRESS, IGNORE, ANNOTATION_IN_PROGRESS", //
            "DOCUMENT, IN_PROGRESS, FINISHED, ANNOTATION_IN_PROGRESS", //
            "DOCUMENT, FINISHED, FINISHED, ANNOTATION_FINISHED", //
            "DOCUMENT, FINISHED, IGNORE, ANNOTATION_FINISHED" })
    void thatResettingCurationFallsBackToStateDerivedFromAnnotationDocuments(
            RecalculationScope aScope, AnnotationDocumentState aState1,
            AnnotationDocumentState aState2, SourceDocumentState aExpectedState)
        throws Exception
    {
        documentService.setSourceDocumentState(sourceDocument,
                SourceDocumentState.CURATION_IN_PROGRESS);
        documentService.setAnnotationDocumentState(annotationDocument1, aState1);
        documentService.setAnnotationDocumentState(annotationDocument2, aState2);

        resetCurationAsTheResetActionsDo(aScope);

        sourceDocument = documentService.getSourceDocument(project.getId(), sourceDocument.getId());

        assertThat(sourceDocument.getState()) //
                .as("Resetting curation must fall back to the state implied by the annotation "
                        + "documents, not stay at the intermediate ANNOTATION_IN_PROGRESS") //
                .isEqualTo(aExpectedState);
        assertStateAlignedWithReadiness(sourceDocument);
    }

    @ParameterizedTest(name = "scope={0}")
    @CsvSource({ "PROJECT", "DOCUMENT" })
    void thatResettingCurationOfFinishedCurationAlsoFallsBack(RecalculationScope aScope)
        throws Exception
    {
        // The reset actions accept CURATION_FINISHED documents as well, not just in-progress ones.
        documentService.setSourceDocumentState(sourceDocument,
                SourceDocumentState.CURATION_FINISHED);
        documentService.setAnnotationDocumentState(annotationDocument1,
                AnnotationDocumentState.NEW);
        documentService.setAnnotationDocumentState(annotationDocument2,
                AnnotationDocumentState.NEW);

        resetCurationAsTheResetActionsDo(aScope);

        sourceDocument = documentService.getSourceDocument(project.getId(), sourceDocument.getId());

        assertThat(sourceDocument.getState()) //
                .as("A reset of a finished curation falls back just like an in-progress one") //
                .isEqualTo(SourceDocumentState.NEW);
    }

    enum RecalculationScope
    {
        PROJECT, DOCUMENT
    }

    /**
     * Mirrors the sequence shared by {@code MatrixWorkloadManagementPage#actionBulkResetCuration}
     * and {@code #actionResetCurationDocument}. The order matters: the curation state must be
     * cleared <b>before</b> recalculating, otherwise {@code isInCuration} makes {@code recalculate}
     * skip the document and the intermediate state sticks. Deleting the curation CAS is omitted -
     * it does not influence the state.
     * <p>
     * <b>Mind what this does and does not cover.</b> Because the sequence is re-implemented here
     * rather than invoked on the page, these tests pin the <em>rules</em> the fallback must follow
     * and the ordering the actions have to use - but they cannot catch an action that fails to call
     * {@code recalculate} at all. That is a Wicket-level concern and this is a {@code @DataJpaTest}
     * with no Wicket context. {@code actionResetCurationDocument} was missing exactly that call and
     * these tests stayed green throughout, so do not read them as coverage of the call sites. The
     * two actions otherwise differ only in {@code bulkSetSourceDocumentState} vs
     * {@code setSourceDocumentState}, which are equivalent for a single document.
     */
    private void resetCurationAsTheResetActionsDo(RecalculationScope aScope)
    {
        documentService.bulkSetSourceDocumentState(asList(sourceDocument),
                SourceDocumentState.ANNOTATION_IN_PROGRESS);

        switch (aScope) {
        case PROJECT:
            matrixWorkloadExtension.recalculate(project);
            break;
        case DOCUMENT:
            matrixWorkloadExtension.recalculate(sourceDocument);
            break;
        }
    }

    private void assertStateAlignedWithReadiness(SourceDocument aSource)
    {
        var readyForCuration = matrixWorkloadExtension.isReadyForCuration(aSource);

        assertThat(aSource.getState() == SourceDocumentState.ANNOTATION_FINISHED) //
                .as("Document state [%s] must be ANNOTATION_FINISHED exactly when the document is "
                        + "ready for curation (ready=%s)", aSource.getState(), readyForCuration) //
                .isEqualTo(readyForCuration);
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
            return importService;
        }
    }
}
