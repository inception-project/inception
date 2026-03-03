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

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.DOCUMENT_FOLDER;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.PROJECT_FOLDER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Optional;

import org.apache.uima.cas.SerialFormat;
import org.apache.uima.util.CasIOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayerReference;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.export.LayerImportExportUtils;
import de.tudarmstadt.ukp.inception.project.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;
import de.tudarmstadt.ukp.inception.versioning.config.VersioningServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link VersioningServiceAutoConfiguration}
 * </p>
 */
public class VersioningServiceImpl
    implements VersioningService
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String REPO_NAME = "git-backup";
    public static final String LAYERS = "layers.json";

    private final RepositoryProperties repositoryProperties;
    private final AnnotationSchemaService annotationService;
    private final DocumentService documentService;
    private final CurationDocumentService curationDocumentService;
    private final CasStorageService casStorageService;
    private final UserDao userDao;

    public VersioningServiceImpl(RepositoryProperties aRepoProperties,
            AnnotationSchemaService aAnnotationService, DocumentService aDocumentService,
            CurationDocumentService aCurationDocumentService, CasStorageService aCasStorageService,
            UserDao aUserDao)
    {
        repositoryProperties = aRepoProperties;
        annotationService = aAnnotationService;
        documentService = aDocumentService;
        curationDocumentService = aCurationDocumentService;
        casStorageService = aCasStorageService;
        userDao = aUserDao;
    }

    @EventListener
    @Transactional
    public void onBeforeProjectRemovedEvent(BeforeProjectRemovedEvent aEvent)
    {
        var project = aEvent.getProject();

        if (repoExists(project)) {
            var repoPath = getRepoDir(project);
            LOG.info("Removing git repository for {} at [{}] because project is being removed",
                    project, repoPath);
            FileSystemUtils.deleteRecursively(repoPath);
        }
    }

    @Override
    public void snapshotCompleteProject(Project aProject, String aCommitMessage)
        throws IOException, GitAPIException
    {
        var repoDir = getRepoDir(aProject);
        var documentDir = repoDir.toPath().resolve(DOCUMENT_FOLDER);

        var git = Git.open(repoDir);

        // Dump layers
        var layersJsonFile = new File(repoDir, LAYERS);
        dumpLayers(layersJsonFile, aProject);
        git.add().addFilepattern(LAYERS).call();

        for (var sourceDocument : documentService.listSourceDocuments(aProject)) {
            var sourceDir = documentDir.resolve(sourceDocument.getName());

            Files.createDirectories(sourceDir);

            // Dump source documents
            var sourceDocumentPath = sourceDir.resolve("source.xmi");
            try (var session = CasStorageSession.openNested();
                    var out = Files.newOutputStream(sourceDocumentPath)) {
                var cas = documentService.createOrReadInitialCas(sourceDocument);
                CasIOUtils.save(WebAnnoCasUtil.getRealCas(cas), out, SerialFormat.XMI);
            }

            // Dump initial cas
            var initialCasPath = sourceDir.resolve("initial.xmi");
            try (var session = CasStorageSession.openNested();
                    var out = Files.newOutputStream(initialCasPath)) {
                var cas = documentService.createOrReadInitialCas(sourceDocument);
                CasIOUtils.save(WebAnnoCasUtil.getRealCas(cas), out, SerialFormat.XMI);
            }

            // Dump curation cas
            var curationCasPath = sourceDir.resolve("curation.xmi");
            try (var session = CasStorageSession.openNested();
                    var out = Files.newOutputStream(curationCasPath)) {
                if (curationDocumentService.existsCurationCas(sourceDocument)) {
                    var cas = curationDocumentService.readCurationCas(sourceDocument);
                    CasIOUtils.save(WebAnnoCasUtil.getRealCas(cas), out, SerialFormat.XMI);
                }
            }

            // Dump annotation documents
            for (var annotationDocument : documentService.listAnnotationDocuments(sourceDocument)) {

                var set = AnnotationSet.forUser(annotationDocument.getUser());
                var annotationDocumentPath = sourceDir.resolve(set + ".xmi");

                try (var session = CasStorageSession.openNested();
                        var out = Files.newOutputStream(annotationDocumentPath)) {
                    var cas = casStorageService.readCas(annotationDocument.getDocument(), set);
                    CasIOUtils.save(WebAnnoCasUtil.getRealCas(cas), out, SerialFormat.XMI);
                }
            }
        }

        git.add().addFilepattern(DOCUMENT_FOLDER).call();
        git.add().addFilepattern(LAYERS).call();

        commit(git, aCommitMessage);
    }

    @Override
    public void labelCurrentVersion(Project aProject, String aLabel)
    {
        // FIXME: Needs to be implemented
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
        var repoDir = getRepoDir(aProject);
        LOG.info("Creating git repository for project {} at [{}]", aProject, repoDir);
        Git.init().setDirectory(repoDir).call();
    }

    @Override
    public boolean repoExists(Project aProject)
    {
        var repoDir = getRepoDir(aProject);
        var gitDir = repoDir.toPath().resolve(".git").toFile();

        return repoDir.isDirectory() && gitDir.isDirectory();
    }

    @Override
    public Optional<String> getRemote(Project aProject) throws IOException, GitAPIException
    {
        var repoDir = getRepoDir(aProject);
        var git = Git.open(repoDir);

        var url = git.getRepository().getConfig().getString("remote", "origin", "url");

        return Optional.ofNullable(url);
    }

    @Override
    public void setRemote(Project aProject, String aValue)
        throws IOException, GitAPIException, URISyntaxException
    {
        var repoDir = getRepoDir(aProject);
        var git = Git.open(repoDir);

        git.remoteSetUrl().setRemoteName("origin").setRemoteUri(new URIish(aValue)).call();
    }

    @Override
    public void pushToOrigin(Project aProject) throws IOException, GitAPIException
    {
        var repoDir = getRepoDir(aProject);
        var git = Git.open(repoDir);

        git.push().setRemote("origin").add("master").call();
    }

    @Override
    public void pushToOrigin(Project aProject, String aUsername, String aPassword)
        throws IOException, GitAPIException
    {
        var repoDir = getRepoDir(aProject);
        var git = Git.open(repoDir);

        git.push() //
                .setRemote("origin") //
                .add("master") //
                .setCredentialsProvider(
                        new UsernamePasswordCredentialsProvider(aUsername, aPassword)) //
                .call();
    }

    private void commit(Git aGit, String aMessage) throws GitAPIException
    {
        var user = userDao.getCurrentUser();

        var userName = user.getUsername();
        var email = user.getEmail();
        if (isBlank(email)) {
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
        var exLayers = new ArrayList<ExportedAnnotationLayer>();
        for (var layer : annotationService.listAnnotationLayer(aProject)) {

            var exMainLayer = LayerImportExportUtils.exportLayerDetails(null, null, layer,
                    annotationService);
            exLayers.add(exMainLayer);

            // If the layer is attached to another layer, then we also have to export
            // that, otherwise we would be missing it during re-import.
            if (layer.getAttachType() != null) {
                var attachLayer = layer.getAttachType();
                var exAttachLayer = LayerImportExportUtils.exportLayerDetails(null, null,
                        attachLayer, annotationService);
                exMainLayer.setAttachType(
                        new ExportedAnnotationLayerReference(exAttachLayer.getName()));
                exLayers.add(exAttachLayer);
            }
        }

        var json = JSONUtil.toPrettyJsonString(exLayers);
        Files.write(aFile.toPath(), json.getBytes(UTF_8));
    }
}
