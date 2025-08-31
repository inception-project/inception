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
package de.tudarmstadt.ukp.inception.workload.matrix.service;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.FileSystemUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.config.ConstraintsServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.exporters.AnnotationDocumentExporter;

@EnableAutoConfiguration
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class, showSql = false, //
        properties = { //
                "spring.main.banner-mode=off", //
                "workload.matrix.enabled=true", //
                "repository.path=" + MatrixWorkloadServiceImplIntegrationTest.TEST_OUTPUT_FOLDER })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.security.model", //
        "de.tudarmstadt.ukp.clarin.webanno.model" })
@Import({ //
        ConstraintsServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class })
class MatrixWorkloadServiceImplIntegrationTest
{
    static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final String TEST_OUTPUT_FOLDER = "target/test-output/MatrixWorkloadServiceImplIntegrationTest";

    private @MockitoBean AnnotationDocumentExporter annotationDocumentExporter;
    private @MockitoBean CasStorageService casStorageService;
    private @MockitoBean DocumentImportExportService documentImportExportService;

    private @Autowired ProjectService projectService;
    private @Autowired UserDao userDao;
    private @Autowired DocumentService documentService;

    private @Autowired MatrixWorkloadService sut;

    private Project project;

    private User user1;
    private User user2;
    private User user3;

    private SourceDocument doc1;
    private SourceDocument doc2;
    private SourceDocument doc3;

    @BeforeAll
    public static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @BeforeEach
    void setup() throws Exception
    {
        project = Project.builder().withName("Test").build();
        projectService.createProject(project);

        user1 = User.builder().withUsername("user1").build();
        user2 = User.builder().withUsername("user2").build();
        user3 = User.builder().withUsername("user3").build();
        userDao.create(user1);
        userDao.create(user2);
        userDao.create(user3);

        projectService.assignRole(project, user1, ANNOTATOR);
        projectService.assignRole(project, user2, ANNOTATOR);
        projectService.assignRole(project, user3, ANNOTATOR);

        doc1 = SourceDocument.builder().withName("doc1").withProject(project).build();
        doc2 = SourceDocument.builder().withName("doc2").withProject(project).build();
        doc3 = SourceDocument.builder().withName("doc3").withProject(project).build();
        documentService.createSourceDocument(doc1);
        documentService.createSourceDocument(doc2);
        documentService.createSourceDocument(doc3);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        projectService.removeProject(project);
    }

    @Test
    void testAssignWorkloadOnePerDoc()
    {
        sut.assignWorkload(project, 1, true);

        assertThat(documentService.listAnnotationDocuments(project)) //
                .extracting(AnnotationDocument::getDocument, AnnotationDocument::getUser,
                        AnnotationDocument::getState) //
                .containsExactlyInAnyOrder( //
                        tuple(doc1, "user2", IGNORE), //
                        tuple(doc1, "user3", IGNORE), //
                        tuple(doc2, "user1", IGNORE), //
                        tuple(doc2, "user3", IGNORE), //
                        tuple(doc3, "user1", IGNORE), //
                        tuple(doc3, "user2", IGNORE));
    }

    @Test
    void testAssignWorkloadTwoPerDoc()
    {
        sut.assignWorkload(project, 2, true);

        assertThat(documentService.listAnnotationDocuments(project)) //
                .extracting(AnnotationDocument::getDocument, AnnotationDocument::getUser,
                        AnnotationDocument::getState) //
                .containsExactlyInAnyOrder( //
                        tuple(doc1, "user3", IGNORE), //
                        tuple(doc2, "user2", IGNORE), //
                        tuple(doc3, "user1", IGNORE));
    }

    @Test
    void testRepeatAssignWithoutReopen()
    {
        sut.assignWorkload(project, 2, false);
        sut.assignWorkload(project, 1, false);

        var sortedDocuments = documentService.listAnnotationDocuments(project).stream()
                .sorted(comparing(AnnotationDocument::getName)
                        .thenComparing(AnnotationDocument::getUser)) //
                .toList();
        assertThat(sortedDocuments) //
                .extracting(AnnotationDocument::getDocument, AnnotationDocument::getUser,
                        AnnotationDocument::getState) //
                .containsExactlyInAnyOrder( //
                        tuple(doc1, "user2", IGNORE), //
                        tuple(doc1, "user3", IGNORE), //
                        tuple(doc2, "user1", IGNORE), //
                        tuple(doc2, "user2", IGNORE), //
                        tuple(doc3, "user1", IGNORE), //
                        tuple(doc3, "user3", IGNORE));
    }

    @Test
    void testAssignWithTakenDocuments()
    {
        var annDoc1 = new AnnotationDocument("user3", doc1);
        annDoc1.setState(IN_PROGRESS);
        documentService.createOrUpdateAnnotationDocument(annDoc1);

        var annDoc2 = new AnnotationDocument("user1", doc2);
        annDoc2.setState(FINISHED);
        documentService.createOrUpdateAnnotationDocument(annDoc2);

        sut.assignWorkload(project, 2, true);

        var sortedDocuments = documentService.listAnnotationDocuments(project).stream()
                .sorted(comparing(AnnotationDocument::getName)
                        .thenComparing(AnnotationDocument::getUser)) //
                .toList();
        assertThat(sortedDocuments) //
                .extracting(AnnotationDocument::getDocument, AnnotationDocument::getUser,
                        AnnotationDocument::getState) //
                .containsExactlyInAnyOrder( //
                        tuple(doc1, "user2", IGNORE), //
                        tuple(doc1, "user3", IN_PROGRESS), //
                        tuple(doc2, "user1", FINISHED), //
                        tuple(doc2, "user3", IGNORE), //
                        tuple(doc3, "user1", IGNORE));
    }

    @Test
    void testRepeatAssignWithReopen()
    {
        sut.assignWorkload(project, 1, true);
        sut.assignWorkload(project, 2, true);

        var sortedDocuments = documentService.listAnnotationDocuments(project).stream()
                .filter($ -> $.getState() == IGNORE)
                .sorted(comparing(AnnotationDocument::getName)
                        .thenComparing(AnnotationDocument::getUser)) //
                .toList();
        assertThat(sortedDocuments) //
                .extracting(AnnotationDocument::getDocument, AnnotationDocument::getUser,
                        AnnotationDocument::getState) //
                .containsExactlyInAnyOrder( //
                        tuple(doc1, "user3", IGNORE), //
                        tuple(doc2, "user2", IGNORE), //
                        tuple(doc3, "user1", IGNORE));
    }

    @Test
    void testAssignWorkloadMoreAnnotatorsThanAvailable()
    {
        var annotators = projectService.listUsersWithRoleInProject(project, ANNOTATOR);

        sut.assignWorkload(project, annotators.size() + 1, true);

        assertThat(documentService.listAnnotationDocuments(project)) //
                .isEmpty();
    }

    @SpringBootConfiguration
    static class SpringConfig
    {
        @Bean
        MatrixWorkloadService matrixWorkloadService(DocumentService aDocumentService,
                ProjectService aProjectService)
        {
            return new MatrixWorkloadServiceImpl(aDocumentService, aProjectService);
        }
    }
}
