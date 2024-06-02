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

import java.io.IOException;
import java.util.Date;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DocumentImportExportServiceAutoConfiguration#projectSettingsExporter}.
 * </p>
 */
public class ProjectSettingsExporter
    implements ProjectExporter
{
    private final ProjectService projectService;

    public ProjectSettingsExporter(ProjectService aProjectService)
    {
        projectService = aProjectService;
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
    {
        var project = aRequest.getProject();
        aExProject.setDescription(project.getDescription());
        // In older versions of WebAnno, the mode was an enum which was serialized as upper-case
        // during export but as lower-case in the database. This is compensating for this case.
        // We keep the mode in the exported model only for a bit of backwards compatibility with
        // WebAnno.
        aExProject.setMode("ANNOTATION");
        aExProject.setScriptDirection(project.getScriptDirection());
        aExProject.setVersion(project.getVersion());
        aExProject.setDisableExport(project.isDisableExport());
        aExProject.setCreated(project.getCreated());
        aExProject.setUpdated(project.getUpdated());
        aExProject.setAnonymousCuration(project.isAnonymousCuration());
    }

    /**
     * create new {@link Project} from the {@link ExportedProject} model
     * 
     * @param aExProject
     *            the project
     * @throws IOException
     *             if an I/O error occurs.
     */
    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws IOException
    {
        // The "mode" is already set in ProjectExportServiceImpl.importProject(...) because there
        // the project is already persisted and when it is persisted the non-null column "mode"
        // must already have a value.
        aProject.setDescription(aExProject.getDescription());
        aProject.setDisableExport(aExProject.isDisableExport());
        aProject.setCreated(aExProject.getCreated());
        aProject.setUpdated(aExProject.getUpdated());
        aProject.setAnonymousCuration(aExProject.isAnonymousCuration());

        // Set dates to now if no dates are set in the exported project
        var now = new Date();
        if (aProject.getCreated() == null || aProject.getCreated().getTime() == 0) {
            aProject.setCreated(now);
        }
        if (aProject.getUpdated() == null || aProject.getUpdated().getTime() == 0) {
            aProject.setUpdated(now);
        }

        // Set default to LTR on import from old WebAnno versions
        if (aExProject.getScriptDirection() == null) {
            aProject.setScriptDirection(ScriptDirection.LTR);
        }
        else {
            aProject.setScriptDirection(aExProject.getScriptDirection());
        }

        projectService.updateProject(aProject);
    }
}
