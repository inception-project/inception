/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.constraints.export;

import java.io.File;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;

@Component
public class ConstraintsExporter
    implements ProjectExporter
{
    private static final String CONSTRAINTS = "/constraints/";

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private @Autowired ConstraintsService constraintsService;

    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aStage)
        throws Exception
    {
        File constraintsDir = new File(aStage + CONSTRAINTS);
        FileUtils.forceMkdir(constraintsDir);
        String fileName;
        for (ConstraintSet set : constraintsService.listConstraintSets(aRequest.getProject())) {
            fileName = set.getName();
            // Copying with file's original name to save ConstraintSet's name
            FileUtils.copyFile(constraintsService.exportConstraintAsFile(set),
                    new File(constraintsDir, fileName));
        }
    }
    
    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        for (Enumeration<? extends ZipEntry> zipEnumerate = aZip.entries(); zipEnumerate
                .hasMoreElements();) {
            ZipEntry entry = zipEnumerate.nextElement();
            
            // Strip leading "/" that we had in ZIP files prior to 2.0.8 (bug #985)
            String entryName = ZipUtils.normalizeEntryName(entry);
            
            if (entryName.startsWith(ConstraintsService.CONSTRAINTS + "/")) {
                String fileName = FilenameUtils.getName(entry.getName());
                if (fileName.trim().isEmpty()) {
                    continue;
                }
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.setProject(aProject);
                constraintSet.setName(fileName);
                constraintsService.createConstraintSet(constraintSet);
                constraintsService.writeConstraintSet(constraintSet, aZip.getInputStream(entry));
                log.info("Imported constraint [" + fileName + "] for project [" + aProject.getName()
                        + "] with id [" + aProject.getId() + "]");
            }
        }
    }
}
