/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.export;

import static de.tudarmstadt.ukp.clarin.webanno.export.ImportUtil.EXPORTED_PROJECT;

import java.io.File;
import java.io.IOException;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;

@Component(ExportService.SERVICE_NAME)
public class ExportServiceImpl implements ExportService
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private @Resource AnnotationSchemaService annotationService;
    private @Resource AutomationService automationService;
    private @Resource DocumentService documentService;
    private @Resource ProjectService projectService;
    private @Resource ConstraintsService constraintsService;
    private @Resource UserDao userRepository;
    private @Resource ImportExportService importExportService;
    
    @Override
    public File generateZipFile(final ProjectExportRequest aRequest)
        throws IOException, UIMAException, ClassNotFoundException, ProjectExportException
    {
        // Directory to store source documents and annotation documents
        File exportTempDir = File.createTempFile("webanno-project", "export");
        exportTempDir.delete();
        exportTempDir.mkdirs();
        
        // Target file
        File projectZipFile = new File(exportTempDir.getAbsolutePath() + ".zip");

        boolean success = false;
        
        try {
            // all metadata and project settings data from the database as JSON file
            File projectSettings = null;
            projectSettings = File.createTempFile(EXPORTED_PROJECT, ".json");
    
            Project project = aRequest.project.getObject();
            
            if (project.getId() == 0) {
                throw new ProjectExportException(
                        "Project not yet created. Please save project details first!");
            }
    
            de.tudarmstadt.ukp.clarin.webanno.export.model.Project exProjekt = ExportUtil
                    .exportProjectSettings(annotationService, automationService, documentService,
                            projectService, project, projectSettings, exportTempDir);
    
            JSONUtil.generatePrettyJson(exProjekt, projectSettings);
            FileUtils.copyFileToDirectory(projectSettings, exportTempDir);
            
            aRequest.progress = 9;
            ExportUtil.exportSourceDocuments(documentService, automationService, aRequest, project,
                    exportTempDir);
            ExportUtil.exportTrainingDocuments(automationService, aRequest, project, exportTempDir);
            ExportUtil.exportAnnotationDocuments(documentService, importExportService,
                    userRepository, aRequest, exportTempDir);
            ExportUtil.exportProjectLog(projectService, project, exportTempDir);
            ExportUtil.exportGuideLine(projectService, project, exportTempDir);
            ExportUtil.exportProjectMetaInf(projectService, project, exportTempDir);
            ExportUtil.exportProjectConstraints(constraintsService, project, exportTempDir);
            aRequest.progress = 90;
            
            ExportUtil.exportCuratedDocuments(documentService, importExportService, aRequest,
                    exportTempDir, true);
    
            try {
                ZipUtils.zipFolder(exportTempDir, projectZipFile);
            }
            finally {
                FileUtils.forceDelete(projectSettings);
                System.gc();
                FileUtils.forceDelete(exportTempDir);
            }
            
            aRequest.progress = 100;
            
            success = true;
    
            return projectZipFile;
        }
        finally {
            if (!success) {
                try {
                    FileUtils.forceDelete(exportTempDir);
                } catch (IOException e) {
                    log.error(
                            "Unable to delete temporary export directory [" + exportTempDir + "]");
                }
            }
        }
    }
}
