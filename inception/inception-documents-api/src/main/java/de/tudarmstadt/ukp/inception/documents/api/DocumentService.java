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
package de.tudarmstadt.ukp.inception.documents.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.validation.ValidationError;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.ConcurentCasModificationException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import jakarta.persistence.NoResultException;

public interface DocumentService
{
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
     * @return the source document
     */
    SourceDocument createSourceDocument(SourceDocument document);

    /**
     * Check if any source document exist in the project.
     *
     * @param project
     *            the project.
     * @return if any source document exists.
     */
    boolean existsSourceDocument(Project project);

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
     * List all source documents in a project. The source documents are the original TCF documents
     * imported.
     *
     * @param aProject
     *            The Project we are looking for source documents
     * @return list of source documents
     */
    List<SourceDocument> listSourceDocuments(Project aProject);

    List<SourceDocument> listSourceDocumentsInState(Project aProject,
            SourceDocumentState... aStates);

    List<SourceDocument> listSupportedSourceDocuments(Project aProject);

    /**
     * ROLE_ADMINs or project admins can remove source documents from a project. removing a a source
     * document also removes an annotation document related to that document
     *
     * @param document
     *            the source document to be deleted
     * @throws IOException
     *             If the source document searched for deletion is not available
     */
    void removeSourceDocument(SourceDocument document) throws IOException;

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
    void uploadSourceDocument(InputStream file, SourceDocument document)
        throws IOException, UIMAException;

    /**
     * Upload a SourceDocument, obtained as {@link InputStream}, such as from remote API ZIP folder
     * to a repository directory. This way we don't need to create the file to a temporary folder
     *
     * @param file
     *            the file.
     * @param document
     *            the source document.
     * @param aFullProjectTypeSystem
     *            the project type system. If this parameter is {@code null}, then the method will
     *            try to resolve the type system itself.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws UIMAException
     *             if a conversion error occurs.
     */
    void uploadSourceDocument(InputStream file, SourceDocument document,
            TypeSystemDescription aFullProjectTypeSystem)
        throws IOException, UIMAException;

    SourceDocumentState transitionSourceDocumentState(SourceDocument aDocument,
            SourceDocumentStateTransition aTransition);

    SourceDocumentState setSourceDocumentState(SourceDocument aDocument,
            SourceDocumentState aState);

    /**
     * Sets the state of multiple source documents at once. This method does not generate
     * {@code DocumentStateChangeEvent} events. This means in particular that webhooks for
     * annotation document changes will not fire and that workload managers will not know that they
     * need to recalculate the document and project states.
     * 
     * @param aDocuments
     *            the documents to update
     * @param aState
     *            the state to update the documents to
     */
    void bulkSetSourceDocumentState(Iterable<SourceDocument> aDocuments,
            SourceDocumentState aState);

    // --------------------------------------------------------------------------------------------
    // Methods related to AnnotationDocuments
    // --------------------------------------------------------------------------------------------

    /**
     * Creates the {@link AnnotationDocument} object in the database.
     *
     * @param annotationDocument
     *            {@link AnnotationDocument} comprises of the the name of the
     *            {@link SourceDocument}, id of {@link SourceDocument}, id of the {@link Project},
     *            and id of {@link User}
     * @return the annotation document.
     */
    AnnotationDocument createOrUpdateAnnotationDocument(AnnotationDocument annotationDocument);

    /**
     * Saves the annotations from the CAS to the storage.
     *
     * @param aCas
     *            the CAS.
     * @param aAnnotationDocument
     *            the annotation document.
     * @param aFlags
     *            indicate that the CAS is written as the result of an explicit annotator user
     *            action (i.e. not as a result of a third person or implicitly by the system).
     * @throws IOException
     *             if an I/O error occurs.
     */
    void writeAnnotationCas(CAS aCas, AnnotationDocument aAnnotationDocument,
            AnnotationDocumentStateChangeFlag... aFlags)
        throws IOException;

    /**
     * Saves the annotations from the CAS to the storage.
     *
     * @param aCas
     *            the CAS.
     * @param aAnnotationDocument
     *            the annotation document.
     * @param aFlags
     *            indicate that the CAS is written as the result of an explicit annotator user
     *            action (i.e. not as a result of a third person or implicitly by the system).
     * @throws IOException
     *             if an I/O error occurs.
     */
    void writeAnnotationCasSilently(CAS aCas, AnnotationDocument aAnnotationDocument,
            AnnotationDocumentStateChangeFlag... aFlags)
        throws IOException;

