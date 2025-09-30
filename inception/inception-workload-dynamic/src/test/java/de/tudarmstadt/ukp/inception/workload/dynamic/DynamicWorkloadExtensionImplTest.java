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
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.util.CasCreationUtils;
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
import org.springframework.util.FileSystemUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
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
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagementAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.dynamic.config.DynamicWorkloadManagerAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.dynamic.trait.DynamicWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.dynamic.workflow.types.DefaultWorkflowExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@EnableAutoConfiguration
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class, showSql = false, //
        properties = { //
                "spring.main.banner-mode=off", //
                "workload.dynamic.enabled=true", //
                "repository.path=" + DynamicWorkloadExtensionImplTest.TEST_OUTPUT_FOLDER })
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
        DynamicWorkloadManagerAutoConfiguration.class })
public class DynamicWorkloadExtensionImplTest
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/DynamicWorkloadExtensionImplTest";

    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired UserDao userService;
    private @Autowired WorkloadManagementService workloadManagementService;
    private @Autowired DynamicWorkloadExtension dynamicWorkloadExtension;
    private @Autowired DataSource dataSource;

    private User annotator;
    private User otherAnnotator;
    private Project project;
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
        otherAnnotator = userService.create(new User("anno2"));
        project = projectService.createProject(new Project("test"));

        var workloadManager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project);
        workloadManager.setType(DynamicWorkloadExtension.DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID);
        traits = new DynamicWorkloadTraits();
        traits.setDefaultNumberOfAnnotations(1);
        traits.setAbandonationTimeout(Duration.of(1, SECONDS));
        traits.setAbandonationState(AnnotationDocumentState.NEW);
        traits.setWorkflowType(DefaultWorkflowExtension.DEFAULT_WORKFLOW);
        dynamicWorkloadExtension.writeTraits(traits, project);
    }

    @Test
    public void thatInProgressDocumentsAreReturnedBeforeNewDocuments() throws Exception
    {
        // Order matters here because documents tend to be returned in alphabetical order and
        // it would be nice if we'd actually not get NEW or IN_PROGRESS document randomly as the
        // first option to pick.
        createAnnotationDocument("1.txt", AnnotationDocumentState.FINISHED);
        createAnnotationDocument("2.txt", AnnotationDocumentState.NEW);
        createAnnotationDocument("3.txt", AnnotationDocumentState.IN_PROGRESS);

        var nextDoc = dynamicWorkloadExtension.nextDocumentToAnnotate(project, annotator);

        assertThat(nextDoc)
                .as("If there is a document already in progress, it should be picked next")
                .map(SourceDocument::getName) //
                .isPresent().get().isEqualTo("3.txt");
    }

    @Test
    public void thatDocumentsWithoutAnnotationDocumentsAreReturned() throws Exception
    {
        // Order matters here because documents tend to be returned in alphabetical order and
        // it would be nice if we'd actually not get NEW or IN_PROGRESS document randomly as the
        // first option to pick.
        createAnnotationDocument("1.txt", AnnotationDocumentState.FINISHED);
        createAnnotationDocument("2.txt", AnnotationDocumentState.IGNORE);
        // For the last document, we don't even have an AnnotationDocument yet! The state is
        // implicitly NEW
        createSourceDocument("3.txt");

        var nextDoc = dynamicWorkloadExtension.nextDocumentToAnnotate(project, annotator);

        assertThat(nextDoc)
                .as("If there is a new document an none that is in-progress, pick the new one")
                .map(SourceDocument::getName) //
                .isPresent().get().isEqualTo("3.txt");
    }

    @Test
    public void thatDocumentsAnotherUserIsWorkingOnAreNotReturned() throws Exception
    {
        var ann = new AnnotationDocument(otherAnnotator.getUsername(),
                createSourceDocument("1.txt"));
        Fixtures.importTestSourceDocumentAndAddNamedEntity(documentService, ann);
        ann.setState(AnnotationDocumentState.IN_PROGRESS);
        ann = documentService.getAnnotationDocument(ann.getDocument(),
                AnnotationSet.forUser(ann.getUser()));
        ann.setTimestamp(new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()));
        documentService.createOrUpdateAnnotationDocument(ann);

        var nextDoc = dynamicWorkloadExtension.nextDocumentToAnnotate(project, annotator);

        assertThat(nextDoc) //
                .as("There are no documents left to annotate") //
                .isNotPresent();
    }

    @Test
    public void thatAbandonedDocumentsAreNotReturned() throws Exception
    {
        var ann = new AnnotationDocument(otherAnnotator.getUsername(),
                createSourceDocument("1.txt"));
        Fixtures.importTestSourceDocumentAndAddNamedEntity(documentService, ann);

        sleep(traits.getAbandonationTimeout().multipliedBy(2).toMillis());

        var nextDoc = dynamicWorkloadExtension.nextDocumentToAnnotate(project, annotator);

        assertThat(nextDoc) //
                .as("Document was abandoned by other user, so it can be worked on now")
                .map(SourceDocument::getName) //
                .isPresent().get().isEqualTo("1.txt");
    }

    private SourceDocument createSourceDocument(String aName)
    {
        return documentService
                .createSourceDocument(new SourceDocument(aName, project, TextFormatSupport.ID));
    }

    private AnnotationDocument createAnnotationDocument(String aName,
            AnnotationDocumentState aState)
    {
        var doc = createSourceDocument(aName);
        var ann = new AnnotationDocument(annotator.getUsername(), doc);
        ann.setState(aState);
        return documentService.createOrUpdateAnnotationDocument(ann);
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
