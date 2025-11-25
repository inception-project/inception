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
package de.tudarmstadt.ukp.inception.project.export;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipFile;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskHandle;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.project.export.model.ProjectExportTask;

public interface ProjectExportService
{
    Project importProject(ProjectImportRequest aRequest, ZipFile aZip)
        throws ProjectExportException;

    File exportProject(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor)
        throws ProjectExportException, IOException, InterruptedException;

    ProjectExportTaskHandle startProjectExportTask(FullProjectExportRequest aModel,
            String aUsername);

    ProjectExportRequest_ImplBase getExportRequest(ProjectExportTaskHandle aHandle);

    boolean cancelTask(ProjectExportTaskHandle aHandle);

    ProjectExportTaskMonitor getTaskMonitor(ProjectExportTaskHandle aHandle);

    ProjectExportTaskHandle startProjectExportCuratedDocumentsTask(
            FullProjectExportRequest aRequest, String aUsername);

    ProjectExportTaskHandle startTask(ProjectExportTask<?> aTask);

    List<ProjectExportTask<?>> listRunningExportTasks(Project aProject);
}