    /**
     * Saves the annotations from the CAS to the storage.
     *
     * @param aCas
     *            the CAS.
     * @param aDocument
     *            the source document.
     * @param aUser
     *            The User who perform this operation
     * @param aFlags
     *            indicate that the CAS is written as the result of an explicit annotator user
     *            action (i.e. not as a result of a third person or implicitly by the system).
     * @throws IOException
     *             if an I/O error occurs.
     */
    void writeAnnotationCas(CAS aCas, SourceDocument aDocument, User aUser,
            AnnotationDocumentStateChangeFlag... aFlags)
        throws IOException;

    /**
     * Saves the annotations from the CAS to the storage.
     *
     * @param aCas
     *            the CAS.
     * @param aDocument
     *            the source document.
     * @param aSet
     *            the set the CAS belongs to.
     * @param aFlags
     *            indicate that the CAS is written as the result of an explicit annotator user
     *            action (i.e. not as a result of a third person or implicitly by the system).
     * @throws IOException
     *             if an I/O error occurs.
     */
    void writeAnnotationCas(CAS aCas, SourceDocument aDocument, AnnotationSet aSet,
            AnnotationDocumentStateChangeFlag... aFlags)
        throws IOException;

    /**
     * Resets the annotation document to its initial state by overwriting it with the initial CAS.
     *
     * @param aDocument
     *            the source document.
     * @param aUser
     *            The User who perform this operation
     * @param aFlags
     *            optional flags controlling the operation
     * @throws UIMAException
     *             if a data error occurs.
     * @throws IOException
     *             if an I/O error occurs.
     */
    void resetAnnotationCas(SourceDocument aDocument, User aUser,
            AnnotationDocumentStateChangeFlag... aFlags)
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
     * A Method that checks if there is already an annotation document created for the source
     * document
     *
     * @param document
     *            the source document.
     * @param aSet
     *            the set to which the CAS belongs.
     * @return if an annotation document metadata exists for the user.
     */
    boolean existsAnnotationDocument(SourceDocument document, AnnotationSet aSet);

    /**
     * check if the CAS for the {@link User} and {@link SourceDocument} in this {@link Project}
     * exists It is important as {@link AnnotationDocument} entry can be populated as
     * {@link AnnotationDocumentState#NEW} from the MonitoringPage before the user actually open the
     * document for annotation.
     *
     * @param sourceDocument
     *            the source document.
     * @param aSet
     *            the set the CAS belongs to.
     * @return if an annotation document file exists.
     * @throws IOException
     *             if an I/O error occurs.
     */
    boolean existsCas(SourceDocument sourceDocument, AnnotationSet aSet) throws IOException;

    boolean existsCas(AnnotationDocument annotationDocument) throws IOException;

    void exportCas(SourceDocument aDocument, AnnotationSet aSet, OutputStream aStream)
        throws IOException;

    void importCas(SourceDocument aDocument, AnnotationSet aSet, InputStream aStream)
        throws IOException;

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
     * @param aSet
     *            the set the CAS belongs to.
     * @return the annotation document.
     * @throws NoResultException
     *             if no annotation document exists for the given source/user.
     */
    AnnotationDocument getAnnotationDocument(SourceDocument aDocument, AnnotationSet aSet);

    /**
     * Gets the CAS for the given annotation document. Converts it form the source document if
     * necessary. The converted CAS is analyzed using CAS doctor and saved.
     *
     * @param annotationDocument
     *            the annotation document.
     * @return the CAS.
     * @throws IOException
     *             if there was an I/O error.
     */
    CAS readAnnotationCas(AnnotationDocument annotationDocument) throws IOException;

    CAS readAnnotationCas(AnnotationDocument aAnnDoc, CasUpgradeMode aUpgradeMode,
            CasAccessMode aMode)
        throws IOException;

    /**
     * Gets the CAS for the given annotation document. Converts it form the source document if
     * necessary. The converted CAS is analyzed using CAS doctor and saved. If the CAS already
     * existed on disk, its type system is <b>NOT</b> upgraded.
     *
     * @param annotationDocument
     *            the annotation document.
     * @param aMode
     *            CAS access mode.
     * @return the CAS.
     * @throws IOException
     *             if there was an I/O error.
     */
    CAS readAnnotationCas(AnnotationDocument annotationDocument, CasAccessMode aMode)
        throws IOException;

    CAS readAnnotationCas(AnnotationDocument aAnnotationDocument, CasUpgradeMode aUpgradeMode)
        throws IOException;

    void deleteAnnotationCas(AnnotationDocument annotationDocument) throws IOException;

