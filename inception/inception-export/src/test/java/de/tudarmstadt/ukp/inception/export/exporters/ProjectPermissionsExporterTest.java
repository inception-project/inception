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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.REALM_PROJECT_PREFIX;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.union;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.project.exporters.ProjectPermissionsExporter;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;

@ExtendWith(MockitoExtension.class)
public class ProjectPermissionsExporterTest
{
    public @TempDir File workFolder;

    private @Mock ApplicationContext appContext;
    private @Mock ProjectService projectService;
    private @Mock UserDao userService;

    private Project project;
    private User manager;
    private List<ProjectPermission> managerPermissions;
    private User annotator;
    private List<ProjectPermission> annotatorPermissions;

    private ProjectPermissionsExporter sut;

    @BeforeEach
    public void setUp() throws Exception
    {
        new ApplicationContextProvider().setApplicationContext(appContext);
        when(appContext.getBean("passwordEncoder", PasswordEncoder.class))
                .thenReturn(new BCryptPasswordEncoder());

        project = new Project();
        project.setId(1l);
        project.setName("Test Project");

        manager = new User("manager", ROLE_USER);
        managerPermissions = asList(new ProjectPermission(project, manager.getUsername(), MANAGER));

        annotator = new User("projectAnnotator", ROLE_USER);
        annotator.setRealm(REALM_PROJECT_PREFIX + project.getId());
        annotatorPermissions = asList(
                new ProjectPermission(project, annotator.getUsername(), ANNOTATOR));

        when(projectService.listProjectUsersWithPermissions(any()))
                .thenReturn(asList(manager, annotator));
        when(projectService.listProjectPermissionLevel(manager, project))
                .thenReturn(managerPermissions);
        when(projectService.listProjectPermissionLevel(annotator, project))
                .thenReturn(annotatorPermissions);

        sut = new ProjectPermissionsExporter(projectService, userService);
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
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userService.create(userCaptor.capture())).thenAnswer(_call -> _call.getArgument(0));

        ArgumentCaptor<ProjectPermission> permissionCaptor = ArgumentCaptor
                .forClass(ProjectPermission.class);
        doNothing().when(projectService).createProjectPermission(permissionCaptor.capture());

        ProjectImportRequest importRequest = new ProjectImportRequest(true);
        ZipFile zipFile = mock(ZipFile.class);
        sut.importData(importRequest, project, exportedProject, zipFile);

        // Check that after re-importing the exported projects, they are identical to the original
        assertThat(userCaptor.getAllValues()) //
                .containsExactlyInAnyOrderElementsOf(asList(manager, annotator));

        assertThat(permissionCaptor.getAllValues()) //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id") //
                .containsExactlyInAnyOrderElementsOf(
                        union(managerPermissions, annotatorPermissions));
    }
}
