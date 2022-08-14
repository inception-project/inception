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
package de.tudarmstadt.ukp.inception.documents;

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.NO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.DOCUMENT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.SOURCE_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.withProjectLogger;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils.addOrUpdateCasMetadata;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.IOUtils.copyLarge;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SourceDocumentStateStats;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.ConcurentCasModificationException;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterCasWrittenEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentResetEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeDocumentRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DocumentServiceAutoConfiguration#documentService}.
 * </p>
 */
public class DocumentServiceImpl
    implements DocumentService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final EntityManager entityManager;
    private final CasStorageService casStorageService;
    private final DocumentImportExportService importExportService;
    private final ProjectService projectService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RepositoryProperties repositoryProperties;

    @Autowired
    public DocumentServiceImpl(RepositoryProperties aRepositoryProperties,
            CasStorageService aCasStorageService, DocumentImportExportService aImportExportService,
            ProjectService aProjectService, ApplicationEventPublisher aApplicationEventPublisher,
            EntityManager aEntityManager)
    {
        repositoryProperties = aRepositoryProperties;
        casStorageService = aCasStorageService;
        importExportService = aImportExportService;
        projectService = aProjectService;
        applicationEventPublisher = aApplicationEventPublisher;
        entityManager = aEntityManager;

        log.info("Document repository path: {}", repositoryProperties.getPath());
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Deprecated
    @Override
    public File getDir()
    {
        return repositoryProperties.getPath();
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public File getSourceDocumentFolder(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aDocument.getProject().getId(),
                "Source document's project must have an ID");
        Validate.notNull(aDocument.getId(), "Source document must have an ID");

        return repositoryProperties.getPath().toPath() //
                .toAbsolutePath() //
                .resolve(PROJECT_FOLDER) //
                .resolve(Long.toString(aDocument.getProject().getId())) //
                .resolve(DOCUMENT_FOLDER)//
                .resolve(Long.toString(aDocument.getId())) //
                .resolve(SOURCE_FOLDER) //
                .toFile();
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public File getSourceDocumentFile(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Source document must be specified");

        return getSourceDocumentFolder(aDocument).toPath().resolve(aDocument.getName()).toFile();
    }

    @Override
    @Transactional
    public SourceDocument createSourceDocument(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Source document must be specified");

        if (isNull(aDocument.getId())) {
            entityManager.persist(aDocument);
            return aDocument;
        }

        return entityManager.merge(aDocument);
    }

    @Override
    @Transactional
    public boolean existsAnnotationDocument(SourceDocument aDocument, User aUser)
    {
        Validate.notNull(aUser, "User must be specified");

        return existsAnnotationDocument(aDocument, aUser.getUsername());
    }

    @Override
    @Transactional
    public boolean existsAnnotationDocument(SourceDocument aDocument, String aUsername)
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aUsername, "Username must be specified");

        try {
            entityManager
                    .createQuery(
                            "FROM AnnotationDocument WHERE project = :project "
                                    + " AND document = :document AND user = :user",
                            AnnotationDocument.class)
                    .setParameter("project", aDocument.getProject())
                    .setParameter("document", aDocument).setParameter("user", aUsername)
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    @Override
    @Transactional
    public AnnotationDocument createAnnotationDocument(AnnotationDocument aAnnotationDocument)
    {
        Validate.notNull(aAnnotationDocument, "Annotation document must be specified");

        if (isNull(aAnnotationDocument.getId())) {
            entityManager.persist(aAnnotationDocument);

            try (var logCtx = withProjectLogger(aAnnotationDocument.getProject())) {
                log.info("Created annotation document {} in project {}", aAnnotationDocument,
                        aAnnotationDocument.getProject());
            }

            return aAnnotationDocument;
        }
        else {
            return entityManager.merge(aAnnotationDocument);
        }
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public boolean existsCas(SourceDocument aDocument, String aUser) throws IOException
    {
        return casStorageService.existsCas(aDocument, aUser);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public boolean existsCas(AnnotationDocument aAnnotationDocument) throws IOException
    {
        Validate.notNull(aAnnotationDocument, "Annotation document must be specified");

        return existsCas(aAnnotationDocument.getDocument(), aAnnotationDocument.getUser());
    }

    @Override
    @Transactional
    public boolean existsSourceDocument(Project aProject, String aFileName)
    {
        Validate.notNull(aProject, "Project must be specified");
        Validate.notBlank(aFileName, "File name must be specified");

        String query = String.join("\n", //
                "SELECT COUNT(*)", //
                "FROM SourceDocument", //
                "WHERE project = :project AND name =:name ");

        long count = entityManager.createQuery(query, Long.class) //
                .setParameter("project", aProject) //
                .setParameter("name", aFileName) //
                .getSingleResult();

        return count > 0;
    }

    @Override
    public void exportCas(SourceDocument aDocument, String aUser, OutputStream aStream)
        throws IOException
    {
        casStorageService.exportCas(aDocument, aUser, aStream);
    }

    @Override
    public void importCas(SourceDocument aDocument, String aUser, InputStream aStream)
        throws IOException
    {
        casStorageService.importCas(aDocument, aUser, aStream);
    }

    @Override
    @Transactional
    public List<AnnotationDocument> createOrGetAnnotationDocuments(SourceDocument aDocument,
            Collection<User> aUsers)
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aUsers, "Users must be specified");

        if (aUsers.isEmpty()) {
            return emptyList();
        }

        Set<String> usersWithoutAnnotationDocument = new HashSet<>();
        aUsers.forEach(user -> usersWithoutAnnotationDocument.add(user.getUsername()));

        List<AnnotationDocument> annDocs = listAnnotationDocuments(aDocument);
        annDocs.stream().forEach(annDoc -> usersWithoutAnnotationDocument.remove(annDoc.getUser()));

        for (var user : usersWithoutAnnotationDocument) {
            var annDoc = new AnnotationDocument(user, aDocument);
            createAnnotationDocument(annDoc);
            annDocs.add(annDoc);
        }

        return annDocs;
    }

    @Override
    @Transactional
    public List<AnnotationDocument> createOrGetAnnotationDocuments(
            Collection<SourceDocument> aDocuments, User aUser)
    {
        Validate.notNull(aDocuments, "Source documents must be specified");
        Validate.notNull(aUser, "User must be specified");

        if (aDocuments.isEmpty()) {
            return emptyList();
        }

        Project project = aDocuments.iterator().next().getProject();
        Set<SourceDocument> sourceDocsWithoutAnnotationDocument = new HashSet<>();
        aDocuments.forEach(srcDoc -> sourceDocsWithoutAnnotationDocument.add(srcDoc));

        List<AnnotationDocument> annDocs = listAnnotationDocuments(project, aUser);
        annDocs.stream().forEach(
                annDoc -> sourceDocsWithoutAnnotationDocument.remove(annDoc.getDocument()));

        for (var srcDoc : sourceDocsWithoutAnnotationDocument) {
            var annDoc = new AnnotationDocument(aUser.getUsername(), srcDoc);
            createAnnotationDocument(annDoc);
            annDocs.add(annDoc);
        }

        return annDocs;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationDocument createOrGetAnnotationDocument(SourceDocument aDocument, User aUser)
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aUser, "User must be specified");

        // Check if there is an annotation document entry in the database. If there is none,
        // create one.
        AnnotationDocument annotationDocument = null;
        if (!existsAnnotationDocument(aDocument, aUser)) {
            annotationDocument = new AnnotationDocument(aUser.getUsername(), aDocument);
            createAnnotationDocument(annotationDocument);
        }
        else {
            annotationDocument = getAnnotationDocument(aDocument, aUser);
        }

        return annotationDocument;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationDocument getAnnotationDocument(SourceDocument aDocument, User aUser)
    {
        Validate.notNull(aUser, "User must be specified");

        return getAnnotationDocument(aDocument, aUser.getUsername());
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationDocument getAnnotationDocument(SourceDocument aDocument, String aUser)
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notBlank(aUser, "User must be specified");

        return entityManager
                .createQuery("FROM AnnotationDocument WHERE document = :document AND "
                        + "user =:user" + " AND project = :project", AnnotationDocument.class)
                .setParameter("document", aDocument) //
                .setParameter("user", aUser) //
                .setParameter("project", aDocument.getProject()) //
                .getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public SourceDocument getSourceDocument(Project aProject, String aDocumentName)
    {
        Validate.notNull(aProject, "Project must be specified");
        Validate.notBlank(aDocumentName, "Document name must be specified");

        return entityManager
                .createQuery("FROM SourceDocument WHERE name = :name AND project =:project",
                        SourceDocument.class)
                .setParameter("name", aDocumentName) //
                .setParameter("project", aProject) //
                .getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public SourceDocument getSourceDocument(long aProjectId, long aSourceDocId)
    {
        return entityManager
                .createQuery("FROM SourceDocument WHERE id = :docid AND project.id =:pid",
                        SourceDocument.class)
                .setParameter("docid", aSourceDocId) //
                .setParameter("pid", aProjectId) //
                .getSingleResult();
    }

    @Override
    @Transactional
    public SourceDocumentState setSourceDocumentState(SourceDocument aDocument,
            SourceDocumentState aState)
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aState, "State must be specified");

        var oldState = aDocument.getState();

        aDocument.setState(aState);

        createSourceDocument(aDocument);

        // Notify about change in document state
        if (!Objects.equals(oldState, aDocument.getState())) {
            applicationEventPublisher
                    .publishEvent(new DocumentStateChangedEvent(this, aDocument, oldState));
        }

        return oldState;
    }

    @Override
    @Transactional
    public SourceDocumentState transitionSourceDocumentState(SourceDocument aDocument,
            SourceDocumentStateTransition aTransition)
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aTransition, "Transition must be specified");

        return setSourceDocumentState(aDocument,
                SourceDocumentStateTransition.transition(aTransition));
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsFinishedAnnotation(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Source document must be specified");

        String query = join("\n", //
                "SELECT COUNT(*) ", //
                "FROM AnnotationDocument ", //
                "WHERE document = :document AND state = :state");

        long count = entityManager.createQuery(query, Long.class)
                .setParameter("document", aDocument)
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .getSingleResult();

        return count > 0;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsFinishedAnnotation(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        String query = join("\n", //
                "SELECT COUNT(*) ", //
                "FROM AnnotationDocument ", //
                "WHERE document.project = :project AND state = :state");

        long count = entityManager.createQuery(query, Long.class) //
                .setParameter("project", aProject) //
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .getSingleResult();

        return count > 0;
    }

    @Override
    public List<AnnotationDocument> listFinishedAnnotationDocuments(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        // Get all annotators in the project
        List<String> users = getAllAnnotators(aProject);

        // Bail out already. HQL doesn't seem to like queries with an empty
        // parameter right of "in"
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        String query = String.join("\n", //
                "FROM AnnotationDocument", //
                "WHERE project = :project", //
                "  AND state   = :state", //
                "  AND user in (:users)");

        return entityManager.createQuery(query, AnnotationDocument.class)
                .setParameter("project", aProject) //
                .setParameter("users", users) //
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .getResultList();
    }

    @Override
    public List<AnnotationDocument> listFinishedAnnotationDocuments(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Source cocument must be specified");

        // Get all annotators in the project
        List<String> users = getAllAnnotators(aDocument.getProject());
        // Bail out already. HQL doesn't seem to like queries with an empty parameter right of "in"
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        String query = String.join("\n", //
                "FROM AnnotationDocument", //
                "WHERE document = :document", //
                "  AND state   = :state", //
                "  AND user in (:users)");

        return entityManager.createQuery(query, AnnotationDocument.class)
                .setParameter("document", aDocument) //
                .setParameter("users", users) //
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<SourceDocument> listSourceDocuments(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        var query = "FROM SourceDocument where project =:project ORDER BY name ASC";

        return entityManager.createQuery(query, SourceDocument.class)
                .setParameter("project", aProject) //
                .getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<SourceDocument> listSourceDocumentsInState(Project aProject,
            SourceDocumentState... aStates)
    {
        Validate.notNull(aProject, "Project must be specified");
        Validate.notNull(aStates, "States must be specified");
        Validate.notEmpty(aStates, "States must not be an empty list");

        String query = String.join("\n", //
                "FROM SourceDocument", //
                "WHERE project =:project", //
                "AND state IN (:states)", //
                "ORDER BY name ASC");

        return entityManager.createQuery(query, SourceDocument.class) //
                .setParameter("states", asList(aStates)) //
                .setParameter("project", aProject) //
                .getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<AnnotationDocument> listAnnotationDocumentsInState(Project aProject,
            AnnotationDocumentState... aStates)
    {
        Validate.notNull(aProject, "Project must be specified");
        Validate.notNull(aStates, "States must be specified");
        Validate.notEmpty(aStates, "States must not be an empty list");

        if (ArrayUtils.contains(aStates, AnnotationDocumentState.NEW)) {
            throw new IllegalArgumentException(
                    "Querying for annotation documents in state NEW because if the state is NEW, "
                            + "the annotation document entity might not even have been created yet.");
        }

        String query = String.join("\n", //
                "FROM AnnotationDocument", //
                "WHERE project =:project", //
                "AND state IN (:states)", //
                "ORDER BY name ASC");

        return entityManager.createQuery(query, AnnotationDocument.class) //
                .setParameter("states", asList(aStates)) //
                .setParameter("project", aProject) //
                .getResultList();
    }

    @Override
    @Transactional
    public List<AnnotationDocument> listAnnotationDocumentsWithStateForUser(Project aProject,
            User aUser, AnnotationDocumentState aState)
    {
        Validate.notNull(aProject, "Project must be specified");
        Validate.notNull(aUser, "User must be specified");
        Validate.notNull(aState, "State must be specified");

        if (aState == AnnotationDocumentState.NEW) {
            throw new IllegalArgumentException(
                    "Querying for annotation documents in state NEW because if the state is NEW, "
                            + "the annotation document entity might not even have been created yet.");
        }

        String query = join("\n", //
                "FROM AnnotationDocument", //
                "WHERE user = :user", //
                "AND project = :project", //
                "AND state = :state", //
                "ORDER BY name ASC");

        return entityManager.createQuery(query, AnnotationDocument.class) //
                .setParameter("project", aProject) //
                .setParameter("user", aUser.getUsername()) //
                .setParameter("state", aState) //
                .getResultList();
    }

    @Override
    @Transactional
    public void removeSourceDocument(SourceDocument aDocument) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");

        // BeforeDocumentRemovedEvent is triggered first, since methods that rely
        // on it might need to have access to the associated annotation documents
        applicationEventPublisher.publishEvent(new BeforeDocumentRemovedEvent(this, aDocument));

        for (AnnotationDocument annotationDocument : listAllAnnotationDocuments(aDocument)) {
            removeAnnotationDocument(annotationDocument);
        }

        entityManager.remove(
                entityManager.contains(aDocument) ? aDocument : entityManager.merge(aDocument));

        String path = repositoryProperties.getPath().getAbsolutePath() + "/" + PROJECT_FOLDER + "/"
                + aDocument.getProject().getId() + "/" + DOCUMENT_FOLDER + "/" + aDocument.getId();

        // remove from file both source and related annotation file
        if (new File(path).exists()) {
            FileUtils.forceDelete(new File(path));
        }

        Project project = aDocument.getProject();
        try (var logCtx = withProjectLogger(project)) {
            log.info("Removed source document {} from project {}", aDocument, project);
        }
    }

    @Override
    @Transactional
    public void removeAnnotationDocument(AnnotationDocument aAnnotationDocument)
    {
        Validate.notNull(aAnnotationDocument, "Annotation document must be specified");

        entityManager.remove(aAnnotationDocument);
    }

    @Override
    @Transactional
    public void uploadSourceDocument(InputStream aIs, SourceDocument aDocument) throws IOException
    {
        uploadSourceDocument(aIs, aDocument, null);
    }

    @Override
    @Transactional
    public void uploadSourceDocument(InputStream aIs, SourceDocument aDocument,
            TypeSystemDescription aFullProjectTypeSystem)
        throws IOException
    {
        // Create the metadata record - this also assigns the ID to the document
        createSourceDocument(aDocument);

        // Import the actual content
        File targetFile = getSourceDocumentFile(aDocument);
        try (var session = CasStorageSession.openNested()) {
            FileUtils.forceMkdir(targetFile.getParentFile());

            try (var os = new FileOutputStream(targetFile)) {
                copyLarge(aIs, os);
            }

            // Check if the file has a valid format / can be converted without error
            // This requires that the document ID has already been assigned
            CAS cas = createOrReadInitialCas(aDocument, NO_CAS_UPGRADE, aFullProjectTypeSystem);

            log.trace("Sending AfterDocumentCreatedEvent for {}", aDocument);
            applicationEventPublisher
                    .publishEvent(new AfterDocumentCreatedEvent(this, aDocument, cas));

            Project project = aDocument.getProject();
            try (var logCtx = withProjectLogger(project)) {
                log.info("Imported source document {} to project {}", aDocument, project);
            }
        }
        catch (IOException e) {
            FileUtils.forceDelete(targetFile);
            removeSourceDocument(aDocument);
            throw e;
        }
        catch (Exception e) {
            FileUtils.forceDelete(targetFile);
            removeSourceDocument(aDocument);
            throw new IOException(e.getMessage(), e);
        }
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS createOrReadInitialCas(SourceDocument aDocument) throws IOException
    {
        return createOrReadInitialCas(aDocument, NO_CAS_UPGRADE);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS createOrReadInitialCas(SourceDocument aDocument, CasUpgradeMode aUpgradeMode)
        throws IOException
    {
        return createOrReadInitialCas(aDocument, aUpgradeMode, EXCLUSIVE_WRITE_ACCESS, null);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS createOrReadInitialCas(SourceDocument aDocument, CasUpgradeMode aUpgradeMode,
            CasAccessMode aAccessMode)
        throws IOException
    {
        return createOrReadInitialCas(aDocument, aUpgradeMode, aAccessMode, null);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS createOrReadInitialCas(SourceDocument aDocument, CasUpgradeMode aUpgradeMode,
            TypeSystemDescription aFullProjectTypeSystem)
        throws IOException
    {
        return createOrReadInitialCas(aDocument, aUpgradeMode, EXCLUSIVE_WRITE_ACCESS,
                aFullProjectTypeSystem);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS createOrReadInitialCas(SourceDocument aDocument, CasUpgradeMode aUpgradeMode,
            CasAccessMode aAccessMode, TypeSystemDescription aFullProjectTypeSystem)
        throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");

        log.debug("Loading initial CAS for source document {} in project {}", aDocument,
                aDocument.getProject());

        return casStorageService.readOrCreateCas(aDocument, INITIAL_CAS_PSEUDO_USER, aUpgradeMode,
                () -> {
                    // Normally, the initial CAS should be created on document import, but after
                    // adding this feature, the existing projects do not yet have initial CASes, so
                    // we create them here lazily
                    try {
                        return importExportService.importCasFromFile(
                                getSourceDocumentFile(aDocument), aDocument,
                                aFullProjectTypeSystem);
                    }
                    catch (UIMAException e) {
                        throw new IOException("Unable to create CAS: " + e.getMessage(), e);
                    }
                }, aAccessMode);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public boolean existsInitialCas(SourceDocument aDocument) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");

        return casStorageService.existsCas(aDocument, INITIAL_CAS_PSEUDO_USER);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS readAnnotationCas(SourceDocument aDocument, String aUserName) throws IOException
    {
        return readAnnotationCas(aDocument, aUserName, NO_CAS_UPGRADE, EXCLUSIVE_WRITE_ACCESS);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS readAnnotationCas(SourceDocument aDocument, String aUserName, CasAccessMode aMode)
        throws IOException
    {
        return readAnnotationCas(aDocument, aUserName, NO_CAS_UPGRADE, aMode);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS readAnnotationCas(AnnotationDocument aAnnotationDocument, CasAccessMode aMode)
        throws IOException
    {
        return readAnnotationCas(aAnnotationDocument.getDocument(), aAnnotationDocument.getUser(),
                NO_CAS_UPGRADE, aMode);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS readAnnotationCas(SourceDocument aDocument, String aUserName,
            CasUpgradeMode aUpgradeMode)
        throws IOException
    {
        return readAnnotationCas(aDocument, aUserName, aUpgradeMode, EXCLUSIVE_WRITE_ACCESS);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS readAnnotationCas(SourceDocument aDocument, String aUserName,
            CasUpgradeMode aUpgradeMode, CasAccessMode aMode)
        throws IOException
    {
        // If there is no CAS yet for the source document, create one.
        CAS cas = casStorageService.readOrCreateCas(aDocument, aUserName, aUpgradeMode,
                // Convert the source file into an annotation CAS
                () -> createOrReadInitialCas(aDocument, NO_CAS_UPGRADE, UNMANAGED_ACCESS, null),
                aMode);

        // We intentionally do not upgrade the CAS here because in general the IDs
        // must remain stable. If an upgrade is required the caller should do it
        return cas;
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS readAnnotationCas(AnnotationDocument aAnnotationDocument) throws IOException
    {
        return readAnnotationCas(aAnnotationDocument, NO_CAS_UPGRADE);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS readAnnotationCas(AnnotationDocument aAnnotationDocument,
            CasUpgradeMode aUpgradeMode)
        throws IOException
    {
        Validate.notNull(aAnnotationDocument, "Annotation document must be specified");

        SourceDocument aDocument = aAnnotationDocument.getDocument();
        String userName = aAnnotationDocument.getUser();

        return readAnnotationCas(aDocument, userName, aUpgradeMode);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public Map<String, CAS> readAllCasesSharedNoUpgrade(List<AnnotationDocument> aDocuments)
        throws IOException
    {
        if (CollectionUtils.isEmpty(aDocuments)) {
            return new HashMap<>();
        }

        SourceDocument doc = aDocuments.get(0).getDocument();
        List<String> usernames = new ArrayList<>();
        for (AnnotationDocument annDoc : aDocuments) {
            if (!doc.equals(annDoc.getDocument())) {
                throw new IllegalArgumentException(format(
                        "Expected all annotation documents to belong to the  same source document "
                                + "%s, but %s belongs to %s",
                        doc, annDoc, annDoc.getDocument()));
            }
            usernames.add(annDoc.getUser());
        }

        return readAllCasesSharedNoUpgrade(doc, usernames.toArray(new String[usernames.size()]));
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public Map<String, CAS> readAllCasesSharedNoUpgrade(SourceDocument aDoc,
            Collection<User> aUsers)
        throws IOException
    {
        return readAllCasesSharedNoUpgrade(aDoc,
                aUsers.stream().map(User::getUsername).toArray(String[]::new));
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public Map<String, CAS> readAllCasesSharedNoUpgrade(SourceDocument aDoc, String... aUsernames)
        throws IOException
    {
        Validate.notNull(aDoc, "Source document must be specified");

        if (isEmpty(aUsernames)) {
            return new HashMap<>();
        }

        Map<String, CAS> casses = new HashMap<>();
        for (String username : aUsernames) {
            CAS cas = readAnnotationCas(aDoc, username, AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
            casses.put(username, cas);
        }
        return casses;
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public Optional<Long> getAnnotationCasTimestamp(SourceDocument aDocument, String aUsername)
        throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aUsername, "Username must be specified");

        return casStorageService.getCasTimestamp(aDocument, aUsername);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public Optional<Long> verifyAnnotationCasTimestamp(SourceDocument aDocument, String aUsername,
            long aExpectedTimeStamp, String aContextAction)
        throws IOException, ConcurentCasModificationException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aUsername, "Username must be specified");

        return casStorageService.verifyCasTimestamp(aDocument, aUsername, aExpectedTimeStamp,
                aContextAction);
    }

    @Override
    @Transactional
    public void writeAnnotationCas(CAS aCas, AnnotationDocument aAnnotationDocument,
            boolean aExplicitAnnotatorUserAction)
        throws IOException
    {
        casStorageService.writeCas(aAnnotationDocument.getDocument(), aCas,
                aAnnotationDocument.getUser());

        if (aExplicitAnnotatorUserAction) {
            aAnnotationDocument.setTimestamp(new Timestamp(new Date().getTime()));
            setAnnotationDocumentState(aAnnotationDocument, AnnotationDocumentState.IN_PROGRESS,
                    EXPLICIT_ANNOTATOR_USER_ACTION);
        }

        applicationEventPublisher
                .publishEvent(new AfterCasWrittenEvent(this, aAnnotationDocument, aCas));
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public void deleteAnnotationCas(SourceDocument aSourceDocument, String aUsername)
        throws IOException
    {
        casStorageService.deleteCas(aSourceDocument, aUsername);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public void deleteAnnotationCas(AnnotationDocument aAnnotationDocument) throws IOException
    {
        casStorageService.deleteCas(aAnnotationDocument.getDocument(),
                aAnnotationDocument.getUser());
    }

    @Override
    @Transactional
    public void writeAnnotationCas(CAS aCas, SourceDocument aDocument, String aUser,
            boolean aExplicitAnnotatorUserAction)
        throws IOException
    {
        AnnotationDocument annotationDocument = getAnnotationDocument(aDocument, aUser);
        writeAnnotationCas(aCas, annotationDocument, aExplicitAnnotatorUserAction);
    }

    @Override
    @Transactional
    public void writeAnnotationCas(CAS aCas, SourceDocument aDocument, User aUser,
            boolean aExplicitAnnotatorUserAction)
        throws IOException
    {
        AnnotationDocument annotationDocument = getAnnotationDocument(aDocument, aUser);
        writeAnnotationCas(aCas, annotationDocument, aExplicitAnnotatorUserAction);
    }

    @Override
    @Transactional
    public void resetAnnotationCas(SourceDocument aDocument, User aUser,
            AnnotationDocumentStateChangeFlag... aFlags)
        throws UIMAException, IOException
    {
        AnnotationDocument adoc = getAnnotationDocument(aDocument, aUser);

        // We read the initial CAS and then use it to override the CAS for the given document/user.
        // In order to do that, we must read the initial CAS unmanaged.
        CAS cas = createOrReadInitialCas(aDocument, FORCE_CAS_UPGRADE, UNMANAGED_ACCESS);

        // Add/update the CAS metadata
        Optional<Long> timestamp = casStorageService.getCasTimestamp(aDocument,
                aUser.getUsername());
        if (timestamp.isPresent()) {
            addOrUpdateCasMetadata(cas, timestamp.get(), aDocument, aUser.getUsername());
        }

        writeAnnotationCas(cas, aDocument, aUser, false);

        adoc.setTimestamp(null);
        adoc.setAnnotatorComment(null);
        setAnnotationDocumentState(adoc, AnnotationDocumentState.NEW, aFlags);

        applicationEventPublisher.publishEvent(new AfterDocumentResetEvent(this, adoc, cas));
    }

    @Override
    @Transactional
    public boolean isAnnotationFinished(SourceDocument aDocument, User aUser)
    {
        return isAnnotationFinished(aDocument, aUser.getUsername());
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean isAnnotationFinished(SourceDocument aDocument, String aUsername)
    {
        String query = String.join("\n", //
                "SELECT COUNT(*) FROM AnnotationDocument", //
                "WHERE document = :document", //
                "  AND user     = :user", //
                "  AND state    = :state");

        return entityManager.createQuery(query, Long.class) //
                .setParameter("document", aDocument) //
                .setParameter("user", aUsername) //
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .getSingleResult() > 0;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<AnnotationDocument> listAnnotationDocuments(Project aProject)
    {
        String query = String.join("\n", //
                "SELECT doc", //
                " FROM AnnotationDocument AS doc", //
                " JOIN ProjectPermission AS perm", //
                "   ON doc.project = perm.project AND doc.user = perm.user", //
                " JOIN User as u", //
                "   ON doc.user = u.username", //
                "WHERE doc.project = :project", //
                "  AND perm.level = :level");

        return entityManager.createQuery(query, AnnotationDocument.class)
                .setParameter("project", aProject) //
                .setParameter("level", ANNOTATOR) //
                .getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<AnnotationDocument> listAnnotationDocuments(SourceDocument aDocument)
    {
        String query = String.join("\n", //
                "SELECT doc", //
                " FROM AnnotationDocument AS doc", //
                " JOIN ProjectPermission AS perm", //
                "   ON doc.project = perm.project AND doc.user = perm.user", //
                " JOIN User as u", //
                "   ON doc.user = u.username", //
                "WHERE doc.project = :project", //
                "  AND doc.document = :document", //
                "  AND perm.level = :level");

        return entityManager.createQuery(query, AnnotationDocument.class) //
                .setParameter("project", aDocument.getProject()) //
                .setParameter("document", aDocument) //
                .setParameter("level", ANNOTATOR) //
                .getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<AnnotationDocument> listAnnotationDocuments(Project aProject, User aUser)
    {
        return entityManager
                .createQuery("FROM AnnotationDocument WHERE project = :project AND user = :user",
                        AnnotationDocument.class)
                .setParameter("project", aProject) //
                .setParameter("user", aUser.getUsername()) //
                .getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<AnnotationDocument> listAllAnnotationDocuments(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Source document must be specified");

        return entityManager
                .createQuery(
                        "FROM AnnotationDocument WHERE project = :project AND document = :document",
                        AnnotationDocument.class)
                .setParameter("project", aDocument.getProject()) //
                .setParameter("document", aDocument) //
                .getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public Map<SourceDocument, AnnotationDocument> listAnnotatableDocuments(Project aProject,
            User aUser)
    {
        Map<SourceDocument, AnnotationDocument> map = listAllDocuments(aProject, aUser);

        Iterator<Entry<SourceDocument, AnnotationDocument>> i = map.entrySet().iterator();
        while (i.hasNext()) {
            Entry<SourceDocument, AnnotationDocument> e = i.next();
            if (e.getValue() != null && IGNORE == e.getValue().getState()) {
                i.remove();
            }
        }

        return map;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public Map<SourceDocument, AnnotationDocument> listAllDocuments(Project aProject, User aUser)
    {
        // First get the source documents
        var sourceDocsQuery = "FROM SourceDocument WHERE project = (:project)";
        List<SourceDocument> sourceDocuments = entityManager
                .createQuery(sourceDocsQuery, SourceDocument.class) //
                .setParameter("project", aProject) //
                .getResultList();

        // Next we get all the annotation document records. We can use these to filter out
        // documents which are IGNOREed for given users.
        var annDocsQuery = "FROM AnnotationDocument WHERE user = (:username) AND project = (:project)";
        List<AnnotationDocument> annotationDocuments = entityManager
                .createQuery(annDocsQuery, AnnotationDocument.class)
                .setParameter("username", aUser.getUsername()) //
                .setParameter("project", aProject) //
                .getResultList();

        // First we add all the source documents
        Map<SourceDocument, AnnotationDocument> map = new TreeMap<>(SourceDocument.NAME_COMPARATOR);
        for (SourceDocument doc : sourceDocuments) {
            map.put(doc, null);
        }

        // Now we link the source documents to the annotation documents and remove IGNOREed
        // documents
        for (AnnotationDocument adoc : annotationDocuments) {
            map.put(adoc.getDocument(), adoc);
        }

        return map;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public Map<AnnotationDocumentState, Long> getAnnotationDocumentStats(SourceDocument aDocument)
    {
        Set<String> users = projectService.listProjectUsersWithPermissions(aDocument.getProject())
                .stream().map(User::getUsername).collect(Collectors.toSet());

        Map<AnnotationDocumentState, AtomicLong> counts = new LinkedHashMap<>();
        for (AnnotationDocument aDoc : listAnnotationDocuments(aDocument)) {
            AtomicLong count = counts.computeIfAbsent(aDoc.getState(), _key -> new AtomicLong(0));
            count.incrementAndGet();
            users.remove(aDoc.getUser());
        }

        counts.computeIfAbsent(AnnotationDocumentState.NEW, _key -> new AtomicLong(0))
                .addAndGet(users.size());

        Map<AnnotationDocumentState, Long> finalCounts = new LinkedHashMap<>();
        for (AnnotationDocumentState state : AnnotationDocumentState.values()) {
            finalCounts.put(state, 0l);
        }
        for (Entry<AnnotationDocumentState, AtomicLong> e : counts.entrySet()) {
            finalCounts.put(e.getKey(), e.getValue().get());
        }

        return finalCounts;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public Map<AnnotationDocumentState, Long> getAnnotationDocumentStats(SourceDocument aDocument,
            List<AnnotationDocument> aRelevantAnnotationDocuments, List<User> aUsersWithPermission)
    {
        Set<String> users = aUsersWithPermission.stream() //
                .map(User::getUsername) //
                .collect(toSet());

        Map<AnnotationDocumentState, AtomicLong> counts = new LinkedHashMap<>();
        aRelevantAnnotationDocuments.stream()
                .filter(annDoc -> annDoc.getDocument().equals(aDocument)) //
                .forEach(aDoc -> {
                    AtomicLong count = counts.computeIfAbsent(aDoc.getState(),
                            _key -> new AtomicLong(0));
                    count.incrementAndGet();
                    users.remove(aDoc.getUser());
                });

        counts.computeIfAbsent(AnnotationDocumentState.NEW, _key -> new AtomicLong(0))
                .addAndGet(users.size());

        Map<AnnotationDocumentState, Long> finalCounts = new LinkedHashMap<>();
        for (AnnotationDocumentState state : AnnotationDocumentState.values()) {
            finalCounts.put(state, 0l);
        }
        for (Entry<AnnotationDocumentState, AtomicLong> e : counts.entrySet()) {
            finalCounts.put(e.getKey(), e.getValue().get());
        }

        return finalCounts;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public SourceDocumentStateStats getSourceDocumentStats(Project aProject)
    {
        // This query is better because we do not inject strings into the query string, but it
        // does not work on HSQLDB (on MySQL it seems to work).
        // See: https://github.com/webanno/webanno/issues/1011
        // String query =
        // "SELECT new " + SourceDocumentStateStats.class.getName() + "(" +
        // "COUNT(*) AS num, " +
        // "SUM(CASE WHEN state = :an THEN 1 ELSE 0 END), " +
        // "SUM(CASE WHEN (state = :aip OR state is NULL) THEN 1 ELSE 0 END), " +
        // "SUM(CASE WHEN state = :af THEN 1 ELSE 0 END), " +
        // "SUM(CASE WHEN state = :cip THEN 1 ELSE 0 END), " +
        // "SUM(CASE WHEN state = :cf THEN 1 ELSE 0 END)) " +
        // "FROM SourceDocument " +
        // "WHERE project = :project";
        //
        // SourceDocumentStateStats stats = entityManager.createQuery(
        // query, SourceDocumentStateStats.class)
        // .setParameter("project", aProject)
        // .setParameter("an", SourceDocumentState.NEW)
        // .setParameter("aip", SourceDocumentState.ANNOTATION_IN_PROGRESS)
        // .setParameter("af", SourceDocumentState.ANNOTATION_FINISHED)
        // .setParameter("cip", SourceDocumentState.CURATION_IN_PROGRESS)
        // .setParameter("cf", SourceDocumentState.CURATION_FINISHED)
        // .getSingleResult();

        // @formatter:off
        String query = 
                "SELECT new " + SourceDocumentStateStats.class.getName() + "(" +
                "COUNT(*), " +
                "SUM(CASE WHEN state = '" + NEW.getId() + "' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN (state = '" + ANNOTATION_IN_PROGRESS.getId() + 
                        "' OR state is NULL) THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN state = '" + ANNOTATION_FINISHED.getId() + 
                        "'  THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN state = '" + CURATION_IN_PROGRESS.getId() + 
                        "' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN state = '" + CURATION_FINISHED.getId() + "' THEN 1 ELSE 0 END)) " +
                "FROM SourceDocument " + 
                "WHERE project = :project";
        // @formatter:on

        return entityManager.createQuery(query, SourceDocumentStateStats.class) //
                .setParameter("project", aProject) //
                .getSingleResult();
    }

    @Override
    @Transactional
    public boolean existsCurationDocument(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        boolean curationDocumentExist = false;
        List<SourceDocument> documents = listSourceDocuments(aProject);

        for (SourceDocument sourceDocument : documents) {
            // If the curation document is finished
            if (SourceDocumentState.CURATION_FINISHED.equals(sourceDocument.getState())) {
                curationDocumentExist = true;
                break;
            }
        }
        return curationDocumentExist;
    }

    private List<String> getAllAnnotators(Project aProject)
    {
        String query = String.join("\n", //
                "SELECT DISTINCT p.user", //
                "FROM ProjectPermission p, User u", //
                "WHERE p.project = :project", //
                "  AND p.level   = :level", //
                "  AND p.user    = u.username");

        // Get all annotators in the project
        List<String> users = entityManager.createQuery(query, String.class) //
                .setParameter("project", aProject) //
                .setParameter("level", ANNOTATOR) //
                .getResultList();

        return users;
    }

    @Override
    @Transactional
    public AnnotationDocumentState setAnnotationDocumentState(AnnotationDocument aDocument,
            AnnotationDocumentState aState, AnnotationDocumentStateChangeFlag... aFlags)
    {
        AnnotationDocumentState oldState = setAnnotationDocumentStateNoEvent(aDocument, aState,
                aFlags);

        if (!Objects.equals(oldState, aDocument.getState())) {
            applicationEventPublisher
                    .publishEvent(new AnnotationStateChangeEvent(this, aDocument, oldState));
        }

        return oldState;
    }

    private AnnotationDocumentState setAnnotationDocumentStateNoEvent(AnnotationDocument aDocument,
            AnnotationDocumentState aState, AnnotationDocumentStateChangeFlag... aFlags)
    {
        AnnotationDocumentState oldState = aDocument.getState();

        aDocument.setState(aState);

        if (aState == AnnotationDocumentState.NEW) {
            // If a document is reset, the annotator state is cleared
            aDocument.setAnnotatorState(null);
        }
        else if (aState == AnnotationDocumentState.IN_PROGRESS) {
            // If a manager/curator send the document back to the annotator, the annotator state
            // is re-set to being in progress
            aDocument.setAnnotatorState(AnnotationDocumentState.IN_PROGRESS);
        }
        else if (asList(aFlags).contains(EXPLICIT_ANNOTATOR_USER_ACTION)) {
            // Otherwise, the annotator state is only update by explicit annotator actions
            aDocument.setAnnotatorState(aState);
        }

        createAnnotationDocument(aDocument);
        return oldState;
    }

    @Override
    @Transactional
    public void bulkSetAnnotationDocumentState(Iterable<AnnotationDocument> aDocuments,
            AnnotationDocumentState aState)
    {
        for (AnnotationDocument doc : aDocuments) {
            setAnnotationDocumentStateNoEvent(doc, aState);
        }
    }

    @EventListener
    @Transactional
    public void beforeProjectRemove(BeforeProjectRemovedEvent aEvent) throws IOException
    {
        Project project = aEvent.getProject();

        Validate.notNull(project, "Project must be specified");

        // Since the project is being deleted anyway, we don't bother sending around
        // BeforeDocumentRemovedEvent anymore. If we did, we would likely trigger a
        // a lot of CPU usage and DB bashing (e.g. for re-calculating the project state
        // (ProjectServiceImpl.recalculateProjectState).
        // List<SourceDocument> sourceDocuments = listSourceDocuments(project);
        // for (SourceDocument doc : sourceDocuments) {
        // applicationEventPublisher.publishEvent(new BeforeDocumentRemovedEvent(this, doc));
        // }

        // There should be a cascade-on-delete for annotation documents when the respective
        // source document is deleted, but that is not there at the moment...
        String deleteAnnotationDocumentsQuery = String.join("\n", //
                "DELETE FROM AnnotationDocument", //
                "WHERE project = :project");
        entityManager.createQuery(deleteAnnotationDocumentsQuery) //
                .setParameter("project", project) //
                .executeUpdate();

        // Delete all the source documents for the given project
        String deleteSourceDocumentsQuery = String.join("\n", //
                "DELETE FROM SourceDocument", //
                "WHERE project = :project");
        entityManager.createQuery(deleteSourceDocumentsQuery) //
                .setParameter("project", project) //
                .executeUpdate();

        // When a project is deleted, the repository folder of the project will be deleted anyway
        // by the ProjectService - we don't have to clean up here
        // // Delete all the source documents files for the given project
        // File docFolder = new File(repositoryProperties.getPath().getAbsolutePath() + "/"
        // + PROJECT_FOLDER + "/" + project.getId() + "/" + DOCUMENT_FOLDER + "/");
        // if (docFolder.exists()) {
        // FastIOUtils.delete(docFolder);
        // }

        try (var logCtx = withProjectLogger(project)) {
            log.info("Removed all documents from project {} being deleted", project);
        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public long countSourceDocuments()
    {
        String query = "SELECT COUNT(*) FROM SourceDocument";
        return entityManager.createQuery(query, Long.class).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public long countAnnotationDocuments()
    {
        String query = "SELECT COUNT(*) FROM AnnotationDocument";
        return entityManager.createQuery(query, Long.class).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public void upgradeAllAnnotationDocuments(Project aProject) throws IOException
    {
        // Perform a forced upgrade on all CASes in the project. This action affects all users
        // currently logged in and working on the project. E.g. an annotator working on a
        // document will be unable to make changes to the document anymore until the user
        // re-opens the document because the force upgrade invalidates the VIDs used in the
        // annotation editor. How exactly (if at all) the user gets information of this is
        // currently undefined.
        for (SourceDocument doc : listSourceDocuments(aProject)) {
            for (AnnotationDocument ann : listAllAnnotationDocuments(doc)) {
                try {
                    casStorageService.upgradeCas(doc, ann.getUser());
                }
                catch (FileNotFoundException e) {
                    // If there is no CAS file, we do not have to upgrade it. Ignoring.
                }
            }

            // Also upgrade the curation CAS if it exists
            try {
                casStorageService.upgradeCas(doc, CURATION_USER);
            }
            catch (FileNotFoundException e) {
                // If there is no CAS file, we do not have to upgrade it. Ignoring.
            }
        }
    }
}
