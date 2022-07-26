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
package de.tudarmstadt.ukp.inception.workload.dynamic;

import static java.lang.Thread.sleep;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.Duration;

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
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagementAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.dynamic.config.DynamicWorkloadManagerAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.dynamic.trait.DynamicWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

@EnableAutoConfiguration
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class, showSql = false, //
        properties = { //
                "spring.main.banner-mode=off", //
                "workload.dynamic.enabled=true", //
                "repository.path=" + DynamicWorkloadExtensionImpl2Test.TEST_OUTPUT_FOLDER })
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
        DynamicWorkloadManagerAutoConfiguration.class })
public class DynamicWorkloadExtensionImpl2Test
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/DynamicWorkloadExtensionImpl2Test";

    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired UserDao userService;
    private @Autowired WorkloadManagementService workloadManagementService;
    private @Autowired DynamicWorkloadExtension dynamicWorkloadExtension;
    private @Autowired SessionRegistry sessionRegistry;

    private User annotator;
    private Project project;
    private SourceDocument sourceDocument;
    private AnnotationDocument annotationDocument;
    private DynamicWorkloadTraits traits;

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
        workloadManager.setType(DynamicWorkloadExtension.DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        projectService.removeProject(project);
    }

    @Test
    public void thatRecalculatingStateDoesNotFallBacKBehindCuration() throws Exception
    {
        documentService.setSourceDocumentState(sourceDocument,
                SourceDocumentState.CURATION_IN_PROGRESS);

        dynamicWorkloadExtension.recalculate(project);

        sourceDocument = documentService.getSourceDocument(project.getId(), sourceDocument.getId());

        assertThat(sourceDocument.getState()).isEqualTo(SourceDocumentState.CURATION_IN_PROGRESS);
    }

    @Test
    public void thatAbandonedDocumentsAreReset() throws Exception
    {
        traits = new DynamicWorkloadTraits();
        traits.setAbandonationTimeout(Duration.of(1, SECONDS));
        traits.setAbandonationState(AnnotationDocumentState.NEW);
        dynamicWorkloadExtension.writeTraits(traits, project);

        var ann = documentService.getAnnotationDocument(annotationDocument.getDocument(),
                annotationDocument.getUser());
        assertThat(ann.getState()) //
                .as("Effective state is in-progress after the CAS update")
                .isEqualTo(AnnotationDocumentState.IN_PROGRESS);
        assertThat(ann.getAnnotatorState()) //
                .as("Annotator state is in-progress as the CAS update was marked as explicit action")
                .isEqualTo(AnnotationDocumentState.IN_PROGRESS);

        sleep(traits.getAbandonationTimeout().multipliedBy(2).toMillis());

        dynamicWorkloadExtension.freshenStatus(project);

        var annAfterRefresh = documentService.getAnnotationDocument(
                annotationDocument.getDocument(), annotationDocument.getUser());
        assertThat(annAfterRefresh.getState())
                .as("After the abandonation timeout has passed, the effective state has been reset")
                .isEqualTo(traits.getAbandonationState());
        assertThat(annAfterRefresh.getAnnotatorState()) //
                .as("After the abandonation timeout has passed, the annotator state has been cleared")
                .isNull();
    }

    @Test
    public void thatDocumentsForUsersLoggedInAreExemptFromAbandonation() throws Exception
    {
        traits = new DynamicWorkloadTraits();
        traits.setAbandonationTimeout(Duration.of(1, SECONDS));
        traits.setAbandonationState(AnnotationDocumentState.NEW);
        dynamicWorkloadExtension.writeTraits(traits, project);

        var ann = documentService.getAnnotationDocument(annotationDocument.getDocument(),
                annotationDocument.getUser());
        assertThat(ann.getState()) //
                .as("Effective state is in-progress after the CAS update")
                .isEqualTo(AnnotationDocumentState.IN_PROGRESS);
        assertThat(ann.getAnnotatorState()) //
                .as("Annotator state is in-progress as the CAS update was marked as explicit action")
                .isEqualTo(AnnotationDocumentState.IN_PROGRESS);

        sessionRegistry.registerNewSession("dummy-session-id", annotator.getUsername());

        sleep(traits.getAbandonationTimeout().multipliedBy(2).toMillis());

        dynamicWorkloadExtension.freshenStatus(project);

        var annAfterRefresh = documentService.getAnnotationDocument(ann.getDocument(),
                ann.getUser());
        assertThat(annAfterRefresh.getUpdated()) //
                .as("Database record was not updated at all") //
                .isEqualTo(ann.getUpdated());
        assertThat(annAfterRefresh.getState())
                .as("Even After the abandonation timeout has passed, nothing has changed")
                .isEqualTo(AnnotationDocumentState.IN_PROGRESS);
        assertThat(annAfterRefresh.getAnnotatorState()) //
                .as("Even After the abandonation timeout has passed, nothing has changed") //
                .isEqualTo(AnnotationDocumentState.IN_PROGRESS);
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
