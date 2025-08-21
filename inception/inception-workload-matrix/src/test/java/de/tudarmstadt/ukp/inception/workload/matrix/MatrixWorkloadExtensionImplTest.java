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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagementAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.matrix.config.MatrixWorkloadManagerAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@EnableAutoConfiguration
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class, showSql = false, //
        properties = { //
                "spring.main.banner-mode=off", //
                "workload.matrix.enabled=true" })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno" })
@Import({ //
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
