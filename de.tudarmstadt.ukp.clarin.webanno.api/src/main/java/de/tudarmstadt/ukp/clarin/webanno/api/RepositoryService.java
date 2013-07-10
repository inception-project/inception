/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.CrowdJob;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;

/**
 * This interface contains methods that are related to accessing/creating/deleting... documents,
 * Users, and Projects for the annotation system. while meta data about documents and projects and
 * users are stored in the database, source and annotation documents are stored in a file system
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public interface RepositoryService
{
    /**
     * creates the {@link AnnotationDocument } object in the database.
     *
     * @param annotationDocument
     *            {@link AnnotationDocument} comprises of the the name of the {@link SourceDocument}
     *            , id of {@link SourceDocument}, id of the {@link Project}, and id of {@link User}
     *
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createAnnotationDocument(AnnotationDocument annotationDocument);

    /**
     * Creates an annotation document which is CAS object seriealized in xmi format using the
     * {@link SerializedCasWriter}. The {@link AnnotationDocument} is stored in the
     * webanno.home/project/Project.id/document/document.id/annotation/username.ser. annotated
     * documents are stored per project, user and document
     *
     * @param user
     *            The User who perform this operation
     */

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createAnnotationDocumentContent(JCas jCas, SourceDocument document, User user)
        throws IOException;

    /**
     * Create a curation annotation document under a special user named as "CURATION_USER"
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createCurationDocumentContent(JCas jCas, SourceDocument document, User user)
        throws IOException;

    /**
     * Creates a {@code Project}. Creating a project needs a global ROLE_ADMIN role. For the first
     * time the project is created, an associated project path will be created on the file system as
     * {@code webanno.home/project/Project.id }
     *
     * @param aProject
     *            The {@link Project} object to be created.
     * @param user
     *            The User who perform this operation
     * @throws IOException
     *             If the specified webanno.home directory is not available no write permission
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_REMOTE')")
    void createProject(Project project, User user)
        throws IOException;

    /**
     * Create a crowd Project which contains some source document. A crowd project contains source documents
     * from {@link Project}(s), a {@link SourceDocument} belongs at most to one {@link CrowdJob}.
     */
    void createCrowdJob(CrowdJob crowdProject);

    /**
     * creates a project permission, adding permission level for the user in the given project
     *
     * @param permission
     * @throws IOException
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER', 'ROLE_REMOTE')")
    void createProjectPermission(ProjectPermission permission)
        throws IOException;

    /**
     * Creates a {@link SourceDocument} in a database. The source document is created by ROLE_ADMIN
     * or Project admins. Source documents are created per project and it should have a unique name
     * in the {@link Project} it belongs. renaming a a source document is not possible, rather the
     * administrator should delete and re create it.
     *
     * @param document
     *            {@link SourceDocument} to be created
     * @param user
     *            The User who perform this operation
     * @throws IOException
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER','ROLE_REMOTE')")
    void createSourceDocument(SourceDocument document, User user)
        throws IOException;

    /**
     * A Method that checks if there is already an annotation document created for the source
     * document
     *
     * @param annotationDocument
     * @return
     */
    boolean existsAnnotationDocument(SourceDocument document, User user);

    /**
     * A method that check is a project exists with the same name already. getSingleResult() fails
     * if the project is not created, hence existProject returns false.
     *
     * @param name
     * @return
     */
    boolean existsProject(String name);
    /**
     * Check if a crowd job already exist or not with its name
     */
    boolean existsCrowdJob(String name);

    /**
     * Check if a user have at least one {@link PermissionLevel } for this {@link Project}
     *
     * @return
     */
    boolean existProjectPermission(User user, Project project);

    /**
     * Check if there is already a {@link PermissionLevel} on a given {@link Project} for a given
     * {@link User}
     *
     * @return
     */
    boolean existProjectPermissionLevel(User user, Project project, PermissionLevel level);

    /**
     * Check if a Source document with this same name exist in the project. The caller method then
     * can decide to override or throw an exception/message to the cleint
     */
    boolean existSourceDocument(Project project, String fileName);
    /**
     * If the user is in the database (exclude some historycal users that have annotations in the system)
     * @return
     */
    boolean existUser(String username);

    /**
     * Exports an {@link AnnotationDocument } CAS Object as TCF/TXT/XMI... file formats.
     *
     * @param document
     *            The {@link SourceDocument} where we get the id which hosts both the source
     *            Document and the annotated document
     * @param project
     *            the project that {@link AnnotationDocument} belongs to
     * @param user
     *            the {@link User } who annotates the document.
     */
    File exportAnnotationDocument(SourceDocument document, Project project, String user,
            Class writer, String fileName, Mode mode)
        throws FileNotFoundException, UIMAException, IOException, WLFormatException,
        ClassNotFoundException;

    /**
     * Exports source documents of a given Project. This is used to copy projects from one
     * application/release to anothor.
     */
    File exportSourceDocument(SourceDocument document, Project project);
    /**
     * Export a Serialized CAS annotation document from the file system
     * @return
     */
    File exportAnnotationDocument(SourceDocument document, Project project, String user);

    /**
     * Export the associated project log for this {@link Project} while copying a project
     * @param project
     * @return
     */
    File exportProjectLog(Project project);
    /**
     * Export the associated project guideline for this {@link Project} while copying a project
     */
    File exportGuideLines(Project project);

    File exportProjectMetaInf(Project project);
    /**
     * Get an {@link AnnotationDocument} object from the database using the {@link SourceDocument}
     * and {@link User} Objects. If {@code getAnnotationDocument} fails, it will be created anew
     *
     * @param annotationDOcument
     * @return {@link AnnotationDocument}
     *
     */
    AnnotationDocument getAnnotationDocument(SourceDocument document, User user);

    /**
     * If already created, returns the CAS object either for document annotation for example in
     * {@link de.tudarmstadt.ukp.clarin.webanno.brat.ajax.controller.BratAjaxCasController#getDocument}
     * or for exporting the annotated document as TCF files as in
     * {@link de.tudarmstadt.ukp.clarin.webanno.brat.ajax.controller.BratAjaxCasController#retrieveStored}
     *
     * @param annotationDocument
     * @return
     * @throws UIMAException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    JCas getAnnotationDocumentContent(AnnotationDocument annotationDocument)
        throws UIMAException, IOException, ClassNotFoundException;

    /**
     * Get a curation document for the given {@link SourceDocument}
     */
    JCas getCurationDocumentContent(SourceDocument document)
        throws UIMAException, IOException, ClassNotFoundException;

    /**
     * Returns a role of a user, globally we will have ROLE_ADMIN and ROLE_USER
     *
     * @param aUser
     *            the {@link User} object
     * @return
     */
    List<Authority> listAuthorities(User user);

    /**
     * The Directory where the {@link SourceDocument}s and {@link AnnotationDocument}s stored
     *
     * @return
     */
    File getDir();

    /**
     * get the annotation guideline document from the file system
     *
     * @param project
     * @return
     */
    File getGuideline(Project project, String fileName);

    /**
     *
     * Get a crowdFlower Template from the WebAnno root directory
     */
    File getTemplate(String fileName) throws IOException;

    /**
     * For a given project, get the permission level(s) of the user if it is granted
     *
     * @param aUser
     *            the user, if already assigned in that project
     * @param aProject
     *            the project to be examined
     * @return list of {@link ProjectPermission#getLevel()}
     */
    ProjectPermission getPermisionLevel(User user, Project project);

    /**
     * get a permission object where a user is granted permission/permissions;
     *
     * @return
     */
    ProjectPermission getProjectPermission(User user, Project project);

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
     * Get a {@link CrowdJob} by its name
     */
    CrowdJob getCrowdJob(String name);

    /**
     * Get a project by its id.
     */

    Project getProject(long id);

    /**
     * Write this {@code content} of the guideline file in the project;
     *
     * @param project
     * @return
     * @throws IOException
     */
    void writeGuideline(Project project, File content, String fileName)
        throws IOException;

    /**
     * Get a {@link ProjectPermission }objects where a project is member of. We need to get them, for
     * example if the associated {@link Project} is deleted, the {@link ProjectPermission } objects
     * too.
     *
     * @param project
     *            The project contained in a projectPermision
     * @return the {@link ProjectPermission } list to be analysed.
     */
    List<ProjectPermission> getProjectPermisions(Project project);

    /**
     * Get meta data information about {@link SourceDocument} from the database. This method is
     * called either for {@link AnnotationDocument} object creation or
     * {@link RepositoryService#createSourceDocument(SourceDocument, User)}
     *
     * @param documentName
     *            the name of the {@link SourceDocument}
     * @param project
     *            the {@link Project} where the {@link SourceDocument} belongs
     * @return
     */
    SourceDocument getSourceDocument(String documentName, Project project);

    /**
     * Return the Master TCF file Directory path. For the first time, all available TCF layers will
     * be read and converted to CAS object. subsequent accesses will be to the annotated document
     * unless and otherwise the document is removed from the project.
     *
     * @param project
     *            The {@link Project} wherever the Source document belongs
     * @param document
     *            The {@link SourceDocument} to be examined
     * @return the Directory path of the source document
     */
    File getSourceDocumentContent(Project project, SourceDocument document);

    /**
     * Get a {@link User} object from the database based on the username.
     *
     * @param username
     *            the username to be searched in the database
     * @return a Single {@code User} object
     */
    User getUser(String username);

    /**
     * Check if the user finished annotating the {@link SourceDocument} in this {@link Project}
     */

    boolean isAnnotationFinished(SourceDocument document, Project project, User user);

    /**
     * Check if at least one annotation document is finished for this {@link SourceDocument} in the
     * project
     *
     * @param document
     * @param project
     * @return
     */
    boolean existsFinishedAnnotation(SourceDocument document, Project project);

    /**
     * List all the {@link AnnotationDocument}s, if available for a given {@link SourceDocument} in
     * the {@link Project}. Returns list of {@link AnnotationDocument}s for all {@link User}s in the
     * {@link Project} that has already annotated the {@link SourceDocument}
     *
     * @param document
     *            the {@link SourceDocument}
     * @return {@link AnnotationDocument}
     */
    List<AnnotationDocument> listAnnotationDocument(Project project, SourceDocument document);

    /**
     * List all {@link AnnotationDocument}s of this {@link SourceDocument} including those created
     * by project admins or super admins for Test purpose. This method is called when a source
     * document is deleted so that associated annotation documents also get removed.
     *
     * @param document
     * @return
     */
    List<AnnotationDocument> listAnnotationDocument(SourceDocument document);

    /**
     * List all annotation documents in the state <b>INPROGRESS</b>
     *
     * @param document
     * @return
     */
    // List<AnnotationDocument> listAnnotationDocumentInProgress(SourceDocument document);

    /**
     * List annotation guideline document already uploaded
     *
     * @param project
     * @return
     */
    List<String> listAnnotationGuidelineDocument(Project project);

    /**
     * List all Projects. If the user logged have a ROLE_ADMIN, he can see all the projects.
     * Otherwise, a user will see projects only he is member of.
     *
     * @return
     */

    List<Project> listProjects();

    /**
     * List {@link CrowdJob}s/Crowd Tasks in the system
     * @return
     */
    List<CrowdJob> listCrowdJobs();

    List<CrowdJob> listCrowdJobs(Project project);
    /**
     * List all source documents in a project. The source documents are the original TCF documents
     * imported.
     *
     * @param aProject
     *            The Project we are looking for source documents
     * @return list of source documents
     */
    List<SourceDocument> listSourceDocuments(Project aProject);

    /**
     * Lists all users in the application.
     *
     * @return list of users
     */
    List<User> listUsers();

    /**
     * Load annotation preferences such as {@link BratAnnotator#windowSize} from a property file
     *
     * @param username
     *            the username.
     * @param project
     *            the project where the user is wroking on.
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    Properties loadUserSettings(String username, Project project)
        throws FileNotFoundException, IOException;

    /**
     * Remove an annotation guideline document from the file system
     *
     * @param project
     * @param fileName
     * @throws IOException
     */
    void removeAnnotationGuideline(Project project, String fileName)
        throws IOException;

    /**
     * Remove a curation annotation document from the file system, for this {@link SourceDocument}
     *
     * @throws IOException
     */
    void removeCurationDocumentContent(SourceDocument sourceDocument)
        throws IOException;

    /**
     * remove a user permission from the project
     *
     * @param projectPermission
     *            The ProjectPermission to be removed
     * @throws IOException
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void removeProjectPermission(ProjectPermission projectPermission)
        throws IOException;

    /**
     * Remove a project. A ROLE_ADMIN or project admin can remove a project. removing a project will
     * remove associated source documents and annotation documents.
     *
     * @param project
     *            the project to be deleted
     * @param aUser
     *            The User who perform this operation
     * @throws IOException
     *             if the project to be deleted is not available in the file system
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void removeProject(Project project, User aser)
        throws IOException;
    /**
     * remove a crowd project
     * @param crowdProject
     */

    void removeCrowdJob(CrowdJob crowdProject);

    /**
     * ROLE_ADMINs or Projetc admins can remove source documents from a project. removing a a source
     * document also removes an annotation document related to that document
     *
     * @param document
     *            the source document to be deleted
     * @param user
     *            The User who perform this operation
     * @throws IOException
     *             If the source document searched for deletion is not availble
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER', 'ROLE_REMOTE')")
    void removeSourceDocument(SourceDocument document, User user)
        throws IOException;

    /**
     * Remove an annotation document, for example, when a user is removed from a project
     *
     * @param annotationDocument
     *            the {@link AnnotationDocument} to be removed
     */
    void removeAnnotationDocument(AnnotationDocument annotationDocument);

    /**
     * This method creates the source document (TCF File) to a file system. It creates a directory
     * structure under webanno.home/project/project.id/document/Document.id/source/document.name.xml
     *
     * @param text
     *            the TCF file content
     * @param document
     *            The {@link SourceDocument} object to be created
     * @param projectId
     *            The id of the {@link Project}
     * @param user
     *            The User who perform this operation
     * @throws IOException
     * @throws UIMAException
     * @throws WLFormatException
     */

    /**
     * Save annotation references, such as {@link BratAnnotator#windowSize}..., in a properties file
     * so that they are not required to configure every time they open the document.
     *
     * @param username
     *            the user name
     * @param subject
     *            differentiate the setting, either it is for {@link AnnotationPage} or
     *            {@link CurationPage}
     * @param configurationObject
     *            The Object to be saved as preference in the properties file.
     * @param project
     *            The project where the user is working on.
     * @throws FileNotFoundException
     * @throws IOException
     */
    <T> void saveUserSettings(String username, Project project, Mode subject, T configurationObject)
        throws FileNotFoundException, IOException;

    /**
     * Save some properties file associated to a project, such as meta-data.properties
     *
     * @param project
     *            The project for which the user save some properties file.
     * @throws IOException
     */
    void savePropertiesFile(Project project, InputStream is, String fileName)
        throws IOException;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER','ROLE_REMOTE')")
    void uploadSourceDocument(File file, SourceDocument document, long projectId, User user)
        throws IOException, UIMAException, WLFormatException;

    /**
     * Upload a SourceDocument, obtained as Inputstream, such as from remote API Zip folder to a
     * repository directory. This way we don't need to create the file to a temporary folder
     */

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER','ROLE_REMOTE')")
    void uploadSourceDocument(InputStream file, SourceDocument document, long projectId, User user)
        throws IOException, UIMAException, WLFormatException;

    /**
     * Returns the labels on the UI for the format of the {@link SourceDocument} to be read from a
     * properties File
     *
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    List<String> getReadableFormatsLabel()
        throws IOException, ClassNotFoundException;

    /**
     * Returns the Id of the format for the {@link SourceDocument} to be read from a properties File
     *
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */

    String getReadableFormatId(String label)
        throws IOException, ClassNotFoundException;

    /**
     * Returns formats of the {@link SourceDocument} to be read from a properties File
     *
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    Map<String, Class> getReadableFormats()
        throws IOException, ClassNotFoundException;

    /**
     * Returns the labels on the UI for the format of {@link AnnotationDocument} while exporting
     *
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    List<String> getWritableFormatsLabel()
        throws IOException, ClassNotFoundException;

    /**
     * Returns the Id of the format for {@link AnnotationDocument} while exporting
     *
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */

    String getWritableFormatId(String label)
        throws IOException, ClassNotFoundException;

    /**
     * Returns formats of {@link AnnotationDocument} while exporting
     *
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    Map<String, Class> getWritableFormats()
        throws IOException, ClassNotFoundException;

    /**
     * Get list of permissions a user have in a given project
     *
     * @return
     */
    List<ProjectPermission> listProjectPermisionLevel(User user, Project project);

    /**
     * List Users those with some {@link PermissionLevel}s in the project
     */
    List<User> listProjectUsersWithPermissions(Project project);

    /**
     * List of users with the a given {@link PermissionLevel}
     *
     * @param project
     *            The {@link Project}
     * @param permissionLevel
     *            The {@link PermissionLevel}
     */
    List<User> listProjectUsersWithPermissions(Project project, PermissionLevel permissionLevel);

    /**
     * Determine if the project is created using the remote API webanno service or not TODO: For
     * now, it checks if the project consists of META-INF folder!!
     *
     * @param project
     * @return
     */

    boolean isRemoteProject(Project project);
}
