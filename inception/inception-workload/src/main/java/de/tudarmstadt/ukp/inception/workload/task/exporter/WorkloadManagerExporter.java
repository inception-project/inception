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
package de.tudarmstadt.ukp.inception.workload.task.exporter;

import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagementAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WorkloadManagementAutoConfiguration#workloadManagerExporter}.
 * </p>
 */
public class WorkloadManagerExporter
    implements ProjectExporter
{
    private static final String KEY = "workload_manager";
    private static final Logger LOG = LoggerFactory.getLogger(WorkloadManagerExporter.class);

    private final WorkloadManagementService workloadManagementService;

    @Autowired
    public WorkloadManagerExporter(WorkloadManagementService aWorkloadManagementService)
    {
        workloadManagementService = aWorkloadManagementService;
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
    {
        var project = aRequest.getProject();
        var workloadManager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(project);

        var exportedWorkloadManager = new ExportedWorkloadManager();
        exportedWorkloadManager.setType(workloadManager.getType());
        exportedWorkloadManager.setTraits(workloadManager.getTraits());

        aExProject.setProperty(KEY, exportedWorkloadManager);
        LOG.info("Exported workload manager for project [{}]", project.getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        var maybeExportedWorkloadManager = aExProject.getProperty(KEY,
                ExportedWorkloadManager.class);

        if (!maybeExportedWorkloadManager.isPresent()) {
            return;
        }

        var exportedWorkloadManager = maybeExportedWorkloadManager.get();

        var workloadManager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(aProject);
        workloadManager.setType(exportedWorkloadManager.getType());
        workloadManager.setTraits(exportedWorkloadManager.getTraits());
        workloadManagementService.saveConfiguration(workloadManager);

        LOG.info("Imported workload manager for project [{}]", aProject.getName());
    }
}
