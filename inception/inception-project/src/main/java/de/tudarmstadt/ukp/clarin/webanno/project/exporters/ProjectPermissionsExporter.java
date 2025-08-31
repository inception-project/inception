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
package de.tudarmstadt.ukp.clarin.webanno.project.exporters;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.EMPTY_PASSWORD;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.startsWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedUser;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectServiceAutoConfiguration#projectPermissionsExporter}.
 * </p>
 */
public class ProjectPermissionsExporter
    implements ProjectExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(ProjectPermissionsExporter.class);
    private static final String KEY_USERS = "project_users";

    private final ProjectService projectService;
    private final UserDao userService;

    @Autowired
    public ProjectPermissionsExporter(ProjectService aProjectService, UserDao aUserService)
    {
        projectService = aProjectService;
        userService = aUserService;
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
    {
        Project project = aRequest.getProject();

        List<ExportedUser> projectUsers = new ArrayList<>();
        List<ExportedProjectPermission> projectPermissions = new ArrayList<>();
        for (var user : projectService.listUsersWithAnyRoleInProject(project)) {
            if (startsWith(user.getRealm(), Realm.REALM_PROJECT_PREFIX)) {
                var exUser = new ExportedUser();
                exUser.setCreated(user.getCreated());
                exUser.setEmail(user.getEmail());
                exUser.setEnabled(user.isEnabled());
                exUser.setLastLogin(user.getLastLogin());
                exUser.setUiName(user.getUiName());
                exUser.setUpdated(user.getUpdated());
                exUser.setUsername(user.getUsername());
                projectUsers.add(exUser);
            }

            for (var permission : projectService.listProjectPermissionLevel(user, project)) {
                var permissionToExport = new ExportedProjectPermission();
                permissionToExport.setLevel(permission.getLevel());
                permissionToExport.setUser(user.getUsername());
                projectPermissions.add(permissionToExport);
            }
        }

        aExProject.setProperty(KEY_USERS, projectUsers);
        aExProject.setProjectPermissions(projectPermissions);

        LOG.info("Exported [{}] permissions for project [{}]", projectPermissions.size(),
                aRequest.getProject().getName());
    }

    /**
     * Create {@link ProjectPermission} from the exported {@link ExportedProjectPermission}
     * 
     * @param aExProject
     *            the imported project.
     * @param aProject
     *            the project.
     * @throws IOException
     *             if an I/O error occurs.
     */
    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        // Always import project-bound users
        var projectUsers = aExProject.getArrayProperty(KEY_USERS, ExportedUser.class);
        Set<String> createdProjectUsers = new HashSet<>();
        Set<String> rejectedProjectUsers = new HashSet<>();
        for (var importedProjectUser : projectUsers) {
            if (userService.exists(importedProjectUser.getUsername())) {
                aRequest.addMessage(format("Unable to create project-bound user [%s] with ID "
                        + "[%s] because a user with this ID already exists in the system. "
                        + "Annotations of this user are not accessible in the imported project.",
                        importedProjectUser.getUiName(), importedProjectUser.getUsername()));
                rejectedProjectUsers.add(importedProjectUser.getUsername());
                continue;
            }

            var u = new User();
            u.setRealm(Realm.REALM_PROJECT_PREFIX + aProject.getId());
            u.setEmail(importedProjectUser.getEmail());
            u.setUiName(importedProjectUser.getUiName());
            u.setUsername(importedProjectUser.getUsername());
            u.setCreated(importedProjectUser.getCreated());
            u.setLastLogin(importedProjectUser.getLastLogin());
            u.setUpdated(importedProjectUser.getUpdated());
            u.setEnabled(importedProjectUser.isEnabled());
            u.setRoles(Set.of(ROLE_USER));
            userService.create(u);

            createdProjectUsers.add(u.getUsername());
        }

        // Import permissions - always import permissions for the importing user and for
        // project-bound users but skip permissions for other users unless permission import was
        // requested.
        for (ExportedProjectPermission importedPermission : aExProject.getProjectPermissions()) {
            if (rejectedProjectUsers.contains(importedPermission.getUser())) {
                // We must not import permissions for project-bound users that could not be created
                // during import because those users are already bound ot a different project on
                // this system.
                continue;
            }

            boolean isPermissionOfImportingUser = aRequest.getManager() //
                    .map(User::getUsername) //
                    .map(importedPermission.getUser()::equals) //
                    .orElse(false);
            if (isPermissionOfImportingUser || aRequest.isImportPermissions()
                    || createdProjectUsers.contains(importedPermission.getUser())) {
                ProjectPermission permission = new ProjectPermission();
                permission.setLevel(importedPermission.getLevel());
                permission.setProject(aProject);
                permission.setUser(importedPermission.getUser());
                projectService.createProjectPermission(permission);
            }
        }

        // Give all permissions to the importing user if requested
        if (aRequest.getManager().isPresent()) {
            User user = aRequest.getManager().get();
            projectService.assignRole(aProject, user, ANNOTATOR, CURATOR, MANAGER);
        }

        // Add any users that are referenced by the project but missing in the current instance.
        // Users are added without passwords and disabled.
        if (aRequest.isCreateMissingUsers()) {
            var nonProjectUsersWithPermissions = aExProject.getProjectPermissions().stream()
                    .map(ExportedProjectPermission::getUser)
                    .filter(name -> !createdProjectUsers.contains(name)
                            && !rejectedProjectUsers.contains(name))
                    .collect(Collectors.toSet());

            for (String user : nonProjectUsersWithPermissions) {
                if (!userService.exists(user)) {
                    User u = new User();
                    u.setUsername(user);
                    u.setPassword(EMPTY_PASSWORD);
                    u.setRoles(Set.of(ROLE_USER));
                    u.setEnabled(false);
                    userService.create(u);
                }
            }
        }
    }
}