    void deleteAnnotationCas(SourceDocument aSourceDocument, AnnotationSet aSet) throws IOException;

    /**
     * Gets the CAS for the given source document. Converts it form the source document if
     * necessary. The state of the source document is not changed.
     *
     * @param document
     *            the source document.
     * @param aSet
     *            the set the CAs belongs to.
     * @return the CAS.
     * @throws IOException
     *             if there was an I/O error.
     */
    CAS readAnnotationCas(SourceDocument document, AnnotationSet aSet) throws IOException;

    /**
     * Gets the CAS for the given source document. Converts it form the source document if
     * necessary. The state of the source document is not changed.
     *
     * @param aDocument
     *            the source document.
     * @param aSet
     *            the set to which the CAS belongs.
     * @param aUpgradeMode
     *            the upgrade mode.
     * @return the CAS.
     * @throws IOException
     *             if there was an I/O error.
     */
    CAS readAnnotationCas(SourceDocument aDocument, AnnotationSet aSet, CasUpgradeMode aUpgradeMode)
        throws IOException;

    /**
     * Gets the CAS for the given source document. Converts it form the source document if
     * necessary. The state of the source document is not changed.
     *
     * @param aDocument
     *            the source document.
     * @param aSet
     *            the set to which the CAS belongs.
     * @param aUpgradeMode
     *            the upgrade mode.
     * @param aAccessMode
     *            the access mode.
     * @return the CAS.
     * @throws IOException
     *             if there was an I/O error.
     */
    CAS readAnnotationCas(SourceDocument aDocument, AnnotationSet aSet, CasUpgradeMode aUpgradeMode,
            CasAccessMode aAccessMode)
        throws IOException;

    Map<String, CAS> readAllCasesSharedNoUpgrade(List<AnnotationDocument> aDocuments)
        throws IOException;

    Map<String, CAS> readAllCasesSharedNoUpgrade(SourceDocument aDoc, Collection<User> aUsers)
        throws IOException;

    Map<String, CAS> readAllCasesSharedNoUpgrade(SourceDocument aDoc, String... aUsernames)
        throws IOException;

    /**
     * Read the initial CAS for the given document. If the CAS does not exist then it is created.
     * This method does not perform an upgrade of the type@Override system in the CAS.
     * 
     * @param aDocument
     *            the source document.
     * @return the CAS.
     * @throws IOException
     *             if there was a problem loading the CAS.
     */
    CAS createOrReadInitialCas(SourceDocument aDocument) throws IOException;

    /**
     * Read the initial CAS for the given document. If the CAS does not exist then it is created.
     * 
     * @param aDocument
     *            the source document.
     * @param aUpgradeMode
     *            whether to upgrade the type system in the CAS.
     * @return the CAS.
     * @throws IOException
     *             if there was a problem loading the CAS.
     */
    CAS createOrReadInitialCas(SourceDocument aDocument, CasUpgradeMode aUpgradeMode)
        throws IOException;

    /**
     * Read the initial CAS for the given document. If the CAS does not exist then it is created.
     * 
     * @param aDocument
     *            the source document.
     * @param aUpgradeMode
     *            whether to upgrade the type system in the CAS.
     * @param aAccessMode
     *            CAS access mode.
     * @return the CAS.
     * @throws IOException
     *             if there was a problem loading the CAS.
     */
    CAS createOrReadInitialCas(SourceDocument aDocument, CasUpgradeMode aUpgradeMode,
            CasAccessMode aAccessMode)
        throws IOException;

    /**
     * Read the initial CAS for the given document. If the CAS does not exist then it is created.
     * This method is good for bulk-importing because it accepts the project type system as a
     * parameter instead of collecting it on every call.
     * 
     * @param aDocument
     *            the source document.
     * @param aUpgradeMode
     *            whether to upgrade the type system in the CAS.
     * @param aFullProjectTypeSystem
     *            the project type system.
     * @return the CAS.
     * @throws IOException
     *             if there was a problem loading the CAS.
     */
    CAS createOrReadInitialCas(SourceDocument aDocument, CasUpgradeMode aUpgradeMode,
            TypeSystemDescription aFullProjectTypeSystem)
        throws IOException;

