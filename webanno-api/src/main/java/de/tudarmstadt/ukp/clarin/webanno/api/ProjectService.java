/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipFile;

import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public interface ProjectService
{
    String SERVICE_NAME = "projectService";
    
    String PROJECT_FOLDER = "project";
    String DOCUMENT_FOLDER = "document";
    String SOURCE_FOLDER = "source";
    String GUIDELINES_FOLDER = "guideline";
    String ANNOTATION_FOLDER = "annotation";
    String SETTINGS_FOLDER = "settings";
    String META_INF_FOLDER = "META-INF";
    String LOG_FOLDER = "log";
    
    /**
     * creates a project permission, adding permission level for the user in the given project
     *
     * @param permission
     *            the permission
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER', 'ROLE_REMOTE')")
    void createProjectPermission(ProjectPermission permission);

    /**
     * Check if a user have at least one {@link PermissionLevel } for this {@link Project}
     *
     * @param user
     *            the user.
     * @param project
     *            the project.
     *
     * @return if the project permission exists.
     */
    boolean existsProjectPermission(User user, Project project);

    /**
     * Check if there is already a {@link PermissionLevel} on a given {@link Project} for a given
     * {@link User}
     *
     * @param user
     *            the user.
     * @param project
     *            the project.
     * @param level
     *            the permission level.
     *
     * @return if the permission exists.
     */
    boolean existsProjectPermissionLevel(User user, Project project, PermissionLevel level);

    /**
     * Get a {@link ProjectPermission }objects where a project is member of. We need to get them,
     * for example if the associated {@link Project} is deleted, the {@link ProjectPermission }
     * objects too.
     *
     * @param project
     *            The project contained in a projectPermision
     * @return the {@link ProjectPermission } list to be analysed.
     */
    List<ProjectPermission> getProjectPermissions(Project project);

    /**
     * Get list of permissions a user have in a given project
     *
     * @param user
     *            the user.
     * @param project
     *            the project.
     *
     * @return the permissions.
     */
    List<ProjectPermission> listProjectPermissionLevel(User user, Project project);

    List<PermissionLevel> getProjectPermissionLevels(User aUser, Project aProject);

    void setProjectPermissionLevels(User aUser, Project aProject,
            Collection<PermissionLevel> aLevels);
    
    /**
     * List Users those with some {@link PermissionLevel}s in the project
     *
     * @param project
     *            the project.
     * @return the users.
     */
    List<User> listProjectUsersWithPermissions(Project project);

    /**
     * List of users with the a given {@link PermissionLevel}
     *
     * @param project
     *            The {@link Project}
     * @param permissionLevel
     *            The {@link PermissionLevel}
     * @return the users.
     */
    List<User> listProjectUsersWithPermissions(Project project, PermissionLevel permissionLevel);

    /**
     * remove a user permission from the project
     *
     * @param projectPermission
     *            The ProjectPermission to be removed
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void removeProjectPermission(ProjectPermission projectPermission);

    /**
     * list Projects which contain with those annotation documents state is finished
     */
    List<Project> listProjectsWithFinishedAnnos();

    // --------------------------------------------------------------------------------------------
    // Methods related to Projects
    // --------------------------------------------------------------------------------------------

    /**
     * Creates a {@code Project}. Creating a project needs a global ROLE_ADMIN role. For the first
     * time the project is created, an associated project path will be created on the file system as
     * {@code webanno.home/project/Project.id }
     *
     * @param project
     *            The {@link Project} object to be created.
     * @throws IOException
     *             If the specified webanno.home directory is not available no write permission
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_REMOTE','ROLE_PROJECT_CREATOR')")
    void createProject(Project project)
        throws IOException;

    /**
     * Update a project. This is only necessary when dealing with a detached project entity.
     * 
     * @param project
     *            The {@link Project} object to be updated.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_REMOTE','ROLE_PROJECT_CREATOR')")
    void updateProject(Project project);

    /**
     * Update the project state.
     * 
     * @param aProject
     *            The {@link Project} to be updated.
     */
    void recalculateProjectState(Project aProject);

    /**
     * A method that check is a project exists with the same name already. getSingleResult() fails
     * if the project is not created, hence existProject returns false.
     *
     * @param name
     *            the project name.
     * @return if the project exists.
     */
    boolean existsProject(String name);

    /**
     * Check if there exists an project timestamp for this user and {@link Project}.
     *
     * @param project
     *            the project.
     * @param username
     *            the username.
     * @return if a timestamp exists.
     */
    boolean existsProjectTimeStamp(Project project, String username);

    /**
     * check if there exists a timestamp for at least one source document in aproject (add when a
     * curator start curating)
     *
     * @param project
     *            the project.
     * @return if a timestamp exists.
     */
    boolean existsProjectTimeStamp(Project project);

    /**
     * Get a timestamp of for this {@link Project} of this username
     *
     * @param project
     *            the project.
     * @param username
     *            the username.
     * @return the timestamp.
     */
    Date getProjectTimeStamp(Project project, String username);

    /**
     * get the timestamp, of the curator, if exist
     *
     * @param project
     *            the project.
     * @return the timestamp.
     */
    Date getProjectTimeStamp(Project project);

    /**
     * Get a {@link Project} from the database the name of the Project
     *
     * @param name
     *            name of the project
     * @return {@link Project} object from the database or an error if the project is not found.
     *         Exception is handled from the calling method.
     */
    Project getProject(String name);

    /**
     * Get a project by its id.
     *
     * @param id
     *            the ID.
     * @return the project.
     */
    Project getProject(long id);

    /**
     * List all Projects. If the user logged have a ROLE_ADMIN, he can see all the projects.
     * Otherwise, a user will see projects only he is member of.
     *
     * @return the projects
     */
    List<Project> listProjects();

    /**
     * Remove a project. A ROLE_ADMIN or project admin can remove a project. removing a project will
     * remove associated source documents and annotation documents.
     *
     * @param project
     *            the project to be deleted
     * @throws IOException
     *             if the project to be deleted is not available in the file system
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void removeProject(Project project)
        throws IOException;

    /**
     * List projects accessible by current user
     *
     * @return list of projects accessible by the user.
     */
    List<Project> listAccessibleProjects(User aUser);

    /**
     * List projects manageable by current user
     *
     * @return list of projects manageable by the user.
     */
    List<Project> listManageableProjects(User aUser);


    /**
     * Export the associated project log for this {@link Project} while copying a project
     *
     * @param project
     *            the project.
     * @return the log file.
     */
    File getProjectLogFile(Project project);

    File getMetaInfFolder(Project project);
    
    /**
     * Save some properties file associated to a project, such as meta-data.properties
     *
     * @param project
     *            The project for which the user save some properties file.
     * @param is
     *            the properties file.
     * @param fileName
     *            the file name.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void savePropertiesFile(Project project, InputStream is, String fileName)
        throws IOException;
    
    // --------------------------------------------------------------------------------------------
    // Methods related to per-project user preferences
    // --------------------------------------------------------------------------------------------

    /**
     * Load annotation preferences such as {@code BratAnnotator#windowSize} from a property file
     *
     * @param username
     *            the username.
     * @param project
     *            the project where the user is working on.
     * @return the properties.
     * @throws IOException
     *             if an I/O error occurs.
     */
    Properties loadUserSettings(String username, Project project)
        throws IOException;

    /**
     * Save annotation references, such as {@code BratAnnotator#windowSize}..., in a properties file
     * so that they are not required to configure every time they open the document.
     *
     * @param <T>
     *            object type to save
     * @param username
     *            the user name
     * @param subject
     *            differentiate the setting, either it is for {@code AnnotationPage} or
     *            {@code CurationPage}
     * @param configurationObject
     *            The Object to be saved as preference in the properties file.
     * @param project
     *            The project where the user is working on.
     * @throws IOException
     *             if an I/O error occurs.
     */
    <T> void saveUserSettings(String username, Project project, Mode subject, T configurationObject)
        throws IOException;

    // --------------------------------------------------------------------------------------------
    // Methods related to guidelines
    // --------------------------------------------------------------------------------------------

    /**
     * Write this {@code content} of the guideline file in the project;
     *
     * @param project
     *            the project.
     * @param content
     *            the guidelines.
     * @param fileName
     *            the filename.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void createGuideline(Project project, File content, String fileName) throws IOException;

    void createGuideline(Project project, InputStream content, String fileName) throws IOException;

    /**
     * get the annotation guideline document from the file system
     *
     * @param project
     *            the project.
     * @param fileName
     *            the filename.
     * @return the file.
     */
    File getGuideline(Project project, String fileName);

    /**
     * Export the associated project guideline for this {@link Project} while copying a project
     *
     * @param project
     *            the project.
     * @return the file.
     */
    File getGuidelinesFolder(Project project);

    /**
     * List annotation guideline document already uploaded
     *
     * @param project
     *            the project.
     * @return the filenames.
     */
    List<String> listGuidelines(Project project);

    /**
     * Remove an annotation guideline document from the file system
     *
     * @param project
     *            the project.
     * @param fileName
     *            the filename.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void removeGuideline(Project project, String fileName)
        throws IOException;
    
    // --------------------------------------------------------------------------------------------
    // Methods related to permissions
    // --------------------------------------------------------------------------------------------

    /**
     * Returns a role of a user, globally we will have ROLE_ADMIN and ROLE_USER
     *
     * @param user
     *            the {@link User} object
     * @return the roles.
     * @deprecated use {@link UserDao#listAuthorities(User)}
     */
    @Deprecated
    List<Authority> listAuthorities(User user);
    
    /**
     * Can the given user access the project setting of <b>some</b> project. 
     */
    public boolean managesAnyProject(User user);
    
    /**
     * Determine if the user is allowed to update a project.
     *
     * @param aProject
     *            the project
     * @param aUser
     *            the user.
     * @return if the user may update a project.
     */
    boolean isProjectAdmin(Project aProject, User aUser);

    /**
     * Determine if the User is an admin of a project
     *
     * @param aProject
     *            the project.
     * @param aUser
     *            the user.
     * @return if the user is an admin.
     */
    boolean isAdmin(Project aProject, User aUser);

    /**
     * Determine if the user is a curator or not.
     *
     * @param aProject
     *            the project.
     * @param aUser
     *            the user.
     * @return if the user is a curator.
     */
    boolean isCurator(Project aProject, User aUser);

    /**
     * Determine if the User is member of a project
     *
     * @param aProject
     *            the project.
     * @param aUser
     *            the user.
     * @return if the user is a member.
     */
    boolean isAnnotator(Project aProject, User aUser);

    
    
    // --------------------------------------------------------------------------------------------
    // Methods related to other things
    // --------------------------------------------------------------------------------------------

    void onProjectImport(ZipFile zip,
            de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject aExportedProject,
            Project aProject)
        throws Exception;
    
    List<ProjectType> listProjectTypes();
}
