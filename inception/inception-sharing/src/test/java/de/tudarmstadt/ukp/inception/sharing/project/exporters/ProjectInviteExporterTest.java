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
package de.tudarmstadt.ukp.inception.sharing.project.exporters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.sharing.InviteService;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;

@ExtendWith(MockitoExtension.class)
public class ProjectInviteExporterTest
{
    public @TempDir File workFolder;

    private @Mock InviteService inviteService;

    private Project project;

    private ProjectInviteExporter sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        project = new Project();
        project.setId(1l);
        project.setName("Test Project");

        when(inviteService.readProjectInvite(Mockito.any())).thenReturn(invite());

        sut = new ProjectInviteExporter(inviteService);
    }

    private ProjectInvite invite()
    {
        ProjectInvite i = new ProjectInvite();
        i.setId(1l);
        i.setProject(project);
        i.setInviteId("deadbeaf");
        i.setInvitationText("Join the fray!");
        i.setUserIdPlaceholder("Nickname");
        i.setGuestAccessible(true);
        return i;
    }

    @Test
    public void thatExportingWorks() throws Exception
    {
        // Export the project
        FullProjectExportRequest exportRequest = new FullProjectExportRequest(project, null, false);
        ProjectExportTaskMonitor monitor = new ProjectExportTaskMonitor(project, null, "test");
        ExportedProject exportedProject = new ExportedProject();

        sut.exportData(exportRequest, monitor, exportedProject, workFolder);

        // Import the project again
        ArgumentCaptor<ProjectInvite> captor = ArgumentCaptor.forClass(ProjectInvite.class);
        doNothing().when(inviteService).writeProjectInvite(captor.capture());

        ProjectImportRequest importRequest = new ProjectImportRequest(true);
        ZipFile zipFile = mock(ZipFile.class);
        sut.importData(importRequest, project, exportedProject, zipFile);

        // Check that after re-importing the exported projects, they are identical to the original
        assertThat(captor.getAllValues()) //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id") //
                .containsExactlyInAnyOrder(invite());
    }
}
