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

import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.util.FileSystemUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagementAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.matrix.config.MatrixWorkloadManagerAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.matrix.trait.MatrixWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

@EnableAutoConfiguration
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class, showSql = false, //
        properties = { //
                "spring.main.banner-mode=off", //
                "workload.dynamic.enabled=true", //
                "repository.path=" + MatrixWorkloadExtensionImplTest.TEST_OUTPUT_FOLDER })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno" })
@Import({ //
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
public class MatrixWorkloadExtensionImplTest
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/MatrixWorkloadExtensionImplTest";

    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired UserDao userService;
    private @Autowired WorkloadManagementService workloadManagementService;
    private @Autowired MatrixWorkloadExtension matrixWorkloadExtension;
    private @Autowired SessionRegistry sessionRegistry;

    private User annotator;
    private Project project;
    private SourceDocument sourceDocument;
    private AnnotationDocument annotationDocument;
    private MatrixWorkloadTraits traits;

    @BeforeAll
    public static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @BeforeEach
    public void setup() throws Exception
    {
        annotator = userService.create(new User("anno1"));
        project = projectService.createProject(new Project("test"));

        sourceDocument = documentService
                .createSourceDocument(new SourceDocument("doc.txt", project, TextFormatSupport.ID));
        annotationDocument = documentService.createAnnotationDocument(
                new AnnotationDocument(annotator.getUsername(), sourceDocument));

        Fixtures.importTestSourceDocumentAndAddNamedEntity(documentService, annotationDocument);

        WorkloadManager workloadManager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project);
        workloadManager.setType(MatrixWorkloadExtension.MATRIX_WORKLOAD_MANAGER_EXTENSION_ID);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        projectService.removeProject(project);
    }

    @Test
    public void thatRecalculatingStateDoesNotFallBackBehindCuration() throws Exception
    {
        documentService.setSourceDocumentState(sourceDocument,
                SourceDocumentState.CURATION_IN_PROGRESS);

        matrixWorkloadExtension.recalculate(project);

        sourceDocument = documentService.getSourceDocument(project.getId(), sourceDocument.getId());

        assertThat(sourceDocument.getState()).isEqualTo(SourceDocumentState.CURATION_IN_PROGRESS);
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        @Bean
        DocumentImportExportService documentImportExportService(
                AnnotationSchemaService aSchemaService)
            throws Exception
        {
            var tsd = createTypeSystemDescription();
            var importService = mock(DocumentImportExportService.class);
            when(importService.importCasFromFile(any(), any(), any()))
                    .thenReturn(CasCreationUtils.createCas(tsd, null, null, null));
            return importService;
        }
    }
}
