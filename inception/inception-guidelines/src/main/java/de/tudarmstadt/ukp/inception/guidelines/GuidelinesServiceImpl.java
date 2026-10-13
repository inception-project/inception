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

import static de.tudarmstadt.ukp.inception.guidelines.GuidelineFiles.NOT_A_PDF_MESSAGE;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.withProjectLogger;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.move;
import static java.nio.file.Files.newOutputStream;
import static java.util.Comparator.naturalOrder;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
        var name = GuidelineFiles.validateName(aFileName);

        // Decide on the content before anything is written
        var in = new BufferedInputStream(aIS);
        in.mark(GuidelineFiles.pdfMagicLength());
        var head = in.readNBytes(GuidelineFiles.pdfMagicLength());
        in.reset();
        if (!GuidelineFiles.isPdf(head, head.length)) {
            throw new InvalidGuidelineException(NOT_A_PDF_MESSAGE);
        }

        try (var logCtx = withProjectLogger(aProject)) {
            var folder = getGuidelinesFolder(aProject);
            forceMkdir(folder);

            Path target;
            try {
                target = new File(folder, name).toPath();
            }
            catch (InvalidPathException e) {
                throw new InvalidGuidelineException(GuidelineFiles.NAME_NOT_ALLOWED_MESSAGE);
            }
            if (exists(target)) {
                throw new InvalidGuidelineException(
                        "A guideline with this name already exists. Delete it first.");
            }

            // Next to the guideline folder so that an unfinished upload is never listed
            var temp = createFile(
                    folder.getParentFile().toPath().resolve("guideline-" + randomUUID() + ".tmp"));
            try {
                try (var os = newOutputStream(temp)) {
                    in.transferTo(os);
                }
                move(temp, target);
            }
            catch (FileAlreadyExistsException e) {
                throw new InvalidGuidelineException(
                        "A guideline with this name already exists. Delete it first.");
            }
            finally {
                deleteIfExists(temp);
            }

            LOG.info("Created guidelines file [{}] in project {}", name, aProject);
        }
    }

    @Override
    public List<String> listGuidelines(Project aProject)
    {
        var files = getGuidelinesFolder(aProject).listFiles(File::isFile);
        if (files == null) {
            return new ArrayList<>();
        }

        return Stream.of(files) //
                .map(File::getName) //
                .sorted(CASE_INSENSITIVE_ORDER.thenComparing(naturalOrder())) //
                .collect(toCollection(ArrayList::new));
    }

    @Override
    public boolean hasGuidelines(Project aProject)
    {
        return !listGuidelines(aProject).isEmpty();
    }

    @Override
    public void removeGuideline(Project aProject, String aFileName) throws IOException
    {
        try (var logCtx = withProjectLogger(aProject)) {
            forceDelete(
                    new File(repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER
                            + "/" + aProject.getId() + "/" + GUIDELINES_FOLDER + "/" + aFileName));

            LOG.info("Removed guidelines file [{}] from project {}", aFileName, aProject.getName());
        }
    }
}
