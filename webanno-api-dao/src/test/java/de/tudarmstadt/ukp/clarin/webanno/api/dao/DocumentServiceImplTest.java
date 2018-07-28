/*
 * Copyright 2018
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class DocumentServiceImplTest
{
    private DocumentService sut;

    private @Mock UserDao userRepository;
    private @Mock ImportExportService importExportService;
    private @Mock ProjectService projectService;
    private @Mock ApplicationEventPublisher applicationEventPublisher;
    private @Mock EntityManager entityManager;

    private BackupProperties backupProperties;
    private RepositoryProperties repositoryProperties;
    private CasStorageService storageService;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception
    {
        initMocks(this);

        backupProperties = new BackupProperties();

        repositoryProperties = new RepositoryProperties();
        repositoryProperties.setPath(testFolder.newFolder());

        storageService = new CasStorageServiceImpl(null, repositoryProperties, backupProperties);

        sut = new DocumentServiceImpl(repositoryProperties, userRepository, storageService,
                importExportService, projectService, applicationEventPublisher, entityManager);
    }

    @Test
    public void thatCreatingOrReadingInitialCasForNewDocumentCreatesNewCas() throws Exception
    {
        when(importExportService.importCasFromFile(any(File.class), any(Project.class),
                any())).thenReturn(JCasFactory.createText("Test"));

        SourceDocument doc = makeSourceDocument(1l, 1l, "test");

        JCas cas = sut.createOrReadInitialCas(doc);

        assertThat(cas).isNotNull();
        assertThat(cas.getDocumentText()).isEqualTo("Test");
        assertThat(storageService.getCasFile(doc, WebAnnoConst.INITIAL_CAS_PSEUDO_USER)).exists();
    }

    @Test
    public void thatReadingNonExistentAnnotationCasCreatesNewCas() throws Exception
    {
        SourceDocument sourceDocument = makeSourceDocument(1l, 1l, "test");
        User user = makeUser();
        when(userRepository.get(user.getUsername())).thenReturn(user);
        when(entityManager.createQuery(anyString(), any())).thenThrow(NoResultException.class);
        when(importExportService.importCasFromFile(any(File.class), any(Project.class),
                any())).thenReturn(JCasFactory.createText("Test"));

        JCas cas = sut.readAnnotationCas(sourceDocument, user.getUsername());

        assertThat(cas).isNotNull();
        assertThat(cas.getDocumentText()).isEqualTo("Test");
        assertThat(storageService.getCasFile(sourceDocument, user.getUsername())).exists();
    }

    private SourceDocument makeSourceDocument(long aProjectId, long aDocumentId, String aDocName)
    {
        Project project = new Project();
        project.setId(aProjectId);

        SourceDocument doc = new SourceDocument();
        doc.setProject(project);
        doc.setId(aDocumentId);
        doc.setName(aDocName);

        return doc;
    }

    private User makeUser() {
        User user = new User();
        user.setUsername("Test user");
        return user;
    }
}
