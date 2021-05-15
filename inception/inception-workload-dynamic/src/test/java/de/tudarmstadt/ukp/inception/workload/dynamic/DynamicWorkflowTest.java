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

import static de.tudarmstadt.ukp.clarin.webanno.support.uima.FeatureStructureBuilder.buildFS;
import static java.lang.Thread.sleep;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.h2.util.IOUtils.getInputStreamFromString;

import java.io.File;
import java.time.Duration;

import org.apache.uima.cas.CAS;
import org.junit.jupiter.api.BeforeAll;
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
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingServiceAutoConfiguration;
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
                "repository.path=" + DynamicWorkflowTest.TEST_OUTPUT_FOLDER })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno" })
@Import({ //
        TextFormatsAutoConfiguration.class, //
        DocumentImportExportServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        SchedulingServiceAutoConfiguration.class, //
        WorkloadManagementAutoConfiguration.class, //
        DynamicWorkloadManagerAutoConfiguration.class })
public class DynamicWorkflowTest
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/DynamicWorkflowTest";

    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired UserDao userService;
    private @Autowired WorkloadManagementService workloadManagementService;
    private @Autowired DynamicWorkloadExtension dynamicWorkloadExtension;

    @BeforeAll
    public static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @Test
    public void thatAbandonedDocumentsAreReset() throws Exception
    {
        User annotator = userService.create(new User("anno1"));
        Project project = projectService.createProject(new Project("test"));

        WorkloadManager workloadManager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project);
        workloadManager.setType(DynamicWorkloadExtension.DYNAMIC_WORKLOAD_MANAGER_EXTENSION_ID);
        DynamicWorkloadTraits traits = new DynamicWorkloadTraits();
        traits.setAbandonationTimeout(Duration.of(1, SECONDS));
        traits.setAbandonationState(AnnotationDocumentState.NEW);
        dynamicWorkloadExtension.writeTraits(traits, project);

        SourceDocument doc = documentService
                .createSourceDocument(new SourceDocument("doc.txt", project, TextFormatSupport.ID));
        AnnotationDocument ann = documentService
                .createAnnotationDocument(new AnnotationDocument(annotator.getUsername(), doc));

        try (var session = CasStorageSession.open()) {
            documentService.uploadSourceDocument(getInputStreamFromString("This is a test."), doc);
            CAS cas = documentService.readAnnotationCas(ann);
            buildFS(cas, NamedEntity.class.getName()) //
                    .withFeature("value", "test") //
                    .buildAndAddToIndexes();
            documentService.writeAnnotationCas(cas, ann, true);
        }

        ann = documentService.getAnnotationDocument(doc, annotator.getUsername());
        assertThat(ann.getState()).as("Effective state is in-progress after the CAS update")
                .isEqualTo(AnnotationDocumentState.IN_PROGRESS);
        assertThat(ann.getAnnotatorState()).as(
                "Annotator state is in-progress as the CAS update was marked as explicit action")
                .isEqualTo(AnnotationDocumentState.IN_PROGRESS);

        sleep(traits.getAbandonationTimeout().multipliedBy(2).toMillis());

        dynamicWorkloadExtension.freshenStatus(project);

        ann = documentService.getAnnotationDocument(doc, annotator.getUsername());
        assertThat(ann.getState())
                .as("After the abandonation timeout has passed, the effective state has been reset")
                .isEqualTo(traits.getAbandonationState());
        assertThat(ann.getAnnotatorState()).as(
                "After the abandonation timeout has passed, the annotator state has been cleared")
                .isNull();
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        // All handled via auto-configuration
    }
}
