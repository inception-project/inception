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
package de.tudarmstadt.ukp.inception.workload.exporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;
import de.tudarmstadt.ukp.inception.workload.task.exporter.WorkloadManagerExporter;

@ExtendWith(MockitoExtension.class)
public class WorkloadManagerExporterTest
{
    private @Mock WorkloadManagementService workloadManagementService;
    private Project project;

    private WorkloadManagerExporter sut;

    @BeforeEach
    public void setUp()
    {
        project = new Project();
        project.setId(1l);
        project.setName("Test Project");

        sut = new WorkloadManagerExporter(workloadManagementService);
    }

    @Test
    public void thatExportingWorks() throws Exception
    {
        when(workloadManagementService.loadOrCreateWorkloadManagerConfiguration(project))
                .thenReturn(workloadManager());

        // Export the project and import it again
        var captor = runExportImportAndFetchWorkloadManager();

        // Check that after re-importing the exported projects, they are identical to the original
        assertThat(captor.getAllValues()) //
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactly(workloadManager());
    }

    private ArgumentCaptor<WorkloadManager> runExportImportAndFetchWorkloadManager()
        throws Exception
    {
        // Export the project
        var exportRequest = new FullProjectExportRequest(project, null, false);
        var monitor = new ProjectExportTaskMonitor(project, null, "test",
                exportRequest.getFilenamePrefix());
        var exportedProject = new ExportedProject();
        var stage = mock(ZipOutputStream.class);

        sut.exportData(exportRequest, monitor, exportedProject, stage);

        // Import the project again
        var captor = ArgumentCaptor.forClass(WorkloadManager.class);
        doNothing().when(workloadManagementService).saveConfiguration(captor.capture());

        var importRequest = ProjectImportRequest.builder() //
                .withCreateMissingUsers(true) //
                .withImportPermissions(true) //
                .build();
        var zipFile = mock(ZipFile.class);
        sut.importData(importRequest, project, exportedProject, zipFile);

        return captor;
    }

    private WorkloadManager workloadManager()
    {
        var workloadManager = new WorkloadManager();
        workloadManager.setProject(project);
        workloadManager.setType("static");
        workloadManager.setTraits("traits");
        return workloadManager;
    }
}
