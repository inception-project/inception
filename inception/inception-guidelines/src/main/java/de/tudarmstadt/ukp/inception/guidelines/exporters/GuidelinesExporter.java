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
package de.tudarmstadt.ukp.inception.guidelines.exporters;

import static java.nio.file.Files.newInputStream;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.guidelines.GuidelinesService;
import de.tudarmstadt.ukp.inception.guidelines.InvalidGuidelineException;
import de.tudarmstadt.ukp.inception.guidelines.config.GuidelinesServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.io.ZipUtils;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link GuidelinesServiceAutoConfiguration#guidelinesExporter}.
 * </p>
 */
public class GuidelinesExporter
    implements ProjectExporter
{
    public static final String GUIDELINE = "guideline";
    private static final String GUIDELINES_FOLDER = "/" + GUIDELINE;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final GuidelinesService guidelinesService;

    public GuidelinesExporter(GuidelinesService aGuidelinesService)
    {
        guidelinesService = aGuidelinesService;
    }

    /**
     * Copy Project guidelines from the file system of this project to the export folder
     * 
     * @throws IOException
     *             if an I/O error occurs.
     */
    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
        throws IOException
    {
        var project = aRequest.getProject();

        for (var name : guidelinesService.listGuidelines(project)) {
            var guideline = guidelinesService.getGuideline(project, name);
            ProjectExporter.writeEntry(aStage, GUIDELINES_FOLDER + "/" + name, os -> {
                try (var is = newInputStream(guideline.toPath())) {
                    is.transferTo(os);
                }
            });
        }
    }

    /**
     * Copy guidelines from the exported project
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
        for (var zipEnumerate = aZip.entries(); zipEnumerate.hasMoreElements();) {
            var entry = (ZipEntry) zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            var entryName = ZipUtils.normalizeEntryName(entry);

            if (entryName.startsWith(GUIDELINE + "/")) {
                // The exporter writes the guidelines flat. Directory entries and anything nested in
                // a subfolder do not come from it and are ignored.
                var fileName = entryName.substring(GUIDELINE.length() + 1);
                if (fileName.isEmpty() || fileName.contains("/")) {
                    continue;
                }
                try (var is = aZip.getInputStream(entry)) {
                    guidelinesService.createGuideline(aProject, is, fileName);
                    LOG.info("Imported guideline [{}] for project {}", fileName, aProject);
                }
                catch (InvalidGuidelineException e) {
                    LOG.warn("Skipped guideline [{}] while importing project {}: {}", fileName,
                            aProject, e.getMessage());
                    aRequest.addMessage(
                            "Guideline [" + fileName + "] was not imported: " + e.getMessage());
                }
            }
        }
    }
}