    /**
     * Read the initial CAS for the given document. If the CAS does not exist then it is created.
     * This method is good for bulk-importing because it accepts the project type system as a
     * parameter instead of collecting it on every call.
     * 
     * @param aDocument
     *            the source document.
     * @param aUpgradeMode
     *            whether to upgrade the type system in the CAS.
     * @param aAccessMode
     *            CAS access mode.
     * @param aFullProjectTypeSystem
     *            the project type system.
     * @return the CAS.
     * @throws IOException
     *             if there was a problem loading the CAS.
     */
    CAS createOrReadInitialCas(SourceDocument aDocument, CasUpgradeMode aUpgradeMode,
            CasAccessMode aAccessMode, TypeSystemDescription aFullProjectTypeSystem)
        throws IOException;

    /**
     * @param document
     *            The {@link SourceDocument} to be examined
     * @return the file size of the initial CAS for the given source document.
     * @throws IOException
     *             accessing the file.
     */
    Optional<Long> getInitialCasFileSize(SourceDocument document) throws IOException;

    /**
     * List all the {@link AnnotationDocument annotation documents} in a given project.
     * <p>
     * Note that this method does may not return an {@link AnnotationDocument annotation document}
     * for every user in the project because they are created lazily when a user opens a document
     * for annotation the first time.
     * <p>
     * Note that this method <b>DOES NOT</b> return an {@link AnnotationDocument annotation
     * document} if the user owning the document does not actually exist in the system! It does not
     * matter whether the user is enabled or not.
     * 
     * @param aProject
     *            the project
     * @return {@link AnnotationDocument}
     * @see #createOrGetAnnotationDocument(SourceDocument, User)
     */
    List<AnnotationDocument> listAnnotationDocuments(Project aProject);

    List<AnnotationDocument> listAnnotationDocumentsInState(Project aProject,
            AnnotationDocumentState... aStates);

    List<AnnotationDocument> listAnnotationDocumentsWithStateForUser(Project aProject, User aUser,
            AnnotationDocumentState aState);

    /**
     * List all the {@link AnnotationDocument annotation documents} for a given
     * {@link SourceDocument}.
     * <p>
     * Note that this method does may not return an {@link AnnotationDocument annotation document}
     * for every user in the project because they are created lazily when a user opens a document
     * for annotation the first time.
     * <p>
     * Note that this method <b>DOES NOT</b> return an {@link AnnotationDocument annotation
     * document} if the user owning the document does not actually exist in the system! It does not
     * matter whether the user is enabled or not.
     * 
     * @param document
     *            the {@link SourceDocument}
     * @return {@link AnnotationDocument}
     * @see #createOrGetAnnotationDocument(SourceDocument, User)
     */
    List<AnnotationDocument> listAnnotationDocuments(SourceDocument document);

    /**
     * List all the {@link AnnotationDocument annotation documents} from a project for a given user.
     * <p>
     * Note that this method does may not return an {@link AnnotationDocument annotation document}
     * for every user in the project because they are created lazily when a user opens a document
     * for annotation the first time.
     * <p>
     * Note that this method returns <b>ALL</b> {@link AnnotationDocument annotation document} even
     * if the user owning the document does not actually exist in the system!
     * 
     * @param project
     *            the {@link SourceDocument}
     * @param user
     *            the {@link User}
     * @return {@link AnnotationDocument}
     * @see #createOrGetAnnotationDocument(SourceDocument, User)
     */
    List<AnnotationDocument> listAnnotationDocuments(Project project, User user);

    /**
     * List all annotation documents in a project that are already closed. used to compute overall
     * project progress
     *
     * @param project
     *            a project.
     * @return the annotation documents.
     */
    List<AnnotationDocument> listFinishedAnnotationDocuments(Project project);

    /**
     * List all annotation documents for a given source document that are already closed.
     * 
     * @param aDocument
     *            a source document.
     * @return the annotation documents.
     */
    List<AnnotationDocument> listFinishedAnnotationDocuments(SourceDocument aDocument);

    /**
     * List all annotation documents for this source document (including in active and deleted user
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
     * Check if the user finished annotating the {@link SourceDocument} in this {@link Project}
     *
     * @param document
     *            the source document.
     * @param username
     *            the user.
     * @return if the user has finished annotation.
     */
    boolean isAnnotationFinished(SourceDocument document, AnnotationSet username);

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
     * @param project
     *            the project
     * @return if at least one {@link AnnotationDocument} is finished in this project.
     */
    boolean existsFinishedAnnotation(Project project);

    /**
     * Remove an annotation document, for example, when a user is removed from a project
     *
     * @param annotationDocument
     *            the {@link AnnotationDocument} to be removed
     * @throws IOException
     *             if there was a problem deleting
     */
    void removeAnnotationDocument(AnnotationDocument annotationDocument) throws IOException;

