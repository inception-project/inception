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

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.ANNOTATION_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.SOURCE_FOLDER;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.util.CasIOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterProjectCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.export.ImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayerReference;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

@Component
public class VersioningServiceImpl
    implements VersioningService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String REPO_NAME = "git-backup";
    public static final String LAYERS = "layers.json";

    private final RepositoryProperties repositoryProperties;
    private final AnnotationSchemaService annotationService;
    private final DocumentService documentService;
    private final ProjectService projectService;
    private final CasStorageService casStorageService;
    private final UserDao userDao;

    @Autowired
    public VersioningServiceImpl(RepositoryProperties aRepoProperties,
            AnnotationSchemaService aAnnotationService, DocumentService aDocumentService,
            ProjectService aProjectService, CasStorageService aCasStorageService, UserDao aUserDao)
    {
        repositoryProperties = aRepoProperties;
        annotationService = aAnnotationService;
        documentService = aDocumentService;
        projectService = aProjectService;
        casStorageService = aCasStorageService;
        userDao = aUserDao;
    }

    @EventListener
    @Transactional
    public void onAfterProjectCreated(AfterProjectCreatedEvent aEvent) throws GitAPIException
    {
        initializeRepo(aEvent.getProject());
    }

    @EventListener
    @Transactional
    public void onBeforeProjectRemovedEvent(BeforeProjectRemovedEvent aEvent)
    {
        Project project = aEvent.getProject();
        File repoPath = getRepoDir(project);
        log.info(
                "Removing git repository for project [{}] at [{}] because project is being removed",
                project.getId(), repoPath);
        FileSystemUtils.deleteRecursively(repoPath);
    }

    @Override
    public void snapshotCompleteProject(Project aProject, String aCommitMessage)
        throws IOException, GitAPIException
    {
        File repoDir = getRepoDir(aProject);
        Path sourceDir = repoDir.toPath().resolve(SOURCE_FOLDER);
        Path annotationDir = repoDir.toPath().resolve(ANNOTATION_FOLDER);

        Git git = Git.open(repoDir);

        // Dump layers
        File layersJsonFile = new File(repoDir, LAYERS);
        dumpLayers(layersJsonFile, aProject);
        git.add().addFilepattern(LAYERS).call();

        // Dump source documents
        Files.createDirectories(sourceDir);

        for (SourceDocument sourceDocument : documentService.listSourceDocuments(aProject)) {
            Path outputPath = sourceDir.resolve(sourceDocument.getName() + ".xmi");
            try (CasStorageSession session = CasStorageSession.openNested();
                    OutputStream out = Files.newOutputStream(outputPath)) {
                CAS cas = documentService.createOrReadInitialCas(sourceDocument);
                CasIOUtils.save(WebAnnoCasUtil.getRealCas(cas), out, SerialFormat.XMI);
            }
        }

        // Dump annotation documents
        for (User user : projectService.listProjectUsersWithPermissions(aProject)) {
            String userName = user.getUsername();
            Path userDir = annotationDir.resolve(userName);

            for (AnnotationDocument annotationDocument : documentService
                    .listAnnotationDocuments(aProject, user)) {
                Files.createDirectories(userDir);

                Path outputPath = userDir.resolve(annotationDocument.getName() + ".xmi");
                try (CasStorageSession session = CasStorageSession.openNested();
                        OutputStream out = Files.newOutputStream(outputPath)) {
                    CAS cas = casStorageService.readCas(annotationDocument.getDocument(), userName);
                    CasIOUtils.save(WebAnnoCasUtil.getRealCas(cas), out, SerialFormat.XMI);
                }
            }
        }

        git.add().addFilepattern(ANNOTATION_FOLDER).call();
        git.add().addFilepattern(SOURCE_FOLDER).call();

        commit(git, aCommitMessage);
    }

    @Override
    public void labelCurrentVersion(Project aProject, String aLabel)
    {

    }

    @Override
    public File getRepoDir(Project aProject)
    {
        File inceptionDir = repositoryProperties.getPath();
        return inceptionDir.toPath() //
                .resolve(PROJECT_FOLDER) //
                .resolve(aProject.getId().toString()) //
                .resolve(REPO_NAME) //
                .toFile();
    }

    @Override
    public void initializeRepo(Project aProject) throws GitAPIException
    {
        File repoDir = getRepoDir(aProject);
        log.info("Creating git repository for project [{}] at [{}]", aProject.getId(), repoDir);
        Git.init().setDirectory(repoDir).call();
    }

    @Override
    public boolean repoExists(Project aProject)
    {
        File repoDir = getRepoDir(aProject);
        File gitDir = repoDir.toPath().resolve(".git").toFile();

        return repoDir.isDirectory() && gitDir.isDirectory();
    }

    @Override
    public Optional<String> getRemote(Project aProject) throws IOException, GitAPIException
    {
        File repoDir = getRepoDir(aProject);
        Git git = Git.open(repoDir);

        String url = git.getRepository().getConfig().getString("remote", "origin", "url");

        return Optional.ofNullable(url);
    }

    @Override
    public void setRemote(Project aProject, String aValue)
        throws IOException, GitAPIException, URISyntaxException
    {
        File repoDir = getRepoDir(aProject);
        Git git = Git.open(repoDir);

        git.remoteSetUrl().setRemoteName("origin").setRemoteUri(new URIish(aValue)).call();
    }

    @Override
    public void pushToOrigin(Project aProject) throws IOException, GitAPIException
    {
        File repoDir = getRepoDir(aProject);
        Git git = Git.open(repoDir);

        git.push().setRemote("origin").add("master").call();
    }

    @Override
    public void pushToOrigin(Project aProject, String aUsername, String aPassword)
        throws IOException, GitAPIException
    {
        File repoDir = getRepoDir(aProject);
        Git git = Git.open(repoDir);

        git.push() //
                .setRemote("origin") //
                .add("master") //
                .setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(aUsername, aPassword)) //
                .call();
    }

    private void commit(Git aGit, String aMessage) throws GitAPIException
    {
        User user = userDao.getCurrentUser();

        String userName = user.getUsername();
        String email = user.getEmail();
        if (StringUtils.isBlank(email)) {
            email = userName + "@" + "inception";
        }

        if (aGit.status().call().hasUncommittedChanges()) {
            aGit.commit().setMessage(aMessage) //
                    .setAuthor(userName, email) //
                    .call();
        }
    }

    private void dumpLayers(File aFile, Project aProject) throws IOException
    {
        List<ExportedAnnotationLayer> exLayers = new ArrayList<>();
        for (AnnotationLayer layer : annotationService.listAnnotationLayer(aProject)) {

            ExportedAnnotationLayer exMainLayer = ImportUtil.exportLayerDetails(null, null, layer,
                    annotationService);
            exLayers.add(exMainLayer);

            // If the layer is attached to another layer, then we also have to export
            // that, otherwise we would be missing it during re-import.
            if (layer.getAttachType() != null) {
                AnnotationLayer attachLayer = layer.getAttachType();
                ExportedAnnotationLayer exAttachLayer = ImportUtil.exportLayerDetails(null, null,
                        attachLayer, annotationService);
                exMainLayer.setAttachType(
                        new ExportedAnnotationLayerReference(exAttachLayer.getName()));
                exLayers.add(exAttachLayer);
            }
        }

        String json = JSONUtil.toPrettyJsonString(exLayers);
        Files.write(aFile.toPath(), json.getBytes(StandardCharsets.UTF_8));
    }
}
