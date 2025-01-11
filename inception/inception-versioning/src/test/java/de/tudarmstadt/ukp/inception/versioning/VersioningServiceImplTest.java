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
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.util.FileSystemUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.constraints.config.ConstraintsServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.diag.config.CasDoctorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.config.CurationDocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.io.xmi.config.UimaFormatsAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.versioning.config.VersioningServiceAutoConfiguration;

@DataJpaTest( //
        showSql = false, //
        excludeAutoConfiguration = LiquibaseAutoConfiguration.class, //
        properties = { //
                "spring.main.banner-mode=off", //
                "repository.path=" + VersioningServiceImplTest.TEST_OUTPUT_FOLDER, //
                "versioning.enabled=true", //
                "debug.cas-doctor.force-release-behavior=true", //
                "document-import.run-cas-doctor-on-import=OFF" })
@EnableAutoConfiguration
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model",
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@Import({ //
        ConstraintsServiceAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        CurationDocumentServiceAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
        UimaFormatsAutoConfiguration.class, //
        CasDoctorAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        DocumentImportExportServiceAutoConfiguration.class, //
        VersioningServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class })
public class VersioningServiceImplTest
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/VersioningServiceImplTest";

    private @Autowired VersioningService sut;
    private @Autowired TestEntityManager testEntityManager;
    private @Autowired ProjectService projectService;
    private @Autowired UserDao userDao;
    private @Autowired DocumentService documentService;

    private Project testProject;

    @BeforeAll
    public static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @BeforeEach
    public void setUp()
    {
        testProject = new Project("test-project");
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
        projectService.assignRole(testProject, admin, ANNOTATOR, CURATOR, MANAGER);
        return admin;
    }

    private User createAnnotator()
    {
        User annotator = addUser("annotator");
        projectService.assignRole(testProject, annotator, ANNOTATOR);
        return annotator;
    }

    private void uploadDocuments() throws Exception
    {

        File doc1 = getResource("docs/dinos.txt");
        File doc2 = getResource("docs/lorem.txt");

        try (var session = CasStorageSession.open()) {
            try (var is = Files.newInputStream(doc1.toPath())) {
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
        try (var session = CasStorageSession.open()) {
            for (var sourceDocument : documentService.listSourceDocuments(testProject)) {
                documentService.createOrGetAnnotationDocument(sourceDocument, aUser);
                var cas = documentService.createOrReadInitialCas(sourceDocument);
                documentService.writeAnnotationCas(cas, sourceDocument, aUser);
            }
        }
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        // All done via auto-config
    }
}
