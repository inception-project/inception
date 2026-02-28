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
package de.tudarmstadt.ukp.inception.curation.service;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.config.ConstraintsServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.config.CurationDocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;

@EnableAutoConfiguration
@DataJpaTest(showSql = false, //
        properties = { //
                "spring.main.banner-mode=off" })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@Import({ //
        EventLoggingAutoConfiguration.class, //
        ConstraintsServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        CurationDocumentServiceAutoConfiguration.class })
class CurationDocumentServiceImplTest
{
    static @TempDir Path tempFolder;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry)
    {
        registry.add("repository.path", () -> tempFolder.toAbsolutePath().toString());
    }

    private @Autowired TestEntityManager testEntityManager;

    private @Autowired DocumentService documentService;
    private @Autowired CurationDocumentServiceImpl sut;

    private User beate;
    private User kevin;
    private Project testProject;
    private SourceDocument testDocument;

    @BeforeEach
    void setup() throws Exception
    {
        // create users
        var current = new User("current", ROLE_USER);
        beate = new User("beate", ROLE_USER);
        kevin = new User("kevin", ROLE_USER);
        testEntityManager.persist(current);
        testEntityManager.persist(beate);
        testEntityManager.persist(kevin);

        // create project
        testProject = new Project("test-project");
        testEntityManager.persist(testProject);
        testEntityManager.persist(new ProjectPermission(testProject, "beate", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "kevin", ANNOTATOR));
        testEntityManager.persist(new ProjectPermission(testProject, "beate", CURATOR));

        testDocument = new SourceDocument("doc", testProject, "text");
        testEntityManager.persist(testDocument);
    }

    @Test
    void testListCuratableSourceDocuments_legacy()
    {
        var ann = documentService.createOrUpdateAnnotationDocument(
                new AnnotationDocument(beate.getUsername(), testDocument));

        assertThat(sut.listCuratableSourceDocuments_legacy(testProject))
                .as("No curatable documents as long as no document is marked as finished")
                .isEmpty();

        documentService.setAnnotationDocumentState(ann, FINISHED);

        assertThat(sut.listCuratableSourceDocuments_legacy(testProject)) //
                .as("Finished documents become curatable") //
                .contains(testDocument);
    }

    @Test
    void testListCuratableSourceDocuments_new()
    {
        assertThat(sut.listCuratableSourceDocuments_new(testProject))
                .as("No curatable documents as long as source document is not marked as finished")
                .isEmpty();

        documentService.setSourceDocumentState(testDocument, ANNOTATION_FINISHED);

        assertThat(sut.listCuratableSourceDocuments_new(testProject)) //
                .as("Source documents marked ANNOTATION_FINISHED become curatable") //
                .contains(testDocument);
    }

    @Test
    void listCuratableUsers_ShouldReturnFinishedUsers()
    {
        // create finished annotation documents
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("beate") //
                .forDocument(testDocument) //
                .withState(FINISHED) //
                .build());

        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("kevin") //
                .forDocument(testDocument) //
                .withAnnotatorState(IGNORE) //
                .build());

        var finishedUsers = sut.listCuratableUsers(testDocument);

        assertThat(finishedUsers).containsExactly(beate, kevin);
    }

    @Test
    void listFinishedUsers_ShouldReturnFinishedUsers()
    {
        // create finished annotation documents
        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("beate") //
                .forDocument(testDocument) //
                .withState(FINISHED) //
                .build());

        testEntityManager.persist(AnnotationDocument.builder() //
                .withUser("kevin") //
                .forDocument(testDocument) //
                .withState(FINISHED) //
                .build());

        var finishedUsers = sut.listCuratableUsers(testDocument);

        assertThat(finishedUsers).containsExactly(beate, kevin);
    }

    @SpringBootConfiguration
    static class TestContext
    {
        @Bean
        DocumentImportExportService documentImportExportService(
                AnnotationSchemaService aSchemaService)
            throws Exception
        {
            var tsd = createTypeSystemDescription();
            var importService = mock(DocumentImportExportService.class);
            when(importService.importCasFromFile(any(), any(), any(), any()))
                    .thenReturn(CasCreationUtils.createCas(tsd, null, null, null));
            return importService;
        }
    }
}
