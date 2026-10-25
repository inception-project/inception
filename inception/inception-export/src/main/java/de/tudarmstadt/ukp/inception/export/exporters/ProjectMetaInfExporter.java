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
package de.tudarmstadt.ukp.inception.export.exporters;

import static org.apache.commons.io.FilenameUtils.normalize;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.io.ZipUtils;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DocumentImportExportServiceAutoConfiguration#projectMetaInfExporter}.
 * </p>
 */
public class ProjectMetaInfExporter
    implements ProjectExporter
{
    private static final String META_INF_FOLDER = "META-INF";
    private static final String META_INF = "/" + META_INF_FOLDER;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ProjectService projectService;

    public ProjectMetaInfExporter(ProjectService aProjectService)
    {
        projectService = aProjectService;
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
        throws IOException
    {
        var metaInf = projectService.getMetaInfFolder(aRequest.getProject()).toPath();

        if (Files.exists(metaInf)) {
            Files.walkFileTree(metaInf, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
                {
                    if (!file.startsWith(metaInf)) {
                        return FileVisitResult.CONTINUE;
                    }

                    var relFile = prependIfMissing(
                            normalize(metaInf.relativize(file).toString(), true), "/");
                    ProjectExporter.writeEntry(aStage, META_INF + relFile, os -> {
                        try (var is = Files.newInputStream(file)) {
                            is.transferTo(os);
                        }
                    });

                    return FileVisitResult.CONTINUE;
                }
            });
        }

    }

    /**
     * Copy project META_INF from the exported project
     * 
     * @param aZip
     *            the ZIP file.
     * @param aProject
     *            the project.
     * @throws IOException
     *             if an I/O error occurs.
     */
    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        var metaInf = projectService.getMetaInfFolder(aProject).toPath().toAbsolutePath()
                .normalize();

        for (var zipEnumerate = aZip.entries(); zipEnumerate.hasMoreElements();) {
            var entry = zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            var entryName = ZipUtils.normalizeEntryName(entry);

            if (entry.isDirectory() || !entryName.startsWith(META_INF_FOLDER + "/")) {
                continue;
            }

            var target = metaInf.resolve(entryName.substring(META_INF_FOLDER.length() + 1))
                    .normalize();

            // Refuse entries such as "META-INF/../../x" that would be written outside the
            // project's META-INF folder
            if (!target.startsWith(metaInf) || target.equals(metaInf)) {
                throw new IOException(
                        "Illegal META-INF entry in project archive: [" + entryName + "]");
            }

            try (var is = aZip.getInputStream(entry)) {
                FileUtils.copyInputStreamToFile(is, target.toFile());
            }

            LOG.info("Imported META-INF for project [" + aProject.getName() + "] with id ["
                    + aProject.getId() + "]");
        }
    }
}
