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
package de.tudarmstadt.ukp.clarin.webanno.api;

import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_REPOSITORY_PATH;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.MDCContext;

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
     * @param aPermission
     *            the permission
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER', 'ROLE_REMOTE')")
    void createProjectPermission(ProjectPermission aPermission);

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
     */
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
     */
    boolean existsProjectPermissionLevel(User aUser, Project aProject, PermissionLevel aLevel);

    /**
     * Get a {@link ProjectPermission }objects where a project is member of. We need to get them,
     * for example if the associated {@link Project} is deleted, the {@link ProjectPermission }
     * objects too.
     *
     * @param aProject
     *            The project contained in a projectPermision
     * @return the {@link ProjectPermission } list to be analysed.
     */
    List<ProjectPermission> getProjectPermissions(Project aProject);

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

    List<PermissionLevel> getProjectPermissionLevels(User aUser, Project aProject);

    List<ProjectPermission> listProjectPermissions(User aUser);

    void setProjectPermissionLevels(User aUser, Project aProject,
            Collection<PermissionLevel> aLevels);

    /**
     * List Users those with some {@link PermissionLevel}s in the project
     *
     * @param aProject
     *            the project.
     * @return the users.
     */
    List<User> listProjectUsersWithPermissions(Project aProject);

    /**
     * List of users with the a given {@link PermissionLevel}
     *
     * @param aProject
     *            The {@link Project}
     * @param aPermissionLevel
     *            The {@link PermissionLevel}
     * @return the users.
     */
    List<User> listProjectUsersWithPermissions(Project aProject, PermissionLevel aPermissionLevel);

    /**
     * Removes all permissions for the given user to the given proejct.
     */
    void leaveProject(User aObject, Project aProject);

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
     */
    Date getProjectTimeStamp(Project aProject, String aUsername);

    /**
     * get the timestamp, of the curator, if exist
     *
     * @param aProject
     *            the project.
     * @return the timestamp.
     */
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
     * @param aProject
     *            the project to be deleted
     * @throws IOException
     *             if the project to be deleted is not available in the file system
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void removeProject(Project aProject) throws IOException;

    /**
     * List projects accessible by current user
     *
     * @return list of projects accessible by the user.
     */
    List<Project> listAccessibleProjects(User aUser);

    /**
     * List projects accessible by current user
     *
     * @return list of projects accessible by the user.
     */
    Map<Project, Set<PermissionLevel>> listAccessibleProjectsWithPermissions(User aUser);

    /**
     * List projects manageable by current user
     *
     * @return list of projects manageable by the user.
     */
    List<Project> listManageableProjects(User aUser);

    /**
     * List projects in which the given user is curator or manager
     *
     * @return list of projects manageable by the user.
     */
    List<Project> listManageableCuratableProjects(User aUser);

    /**
     * List projects that allow calculation of pairwise agreement
     */
    List<Project> listProjectsForAgreement();

    File getProjectFolder(Project aProject);

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
     */
    void savePropertiesFile(Project aProject, InputStream aInputStream, String aFileName)
        throws IOException;

    // --------------------------------------------------------------------------------------------
    // Methods related to guidelines
    // --------------------------------------------------------------------------------------------

    /**
     * Write this {@code content} of the guideline file in the project;
     *
     * @param aProject
     *            the project.
     * @param aContent
     *            the guidelines.
     * @param aFileName
     *            the filename.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void createGuideline(Project aProject, File aContent, String aFileName) throws IOException;

    void createGuideline(Project aProject, InputStream aContent, String aFileName)
        throws IOException;

    /**
     * get the annotation guideline document from the file system
     *
     * @param aProject
     *            the project.
     * @param aFileName
     *            the filename.
     * @return the file.
     */
    File getGuideline(Project aProject, String aFileName);

    /**
     * Export the associated project guideline for this {@link Project} while copying a project
     *
     * @param aProject
     *            the project.
     * @return the file.
     */
    File getGuidelinesFolder(Project aProject);

    /**
     * List annotation guideline document already uploaded
     *
     * @param aProject
     *            the project.
     * @return the filenames.
     */
    List<String> listGuidelines(Project aProject);

    /**
     * Checks if the given project defines any guidelines.
     *
     * @param aProject
     *            the project.
     * @return the filenames.
     */
    boolean hasGuidelines(Project aProject);

    /**
     * Remove an annotation guideline document from the file system
     *
     * @param aProject
     *            the project.
     * @param aFileName
     *            the filename.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void removeGuideline(Project aProject, String aFileName) throws IOException;

    // --------------------------------------------------------------------------------------------
    // Methods related to permissions
    // --------------------------------------------------------------------------------------------

    /**
     * Can the given user access the project setting of <b>some</b> project.
     */
    public boolean managesAnyProject(User aUser);

    /**
     * Determine if the user is allowed to update a project.
     *
     * @param aProject
     *            the project
     * @param aUser
     *            the user.
     * @return if the user may update a project.
     */
    boolean isManager(Project aProject, User aUser);

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

    boolean hasRole(User aUser, Project aProject, PermissionLevel... aRole);

    // --------------------------------------------------------------------------------------------
    // Methods related to other things
    // --------------------------------------------------------------------------------------------

    /**
     * Initialize the project with default {@link AnnotationLayer}, {@link TagSet}s, and {@link Tag}
     * s. This is done per Project.
     * 
     * @param aProject
     *            the project.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void initializeProject(Project aProject) throws IOException;

    void initializeProject(Project aProject, List<ProjectInitializer> aInitializers)
        throws IOException;

    List<ProjectInitializer> listProjectInitializers();

    static MDCContext withProjectLogger(Project aProject)
    {
        Validate.notNull(aProject, "Project must be given");
        Validate.notNull(aProject.getId(), "Project must have been saved already");
        Validate.notNull(MDC.get(KEY_REPOSITORY_PATH), "Repository path must be set in MDC");

        return MDCContext.open().with(KEY_PROJECT_ID, String.valueOf(aProject.getId()));
    }

    String deriveSlugFromName(String aName);

    String deriveUniqueSlug(String aSlug);
}
