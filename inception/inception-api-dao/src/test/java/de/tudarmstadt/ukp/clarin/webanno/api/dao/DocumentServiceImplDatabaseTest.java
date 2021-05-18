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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.h2.util.IOUtils.getInputStreamFromString;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.util.FileSystemUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.annotationservice.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.documentservice.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingServiceAutoConfiguration;

@EnableAutoConfiguration
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class, showSql = false, //
        properties = { //
                "spring.main.banner-mode=off", //
                "repository.path=" + DocumentServiceImplDatabaseTest.TEST_OUTPUT_FOLDER })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@Import({ //
        DocumentImportExportServiceAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        SchedulingServiceAutoConfiguration.class })
public class DocumentServiceImplDatabaseTest
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/DocumentServiceImplDatabaseTest";

    private @Autowired ProjectService projectService;
    private @Autowired UserDao userRepository;
    private @Autowired DocumentService sut;

    private User annotator1;
    private User annotator2;
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
        annotator2 = userRepository.create(new User("anno2"));

        project = projectService.createProject(new Project("project"));
        projectService.createProjectPermission(
                new ProjectPermission(project, annotator1.getUsername(), ANNOTATOR));
    }

    @Test
    public void testThatAnnotationDocumentsForNonExistingUserAreNotReturned() throws Exception
    {
        SourceDocument doc = sut.createSourceDocument(new SourceDocument("doc", project, "text"));

        AnnotationDocument ann = sut
                .createAnnotationDocument(new AnnotationDocument(annotator1.getUsername(), doc));

        assertThat(sut.listAnnotationDocuments(doc))
                .as("As long as the user exists, the annotation document must be found")
                .containsExactly(ann);

        userRepository.delete(annotator1);

        assertThat(sut.listAnnotationDocuments(doc))
                .as("When the user is deleted, the document must no longer be found") //
                .isEmpty();
    }

    @Test
    public void thatExplicitUserActionsSetAnnotatorState()
    {
        SourceDocument doc = sut.createSourceDocument(new SourceDocument("doc", project, "text"));

        AnnotationDocument ann = sut
                .createAnnotationDocument(new AnnotationDocument(annotator1.getUsername(), doc));

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
        SourceDocument doc = sut
                .createSourceDocument(new SourceDocument("doc.txt", project, TextFormatSupport.ID));

        AnnotationDocument ann = sut
                .createAnnotationDocument(new AnnotationDocument(annotator1.getUsername(), doc));

        try (var session = CasStorageSession.open()) {
            sut.uploadSourceDocument(getInputStreamFromString("This is a test."), doc);
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
        // All handled via auto-configuration
    }
}
