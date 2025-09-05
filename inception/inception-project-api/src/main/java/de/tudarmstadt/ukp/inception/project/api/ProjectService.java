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
package de.tudarmstadt.ukp.inception.project.api;

import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_PROJECT_ID;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_REPOSITORY_PATH;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.MDC;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.inception.support.logging.MDCContext;

public interface ProjectService
{
    String SERVICE_NAME = "projectService";

    String PROJECT_FOLDER = "project";
    String DOCUMENT_FOLDER = "document";
    String SOURCE_FOLDER = "source";
    String ANNOTATION_FOLDER = "annotation";
    String SETTINGS_FOLDER = "settings";
    String META_INF_FOLDER = "META-INF";
    String LOG_FOLDER = "log";

    /**
     * creates a project permission, adding permission level for the user in the given project
     *
     * @param aPermission
     *            the permission
     * @deprecated Use {@link #assignRole(Project, User, PermissionLevel...)} instead.
     */
    @Deprecated
    void createProjectPermission(ProjectPermission aPermission);

    /**
     * @deprecated Use {@link #revokeRole(Project, User, PermissionLevel...)} instead.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    void removeProjectPermission(ProjectPermission aPermission);

    /**
     * Check if a user have at least one {@link PermissionLevel } for this {@link Project}
     *
     * @param aUser
     *            the user.
     * @param aProject
     *            the project.
     *
     * @return if the project permission exists.
     * @deprecated Use {@link #hasAnyRole(User, Project)}
     */
    @Deprecated
    boolean existsProjectPermission(User aUser, Project aProject);

    /**
     * Check if there is already a {@link PermissionLevel} on a given {@link Project} for a given
     * {@link User}
     *
     * @param aUser
     *            the user.
     * @param aProject
     *            the project.
     * @param aLevel
     *            the permission level.
     *
     * @return if the permission exists.
     * @deprecated Use {@link #hasRole(User, Project, PermissionLevel, PermissionLevel...)}
     */
    @Deprecated
    boolean existsProjectPermissionLevel(User aUser, Project aProject, PermissionLevel aLevel);

    /**
     * Get a {@link ProjectPermission} objects where a project is member of. We need to get them,
     * for example if the associated {@link Project} is deleted, the {@link ProjectPermission }
     * objects too.
     *
     * @param aProject
     *            The project contained in a projectPermision
     * @return the {@link ProjectPermission } list to be analysed.
     */
    List<ProjectPermission> listProjectPermissions(Project aProject);

    /**
     * @deprecated Use {@link #listProjectPermissions(Project)} instead
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    List<ProjectPermission> getProjectPermissions(Project aProject);

    List<ProjectUserPermissions> listProjectUserPermissions(Project aProject);

    ProjectUserPermissions getProjectUserPermissions(Project aProject, User aUser);

    /**
     * Get list of permissions a user have in a given project
     *
     * @param aUser
     *            the user.
     * @param aProject
     *            the project.
     *
     * @return the permissions.
     */
    List<ProjectPermission> listProjectPermissionLevel(User aUser, Project aProject);

    List<ProjectPermission> listProjectPermissionLevel(String aUser, Project aProject);

    /**
     * @deprecated Use {@link #listRoles(Project, User)} instead.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    List<PermissionLevel> getProjectPermissionLevels(User aUser, Project aProject);

    List<ProjectPermission> listProjectPermissions(User aUser);

    void setProjectPermissionLevels(String aUser, Project aProject,
            Collection<PermissionLevel> aLevels);

    void setProjectPermissionLevels(User aUser, Project aProject,
            Collection<PermissionLevel> aLevels);

    void assignRole(Project aProject, User aUser, PermissionLevel... aRoles);

    /**
     * It may be necessary to use this method e.g. when importing data in case the user does not
     * exist in the system.
     * 
     * @deprecated Should not be used. Better use
     *             {@link #assignRole(Project, User, PermissionLevel...)}
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    void assignRole(Project aProject, String aUser, PermissionLevel... aRoles);

    void revokeRole(Project aProject, User aUser, PermissionLevel... aRoles);

    void revokeAllRoles(Project aProject, User aUser);

    List<PermissionLevel> listRoles(Project aProject, User aUser);

    List<PermissionLevel> listRoles(Project aProject, String aUser);

    /**
     * List Users those with some {@link PermissionLevel}s in the project
     *
     * @param aProject
     *            the project.
     * @return the users.
     */
    List<User> listUsersWithAnyRoleInProject(Project aProject);

    /**
     * List of users with the a given {@link PermissionLevel}
     *
     * @param aProject
     *            The {@link Project}
     * @param aPermissionLevel
     *            The {@link PermissionLevel}
     * @return the users.
     */
    List<User> listUsersWithRoleInProject(Project aProject, PermissionLevel aPermissionLevel);

    /**
     * Removes all permissions for the given user to the given project.
     * 
     * @deprecated use {@link #revokeAllRoles(Project, User)}
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    void leaveProject(User aObject, Project aProject);

    /**
     * @return list of projects which contain with those annotation documents state is finished
     */
    List<Project> listProjectsWithFinishedAnnos();

