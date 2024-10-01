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
package de.tudarmstadt.ukp.clarin.webanno.constraints.export;

import static java.nio.file.Files.newInputStream;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.support.io.ZipUtils;

public class ConstraintsExporter
    implements ProjectExporter
{
    private static final String CONSTRAINTS = "/constraints/";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ConstraintsService constraintsService;

    public ConstraintsExporter(ConstraintsService aConstraintsService)
    {
        constraintsService = aConstraintsService;
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
        throws IOException
    {
        for (var set : constraintsService.listConstraintSets(aRequest.getProject())) {
            // Copying with file's original name to save ConstraintSet's name
            ProjectExporter.writeEntry(aStage, CONSTRAINTS + set.getName(), os -> {
                try (var is = newInputStream(
                        constraintsService.exportConstraintAsFile(set).toPath())) {
                    is.transferTo(os);
                }
            });
        }
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        for (var zipEnumerate = aZip.entries(); zipEnumerate.hasMoreElements();) {
            var entry = zipEnumerate.nextElement();

            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            var entryName = ZipUtils.normalizeEntryName(entry);

            if (entryName.startsWith(ConstraintsService.CONSTRAINTS + "/")) {
                var fileName = FilenameUtils.getName(entry.getName());
                if (fileName.trim().isEmpty()) {
                    continue;
                }

                var constraintSet = new ConstraintSet();
                constraintSet.setProject(aProject);
                constraintSet.setName(fileName);
                constraintsService.createOrUpdateConstraintSet(constraintSet);
                constraintsService.writeConstraintSet(constraintSet, aZip.getInputStream(entry));
                LOG.info("Imported constraint [" + fileName + "] for project [" + aProject.getName()
                        + "] with id [" + aProject.getId() + "]");
            }
        }
    }
}
