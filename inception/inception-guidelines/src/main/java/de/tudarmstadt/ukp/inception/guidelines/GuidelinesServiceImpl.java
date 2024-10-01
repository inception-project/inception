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
package de.tudarmstadt.ukp.inception.guidelines;

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.withProjectLogger;
import static java.nio.file.Files.newDirectoryStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.guidelines.config.GuidelinesServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link GuidelinesServiceAutoConfiguration#guidelinesService}.
 * </p>
 */
public class GuidelinesServiceImpl
    implements GuidelinesService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final RepositoryProperties repositoryProperties;

    public GuidelinesServiceImpl(RepositoryProperties aRepositoryProperties)
    {
        super();
        repositoryProperties = aRepositoryProperties;
    }

    @Override
    public File getGuidelinesFolder(Project aProject)
    {
        return new File(repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER
                + "/" + aProject.getId() + "/" + GUIDELINES_FOLDER + "/");
    }

    @Override
    public File getGuideline(Project aProject, String aFilename)
    {
        return new File(repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER
                + "/" + aProject.getId() + "/" + GUIDELINES_FOLDER + "/" + aFilename);
    }

    @Override
    public void createGuideline(Project aProject, File aContent, String aFileName)
        throws IOException
    {
        try (var is = new FileInputStream(aContent)) {
            createGuideline(aProject, is, aFileName);
        }
    }

    @Override
    public void createGuideline(Project aProject, InputStream aIS, String aFileName)
        throws IOException
    {
        try (var logCtx = withProjectLogger(aProject)) {
            var guidelinePath = repositoryProperties.getPath().getAbsolutePath() + "/"
                    + PROJECT_FOLDER + "/" + aProject.getId() + "/" + GUIDELINES_FOLDER + "/";
            FileUtils.forceMkdir(new File(guidelinePath));
            try (var os = new FileOutputStream(new File(guidelinePath, aFileName))) {
                aIS.transferTo(os);
            }

            LOG.info("Created guidelines file [{}] in project {}", aFileName, aProject);
        }
    }

    @Override
    public List<String> listGuidelines(Project aProject)
    {
        // list all guideline files
        var files = getGuidelinesFolder(aProject).listFiles();

        // Name of the guideline files
        var annotationGuidelineFiles = new ArrayList<String>();
        if (files != null) {
            for (var file : files) {
                annotationGuidelineFiles.add(file.getName());
            }
        }

        return annotationGuidelineFiles;
    }

    @Override
    public boolean hasGuidelines(Project aProject)
    {
        try (var d = newDirectoryStream(getGuidelinesFolder(aProject).toPath())) {
            return d.iterator().hasNext();
        }
        catch (IOException e) {
            // This may not be the best way to handle it, but if is a fairly sound assertion and
            // saves the calling code from having to handle the exception.
            return false;
        }
    }

    @Override
    public void removeGuideline(Project aProject, String aFileName) throws IOException
    {
        try (var logCtx = withProjectLogger(aProject)) {
            FileUtils.forceDelete(
                    new File(repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER
                            + "/" + aProject.getId() + "/" + GUIDELINES_FOLDER + "/" + aFileName));

            LOG.info("Removed guidelines file [{}] from project {}", aFileName, aProject.getName());
        }
    }
}