    /**
     * List all projects in which the given user has any of the provided roles. Note that the split
     * into two arguments is only for the compiler to be able to check if at least one role has been
     * specified. The first role is not privileged over the other roles in any way!
     * 
     * @param aUser
     *            the user.
     * @param aRole
     *            at least one role must be given, but the check is against this role OR any of the
     *            additional roles.
     * @param aMoreRoles
     *            more roles.
     * @return the list of projects.
     */
    List<Project> listProjectsWithUserHavingRole(User aUser, PermissionLevel aRole,
            PermissionLevel... aMoreRoles);

    /**
     * @return list of all projects in which the given user has any role at all.
     * 
     * @param aUser
     *            the user.
     */
    List<Project> listProjectsWithUserHavingAnyRole(User aUser);

    // --------------------------------------------------------------------------------------------
    // Methods related to Projects
    // --------------------------------------------------------------------------------------------

    /**
     * Creates a {@code Project}. Creating a project needs a global ROLE_ADMIN role. For the first
     * time the project is created, an associated project path will be created on the file system as
     * {@code webanno.home/project/Project.id }
     *
     * @param aProject
     *            The {@link Project} object to be created.
     * @throws IOException
     *             If the specified webanno.home directory is not available no write permission
     * @return the project;
     */
    Project createProject(Project aProject) throws IOException;

    /**
     * Update a project. This is only necessary when dealing with a detached project entity.
     * 
     * @param aProject
     *            The {@link Project} object to be updated.
     */
    void updateProject(Project aProject);

    /**
     * Update the project state and issue a {@link ProjectStateChangedEvent} if necessary. Make sure
     * that the status of the project object is fresh to avoid getting spurious events.
     * 
     * @param aProject
     *            The {@link Project} object to be updated.
     * @param aState
     *            the new state.
     */
    void setProjectState(Project aProject, ProjectState aState);

    /**
     * Check if a project with the given name already exists.
     *
     * @param aName
     *            the project name.
     * @return if the project exists.
     */
    boolean existsProjectWithName(String aName);

    /**
     * Check if a project with the given URL slug already exists.
     *
     * @param aSlug
     *            the project slug.
     * @return if the project exists.
     */
    boolean existsProjectWithSlug(String aSlug);

    /**
     * Get a timestamp of for this {@link Project} of this username
     *
     * @param aProject
     *            the project.
     * @param aUsername
     *            the username.
     * @return the timestamp.
     * @deprecated To be removed without replacement
     */
    @Deprecated
    Date getProjectTimeStamp(Project aProject, String aUsername);

    /**
     * get the timestamp, of the curator, if exist
     *
     * @param aProject
     *            the project.
     * @return the timestamp.
     * @deprecated To be removed without replacement
     */
    @Deprecated
    Date getProjectTimeStamp(Project aProject);

    /**
     * Get a {@link Project} from the database the name of the Project
     *
     * @param aSlug
     *            URL slug of the project
     * @return {@link Project} object from the database or an error if the project is not found.
     *         Exception is handled from the calling method.
     */
    Project getProjectBySlug(String aSlug);

    /**
     * Get a project by its id.
     *
     * @param aId
     *            the ID.
     * @return the project.
     */
    Project getProject(long aId);

    /**
     * List all Projects.
     *
     * @return the projects
     */
    List<Project> listProjects();

    /**
     * Remove a project. A ROLE_ADMIN or project admin can remove a project. removing a project will
     * remove associated source documents and annotation documents.
     *
     * @param aProject
     *            the project to be deleted
     * @throws IOException
     *             if the project to be deleted is not available in the file system
     */
    void removeProject(Project aProject) throws IOException;

    /**
     * List projects accessible by the given user
     *
     * @param aUser
     *            the user
     * @return list of projects accessible by the user.
     */
    List<Project> listAccessibleProjects(User aUser);

    /**
     * List projects accessible by the given user along with the roles that user has on the
     * projects.
     *
     * @param aUser
     *            the user
     * @return list of projects accessible by the user.
     */
    Map<Project, Set<PermissionLevel>> listAccessibleProjectsWithPermissions(User aUser);

    /**
     * List projects manageable by current user
     * 
     * @param aUser
     *            the user
     * @return list of projects manageable by the user.
     */
    List<Project> listManageableProjects(User aUser);

    /**
     * Export the associated project log for this {@link Project} while copying a project
     *
     * @param aProject
     *            the project.
     * @return the log file.
     */
    File getProjectLogFile(Project aProject);

    File getMetaInfFolder(Project aProject);

    /**
     * Save some properties file associated to a project, such as meta-data.properties
     *
     * @param aProject
     *            The project for which the user save some properties file.
     * @param aInputStream
     *            the properties file.
     * @param aFileName
     *            the file name.
     * @throws IOException
     *             if an I/O error occurs.
     * @deprecated To be removed without replacement
     */
    @Deprecated()
    void savePropertiesFile(Project aProject, InputStream aInputStream, String aFileName)
        throws IOException;

