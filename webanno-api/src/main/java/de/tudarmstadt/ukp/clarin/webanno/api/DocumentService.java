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
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;

import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;


public interface DocumentService
{
    String SERVICE_NAME = "documentService";
    
    /**
     * The Directory where the {@link SourceDocument}s and {@link AnnotationDocument}s stored
     *
     * @return the directory.
     */
    File getDir();

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
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER','ROLE_REMOTE')")
    void createSourceDocument(SourceDocument document);

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
     * Get meta data information about {@link SourceDocument} from the database. This method is
     * called either for {@link AnnotationDocument} object creation or
     * {@link #createSourceDocument(SourceDocument)}
     *
     * @param project
     *            the {@link Project} where the {@link SourceDocument} belongs
     * @param documentName
     *            the name of the {@link SourceDocument}
     * @return the source document.
     */
    SourceDocument getSourceDocument(Project project, String documentName);

    /**
     * Get meta data information about {@link SourceDocument} from the database.
     * 
     * @param projectId
     *            the id for the {@link Project}
     * @param documentId
     *            the id for the {@link SourceDocument}
     * @return the source document
     */
    SourceDocument getSourceDocument(long projectId, long documentId);

    /**
     * Return the Master TCF file Directory path. For the first time, all available TCF layers will
     * be read and converted to CAS object. subsequent accesses will be to the annotated document
     * unless and otherwise the document is removed from the project.
     *
     * @param document
     *            The {@link SourceDocument} to be examined
     * @return the Directory path of the source document
     */
    File getSourceDocumentFile(SourceDocument document);

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
     * ROLE_ADMINs or project admins can remove source documents from a project. removing a a source
     * document also removes an annotation document related to that document
     *
     * @param document
     *            the source document to be deleted
     * @throws IOException
     *             If the source document searched for deletion is not available
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER', 'ROLE_REMOTE')")
    void removeSourceDocument(SourceDocument document)
        throws IOException;

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER','ROLE_REMOTE')")
    void uploadSourceDocument(File file, SourceDocument document)
        throws IOException, UIMAException;

    /**
     * Upload a SourceDocument, obtained as Inputstream, such as from remote API Zip folder to a
     * repository directory. This way we don't need to create the file to a temporary folder
     *
     * @param file
     *            the file.
     * @param document
     *            the source document.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws UIMAException
     *             if a conversion error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER','ROLE_REMOTE')")
    void uploadSourceDocument(InputStream file, SourceDocument document)
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

    SourceDocumentState setSourceDocumentState(SourceDocument aDocument,
            SourceDocumentState aState);

    SourceDocumentState transitionSourceDocumentState(SourceDocument aDocument,
            SourceDocumentStateTransition aTransition);

    // --------------------------------------------------------------------------------------------
    // Methods related to AnnotationDocuments
    // --------------------------------------------------------------------------------------------

    /**
     * creates the {@link AnnotationDocument } object in the database.
     *
     * @param annotationDocument
     *            {@link AnnotationDocument} comprises of the the name of the {@link SourceDocument}
     *            , id of {@link SourceDocument}, id of the {@link Project}, and id of {@link User}
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createAnnotationDocument(AnnotationDocument annotationDocument);

    /**
     * Creates an annotation document. The {@link AnnotationDocument} is stored in the
     * webanno.home/project/Project.id/document/document.id/annotation/username.ser. annotated
     * documents are stored per project, user and document
     *
     * @param jCas
     *            the JCas.
     * @param annotationDocument
     *            the annotation document.
     * @throws IOException
     *             if an I/O error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void writeAnnotationCas(JCas jCas, AnnotationDocument annotationDocument,
            boolean aUpdateTimestamp)
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
    void writeAnnotationCas(JCas jCas, SourceDocument document, User user, boolean aUpdateTimestamp)
        throws IOException;

    /**
     * Resets the annotation document to its initial state by overwriting it with the initial
     * CAS.
     *
     * @param aDocument
     *            the source document.
     * @param aUser
     *            The User who perform this operation
     * @throws UIMAException
     *             if a data error occurs.
     * @throws IOException
     *             if an I/O error occurs.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void resetAnnotationCas(SourceDocument aDocument, User aUser)
            throws UIMAException, IOException;

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
    boolean existsCas(SourceDocument sourceDocument, String username)
        throws IOException;

    boolean existsAnnotationCas(AnnotationDocument annotationDocument)
            throws IOException;

    /**
     * Export a Serialized CAS annotation document from the file system
     *
     * @param document
     *            the source document.
     * @param user
     *            the username.
     * @return the serialized CAS file.
     */
    File getCasFile(SourceDocument document, String user) throws IOException;

    /**
     * Get the annotation document.
     *
     * @param aDocument
     *            the source document.
     * @param aUser
     *            the user.
     * @return the annotation document.
     * @throws NoResultException
     *             if no annotation document exists for the given source/user.
     */
    AnnotationDocument getAnnotationDocument(SourceDocument aDocument, User aUser);

    /**
     * Get the annotation document.
     *
     * @param aDocument
     *            the source document.
     * @param aUser
     *            the user.
     * @return the annotation document.
     * @throws NoResultException
     *             if no annotation document exists for the given source/user.
     */
    AnnotationDocument getAnnotationDocument(SourceDocument aDocument, String aUser);

    /**
     * Gets the CAS for the given annotation document. Converts it form the source document if
     * necessary. The converted CAS is analyzed using CAS doctor and saved.
     *
     * @param annotationDocument
     *            the annotation document.
     * @return the JCas.
     * @throws IOException
     *             if there was an I/O error.
     */
    JCas readAnnotationCas(AnnotationDocument annotationDocument)
        throws IOException;

    void deleteAnnotationCas(AnnotationDocument annotationDocument)
        throws IOException;
    
    /**
     * Gets the CAS for the given annotation document. Converts it form the source document if
     * necessary. If necessary, no annotation document exists, one is created. The source document
     * is set into state {@link SourceDocumentState#ANNOTATION_IN_PROGRESS}.
     *
     * @param document
     *            the source document.
     * @param user
     *            the user.
     * @return the JCas.
     * @throws IOException
     *             if there was an I/O error.
     * @deprecated use {@link #createOrGetAnnotationDocument(SourceDocument, User)} and
     *             {@link #readAnnotationCas(AnnotationDocument)} instead and manually set source
     *             document status manually if desired.
     */
    @Deprecated
    JCas readAnnotationCas(SourceDocument document, User user)
        throws IOException;

    /**
     * Read the initial CAS for the given document. If the CAS does not exist then it is created. 
     * 
     * @param aDocument
     *            the source document.
     * @return the CAS.
     * @throws IOException
     *             if there was a problem loading the CAS.
     */
    JCas createOrReadInitialCas(SourceDocument aDocument)
        throws IOException;

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

    List<AnnotationDocument> listAnnotationDocuments(Project project, User user);

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
     * If at least one {@link AnnotationDocument} is finished in this project
     */
    boolean existsFinishedAnnotation(Project project);

    /**
     * Remove an annotation document, for example, when a user is removed from a project
     *
     * @param annotationDocument
     *            the {@link AnnotationDocument} to be removed
     */
    void removeAnnotationDocument(AnnotationDocument annotationDocument);

    AnnotationDocument createOrGetAnnotationDocument(SourceDocument aDocument, User aUser);
    
    /**
     * Returns the annotatable {@link SourceDocument source documents} from the given project for
     * the given user. Annotatable documents are those for which there is no corresponding
     * {@link AnnotationDocument annotation document} with the state
     * {@link AnnotationDocumentState#IGNORE}. Mind that annotation documents are created lazily
     * in the database, thus there may be source documents without associated annotation documents.
     * In order to provide access to the status of a document for a given user, the results is
     * returned as a map where the source document is the key and the annotation document is the
     * value. The annotation document may be {@code null}.
     * 
     * @param aProject
     *            the project for which annotatable documents should be returned.
     * @param aUser
     *            the user for whom annotatable documents should be returned.
     * @return annotatable documents.
     */
    Map<SourceDocument, AnnotationDocument> listAnnotatableDocuments(Project aProject, User aUser);
    
    AnnotationDocumentState setAnnotationDocumentState(AnnotationDocument aDocument,
            AnnotationDocumentState aState);

    AnnotationDocumentState transitionAnnotationDocumentState(AnnotationDocument aDocument,
            AnnotationDocumentStateTransition aTransition);

    /**
     * Check if any curation documents exists in the given project.
     * 
     * @param aProject
     *            the project.
     * @return whether any curation documents exist.
     */
    boolean existsCurationDocument(Project aProject);
}
