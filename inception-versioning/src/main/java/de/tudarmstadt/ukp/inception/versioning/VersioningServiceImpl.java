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

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterProjectCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@Component
public class VersioningServiceImpl
    implements VersioningService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String REPO_NAME = "git-backup";
    private static final String TYPESYSTEM = "typesystem.xml";

    private final RepositoryProperties repositoryProperties;
    private final AnnotationSchemaService annotationService;

    @Autowired
    public VersioningServiceImpl(RepositoryProperties aRepoProperties,
            AnnotationSchemaService aAnnotationService)
    {
        repositoryProperties = aRepoProperties;
        annotationService = aAnnotationService;
    }

    @EventListener
    @Transactional
    public void onAfterProjectCreated(AfterProjectCreatedEvent aEvent) throws GitAPIException
    {
        Project project = aEvent.getProject();
        File repoPath = getRepoDir(project.getId());
        log.info("Creating git repository for project [{}] at [{}]", project.getId(), repoPath);
        Git git = Git.init().setDirectory(repoPath).call();
    }

    @EventListener
    @Transactional
    public void onBeforeProjectRemovedEvent(BeforeProjectRemovedEvent aEvent)
    {
        Project project = aEvent.getProject();
        File repoPath = getRepoDir(project.getId());
        log.info(
                "Removing git repository for project [{}] at [{}] because project is being removed",
                project.getId(), repoPath);
        FileSystemUtils.deleteRecursively(repoPath);
    }

    @Override
    public void snapshotUserAnnotations(Project aProject, User aUser)
    {

    }

    @Override
    public void snapshotProject(Project aProject)
    {
        File repoPath = getRepoDir(aProject.getId());
        File typeSystemPath = new File(repoPath, TYPESYSTEM);

        try {
            Git git = Git.open(repoPath);

            saveUimaTypeSystem(typeSystemPath, aProject);
            git.add().addFilepattern(TYPESYSTEM).call();

            git.commit().setMessage("Update project settings");
        }
        catch (IOException | GitAPIException e) {
            log.error("Unable to snapshot project settings", e);
        }
    }

    @Override
    public void labelCurrentVersion(Project aProject, String aLabel)
    {

    }

    @Override
    public File getRepoDir(Long aProjectId)
    {
        File repositoryDir = repositoryProperties.getPath();
        return new File(repositoryDir, "/" + PROJECT_FOLDER + "/" + aProjectId + "/" + REPO_NAME);
    }

    private void saveUimaTypeSystem(File aFile, Project aProject)
    {
        try (FileOutputStream fOut = new FileOutputStream(aFile)) {
            TypeSystemDescription tsd = annotationService.getAllProjectTypes(aProject);
            tsd.toXML(fOut);
        }
        catch (Exception e) {
            log.error("Unable to generate the UIMA type system file", e);
        }
    }
}
