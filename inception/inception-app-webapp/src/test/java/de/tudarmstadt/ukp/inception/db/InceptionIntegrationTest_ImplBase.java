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

import java.lang.invoke.MethodHandles;
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
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.webanno.StandardProjectInitializer;
import de.tudarmstadt.ukp.inception.INCEpTION;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.app.config.InceptionApplicationContextInitializer;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
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
}