    // --------------------------------------------------------------------------------------------
    // Methods related to permissions
    // --------------------------------------------------------------------------------------------

    /**
     * @param aUser
     *            the user
     * @return if the user manages any project or has the ability to create new projects that the
     *         user would then manage.
     */
    public boolean managesAnyProject(User aUser);

    /**
     * Check whether the given user has any role at all in the given project.
     * 
     * @param aUser
     *            a user.
     * @param aProject
     *            a project.
     * @return whether the user has any role in the project.
     */
    boolean hasAnyRole(User aUser, Project aProject);

    /**
     * Check whether the given user has one or more roles in a project. Note that the split into two
     * arguments is only for the compiler to be able to check if at least one role has been
     * specified. The first role is not privileged over the other roles in any way!
     * 
     * @param aUser
     *            a user.
     * @param aProject
     *            a project.
     * @param aRole
     *            at least one role must be given, but the check is against this role OR any of the
     *            additional roles.
     * @param aMoreRoles
     *            more roles.
     * @return whether the user has any of the roles in the project.
     */
    boolean hasRole(User aUser, Project aProject, PermissionLevel aRole,
            PermissionLevel... aMoreRoles);

    /**
     * Check whether the given user has one or more roles in a project. Note that the split into two
     * arguments is only for the compiler to be able to check if at least one role has been
     * specified. The first role is not privileged over the other roles in any way!
     * 
     * @param aUser
     *            a user.
     * @param aProject
     *            a project.
     * @param aRole
     *            at least one role must be given, but the check is against this role OR any of the
     *            additional roles.
     * @param aMoreRoles
     *            more roles.
     * @return whether the user has any of the roles in the project.
     */
    boolean hasRole(String aUser, Project aProject, PermissionLevel aRole,
            PermissionLevel... aMoreRoles);

    /**
     * Check whether the given user has one or more roles in any project. Note that the split into
     * two arguments is only for the compiler to be able to check if at least one role has been
     * specified. The first role is not privileged over the other roles in any way!
     * 
     * @param aUser
     *            a user.
     * @param aRole
     *            at least one role must be given, but the check is against this role OR any of the
     *            additional roles.
     * @param aMoreRoles
     *            more roles.
     * @return whether the user has any of the roles in any project.
     */
    boolean hasRoleInAnyProject(User aUser, PermissionLevel aRole, PermissionLevel... aMoreRoles);

    /**
     * Check whether the given user has one or more roles in any project. Note that the split into
     * two arguments is only for the compiler to be able to check if at least one role has been
     * specified. The first role is not privileged over the other roles in any way!
     * 
     * @param aUser
     *            a user.
     * @param aRole
     *            at least one role must be given, but the check is against this role OR any of the
     *            additional roles.
     * @param aMoreRoles
     *            more roles.
     * @return whether the user has any of the roles in any project.
     */
    boolean hasRoleInAnyProject(String aUser, PermissionLevel aRole, PermissionLevel... aMoreRoles);

    // --------------------------------------------------------------------------------------------
    // Methods related to other things
    // --------------------------------------------------------------------------------------------

    /**
     * Initialize the project with default {@link AnnotationLayer}, {@link TagSet}s, and {@link Tag}
     * s. This is done per Project.
     * 
     * @param aRequest
     *            the project initialization request.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void initializeProject(ProjectInitializationRequest aRequest) throws IOException;

    void initializeProject(ProjectInitializationRequest aRequest,
            List<ProjectInitializer> aInitializers)
        throws IOException;

    default void initializeProject(Project aProject) throws IOException
    {
        var request = ProjectInitializationRequest.builder().withProject(aProject).build();
        initializeProject(request);
    }

    default void initializeProject(Project aProject, List<ProjectInitializer> aInitializers)
        throws IOException
    {
        var request = ProjectInitializationRequest.builder().withProject(aProject).build();
        initializeProject(request, aInitializers);
    }

    List<ProjectInitializer> listProjectInitializers();

    List<FeatureInitializer> listFeatureInitializers();

    static MDCContext withProjectLogger(Project aProject)
    {
        Validate.notNull(aProject, "Project must be given");
        Validate.notNull(aProject.getId(), "Project must have been saved already");
        Validate.notNull(MDC.get(KEY_REPOSITORY_PATH), "Repository path must be set in MDC");

        return MDCContext.open().with(KEY_PROJECT_ID, String.valueOf(aProject.getId()));
    }

    String deriveSlugFromName(String aName);

    String deriveUniqueSlug(String aSlug);

    Realm getRealm(String aRealmId);

    Realm getRealm(Project aProject);

    Optional<User> getProjectBoundUser(Project aProject, String aUsername);

    User getOrCreateProjectBoundUser(Project aProject, String aUsername);

    List<User> listProjectBoundUsers(Project aProject);

    void deleteProjectBoundUser(Project aProject, User aUser);

    User createProjectBoundUser(Project aProject, String aUiName);
}
