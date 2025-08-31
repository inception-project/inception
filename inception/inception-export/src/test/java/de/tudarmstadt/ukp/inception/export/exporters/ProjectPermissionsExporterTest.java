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
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.union;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.project.exporters.ProjectPermissionsExporter;
import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

@ExtendWith(MockitoExtension.class)
public class ProjectPermissionsExporterTest
{
    private @Mock ApplicationContext appContext;
    private @Mock ProjectService projectService;
    private @Mock UserDao userService;

    private Project project;
    private ExportedProject exportedProject;

    private User manager;
    private List<ProjectPermission> managerPermissions;
    private User annotator;
    private List<ProjectPermission> annotatorPermissions;

    private ProjectPermissionsExporter sut;

    private ArgumentCaptor<User> userCaptor;
    private ArgumentCaptor<ProjectPermission> permissionCaptor;

    @BeforeEach
    public void setUp() throws Exception
    {
        new ApplicationContextProvider().setApplicationContext(appContext);
        lenient().when(appContext.getBean("passwordEncoder", PasswordEncoder.class))
                .thenReturn(new BCryptPasswordEncoder());

        project = Project.builder().withId(1l).withName("Test Project").build();
        exportedProject = new ExportedProject();

        manager = User.builder().withUsername("manager").withRoles(ROLE_USER).build();
        managerPermissions = asList(new ProjectPermission(project, manager.getUsername(), MANAGER));

        annotator = new User("projectAnnotator", ROLE_USER);
        annotatorPermissions = asList(
                new ProjectPermission(project, annotator.getUsername(), ANNOTATOR));

        sut = new ProjectPermissionsExporter(projectService, userService);

        when(projectService.listUsersWithAnyRoleInProject(any()))
                .thenReturn(asList(manager, annotator));
        when(projectService.listProjectPermissionLevel(manager, project))
                .thenReturn(managerPermissions);
        when(projectService.listProjectPermissionLevel(annotator, project))
                .thenReturn(annotatorPermissions);

        userCaptor = captureCreatedUsers();
        permissionCaptor = captureCreatedPermissions();
    }

    @Test
    public void thatUserPermissionsAreExportedAndImported() throws Exception
    {
        annotator.setRealm(Realm.REALM_GLOBAL);

        exportProject();

        var importRequest = ProjectImportRequest.builder() //
                .withCreateMissingUsers(true) //
                .withImportPermissions(true) //
                .build();
        sut.importData(importRequest, project, exportedProject, mock(ZipFile.class));

        assertThat(userCaptor.getAllValues()) //
                .as("Missing users have been created") //
                .containsExactlyInAnyOrderElementsOf(asList(manager, annotator));

        assertThat(permissionCaptor.getAllValues()) //
                .as("Permissions have been imported") //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id") //
                .containsExactlyInAnyOrderElementsOf(
                        union(managerPermissions, annotatorPermissions));
    }

    @Test
    public void thatUserPermissionsAreNotImported() throws Exception
    {
        annotator.setRealm(Realm.REALM_GLOBAL);

        exportProject();

        var importRequest = ProjectImportRequest.builder() //
                .withCreateMissingUsers(false) //
                .withImportPermissions(false) //
                .withManager(manager) //
                .build();
        sut.importData(importRequest, project, exportedProject, mock(ZipFile.class));

        assertThat(userCaptor.getAllValues()) //
                .as("No missing users have been created") //
                .isEmpty();

        assertThat(permissionCaptor.getAllValues()) //
                .as("Permissions have been imported") //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id") //
                .containsExactlyInAnyOrderElementsOf(managerPermissions);
    }

    @Test
    public void thatProjectSpecificPermissionsAreCreatedIfUserDidNotYetExist() throws Exception
    {
        annotator.setRealm(Realm.REALM_PROJECT_PREFIX + project.getId());

        exportProject();

        // Trying to import project into another instance from which the project was exported, so
        // the project-bound user should not yet exist. Thus, we can create this user and set up the
        // permissions
        when(userService.exists(annotator.getUsername())).thenReturn(false);

        var importRequest = ProjectImportRequest.builder() //
                .withCreateMissingUsers(false) // project-bound users are always created ...
                .withImportPermissions(false) // ... and always get their permissions
                .build();
        sut.importData(importRequest, project, exportedProject, mock(ZipFile.class));

        assertThat(userCaptor.getAllValues()) //
                .as("Missing users have been created") //
                .containsExactlyInAnyOrderElementsOf(asList(annotator));

        assertThat(permissionCaptor.getAllValues()) //
                .as("Permissions have been imported") //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id") //
                .containsExactlyInAnyOrderElementsOf(annotatorPermissions);
    }

    @Test
    public void thatProjectSpecificPermissionsAreNotCreatedIfUserAlreadyExisted() throws Exception
    {
        annotator.setRealm(Realm.REALM_PROJECT_PREFIX + project.getId());

        exportProject();

        // Trying to import project into the same instance from which the project was exported, so
        // the project-bound user already exists. We must not bind this used to another project in
        // the same instance.
        when(userService.exists(annotator.getUsername())).thenReturn(true);

        var importRequest = ProjectImportRequest.builder() //
                .withCreateMissingUsers(false) // project-bound users are always created ...
                .withImportPermissions(true) // ... and always get their permissions unless they
                                             // existed!
                .build();
        sut.importData(importRequest, project, exportedProject, mock(ZipFile.class));

        assertThat(userCaptor.getAllValues()) //
                .as("No missing users have been created") //
                .isEmpty();

        assertThat(permissionCaptor.getAllValues()) //
                .as("Permissions have been imported") //
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id") //
                .containsExactlyInAnyOrderElementsOf(managerPermissions);
    }

    private void exportProject() throws Exception
    {
        var exportRequest = new FullProjectExportRequest(project, null, false);
        var monitor = new ProjectExportTaskMonitor(project, null, "test",
                exportRequest.getFilenamePrefix());
        var stage = mock(ZipOutputStream.class);
        sut.exportData(exportRequest, monitor, exportedProject, stage);
    }

    private ArgumentCaptor<ProjectPermission> captureCreatedPermissions()
    {
        var captor = ArgumentCaptor.forClass(ProjectPermission.class);
        lenient().doNothing().when(projectService).createProjectPermission(captor.capture());
        return captor;
    }

    private ArgumentCaptor<User> captureCreatedUsers()
    {
        var captor = ArgumentCaptor.forClass(User.class);
        lenient().when(userService.create(captor.capture()))
                .thenAnswer(_call -> _call.getArgument(0));
        return captor;
    }
}
