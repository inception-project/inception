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
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.AutomationStatus;
import de.tudarmstadt.ukp.clarin.webanno.model.CrowdJob;
import de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

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
    // --------------------------------------------------------------------------------------------
    // Methods related to users
    // --------------------------------------------------------------------------------------------

    /**
     * If the user is in the database (exclude some historical users that have annotations in the
     * system)
     * 
     * @param username
     *            the username.
     *
     * @return if the user exists.
     */
    boolean existsUser(String username);

    /**
     * Get a {@link User} object from the database based on the username.
     *
     * @param username
     *            the username to be searched in the database
     * @return a Single {@code User} object
     */
    User getUser(String username);

    /**
     * Lists all users in the application.
     *
     * @return list of users
     */
    List<User> listUsers();

    // --------------------------------------------------------------------------------------------
    // Methods related to permissions
    // --------------------------------------------------------------------------------------------

    /**
     * Returns a role of a user, globally we will have ROLE_ADMIN and ROLE_USER
     *
     * @param user
     *            the {@link User} object
     * @return the roles.
     */
    List<Authority> listAuthorities(User user);

    /**
     * creates a project permission, adding permission level for the user in the given project
     *
     * @param permission the permission
     * @throws IOException if an I/O error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER', 'ROLE_REMOTE')")
    void createProjectPermission(ProjectPermission permission)
        throws IOException;

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
     * Get list of permissions a user have in a given project
     * 
     * @param user
     *            the user.
     * @param project
     *            the project.
     *
     * @return the permissions.
     */
    List<ProjectPermission> listProjectPermisionLevel(User user, Project project);

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
     * @throws IOException
     *             if an I/O error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void removeProjectPermission(ProjectPermission projectPermission)
        throws IOException;

    // --------------------------------------------------------------------------------------------
    // Methods related to SourceDocuments
    // --------------------------------------------------------------------------------------------

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
     *             if an I/O error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER','ROLE_REMOTE')")
    void createSourceDocument(SourceDocument document, User user)
        throws IOException;

    /**
     * Check if a Source document with this same name exist in the project. The caller method then
     * can decide to override or throw an exception/message to the client
     * 
     * @param project
     *            the project.
     * @param fileName
     *            the source document name.
     * @return if the source document exists.
     */
    boolean existsSourceDocument(Project project, String fileName);

    /**
     * Exports source documents of a given Project. This is used to copy projects from one
     * application/release to another.
     * 
     * @param document
     *            the source document.
     * @return the source document file.
     */
    File exportSourceDocument(SourceDocument document);

    /**
     * Get meta data information about {@link SourceDocument} from the database. This method is
     * called either for {@link AnnotationDocument} object creation or
     * {@link RepositoryService#createSourceDocument(SourceDocument, User)}
     *
     * @param project
     *            the {@link Project} where the {@link SourceDocument} belongs
     * @param documentName
     *            the name of the {@link SourceDocument}
     * @return the source document.
     */
    SourceDocument getSourceDocument(Project project, String documentName);

    /**
     * Return the Master TCF file Directory path. For the first time, all available TCF layers will
     * be read and converted to CAS object. subsequent accesses will be to the annotated document
     * unless and otherwise the document is removed from the project.
     *
     * @param document
     *            The {@link SourceDocument} to be examined
     * @return the Directory path of the source document
     */
    File getSourceDocumentContent(SourceDocument document);

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
     * Return list of training documents that are in the TOKEN TAB FEAURE formats
     *
     * @param aProject
     *            the project.
     * @return the source documents.
     */
    List<SourceDocument> listTabSepDocuments(Project aProject);

    /**
     * ROLE_ADMINs or project admins can remove source documents from a project. removing a a source
     * document also removes an annotation document related to that document
     *
     * @param document
     *            the source document to be deleted
     * @param user
     *            The User who perform this operation
     * @throws IOException
     *             If the source document searched for deletion is not available
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER', 'ROLE_REMOTE')")
    void removeSourceDocument(SourceDocument document, User user)
        throws IOException;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER','ROLE_REMOTE')")
    void uploadSourceDocument(File file, SourceDocument document, User user)
        throws IOException, UIMAException;

    /**
     * Upload a SourceDocument, obtained as Inputstream, such as from remote API Zip folder to a
     * repository directory. This way we don't need to create the file to a temporary folder
     * 
     * @param file
     *            the file.
     * @param document
     *            the source document.
     * @param user
     *            he user.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws UIMAException
     *             if a conversion error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER','ROLE_REMOTE')")
    void uploadSourceDocument(InputStream file, SourceDocument document, User user)
        throws IOException, UIMAException;

    /**
     * Get the directory of this {@link SourceDocument} usually to read the content of the document
     * 
     * @param aDocument
     *            the source document.
     * @return the source document folder.
     * @throws IOException
     *             if an I/O error occurs.
     */
    File getDocumentFolder(SourceDocument aDocument)
        throws IOException;

    // --------------------------------------------------------------------------------------------
    // Methods related to AnnotationDocuments
    // --------------------------------------------------------------------------------------------

    /**
     * creates the {@link AnnotationDocument } object in the database.
     *
     * @param annotationDocument
     *            {@link AnnotationDocument} comprises of the the name of the {@link SourceDocument}
     *            , id of {@link SourceDocument}, id of the {@link Project}, and id of {@link User}
     * @throws IOException
     *             if an I/O error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createAnnotationDocument(AnnotationDocument annotationDocument)
        throws IOException;

    /**
     * Creates an annotation document. The {@link AnnotationDocument} is stored in the
     * webanno.home/project/Project.id/document/document.id/annotation/username.ser. annotated
     * documents are stored per project, user and document
     * 
     * @param jCas
     *            the JCas.
     * @param document
     *            the source document.
     * @param user
     *            The User who perform this operation
     * @throws IOException
     *             if an I/O error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createAnnotationDocumentContent(JCas jCas, SourceDocument document, User user)
        throws IOException;

    /**
     * A Method that checks if there is already an annotation document created for the source
     * document
     * 
     * @param document
     *            the source document.
     * @param user
     *            the user.
     * @return if an annotation document metadata exists for the user.
     */
    boolean existsAnnotationDocument(SourceDocument document, User user);

    /**
     * A method to check if there exist a correction document already. Base correction document
     * should be the same for all users
     * 
     * @param document
     *            the source document.
     * @return if a correction document exists.
     */
    boolean existsCorrectionDocument(SourceDocument document);

    /**
     * check if the JCAS for the {@link User} and {@link SourceDocument} in this {@link Project}
     * exists It is important as {@link AnnotationDocument} entry can be populated as
     * {@link AnnotationDocumentState#NEW} from the MonitoringPage before the user actually open the
     * document for annotation.
     * 
     * @param sourceDocument
     *            the source document.
     * @param username
     *            the username.
     * @return if an annotation document file exists.
     * @throws IOException
     *             if an I/O error occurs.
     */
    boolean existsAnnotationDocumentContent(SourceDocument sourceDocument, String username)
        throws IOException;

    /**
     * check if there is an already automated document. This is important as automated document
     * should appear the same among users
     * 
     * @param sourceDocument
     *            the source document.
     * @return if an automation document exists.
     */
    boolean existsAutomatedDocument(SourceDocument sourceDocument);

    /**
     * Exports an {@link AnnotationDocument } CAS Object as TCF/TXT/XMI... file formats.
     *
     * @param document
     *            The {@link SourceDocument} where we get the id which hosts both the source
     *            Document and the annotated document
     * @param user
     *            the {@link User} who annotates the document.
     * @param writer
     *            the DKPro Core writer.
     * @param fileName
     *            the file name.
     * @param mode
     *            the mode.
     * @return a temporary file.
     * @throws UIMAException
     *             if there was a conversion error.
     * @throws IOException
     *             if there was an I/O error.
     * @throws ClassNotFoundException
     *             if the DKPro Core writer could not be found.
     */
    @SuppressWarnings("rawtypes")
    File exportAnnotationDocument(SourceDocument document, String user, Class writer,
            String fileName, Mode mode)
        throws UIMAException, IOException, ClassNotFoundException;

    @SuppressWarnings("rawtypes")
    File exportAnnotationDocument(SourceDocument document, String user, Class writer,
            String fileName, Mode mode, boolean stripExtension)
        throws UIMAException, IOException, ClassNotFoundException;

    /**
     * Export a Serialized CAS annotation document from the file system
     * 
     * @param document
     *            the source document.
     * @param user
     *            the username.
     * @return the serialized CAS file.
     */
    File exportserializedCas(SourceDocument document, String user);

    /**
     * Get an {@link AnnotationDocument} object from the database using the {@link SourceDocument}
     * and {@link User} Objects. If {@code getAnnotationDocument} fails, it will be created anew
     * 
     * @param document
     *            the source document.
     * @param user
     *            the user.
     * @return the annotation document.
     */
    AnnotationDocument getAnnotationDocument(SourceDocument document, User user);

    /**
     * If already created, returns the CAS object either for document annotation for example in
     * {@code de.tudarmstadt.ukp.clarin.webanno.brat.ajax.controller.BratAjaxCasController#getDocument}
     * or for exporting the annotated document as TCF files as in
     * {@code de.tudarmstadt.ukp.clarin.webanno.brat.ajax.controller.BratAjaxCasController#retrieveStored}
     *
     * @param annotationDocument
     *            the annotation document.
     * @return the JCas.
     * @throws UIMAException
     *             if there was a conversion error.
     * @throws IOException
     *             if there was an I/O error.
     * @throws ClassNotFoundException
     *             if the DKPro Core reader/writer could not be loaded.
     */
    JCas getAnnotationDocumentContent(AnnotationDocument annotationDocument)
        throws UIMAException, IOException, ClassNotFoundException;

    /**
     * List all the {@link AnnotationDocument}s, if available for a given {@link SourceDocument} in
     * the {@link Project}. Returns list of {@link AnnotationDocument}s for all {@link User}s in the
     * {@link Project} that has already annotated the {@link SourceDocument}
     *
     * @param document
     *            the {@link SourceDocument}
     * @return {@link AnnotationDocument}
     */
    List<AnnotationDocument> listAnnotationDocuments(SourceDocument document);

    /**
     * Number of expected annotation documents in this project (numUser X document - Ignored)
     * 
     * @param project
     *            the project.
     * @return the number of annotation documents.
     */
    int numberOfExpectedAnnotationDocuments(Project project);

    /**
     * List all annotation Documents in a project that are already closed. used to compute overall
     * project progress
     * 
     * @param project
     *            the project.
     * @return the annotation documents.
     */
    List<AnnotationDocument> listFinishedAnnotationDocuments(Project project);

    /**
     * List all annotation documents for this source document (including in active and delted user
     * annotation and those created by project admins or super admins for Test purpose. This method
     * is called when a source document (or Project) is deleted so that associated annotation
     * documents also get removed.
     * 
     * @param document
     *            the source document.
     * @return the annotation documents.
     */
    List<AnnotationDocument> listAllAnnotationDocuments(SourceDocument document);

    /**
     * Check if the user finished annotating the {@link SourceDocument} in this {@link Project}
     * 
     * @param document
     *            the source document.
     * @param user
     *            the user.
     * @return if the user has finished annotation.
     */
    boolean isAnnotationFinished(SourceDocument document, User user);

    /**
     * Check if at least one annotation document is finished for this {@link SourceDocument} in the
     * project
     *
     * @param document
     *            the source document.
     * @return if any finished annotation exists.
     */
    boolean existsFinishedAnnotation(SourceDocument document);

    /**
     * Remove an annotation document, for example, when a user is removed from a project
     *
     * @param annotationDocument
     *            the {@link AnnotationDocument} to be removed
     */
    void removeAnnotationDocument(AnnotationDocument annotationDocument);

    // --------------------------------------------------------------------------------------------
    // Methods related to correction
    // --------------------------------------------------------------------------------------------

    /**
     * Create an annotation document under a special user named "CORRECTION_USER"
     * 
     * @param jCas
     *            the JCas.
     * @param document
     *            the source document.
     * @param user
     *            the user.
     * @throws IOException
     *             if an I/O error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createCorrectionDocumentContent(JCas jCas, SourceDocument document, User user)
        throws IOException;

    JCas getCorrectionDocumentContent(SourceDocument document)
        throws UIMAException, IOException, ClassNotFoundException;

    // --------------------------------------------------------------------------------------------
    // Methods related to curation
    // --------------------------------------------------------------------------------------------

    /**
     * Create a curation annotation document under a special user named as "CURATION_USER"
     * 
     * @param jCas
     *            the JCas.
     * @param document
     *            the source document.
     * @param user
     *            the user.
     * @throws IOException
     *             if an I/O error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createCurationDocumentContent(JCas jCas, SourceDocument document, User user)
        throws IOException;

    /**
     * Get a curation document for the given {@link SourceDocument}
     * 
     * @param document
     *            the source document.
     * @return the curation JCas.
     * @throws UIMAException
     *             if a conversion error occurs.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws ClassNotFoundException
     *             if the DKPro Core reader/writer cannot be loaded.
     */
    JCas getCurationDocumentContent(SourceDocument document)
        throws UIMAException, IOException, ClassNotFoundException;

    /**
     * Remove a curation annotation document from the file system, for this {@link SourceDocument}
     * 
     * @param sourceDocument
     *            the source document.
     * @param username
     *            the username.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void removeCurationDocumentContent(SourceDocument sourceDocument, String username)
        throws IOException;

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
     * @param user
     *            The User who perform this operation
     * @throws IOException
     *             If the specified webanno.home directory is not available no write permission
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_REMOTE')")
    void createProject(Project project, User user)
        throws IOException;

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
     * Export the associated project log for this {@link Project} while copying a project
     *
     * @param project
     *            the project.
     * @return the log file.
     */
    File exportProjectLog(Project project);

    File exportProjectMetaInf(Project project);

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
     * Determine if the project is created using the remote API webanno service or not TODO: For
     * now, it checks if the project consists of META-INF folder!!
     *
     * @param project
     *            the project.
     * @return if it was created using the remote API.
     */
    boolean isRemoteProject(Project project);

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
     * @param user
     *            The User who perform this operation
     * @throws IOException
     *             if the project to be deleted is not available in the file system
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void removeProject(Project project, User user)
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
     * @param username
     *            the username.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void createGuideline(Project project, File content, String fileName, String username)
        throws IOException;

    void createTemplate(Project project, File content, String fileName, String username)
        throws IOException;

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
    File exportGuidelines(Project project);

    /**
     * List annotation guideline document already uploaded
     *
     * @param project
     *            the project.
     * @return the filenames.
     */
    List<String> listGuidelines(Project project);

    /**
     * List MIRA template files
     * 
     * @param project
     *            the project.
     * @return the templates.
     */
    List<String> listTemplates(Project project);

    /**
     * Remove an annotation guideline document from the file system
     *
     * @param project
     *            the project.
     * @param fileName
     *            the filename.
     * @param username
     *            the username.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void removeGuideline(Project project, String fileName, String username)
        throws IOException;

    /**
     * Remove an MIRA template
     * 
     * @param project
     *            the project.
     * @param fileName
     *            the filename.
     * @param username
     *            the username.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void removeTemplate(Project project, String fileName, String username)
        throws IOException;

    // --------------------------------------------------------------------------------------------
    // Methods related to CrowdJobs
    // --------------------------------------------------------------------------------------------

    /**
     * Create a crowd Project which contains some source document. A crowd project contains source
     * documents from {@link Project}(s), a {@link SourceDocument} belongs at most to one
     * {@link CrowdJob}.
     * 
     * @param crowdProject
     *            the job.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void createCrowdJob(CrowdJob crowdProject)
        throws IOException;

    /**
     * Check if a crowd job already exist or not with its name
     * 
     * @param name
     *            the name.
     * @return if the job exists.
     */
    boolean existsCrowdJob(String name);

    /**
     * Get a {@link CrowdJob} by its name in a {@link Project}
     * 
     * @param name
     *            the name.
     * @param project
     *            the project.
     * @return the job.
     */
    CrowdJob getCrowdJob(String name, Project project);

    /**
     * Get a crowdFlower Template from the WebAnno root directory
     * 
     * @param fileName
     *            the name.
     * @return the template.
     * @throws IOException
     *             if an I/O error occurs.
     */
    File getTemplate(String fileName)
        throws IOException;

    /**
     * List {@link CrowdJob}s/Crowd Tasks in the system
     * 
     * @return the jobs.
     */
    List<CrowdJob> listCrowdJobs();

    List<CrowdJob> listCrowdJobs(Project project);

    /**
     * remove a crowd project
     *
     * @param crowdProject
     */
    void removeCrowdJob(CrowdJob crowdProject);

    // --------------------------------------------------------------------------------------------
    // Methods related to import/export data formats
    // --------------------------------------------------------------------------------------------

    /**
     * Returns the labels on the UI for the format of the {@link SourceDocument} to be read from a
     * properties File
     *
     * @throws IOException
     *             if an I/O error occurs.
     * @throws ClassNotFoundException
     *             if a DKPro Core reader/writer cannot be loaded.
     */
    List<String> getReadableFormatLabels()
        throws IOException, ClassNotFoundException;

    /**
     * Returns the Id of the format for the {@link SourceDocument} to be read from a properties File
     * 
     * @param label
     *            the label.
     *
     * @return the ID.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws ClassNotFoundException
     *             if a DKPro Core reader/writer cannot be loaded.
     */
    String getReadableFormatId(String label)
        throws IOException, ClassNotFoundException;

    /**
     * Returns formats of the {@link SourceDocument} to be read from a properties File
     *
     * @return the formats.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws ClassNotFoundException
     *             if a DKPro Core reader/writer cannot be loaded.
     */
    @SuppressWarnings("rawtypes")
    Map<String, Class> getReadableFormats()
        throws IOException, ClassNotFoundException;

    /**
     * Returns the labels on the UI for the format of {@link AnnotationDocument} while exporting
     *
     * @return the labels.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws ClassNotFoundException
     *             if a DKPro Core reader/writer cannot be loaded.
     */
    List<String> getWritableFormatLabels()
        throws IOException, ClassNotFoundException;

    /**
     * Returns the Id of the format for {@link AnnotationDocument} while exporting
     * 
     * @param label
     *            the label.
     * @return the ID.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws ClassNotFoundException
     *             if a DKPro Core reader/writer cannot be loaded.
     */
    String getWritableFormatId(String label)
        throws IOException, ClassNotFoundException;

    /**
     * Returns formats of {@link AnnotationDocument} while exporting
     *
     * @return the formats.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws ClassNotFoundException
     *             if a DKPro Core reader/writer cannot be loaded.
     */
    @SuppressWarnings("rawtypes")
    Map<String, Class> getWritableFormats()
        throws IOException, ClassNotFoundException;

    // --------------------------------------------------------------------------------------------
    // Methods related to user settings
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
    // Methods related to Help file contents
    // --------------------------------------------------------------------------------------------

    /**
     * Load contents that will be displayed as a popup window for help from a property file
     * 
     * @return the properties.
     * @throws IOException
     *             if an I/O error occurs.
     */
    Properties loadHelpContents()
        throws IOException;

    <T> void saveHelpContents(T configurationObject)
        throws IOException;

    // --------------------------------------------------------------------------------------------
    // Methods related to anything else
    // --------------------------------------------------------------------------------------------

    /**
     * The Directory where the {@link SourceDocument}s and {@link AnnotationDocument}s stored
     *
     * @return the directory.
     */
    File getDir();

    /**
     * Upgrade JCAS
     *
     * @param aDocument
     *            the source document.
     * @param aMode
     *            the mode.
     * @param username
     *            the username.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void upgradeCasAndSave(SourceDocument aDocument, Mode aMode, String username)
        throws IOException;

    /**
     * Get the CAS object for the document in the project created by the the User. If this is the
     * first time the user is accessing the annotation document, it will be read from the source
     * document, and converted to CAS
     * 
     * @param document
     *            the source document.
     * @param project
     *            the project.
     * @param user
     *            the user.
     * @return the JCas.
     * @throws UIMAException
     *             if a conversion error occurs.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws ClassNotFoundException
     *             if a DKPro Core reader/writer cannot be loaded.
     */
    JCas readJCas(SourceDocument document, Project project, User user)
        throws UIMAException, IOException, ClassNotFoundException;

    /**
     * Save the modified CAS in the file system as Serialized CAS
     * 
     * @param mode
     *            the mode.
     * @param document
     *            the source document.
     * @param user
     *            the user.
     * @param jCas
     *            the JCas.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void updateJCas(Mode mode, SourceDocument document, User user, JCas jCas)
        throws IOException;

    JCas createJCas(SourceDocument document, AnnotationDocument annoDoc, Project project, User user)
        throws IOException;

    /**
     * Get CAS object for the first time, from the source document using the provided reader
     * 
     * @param file
     *            the file.
     * @param reader
     *            the DKPro Core reader.
     * @param aDocument
     *            the source document.
     * @return the JCas.
     * @throws UIMAException
     *             if a conversion error occurs.
     * @throws IOException
     *             if an I/O error occurs.
     */
    @SuppressWarnings("rawtypes")
    JCas getJCasFromFile(File file, Class reader, SourceDocument aDocument)
        throws UIMAException, IOException;

    void updateTimeStamp(SourceDocument document, User user, Mode mode)
        throws IOException;

    /**
     * Get the name of the database driver in use.
     * 
     * @return the driver name.
     */
    String getDatabaseDriverName();

    /**
     * For 1.0.0 release, the settings.properties file contains a key that is indicates if
     * crowdsourcing is enabled or not (0 disabled, 1 enabled)
     *
     * @return if crowdsourcing is enabled.
     */
    int isCrowdSourceEnabled();

    /**
     * Get the a model for a given automation layer or other layers used as feature for the
     * automation layer. model will be generated per layer
     * 
     * @param feature
     *            the feature.
     * @param otherLayer
     *            if this is a primary or secondary feature.
     * @param document
     *            the source document.
     * @return the model.
     */
    File getMiraModel(AnnotationFeature feature, boolean otherLayer, SourceDocument document);

    /**
     * Get the MIRA director where models, templates and training data will be stored
     * 
     * @param feature
     *            the feature.
     * @return the directory.
     */
    File getMiraDir(AnnotationFeature feature);

    /**
     * Create a MIRA template and save the configurations in a database
     * 
     * @param template
     *            the template.
     */
    void createTemplate(MiraTemplate template);

    /**
     * Get the MIRA template (and hence the template configuration) for a given layer
     * 
     * @param feature
     *            the feature.
     * @return the template.
     */
    MiraTemplate getMiraTemplate(AnnotationFeature feature);

    /**
     * Check if a MIRA template is already created for this layer
     * 
     * @param feature
     *            the feature.
     * @return if a template exists.
     */
    boolean existsMiraTemplate(AnnotationFeature feature);

    /**
     * List all the MIRA templates created, hence know which layer do have a training conf already!
     * 
     * @param project the project.
     * @return the templates.
     */
    List<MiraTemplate> listMiraTemplates(Project project);

    void removeMiraTemplate(MiraTemplate template);

    void removeAutomationStatus(AutomationStatus status);

    void createAutomationStatus(AutomationStatus status);

    boolean existsAutomationStatus(MiraTemplate template);

    AutomationStatus getAutomationStatus(MiraTemplate template);

    void upgrade(CAS aCurCas, Project aProject)
        throws UIMAException, IOException;
}
