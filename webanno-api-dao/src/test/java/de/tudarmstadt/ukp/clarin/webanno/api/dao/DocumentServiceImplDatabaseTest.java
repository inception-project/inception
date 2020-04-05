/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;

@RunWith(SpringRunner.class) 
@EnableAutoConfiguration
@DataJpaTest
@EntityScan({
    "de.tudarmstadt.ukp.clarin.webanno.model",
    "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@Transactional(propagation = Propagation.NEVER)
public class DocumentServiceImplDatabaseTest
{
    private @Autowired ProjectService projectService;
    private @Autowired UserDao userRepository;
    private @Autowired DocumentService documentService;
    
    @Test
    public void testThatAnnotationDocumentsForNonExistingUserAreNotReturned() throws Exception
    {
        User user1 = new User("user1");
        userRepository.create(user1);
        User user2 = new User("user2");
        userRepository.create(user2);
        
        Project project = new Project("project");
        projectService.createProject(project);
        projectService.createProjectPermission(
                new ProjectPermission(project, user1.getUsername(), ANNOTATOR));
        
        SourceDocument doc = new SourceDocument("doc", project, "text");
        documentService.createSourceDocument(doc);
        
        AnnotationDocument ann = new AnnotationDocument("ann", project, user1.getUsername(), doc);
        documentService.createAnnotationDocument(ann);

        // As long as the user exists, the annotation document must be found
        assertThat(documentService.listAnnotationDocuments(doc))
            .containsExactly(ann);
        
        userRepository.delete(user1);
        
        // When the user is deleted, the document must no longer be found
        assertThat(documentService.listAnnotationDocuments(doc)).isEmpty();
    }
    
    @Configuration
    public static class TestContext {
        @Autowired ApplicationEventPublisher applicationEventPublisher;
        
        @Bean
        public ProjectService projectService()
        {
            return new ProjectServiceImpl();
        }
        
        @Bean
        public UserDao userRepository()
        {
            return new UserDaoImpl();
        }
        
        @Bean
        public DocumentService documentService()
        {
            return new DocumentServiceImpl(repositoryProperties(), userRepository(),
                    casStorageService(), null, null, applicationEventPublisher);
        }
        
        @Bean
        public CasStorageService casStorageService()
        {
            return new CasStorageServiceImpl(null, null, repositoryProperties(),
                    backupProperties());
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
