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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.export.exporters;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;

@Component
public class ProjectSettingsExporter
    implements ProjectExporter
{
    private @Autowired ProjectService projectService;
    
    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aStage)
        throws Exception
    {
        Project project = aRequest.getProject();
        aExProject.setDescription(project.getDescription());
        // In older versions of WebAnno, the mode was an enum which was serialized as upper-case
        // during export but as lower-case in the database. This is compensating for this case.
        aExProject.setMode(StringUtils.upperCase(project.getMode(), Locale.US));
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