    AnnotationDocument createOrGetAnnotationDocument(SourceDocument aDocument, User aUser);

    AnnotationDocument createOrGetAnnotationDocument(SourceDocument aDocument, AnnotationSet aSet);

    List<AnnotationDocument> createOrGetAnnotationDocuments(SourceDocument aDocument,
            Collection<User> aUsers);

    List<AnnotationDocument> createOrGetAnnotationDocuments(Collection<SourceDocument> aDocuments,
            User aUsers);

    /**
     * Returns the annotatable {@link SourceDocument source documents} from the given project for
     * the given user. Annotatable documents are those for which there is no corresponding
     * {@link AnnotationDocument annotation document} with the state
     * {@link AnnotationDocumentState#IGNORE}. Mind that annotation documents are created lazily in
     * the database, thus there may be source documents without associated annotation documents. In
     * order to provide access to the status of a document for a given user, the results is returned
     * as a map where the source document is the key and the annotation document is the value. The
     * annotation document may be {@code null}.
     * 
     * @param aProject
     *            the project for which annotatable documents should be returned.
     * @param aUser
     *            the user for whom annotatable documents should be returned.
     * @return annotatable documents.
     */
    Map<SourceDocument, AnnotationDocument> listAnnotatableDocuments(Project aProject, User aUser);

    /**
     * Returns the {@link SourceDocument source documents} with optionally associated
     * {@link AnnotationDocument annotation documents} from the given project for the given user.
     * Mind that annotation documents are created lazily in the database, thus there may be source
     * documents without associated annotation documents. In order to provide access to the status
     * of a document for a given user, the results is returned as a map where the source document is
     * the key and the annotation document is the value. The annotation document may be
     * {@code null}.
     * 
     * @param aProject
     *            the project for which documents should be returned.
     * @param aSet
     *            the set to which the CASes belong.
     * @return documents.
     */
    Map<SourceDocument, AnnotationDocument> listAllDocuments(Project aProject, AnnotationSet aSet);

    AnnotationDocumentState setAnnotationDocumentState(AnnotationDocument aDocument,
            AnnotationDocumentState aState, AnnotationDocumentStateChangeFlag... aFlags);

    /**
     * Sets the state of multiple annotation documents at once. This method does not generate
     * {@code AnnotationStateChangeEvent} events. This means in particular that webhooks for
     * annotation document changes will not fire and that workload managers will not know that they
     * need to recalculate the document and project states.
     * 
     * @param aDocuments
     *            the documents to update
     * @param aState
     *            the state to update the documents to
     */
    void bulkSetAnnotationDocumentState(Iterable<AnnotationDocument> aDocuments,
            AnnotationDocumentState aState);

    /**
     * Check if any curation documents exists in the given project.
     * 
     * @param aProject
     *            the project.
     * @return whether any curation documents exist.
     */
    boolean existsCurationDocument(Project aProject);

    /**
     * @return the time at which the CAS was last changed on disk. If the CAS does not exist yet, an
     *         empty optional is returned.
     * @param aDocument
     *            the document for which to retrieve the timestamp
     * @param aSet
     *            the set the CAS belongs to
     * @throws IOException
     *             if there was an I/O-level problem
     */
    Optional<Long> getAnnotationCasTimestamp(SourceDocument aDocument, AnnotationSet aSet)
        throws IOException;

    Optional<Long> verifyAnnotationCasTimestamp(SourceDocument aDocument, AnnotationSet aSet,
            long aExpectedTimeStamp, String aContextAction)
        throws IOException, ConcurentCasModificationException;

    boolean existsInitialCas(SourceDocument aDocument) throws IOException;

    /**
     * @return overall number of source documents
     */
    long countSourceDocuments();

    /**
     * @return overall number of annotation documents
     */
    long countAnnotationDocuments();

    void upgradeAllAnnotationDocuments(Project aProject) throws IOException;

    Map<AnnotationDocumentState, Long> getAnnotationDocumentStats(SourceDocument aDocument);

    Map<AnnotationDocumentState, Long> getAnnotationDocumentStats(SourceDocument aDocument,
            List<AnnotationDocument> aAllAnnotationDocumentsInProject, List<User> aRelevantUsers);

    SourceDocumentStateStats getSourceDocumentStats(Project aProject);

    void exportSourceDocuments(OutputStream aOs, List<SourceDocument> aSelectedDocuments)
        throws IOException;

    boolean isValidDocumentName(String aDocumentName);

    List<ValidationError> validateDocumentName(String aName);

    void renameSourceDocument(SourceDocument aDocument, String aNewName);
}
