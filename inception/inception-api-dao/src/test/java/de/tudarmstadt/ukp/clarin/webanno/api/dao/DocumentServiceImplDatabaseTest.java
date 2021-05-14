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

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.BackupProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.CasStoragePropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

@EnableAutoConfiguration
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class, showSql = false)
@EntityScan({ "de.tudarmstadt.ukp.clarin.webanno.model",
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
public class DocumentServiceImplDatabaseTest
{
    public @TempDir File testFolder;

    private @Autowired ProjectService projectService;
    private @Autowired UserDao userRepository;
    private @Autowired DocumentService sut;
    private @Autowired RepositoryProperties repositoryProperties;

    private User annotator1;
    private User annotator2;
    private Project project;

    @BeforeEach
    public void setup() throws Exception
    {
        repositoryProperties.setPath(testFolder);
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

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

        AnnotationDocument ann = sut.createAnnotationDocument(
                new AnnotationDocument("ann", annotator1.getUsername(), doc));

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

        AnnotationDocument ann = sut.createAnnotationDocument(
                new AnnotationDocument("ann", annotator1.getUsername(), doc));

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

    @Configuration
    public static class TestContext
    {
        @Autowired
        ApplicationEventPublisher applicationEventPublisher;

        @Bean
        public ProjectService projectService()
        {
            return new ProjectServiceImpl(userRepository(), applicationEventPublisher,
                    repositoryProperties(), null);
        }

        @Bean
        public UserDao userRepository()
        {
            return new UserDaoImpl();
        }

        @Bean
        public DocumentService documentService()
        {
            return new DocumentServiceImpl(repositoryProperties(), casStorageService(), null, null,
                    applicationEventPublisher);
        }

        @Bean
        public CasStorageService casStorageService()
        {
            return new CasStorageServiceImpl(null, null, repositoryProperties(),
                    new CasStoragePropertiesImpl(), backupProperties());
        }

        @Bean
        public RepositoryProperties repositoryProperties()
        {
            return new RepositoryProperties();
        }

        @Bean
        public BackupProperties backupProperties()
        {
            return new BackupProperties();
        }

        @Bean
        public ApplicationContextProvider contextProvider()
        {
            return new ApplicationContextProvider();
        }
    }
}
