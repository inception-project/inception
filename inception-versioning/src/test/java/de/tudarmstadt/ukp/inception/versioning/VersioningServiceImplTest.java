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
package de.tudarmstadt.ukp.inception.versioning;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javax.persistence.EntityManager;

import org.apache.uima.cas.CAS;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.BooleanFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.NumberFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.StringFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.BackupProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.ImportExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityTagSetInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.PretokenizedTextFormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.xmi.XmiFormatSupport;

@RunWith(SpringRunner.class)
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
public class VersioningServiceImplTest
{
    private @Autowired VersioningService sut;
    private @Autowired TestEntityManager testEntityManager;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired AnnotationSchemaService annotationSchemaService;
    private @Autowired ProjectService projectService;
    private @Autowired UserDao userDao;
    private @Autowired ImportExportService importExportService;
    private @Autowired DocumentService documentService;

    @Rule
    public TemporaryFolder repoDir = new TemporaryFolder();

    private Project testProject;

    @Before
    public void setUp() throws Exception
    {
        repositoryProperties.setPath(repoDir.getRoot());
        testProject = new Project("testProject");

    }

    @After
    public void tearDown()
    {
        testEntityManager.clear();
    }

    @Test
    public void creatingProject_ShouldCreateGitRepository() throws Exception
    {
        createProject(testProject);

        assertThat(sut.getRepoDir(testProject))
                .isDirectoryContaining(f -> f.getName().equals(".git") && f.isDirectory());
    }

    @Test
    public void deletingProject_ShouldRemoveGitRepository() throws IOException
    {
        projectService.createProject(testProject);
        assertThat(sut.getRepoDir(testProject))
                .isDirectoryContaining(f -> f.getName().equals(".git") && f.isDirectory());

        projectService.removeProject(testProject);

        assertThat(sut.getRepoDir(testProject)).doesNotExist();
    }

    @Test
    @WithMockUser(username = "admin")
    public void snapshottingProject_ShouldCommitFiles() throws Exception
    {
        createProject(testProject);
        User admin = createAdmin();
        User annotator = createAnnotator();

        uploadDocuments();

        createAnnotationDocuments(admin);
        createAnnotationDocuments(annotator);

        sut.snapshotCompleteProject(testProject);

        File repoDir = sut.getRepoDir(testProject);
        Path annotationRoot = repoDir.toPath().resolve(VersioningServiceImpl.DOCUMENTS);
        assertThat(repoDir).isDirectoryContaining(
                f -> f.getName().equals(VersioningServiceImpl.LAYERS) && f.isFile());
        assertThat(annotationRoot.resolve("admin").resolve("dinos.txt.xmi")).isRegularFile();
        assertThat(annotationRoot.resolve("admin").resolve("lorem.txt.xmi")).isRegularFile();
        assertThat(annotationRoot.resolve("annotator").resolve("dinos.txt.xmi")).isRegularFile();
        assertThat(annotationRoot.resolve("annotator").resolve("lorem.txt.xmi")).isRegularFile();
    }

    @Test
    public void getRemote_IfUrlSet_ShouldReturnRemoteUrl() throws Exception
    {
        String url = "git@github.com:inception-project/inception.git";
        createProject(testProject);

        sut.setRemote(testProject, url);

        assertThat(sut.getRemote(testProject)).contains(url);
    }

    @Test
    public void getRemote_IfUrlNotSet_ShouldReturnEmpty() throws Exception
    {
        createProject(testProject);

        assertThat(sut.getRemote(testProject)).isEmpty();
    }

    @Test
    public void repoExists_IfRepoExists_ShouldReturnTrue() throws Exception
    {
        projectService.createProject(testProject);

        assertThat(sut.repoExists(testProject)).isTrue();
    }

    @Test
    public void repoExists_IfRepoDoesNotExists_ShouldReturnFalse() throws Exception
    {
        testProject.setId(42L);

        assertThat(sut.repoExists(testProject)).isFalse();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = { "de.tudarmstadt.ukp.clarin.webanno.model",
            "de.tudarmstadt.ukp.clarin.webanno.security.model" })
    public static class TestContext
    {
        private @Autowired ApplicationEventPublisher applicationEventPublisher;
        private @Autowired EntityManager entityManager;

        @Bean
        public RepositoryProperties repositoryProperties()
        {
            return new RepositoryProperties();
        }

        @Bean
        public AnnotationSchemaService annotationSchemaService(
                LayerSupportRegistry aLayerSupportRegistry,
                FeatureSupportRegistry aFeatureSupportRegistry)
        {
            return new AnnotationSchemaServiceImpl(aLayerSupportRegistry, aFeatureSupportRegistry,
                    entityManager);
        }

        @Bean
        public LayerSupportRegistry layerSupportRegistry(
                FeatureSupportRegistry aFeatureSupportRegistry)
        {
            return new LayerSupportRegistryImpl(
                    asList(new SpanLayerSupport(aFeatureSupportRegistry, null, null),
                            new RelationLayerSupport(aFeatureSupportRegistry, null, null),
                            new ChainLayerSupport(aFeatureSupportRegistry, null, null)));
        }

