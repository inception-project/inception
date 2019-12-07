/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.export;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskHandle;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;

public class ProjectExportFullProjectTask
    extends ProjectExportTask_ImplBase
{
    private @Autowired ProjectExportService exportService;

    public ProjectExportFullProjectTask(ProjectExportTaskHandle aHandle,
            ProjectExportTaskMonitor aMonitor, ProjectExportRequest aRequest, String aUsername)
    {
        super(aHandle, aMonitor, aRequest, aUsername);
    }

    @Override
    public File export(ProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor)
        throws ProjectExportException, IOException
    {
        return exportService.exportProject(aRequest, aMonitor);
    }
}
