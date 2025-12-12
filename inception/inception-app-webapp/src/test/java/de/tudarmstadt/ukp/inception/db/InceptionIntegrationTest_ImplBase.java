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
package de.tudarmstadt.ukp.inception.db;

import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_APPLICATION_MODE;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_AUTH_MODE_DATABASE;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_INTERNAL_SERVER;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_PRODUCTION_MODE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.webanno.StandardProjectInitializer;
import de.tudarmstadt.ukp.inception.INCEpTION;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.app.config.InceptionApplicationContextInitializer;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.cli.RebuildStateUpdatedFieldsCliCommand;
import de.tudarmstadt.ukp.inception.log.EventLoggingListener;
import de.tudarmstadt.ukp.inception.log.api.EventRepository;
import de.tudarmstadt.ukp.inception.log.api.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.Logging;

@ActiveProfiles({ //
        PROFILE_APPLICATION_MODE, //
        PROFILE_AUTH_MODE_DATABASE, //
        PROFILE_INTERNAL_SERVER, //
        PROFILE_PRODUCTION_MODE })
@ExtendWith(SpringExtension.class)
@SpringBootTest( //
        classes = { INCEpTION.class }, //
        properties = { "spring.main.banner-mode=off" })
@ContextConfiguration(initializers = { InceptionApplicationContextInitializer.class })
public abstract class InceptionIntegrationTest_ImplBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectExportService projectExportService;

    @Autowired
    StandardProjectInitializer standardProjectInitializer;

    @Autowired
    RepositoryProperties repositoryProperties;

    @Autowired
    DocumentService documentService;

    @Autowired
    AnnotationSchemaService schemaService;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    EventLoggingListener eventLoggingListener;

    @Autowired
    LearningRecordService learningRecordService;

    @BeforeAll
    static void setupClass(TestInfo aInfo)
    {
        LOG.info("============================================================");
        LOG.info("= Running {}", aInfo.getTestClass().get().getSimpleName());
        LOG.info("============================================================");
    }

    @BeforeEach
    void setup()
    {
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
    }

    @AfterEach
    void tearDown()
    {
        MDC.clear();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry)
    {
        registry.add("running.from.commandline", () -> "true");
    }

    @Test
    void testContextStartsUpSuccessfully() throws Exception
    {
        // Nothing
    }

    @Test
    void testCreatingAndDeletingProject() throws Exception
    {
        var project = Project.builder().withName("test").withSlug("test").build();
        projectService.createProject(project);

        var request = ProjectInitializationRequest.builder() //
                .withProject(project) //
                .withIncludeSampleData(true) //
                .build();

        projectService.initializeProject(request, asList(standardProjectInitializer));

        projectService.removeProject(project);
    }

    @Test
    void testImportingAndDeletingRemovingProject() throws Exception
    {
        var request = ProjectImportRequest.builder() //
                .withCreateMissingUsers(true) //
                .withImportPermissions(true) //
                .build();

        Project project;
        try (var zipFile = new ZipFile("src/test/resources/test-project-with-recommenders.zip")) {
            project = projectExportService.importProject(request, zipFile);
        }

        projectService.removeProject(project);
    }

    @Test
    void testDeletingSourceDocumentWithLearningRecordAttached() throws Exception
    {
        var project = Project.builder() //
                .withName("test") //
                .build();

        var layer = AnnotationLayer.builder() //
                .withProject(project) //
                .withName("Layer") //
                .withUiName("Layer") //
                .withType(SpanLayerSupport.TYPE) //
                .build();

        var doc = SourceDocument.builder() //
                .withProject(project) //
                .withName("Blah") //
                .build();

        var learningRecord = LearningRecord.builder() //
                .withLayer(layer) //
                .withSourceDocument(doc) //
                .withSuggestionType(layer.getType()) //
                .build();
        try {
            projectService.createProject(project);
            schemaService.createOrUpdateLayer(layer);
            documentService.createSourceDocument(doc);
            learningRecordService.createLearningRecord(learningRecord);

            documentService.removeSourceDocument(doc);
        }
        finally {
            projectService.removeProject(project);
        }
    }

    @Test
    void testRebuildStateUpdatedSetsProjectStateUpdatedFromEvent() throws Exception
    {
        // Create a project and ensure the state_updated database field is null
        var project = new Project("rebuild-state-test");
        project = projectService.createProject(project);

        try {
            // Ensure the DB field is NULL
            projectService.updateProjectStateUpdatedDirectly(project.getId(), null);

            assertThat(projectService.getProject(project.getId()).getStateUpdated()) //
                    .as("stateUpdated") //
                    .isNull();

            // Change the project state to generate an event
            projectService.setProjectState(project, ProjectState.ANNOTATION_IN_PROGRESS);

            // Ensure events are flushed to the DB (EventLoggingListener schedules flushes)
            eventLoggingListener.flush();

            var eventsCreated = new ArrayList<LoggedEvent>();
            eventRepository.forEachLoggedEvent(project, event -> {
                if ("ProjectStateChangedEvent".equals(event.getEvent())) {
                    eventsCreated.add(event);
                }
            });

            assertThat(eventsCreated) //
                    .hasSize(1) //
                    .allSatisfy(e -> assertThat(e.getCreated()).isNotNull());

            // Set the database field back to NULL to ensure it has to be rebuilt
            projectService.updateProjectStateUpdatedDirectly(project.getId(), null);
            assertThat(projectService.getProject(project.getId()).getStateUpdated()) //
                    .as("stateUpdated") //
                    .isNull();

            // Run the rebuild and check that the project state_updated field gets set to the event
            // timestamp
            var rebuildStateUpdatedFieldsCliCommand = new RebuildStateUpdatedFieldsCliCommand(
                    projectService, documentService, eventRepository);
            rebuildStateUpdatedFieldsCliCommand
                    .rebuildAll(LoggerFactory.getLogger("inception.test"));

            var projectAfter = projectService.getProject(project.getId());
            // Allow a small time tolerance for the state_updated value because some DB backends
            // apply timestamps with slightly different resolution. The important thing is that
            // the timestamp was propagated from the event, not that it matches exactly down to
            // the millisecond.
            var expectedInstant = eventsCreated.get(0).getCreated().toInstant();
            var actualInstant = projectAfter.getStateUpdated().toInstant();
            var deltaMillis = Math.abs(Duration.between(expectedInstant, actualInstant).toMillis());
            assertThat(deltaMillis).as("Timestamp difference (ms)").isLessThanOrEqualTo(200);
        }
        finally {
            projectService.removeProject(project);
        }
    }
}