        @Bean
        public FeatureSupportRegistry featureSupportRegistry()
        {
            return new FeatureSupportRegistryImpl(asList(new NumberFeatureSupport(),
                    new BooleanFeatureSupport(), new StringFeatureSupport()));
        }

        @Bean
        public ProjectService projectService(UserDao aUserDao,
                RepositoryProperties aRepositoryProperties,
                @Lazy @Autowired(required = false) List<ProjectInitializer> aInitializerProxy)
        {
            return new ProjectServiceImpl(aUserDao, applicationEventPublisher,
                    aRepositoryProperties, aInitializerProxy);
        }

        @Bean
        public UserDao userRepository()
        {
            return new UserDaoImpl();
        }

        @Bean
        public DocumentService documentService(RepositoryProperties aRepositoryProperties,
                CasStorageService aCasStorageService, ImportExportService aImportExportService,
                ProjectService aProjectService,
                ApplicationEventPublisher aApplicationEventPublisher)
        {
            return new DocumentServiceImpl(aRepositoryProperties, aCasStorageService,
                    aImportExportService, aProjectService, aApplicationEventPublisher);
        }

        @Bean
        public CasStorageService casStorageService(AnnotationSchemaService aAnnotationSchemaService,
                RepositoryProperties aRepositoryProperties)
        {
            return new CasStorageServiceImpl(new CasDoctor(), aAnnotationSchemaService,
                    aRepositoryProperties, new BackupProperties());
        }

        @Bean
        public ImportExportService importExportService(RepositoryProperties aRepositoryProperties,
                AnnotationSchemaService aAnnotationSchemaService,
                CasStorageService aCasStorageService)
        {
            return new ImportExportServiceImpl(aRepositoryProperties,
                    List.of(new XmiFormatSupport(), new PretokenizedTextFormatSupport()),
                    aCasStorageService, aAnnotationSchemaService);
        }

        @Bean
        public VersioningService versioningService(RepositoryProperties aRepositoryProperties,
                AnnotationSchemaService aAnnotationSchemaService, DocumentService aDocumentService,
                ProjectService aProjectservice, CasStorageService aCasStorageService,
                UserDao aUserDao)
        {
            return new VersioningServiceImpl(aRepositoryProperties, aAnnotationSchemaService,
                    aDocumentService, aProjectservice, aCasStorageService, aUserDao);
        }

        @Lazy
        @Bean
        public TokenLayerInitializer TokenLayerInitializer(
                @Lazy @Autowired AnnotationSchemaService aAnnotationSchemaService)
        {
            return new TokenLayerInitializer(aAnnotationSchemaService);
        }

        @Lazy
        @Bean
        public NamedEntityLayerInitializer namedEntityLayerInitializer(
                @Lazy @Autowired AnnotationSchemaService aAnnotationService)
        {
            return new NamedEntityLayerInitializer(aAnnotationService);
        }

        @Lazy
        @Bean
        public NamedEntityTagSetInitializer namedEntityTagSetInitializer(
                @Lazy @Autowired AnnotationSchemaService aAnnotationService)
        {
            return new NamedEntityTagSetInitializer(aAnnotationService);
        }
    }

    private User addUser(String aUserName)
    {
        User user = new User();
        user.setUsername(aUserName);
        userDao.create(user);

        return user;
    }

    private void createProject(Project aProject) throws Exception
    {
        projectService.createProject(aProject);
        projectService.initializeProject(aProject);
    }

    private SourceDocument buildSourceDocument(long aDocumentId)
    {
        SourceDocument doc = new SourceDocument();
        doc.setProject(testProject);
        doc.setId(aDocumentId);

        return doc;
    }

    private File getResource(String aResourceName)
    {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(aResourceName);
        Objects.requireNonNull(resource);
        return new File(resource.getFile());
    }

    private User createAdmin()
    {
        User admin = addUser("admin");
        projectService.setProjectPermissionLevels(admin, testProject,
                List.of(ANNOTATOR, CURATOR, MANAGER));
        return admin;
    }

    private User createAnnotator()
    {
        User annotator = addUser("annotator");
        projectService.setProjectPermissionLevels(annotator, testProject, List.of(ANNOTATOR));
        return annotator;
    }

    private void uploadDocuments() throws Exception
    {

        File doc1 = getResource("docs/dinos.txt");
        File doc2 = getResource("docs/lorem.txt");

        try (CasStorageSession session = CasStorageSession.open()) {
            try (InputStream is = Files.newInputStream(doc1.toPath())) {
                documentService.uploadSourceDocument(is,
                        new SourceDocument(doc1.getName(), testProject, "pretokenized-textlines"));
            }

            try (InputStream is = Files.newInputStream(doc2.toPath())) {
                documentService.uploadSourceDocument(is,
                        new SourceDocument(doc2.getName(), testProject, "pretokenized-textlines"));
            }
        }
    }

    private void createAnnotationDocuments(User aUser) throws Exception
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            for (SourceDocument sourceDocument : documentService.listSourceDocuments(testProject)) {
                AnnotationDocument annotationDocument = documentService
                        .createOrGetAnnotationDocument(sourceDocument, aUser);
                CAS cas = documentService.createOrReadInitialCas(sourceDocument);
                documentService.writeAnnotationCas(cas, sourceDocument, aUser, false);
            }
        }
    }

}
