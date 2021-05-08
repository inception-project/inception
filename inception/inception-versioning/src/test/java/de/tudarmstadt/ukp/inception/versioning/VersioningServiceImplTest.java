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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManager;

import org.apache.uima.cas.CAS;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
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

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
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
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentImportExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.BackupProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.CasStoragePropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServiceProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServicePropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentServiceImpl;
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
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.clarin.webanno.text.PretokenizedTextFormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.xmi.XmiFormatSupport;

@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
public class VersioningServiceImplTest
{
    private @Autowired VersioningService sut;
    private @Autowired TestEntityManager testEntityManager;
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired ProjectService projectService;
    private @Autowired UserDao userDao;
    private @Autowired DocumentService documentService;

    @TempDir
    File repositoryDir;

    private Project testProject;

    @BeforeEach
    public void setUp()
    {
        repositoryProperties.setPath(repositoryDir);
        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        testProject = new Project("testProject");

    }

    @AfterEach
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
    public void deletingProject_ShouldRemoveGitRepository() throws Exception
    {
        createProject(testProject);
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

        sut.snapshotCompleteProject(testProject, "Test snapshotting");

        File repoDir = sut.getRepoDir(testProject);

        // Check commit
        Git git = Git.open(repoDir);

        List<RevCommit> commits = StreamSupport.stream(git.log() //
                .call().spliterator(), false) //
                .collect(Collectors.toList());
        assertThat(commits).hasSize(1);

        RevCommit commit = commits.get(0);
        assertThat(commit.getShortMessage()).isEqualTo("Test snapshotting");
        assertThat(commit.getAuthorIdent().getName()).isEqualTo("admin");
        assertThat(commit.getAuthorIdent().getEmailAddress()).isEqualTo("admin@inception");

        // Check repo contents
        TreeWalk treeWalk = new TreeWalk(git.getRepository());
        treeWalk.addTree(commit.getTree());
        treeWalk.setRecursive(true);

        List<String> documents = new ArrayList<>();
        while (treeWalk.next()) {
            documents.add(treeWalk.getPathString());
        }

        assertThat(documents).containsExactlyInAnyOrder( //
                "layers.json", //

                "document/dinos.txt/source.xmi", //
                "document/dinos.txt/initial.xmi", //
                "document/dinos.txt/curation.xmi", //
                "document/dinos.txt/admin.xmi", //
                "document/dinos.txt/annotator.xmi", //

                "document/lorem.txt/source.xmi", //
                "document/lorem.txt/initial.xmi", //
                "document/lorem.txt/curation.xmi", //
                "document/lorem.txt/admin.xmi", //
                "document/lorem.txt/annotator.xmi" //
        );
    }

    @Test
    @WithMockUser(username = "admin")
    public void pushingRepository_WithLocalRemote_ShouldPushFiles() throws Exception
    {
        createProject(testProject);
        User admin = createAdmin();
        uploadDocuments();
        createAnnotationDocuments(admin);

        File remoteDir = Files.createTempDirectory("remote").toFile();
        Git remoteGit = Git.init().setDirectory(remoteDir).setBare(true).call();

        sut.snapshotCompleteProject(testProject, "Test snapshotting");
        sut.setRemote(testProject, remoteDir.getAbsolutePath());
        sut.pushToOrigin(testProject);

        // Check commit
        List<RevCommit> commits = StreamSupport.stream(remoteGit.log() //
                .call().spliterator(), false) //
                .collect(Collectors.toList());
        assertThat(commits).hasSize(1);

        RevCommit commit = commits.get(0);
        assertThat(commit.getFullMessage()).isEqualTo("Test snapshotting");
        assertThat(commit.getAuthorIdent().getName()).isEqualTo("admin");
        assertThat(commit.getAuthorIdent().getEmailAddress()).isEqualTo("admin@inception");

        // Check remote repo contents
        TreeWalk treeWalk = new TreeWalk(remoteGit.getRepository());
        treeWalk.addTree(commit.getTree());
        treeWalk.setRecursive(true);

        List<String> remoteFiles = new ArrayList<>();
        while (treeWalk.next()) {
            remoteFiles.add(treeWalk.getPathString());
        }
        assertThat(remoteFiles).containsExactlyInAnyOrder( //
                "layers.json", //

                "document/dinos.txt/source.xmi", //
                "document/dinos.txt/initial.xmi", //
                "document/dinos.txt/curation.xmi", //
                "document/dinos.txt/admin.xmi", //

                "document/lorem.txt/source.xmi", //
                "document/lorem.txt/initial.xmi", //
                "document/lorem.txt/curation.xmi", //
                "document/lorem.txt/admin.xmi" //
        );
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
        createProject(testProject);

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
                    asList(new SpanLayerSupport(aFeatureSupportRegistry, null, null, null),
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
                CasStorageService aCasStorageService,
                DocumentImportExportService aImportExportService, ProjectService aProjectService,
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
                    aRepositoryProperties, new CasStoragePropertiesImpl(), new BackupProperties());
        }

        @Bean
        public DocumentImportExportService importExportService(
                RepositoryProperties aRepositoryProperties,
                AnnotationSchemaService aAnnotationSchemaService,
                CasStorageService aCasStorageService)
        {
            DocumentImportExportServiceProperties properties = new DocumentImportExportServicePropertiesImpl();
            return new DocumentImportExportServiceImpl(aRepositoryProperties,
                    List.of(new XmiFormatSupport(), new PretokenizedTextFormatSupport()),
                    aCasStorageService, aAnnotationSchemaService, properties);
        }

        @Bean
        public CurationDocumentService curationDocumentService(CasStorageService aCasStorageService,
                AnnotationSchemaService aAnnotationService)
        {
            return new CurationDocumentServiceImpl(aCasStorageService, aAnnotationService);
        }

        @Bean
        public VersioningService versioningService(RepositoryProperties aRepositoryProperties,
                AnnotationSchemaService aAnnotationSchemaService, DocumentService aDocumentService,
                CurationDocumentService aCurationDocumentService,
                CasStorageService aCasStorageService, UserDao aUserDao)
        {
            return new VersioningServiceImpl(aRepositoryProperties, aAnnotationSchemaService,
                    aDocumentService, aCurationDocumentService, aCasStorageService, aUserDao);
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
        sut.initializeRepo(aProject);
    }

    private File getResource(String aResourceName)
    {
        return Paths.get("src", "test", "resources", aResourceName).toFile();
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
