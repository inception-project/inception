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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.NO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.INITIAL_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.security.ValidationUtils.FILESYSTEM_ILLEGAL_PREFIX_CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.security.ValidationUtils.FILESYSTEM_RESERVED_CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.security.ValidationUtils.RELAXED_SHELL_SPECIAL_CHARACTERS;
import static de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils.addOrUpdateCasMetadata;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.withProjectLogger;
import static de.tudarmstadt.ukp.inception.support.text.TextUtils.containsAnyCharacterMatching;
import static de.tudarmstadt.ukp.inception.support.text.TextUtils.endsWithMatching;
import static de.tudarmstadt.ukp.inception.support.text.TextUtils.sortAndRemoveDuplicateCharacters;
import static de.tudarmstadt.ukp.inception.support.text.TextUtils.startsWithMatching;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.ConcurentCasModificationException;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument_;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentStorageService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.api.SourceDocumentStateStats;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.event.AfterCasWrittenEvent;
import de.tudarmstadt.ukp.inception.documents.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.inception.documents.event.AfterDocumentResetEvent;
import de.tudarmstadt.ukp.inception.documents.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.inception.documents.event.BeforeDocumentRemovedEvent;
import de.tudarmstadt.ukp.inception.documents.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;
import de.tudarmstadt.ukp.inception.support.text.TextUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DocumentServiceAutoConfiguration#documentService}.
 * </p>
 */
public class DocumentServiceImpl
    implements DocumentService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String MSG_DOCUMENT_NAME_TOO_LONG = "document.name.error.too-long";
    private static final String MSG_DOCUMENT_NAME_EMPTY = "document.name.error.empty";
    private static final String MSG_DOCUMENT_NAME_WHITESPACE = "document.name.error.whitespace";
    private static final String MSG_DOCUMENT_NAME_ILLEGAL = "document.name.error.illegal";
    private static final String MSG_DOCUMENT_NAME_ILLEGAL_PREFIX = "document.name.error.illegal-prefix";
    private static final String MSG_DOCUMENT_NAME_ERROR_CONTROL_CHARACTERS = "document.name.error.control-characters";
    private static final String MVAR_LIMIT = "limit";
    private static final String MVAR_DETAIL = "detail";
    private static final String MVAR_CHARS = "chars";

    private static final String DOCUMENT_NAME_ILLEGAL_PREFIX_CHARACTERS = FILESYSTEM_ILLEGAL_PREFIX_CHARACTERS;
    private static final String DOCUMENT_NAME_ILLEGAL_CHARACTERS = sortAndRemoveDuplicateCharacters(
            RELAXED_SHELL_SPECIAL_CHARACTERS, FILESYSTEM_RESERVED_CHARACTERS);

    private final EntityManager entityManager;
    private final CasStorageService casStorageService;
    private final DocumentImportExportService importExportService;
    private final ProjectService projectService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RepositoryProperties repositoryProperties;
    private final DocumentStorageService documentStorageService;

    @Autowired
    public DocumentServiceImpl(RepositoryProperties aRepositoryProperties,
            CasStorageService aCasStorageService, DocumentImportExportService aImportExportService,
            ProjectService aProjectService, ApplicationEventPublisher aApplicationEventPublisher,
            EntityManager aEntityManager, DocumentStorageService aDocumentStorageService)
    {
        repositoryProperties = aRepositoryProperties;
        casStorageService = aCasStorageService;
        importExportService = aImportExportService;
        projectService = aProjectService;
        applicationEventPublisher = aApplicationEventPublisher;
        entityManager = aEntityManager;
        documentStorageService = aDocumentStorageService;

        if (repositoryProperties != null) {
            BaseLoggers.BOOT_LOG.info("Document repository path: {}",
                    repositoryProperties.getPath());
        }
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public void exportSourceDocuments(OutputStream os, List<SourceDocument> selectedDocuments)
        throws IOException
    {
        try (var zos = new ZipOutputStream(os)) {
            for (var doc : selectedDocuments) {
                try (var dis = documentStorageService.openSourceDocumentFile(doc)) {
                    zos.putNextEntry(new ZipEntry(doc.getName()));
                    IOUtils.copyLarge(dis, zos);
                }
            }
        }
    }

    @Override
    @Transactional
    public void renameSourceDocument(SourceDocument aDocument, String aNewName)
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notBlank(aNewName, "New name must be specified");

        var nameValidationResult = validateDocumentName(aNewName);
        if (!nameValidationResult.isEmpty()) {
            throw new IllegalArgumentException(nameValidationResult.get(0).getMessage());
        }

        // Rename the source document file on disk
        try {
            documentStorageService.renameSourceDocumentFile(aDocument, aNewName);
        }
        catch (IOException e) {
            throw new IllegalStateException("Error renaming file for document " + aDocument, e);
        }

        // Update the name
        aDocument.setName(aNewName);

        // Persist the change
        createSourceDocument(aDocument);

        // Update all the annotation documents for this source document
        for (var annDoc : listAnnotationDocuments(aDocument)) {
            annDoc.setName(aNewName);
            createOrUpdateAnnotationDocument(annDoc);
        }
    }

    @Override
    @Transactional
    public SourceDocument createSourceDocument(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aDocument.getProject(),
                "Source document must be associated with a project");

        if (isNull(aDocument.getId())) {
            entityManager.persist(aDocument);
            return aDocument;
        }

        return entityManager.merge(aDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAnnotationDocument(SourceDocument aDocument, User aUser)
    {
        Validate.notNull(aUser, "User must be specified");

        return existsAnnotationDocument(aDocument, AnnotationSet.forUser(aUser));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAnnotationDocument(SourceDocument aDocument, AnnotationSet aSet)
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aSet, "Set must be specified");

        try {
            entityManager
                    .createQuery(
                            "FROM AnnotationDocument WHERE project = :project "
                                    + " AND document = :document AND user = :user",
                            AnnotationDocument.class)
                    .setParameter("project", aDocument.getProject()) //
                    .setParameter("document", aDocument) //
                    .setParameter("user", aSet.id()) //
                    .getSingleResult();
            return true;
        }
        catch (NoResultException ex) {
            return false;
        }
    }

    @Override
    @Transactional
    public AnnotationDocument createOrUpdateAnnotationDocument(
            AnnotationDocument aAnnotationDocument)
    {
        Validate.notNull(aAnnotationDocument, "Annotation document must be specified");

        if (isNull(aAnnotationDocument.getId())) {
            entityManager.persist(aAnnotationDocument);

            try (var logCtx = withProjectLogger(aAnnotationDocument.getProject())) {
                LOG.info("Created annotation document {} in project {}", aAnnotationDocument,
                        aAnnotationDocument.getProject());
            }

            return aAnnotationDocument;
        }

        return entityManager.merge(aAnnotationDocument);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public boolean existsCas(SourceDocument aDocument, AnnotationSet aSet) throws IOException
    {
        return casStorageService.existsCas(aDocument, aSet);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public boolean existsCas(AnnotationDocument aAnnotationDocument) throws IOException
    {
        Validate.notNull(aAnnotationDocument, "Annotation document must be specified");

        return existsCas(aAnnotationDocument.getDocument(),
                AnnotationSet.forUser(aAnnotationDocument.getUser()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsSourceDocument(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var doc = query.from(SourceDocument.class);

        query.select(cb.count(doc)) //
                .where(cb.equal(doc.get(SourceDocument_.project), aProject));

        return entityManager.createQuery(query).getSingleResult() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsSourceDocument(Project aProject, String aFileName)
    {
        Validate.notNull(aProject, "Project must be specified");
        Validate.notBlank(aFileName, "File name must be specified");

        var query = String.join("\n", //
                "SELECT COUNT(*)", //
                "FROM SourceDocument", //
                "WHERE project = :project AND name =:name ");

        var count = entityManager.createQuery(query, Long.class) //
                .setParameter("project", aProject) //
                .setParameter("name", aFileName) //
                .getSingleResult();

        return count > 0;
    }

    @Override
    public void exportCas(SourceDocument aDocument, AnnotationSet aSet, OutputStream aStream)
        throws IOException
    {
        casStorageService.exportCas(aDocument, aSet, aStream);
    }

    @Override
    public void importCas(SourceDocument aDocument, AnnotationSet aSet, InputStream aStream)
        throws IOException
    {
        casStorageService.importCas(aDocument, aSet, aStream);
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

        var usersWithoutAnnotationDocument = new HashSet<String>();
        aUsers.forEach(user -> usersWithoutAnnotationDocument.add(user.getUsername()));

        var annDocs = listAnnotationDocuments(aDocument);
        annDocs.stream().forEach(annDoc -> usersWithoutAnnotationDocument.remove(annDoc.getUser()));

        for (var user : usersWithoutAnnotationDocument) {
            var annDoc = new AnnotationDocument(user, aDocument);
            createOrUpdateAnnotationDocument(annDoc);
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

        var project = aDocuments.iterator().next().getProject();
        var sourceDocsWithoutAnnotationDocument = new HashSet<SourceDocument>();
        aDocuments.forEach(srcDoc -> sourceDocsWithoutAnnotationDocument.add(srcDoc));

        var annDocs = listAnnotationDocuments(project, aUser);
        annDocs.stream().forEach(
                annDoc -> sourceDocsWithoutAnnotationDocument.remove(annDoc.getDocument()));

        for (var srcDoc : sourceDocsWithoutAnnotationDocument) {
            var annDoc = new AnnotationDocument(aUser.getUsername(), srcDoc);
            createOrUpdateAnnotationDocument(annDoc);
            annDocs.add(annDoc);
        }

        return annDocs;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationDocument createOrGetAnnotationDocument(SourceDocument aDocument, User aUser)
    {
        return createOrGetAnnotationDocument(aDocument, AnnotationSet.forUser(aUser));
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationDocument createOrGetAnnotationDocument(SourceDocument aDocument,
            AnnotationSet aSet)
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aSet, "Set must be specified");

        // Check if there is an annotation document entry in the database. If there is none,
        // create one.
        if (!existsAnnotationDocument(aDocument, aSet)) {
            var annotationDocument = new AnnotationDocument(aSet.id(), aDocument);
            createOrUpdateAnnotationDocument(annotationDocument);
            return annotationDocument;
        }

        return getAnnotationDocument(aDocument, aSet);
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationDocument getAnnotationDocument(SourceDocument aDocument, User aUser)
    {
        Validate.notNull(aUser, "User must be specified");

        return getAnnotationDocument(aDocument, AnnotationSet.forUser(aUser.getUsername()));
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public AnnotationDocument getAnnotationDocument(SourceDocument aDocument, AnnotationSet aSet)
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aSet, "Set must be specified");

        return entityManager
                .createQuery("FROM AnnotationDocument WHERE document = :document AND "
                        + "user =:user" + " AND project = :project", AnnotationDocument.class)
                .setParameter("document", aDocument) //
                .setParameter("user", aSet.id()) //
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
        var oldState = setSourceDocumentStateNoEvent(aDocument, aState);

        // Notify about change in document state
        if (!Objects.equals(oldState, aDocument.getState())) {
            applicationEventPublisher
                    .publishEvent(new DocumentStateChangedEvent(this, aDocument, oldState));
        }

        return oldState;
    }

    private SourceDocumentState setSourceDocumentStateNoEvent(SourceDocument aDocument,
            SourceDocumentState aState)
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aState, "State must be specified");

        var oldState = aDocument.getState();

        aDocument.setState(aState);

        createSourceDocument(aDocument);
        return oldState;
    }

    @Override
    @Transactional
    public void bulkSetSourceDocumentState(Iterable<SourceDocument> aDocuments,
            SourceDocumentState aState)
    {
        for (var doc : aDocuments) {
            setSourceDocumentStateNoEvent(doc, aState);
        }
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
    @Transactional(noRollbackFor = NoResultException.class, readOnly = true)
    public boolean existsFinishedAnnotation(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Source document must be specified");

        var query = join("\n", //
                "SELECT COUNT(*) ", //
                "FROM AnnotationDocument ", //
                "WHERE document = :document AND state = :state");

        var count = entityManager.createQuery(query, Long.class) //
                .setParameter("document", aDocument) //
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .getSingleResult();

        return count > 0;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class, readOnly = true)
    public boolean existsFinishedAnnotation(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        var query = join("\n", //
                "SELECT COUNT(*) ", //
                "FROM AnnotationDocument ", //
                "WHERE document.project = :project AND state = :state");

        var count = entityManager.createQuery(query, Long.class) //
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
        var users = getAllAnnotators(aProject);

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
        Validate.notNull(aDocument, "Source document must be specified");

        // Get all annotators in the project
        var users = getAllAnnotators(aDocument.getProject());
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
    public List<SourceDocument> listSupportedSourceDocuments(Project aProject)
    {
        var allDocuments = listSourceDocuments(aProject);

        if (allDocuments.isEmpty()) {
            return allDocuments;
        }

        // Filter out documents that do not have a supported format
        return allDocuments.stream()
                .filter(doc -> importExportService.getFormatById(doc.getFormat()).isPresent())
                .toList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<SourceDocument> listSourceDocumentsInState(Project aProject,
            SourceDocumentState... aStates)
    {
        Validate.notNull(aProject, "Project must be specified");
        Validate.notNull(aStates, "States must be specified");
        Validate.notEmpty(aStates, "States must not be an empty list");

        var query = String.join("\n", //
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
                    "Querying for annotation documents in state NEW is not allowed because if the state is NEW, "
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

        for (var annotationDocument : listAllAnnotationDocuments(aDocument)) {
            removeAnnotationDocument(annotationDocument);
        }

        entityManager.remove(
                entityManager.contains(aDocument) ? aDocument : entityManager.merge(aDocument));
        documentStorageService.removeSourceDocumentFile(aDocument);

        var project = aDocument.getProject();
        try (var logCtx = withProjectLogger(project)) {
            LOG.info("Removed source document {} from project {}", aDocument, project);
        }
    }

    @Override
    @Transactional
    public void removeAnnotationDocument(AnnotationDocument aAnnotationDocument) throws IOException
    {
        Validate.notNull(aAnnotationDocument, "Annotation document must be specified");
        casStorageService.deleteCas(aAnnotationDocument.getDocument(),
                AnnotationSet.forUser(aAnnotationDocument.getUser()));
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
        var nameValidationResult = validateDocumentName(aDocument.getName());
        if (!nameValidationResult.isEmpty()) {
            throw new IllegalArgumentException(nameValidationResult.get(0).getMessage());
        }

        // Create the metadata record - this also assigns the ID to the document
        createSourceDocument(aDocument);

        // Import the actual content
        try (var session = CasStorageSession.openNested()) {
            documentStorageService.writeSourceDocumentFile(aDocument, aIs);

            // Check if the file has a valid format / can be converted without error
            // This requires that the document ID has already been assigned
            var cas = createOrReadInitialCas(aDocument, NO_CAS_UPGRADE, aFullProjectTypeSystem);

            LOG.trace("Sending AfterDocumentCreatedEvent for {}", aDocument);
            applicationEventPublisher
                    .publishEvent(new AfterDocumentCreatedEvent(this, aDocument, cas));

            Project project = aDocument.getProject();
            try (var logCtx = withProjectLogger(project)) {
                LOG.info("Imported source document {} to project {}", aDocument, project);
            }
        }
        catch (IOException e) {
            removeSourceDocument(aDocument);
            throw e;
        }
        catch (Exception e) {
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

        LOG.debug("Loading initial CAS for source document {} in project {}", aDocument,
                aDocument.getProject());

        return casStorageService.readOrCreateCas(aDocument, INITIAL_SET, aUpgradeMode, () -> {
            // Normally, the initial CAS should be created on document import, but after
            // adding this feature, the existing projects do not yet have initial CASes, so
            // we create them here lazily
            try {
                return importExportService.importCasFromFileNoChecks(
                        documentStorageService.getSourceDocumentFile(aDocument), aDocument,
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

        return casStorageService.existsCas(aDocument, INITIAL_SET);
    }

    @Override
    public Optional<Long> getInitialCasFileSize(SourceDocument aDocument) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");

        return casStorageService.getCasFileSize(aDocument, INITIAL_SET);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS readAnnotationCas(SourceDocument aDocument, AnnotationSet aSet) throws IOException
    {
        return readAnnotationCas(aDocument, aSet, NO_CAS_UPGRADE, EXCLUSIVE_WRITE_ACCESS);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS readAnnotationCas(AnnotationDocument aAnnotationDocument, CasAccessMode aMode)
        throws IOException
    {
        return readAnnotationCas(aAnnotationDocument.getDocument(),
                AnnotationSet.forUser(aAnnotationDocument.getUser()), NO_CAS_UPGRADE, aMode);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS readAnnotationCas(SourceDocument aDocument, AnnotationSet aSet,
            CasUpgradeMode aUpgradeMode)
        throws IOException
    {
        return readAnnotationCas(aDocument, aSet, aUpgradeMode, EXCLUSIVE_WRITE_ACCESS);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS readAnnotationCas(AnnotationDocument aAnnDoc, CasUpgradeMode aUpgradeMode,
            CasAccessMode aMode)
        throws IOException
    {
        return readAnnotationCas(aAnnDoc.getDocument(), AnnotationSet.forUser(aAnnDoc.getUser()),
                aUpgradeMode, aMode);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public CAS readAnnotationCas(SourceDocument aDocument, AnnotationSet aSet,
            CasUpgradeMode aUpgradeMode, CasAccessMode aMode)
        throws IOException
    {
        // If there is no CAS yet for the source document, create one.
        var cas = casStorageService.readOrCreateCas(aDocument, aSet, aUpgradeMode,
                // Convert the source file into an annotation CAS
                () -> {
                    var initialCas = createOrReadInitialCas(aDocument, NO_CAS_UPGRADE,
                            UNMANAGED_ACCESS, null);

                    var maybeFormatSupport = importExportService
                            .getFormatById(aDocument.getFormat());
                    maybeFormatSupport
                            .ifPresent(fmt -> fmt.prepareAnnotationCas(initialCas, aDocument));

                    return initialCas;
                }, aMode);

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

        var doc = aAnnotationDocument.getDocument();
        var set = AnnotationSet.forUser(aAnnotationDocument.getUser());

        return readAnnotationCas(doc, set, aUpgradeMode);
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

        var doc = aDocuments.get(0).getDocument();
        var usernames = new ArrayList<String>();
        for (var annDoc : aDocuments) {
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

        var casses = new HashMap<String, CAS>();
        for (var username : aUsernames) {
            var cas = readAnnotationCas(aDoc, AnnotationSet.forUser(username), AUTO_CAS_UPGRADE,
                    SHARED_READ_ONLY_ACCESS);
            casses.put(username, cas);
        }
        return casses;
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public Optional<Long> getAnnotationCasTimestamp(SourceDocument aDocument, AnnotationSet aSet)
        throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aSet, "Set must be specified");

        return casStorageService.getCasTimestamp(aDocument, aSet);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public Optional<Long> verifyAnnotationCasTimestamp(SourceDocument aDocument, AnnotationSet aSet,
            long aExpectedTimeStamp, String aContextAction)
        throws IOException, ConcurentCasModificationException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        Validate.notNull(aSet, "Set must be specified");

        return casStorageService.verifyCasTimestamp(aDocument, aSet, aExpectedTimeStamp,
                aContextAction);
    }

    @Override
    @Transactional
    public void writeAnnotationCas(CAS aCas, AnnotationDocument aAnnotationDocument,
            AnnotationDocumentStateChangeFlag... aFlags)
        throws IOException
    {
        writeAnnotationCasSilently(aCas, aAnnotationDocument, aFlags);

        applicationEventPublisher
                .publishEvent(new AfterCasWrittenEvent(this, aAnnotationDocument, aCas));
    }

    @Override
    @Transactional
    public void writeAnnotationCasSilently(CAS aCas, AnnotationDocument aAnnotationDocument,
            AnnotationDocumentStateChangeFlag... aFlags)
        throws IOException
    {
        casStorageService.writeCas(aAnnotationDocument.getDocument(), aCas,
                AnnotationSet.forUser(aAnnotationDocument.getUser()));

        if (asList(aFlags).contains(EXPLICIT_ANNOTATOR_USER_ACTION)) {
            aAnnotationDocument.setTimestamp(new Timestamp(new Date().getTime()));
            setAnnotationDocumentState(aAnnotationDocument, AnnotationDocumentState.IN_PROGRESS,
                    aFlags);
        }
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public void deleteAnnotationCas(SourceDocument aSourceDocument, AnnotationSet aSet)
        throws IOException
    {
        casStorageService.deleteCas(aSourceDocument, aSet);
    }

    // NO TRANSACTION REQUIRED - This does not do any should not do a database access, so we do not
    // need to be in a transaction here. Avoiding the transaction speeds up the call.
    @Override
    public void deleteAnnotationCas(AnnotationDocument aAnnotationDocument) throws IOException
    {
        casStorageService.deleteCas(aAnnotationDocument.getDocument(),
                AnnotationSet.forUser(aAnnotationDocument.getUser()));
    }

    @Override
    @Transactional
    public void writeAnnotationCas(CAS aCas, SourceDocument aDocument, AnnotationSet aUser,
            AnnotationDocumentStateChangeFlag... aFlags)
        throws IOException
    {
        var annotationDocument = getAnnotationDocument(aDocument, aUser);
        writeAnnotationCas(aCas, annotationDocument, aFlags);
    }

    @Override
    @Transactional
    public void writeAnnotationCas(CAS aCas, SourceDocument aDocument, User aUser,
            AnnotationDocumentStateChangeFlag... aFlags)
        throws IOException
    {
        var annotationDocument = getAnnotationDocument(aDocument, aUser);
        writeAnnotationCas(aCas, annotationDocument, aFlags);
    }

    @Override
    @Transactional
    public void resetAnnotationCas(SourceDocument aDocument, User aUser,
            AnnotationDocumentStateChangeFlag... aFlags)
        throws UIMAException, IOException
    {
        var adoc = getAnnotationDocument(aDocument, aUser);

        // We read the initial CAS and then use it to override the CAS for the given document/user.
        // In order to do that, we must read the initial CAS unmanaged.
        var cas = createOrReadInitialCas(aDocument, FORCE_CAS_UPGRADE, UNMANAGED_ACCESS);

        // Add/update the CAS metadata
        var timestamp = casStorageService.getCasTimestamp(aDocument,
                AnnotationSet.forUser(aUser.getUsername()));
        if (timestamp.isPresent()) {
            addOrUpdateCasMetadata(cas, timestamp.get(), aDocument, aUser.getUsername());
        }

        writeAnnotationCas(cas, aDocument, aUser);

        adoc.setTimestamp(null);
        adoc.setAnnotatorComment(null);
        setAnnotationDocumentState(adoc, AnnotationDocumentState.NEW, aFlags);

        applicationEventPublisher.publishEvent(new AfterDocumentResetEvent(this, adoc, cas));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAnnotationFinished(SourceDocument aDocument, User aUser)
    {
        return isAnnotationFinished(aDocument, AnnotationSet.forUser(aUser));
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class, readOnly = true)
    public boolean isAnnotationFinished(SourceDocument aDocument, AnnotationSet aSet)
    {
        var query = String.join("\n", //
                "SELECT COUNT(*) FROM AnnotationDocument", //
                "WHERE document = :document", //
                "  AND user     = :user", //
                "  AND state    = :state");

        return entityManager.createQuery(query, Long.class) //
                .setParameter("document", aDocument) //
                .setParameter("user", aSet.id()) //
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .getSingleResult() > 0;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public List<AnnotationDocument> listAnnotationDocuments(Project aProject)
    {
        var query = String.join("\n", //
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
        var query = String.join("\n", //
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
        var map = listAllDocuments(aProject, AnnotationSet.forUser(aUser));

        var i = map.entrySet().iterator();
        while (i.hasNext()) {
            var e = i.next();
            if (e.getValue() != null && IGNORE == e.getValue().getState()) {
                i.remove();
            }
        }

        return map;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public Map<SourceDocument, AnnotationDocument> listAllDocuments(Project aProject,
            AnnotationSet aSet)
    {
        // First get the source documents
        var sourceDocsQuery = "FROM SourceDocument WHERE project = (:project)";
        var sourceDocuments = entityManager.createQuery(sourceDocsQuery, SourceDocument.class) //
                .setParameter("project", aProject) //
                .getResultList();

        // Next we get all the annotation document records. We can use these to filter out
        // documents which are IGNOREed for given users.
        var annDocsQuery = "FROM AnnotationDocument WHERE user = (:username) AND project = (:project)";
        var annotationDocuments = entityManager.createQuery(annDocsQuery, AnnotationDocument.class) //
                .setParameter("username", aSet.id()) //
                .setParameter("project", aProject) //
                .getResultList();

        // First we add all the source documents
        var map = new TreeMap<SourceDocument, AnnotationDocument>(SourceDocument.NAME_COMPARATOR);
        for (var doc : sourceDocuments) {
            map.put(doc, null);
        }

        // Now we link the source documents to the annotation documents and remove IGNOREed
        // documents
        for (var adoc : annotationDocuments) {
            map.put(adoc.getDocument(), adoc);
        }

        return map;
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public Map<AnnotationDocumentState, Long> getAnnotationDocumentStats(SourceDocument aDocument)
    {
        var users = projectService.listUsersWithRoleInProject(aDocument.getProject(), ANNOTATOR)
                .stream().toList();

        var annDocs = listAnnotationDocuments(aDocument);

        return getAnnotationDocumentStats(aDocument, annDocs, users);
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public Map<AnnotationDocumentState, Long> getAnnotationDocumentStats(SourceDocument aDocument,
            List<AnnotationDocument> aRelevantAnnotationDocuments, List<User> aRelevantUsers)
    {
        var users = aRelevantUsers.stream() //
                .map(User::getUsername) //
                .collect(toSet());

        // For each AnnotationDocumentState count the users having that for the given document
        var counts = new LinkedHashMap<AnnotationDocumentState, AtomicLong>();
        aRelevantAnnotationDocuments.stream()
                .filter(annDoc -> annDoc.getDocument().equals(aDocument)) //
                .filter(anndoc -> users.contains(anndoc.getUser())) //
                .forEach(aDoc -> {
                    var count = counts.computeIfAbsent(aDoc.getState(), _key -> new AtomicLong(0));
                    count.incrementAndGet();
                    users.remove(aDoc.getUser());
                });

        // If we do not have an AnnotationDocumentState for users, we count them as NEW
        counts.computeIfAbsent(AnnotationDocumentState.NEW, _key -> new AtomicLong(0))
                .addAndGet(users.size());

        var finalCounts = new LinkedHashMap<AnnotationDocumentState, Long>();
        for (var state : AnnotationDocumentState.values()) {
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
        var query = 
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
    @Transactional(readOnly = true)
    public boolean existsCurationDocument(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        var criteriaBuilder = entityManager.getCriteriaBuilder();
        var query = criteriaBuilder.createQuery(Long.class);
        var root = query.from(SourceDocument.class);

        query.select(criteriaBuilder.count(root))
                .where(criteriaBuilder.and(
                        criteriaBuilder.equal(root.get(SourceDocument_.project), aProject),
                        criteriaBuilder.equal(root.get(SourceDocument_.state), CURATION_FINISHED)));

        return entityManager.createQuery(query).getSingleResult() > 0;
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
        var oldState = setAnnotationDocumentStateNoEvent(aDocument, aState, aFlags);

        if (!Objects.equals(oldState, aDocument.getState())) {
            applicationEventPublisher
                    .publishEvent(new AnnotationStateChangeEvent(this, aDocument, oldState));
        }

        return oldState;
    }

    private AnnotationDocumentState setAnnotationDocumentStateNoEvent(AnnotationDocument aDocument,
            AnnotationDocumentState aState, AnnotationDocumentStateChangeFlag... aFlags)
    {
        var oldState = aDocument.getState();

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

        createOrUpdateAnnotationDocument(aDocument);
        return oldState;
    }

    @Override
    @Transactional
    public void bulkSetAnnotationDocumentState(Iterable<AnnotationDocument> aDocuments,
            AnnotationDocumentState aState)
    {
        for (var doc : aDocuments) {
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
            LOG.info("Removed all documents from project {} being deleted", project);
        }
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class, readOnly = true)
    public long countSourceDocuments()
    {
        var query = "SELECT COUNT(*) FROM SourceDocument";
        return entityManager.createQuery(query, Long.class).getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class, readOnly = true)
    public long countAnnotationDocuments()
    {
        var query = "SELECT COUNT(*) FROM AnnotationDocument";
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
                    casStorageService.upgradeCas(doc, AnnotationSet.forUser(ann.getUser()));
                }
                catch (FileNotFoundException e) {
                    // If there is no CAS file, we do not have to upgrade it. Ignoring.
                }
            }

            // Also upgrade the curation CAS if it exists
            try {
                casStorageService.upgradeCas(doc, CURATION_SET);
            }
            catch (FileNotFoundException e) {
                // If there is no CAS file, we do not have to upgrade it. Ignoring.
            }
        }
    }

    @Override
    public boolean isValidDocumentName(String aDocumentName)
    {
        return validateDocumentName(aDocumentName).isEmpty();
    }

    @Override
    public List<ValidationError> validateDocumentName(String aName)
    {
        var errors = new ArrayList<ValidationError>();

        if (isBlank(aName)) {
            errors.add(new ValidationError("Document name cannot be empty.") //
                    .addKey(MSG_DOCUMENT_NAME_EMPTY));
            return errors;
        }

        if (startsWithMatching(aName, Character::isWhitespace)
                || endsWithMatching(aName, Character::isWhitespace)) {
            errors.add(new ValidationError("Document name cannot start or end with whitespace.") //
                    .addKey(MSG_DOCUMENT_NAME_WHITESPACE));
            return errors;
        }

        if (startsWithMatching(aName, c -> contains(DOCUMENT_NAME_ILLEGAL_PREFIX_CHARACTERS, c))) {
            errors.add(
                    new ValidationError("Document name cannot start with any of these characters: "
                            + DOCUMENT_NAME_ILLEGAL_PREFIX_CHARACTERS) //
                                    .addKey(MSG_DOCUMENT_NAME_ILLEGAL_PREFIX).setVariable(
                                            MVAR_CHARS, DOCUMENT_NAME_ILLEGAL_PREFIX_CHARACTERS));
            return errors;
        }

        if (containsAnyCharacterMatching(aName, TextUtils::isControlCharacter)) {
            errors.add(new ValidationError("Username cannot contain any control characters") //
                    .addKey(MSG_DOCUMENT_NAME_ERROR_CONTROL_CHARACTERS));
            return errors;
        }

        if (containsAny(aName, DOCUMENT_NAME_ILLEGAL_CHARACTERS)) {
            errors.add(new ValidationError("Document name contains illegal characters. It must not "
                    + "contain any of the following characters [" + DOCUMENT_NAME_ILLEGAL_CHARACTERS
                    + "]") //
                            .addKey(MSG_DOCUMENT_NAME_ILLEGAL)
                            .setVariable(MVAR_DETAIL, DOCUMENT_NAME_ILLEGAL_CHARACTERS));
            return errors;
        }

        var len = aName.length();
        int maximumUiNameLength = 200;
        if (len > maximumUiNameLength) {
            errors.add(new ValidationError("Document name is too long. It can at most consist of "
                    + maximumUiNameLength + " characters.") //
                            .addKey(MSG_DOCUMENT_NAME_TOO_LONG)
                            .setVariable(MVAR_LIMIT, maximumUiNameLength));
        }

        return errors;
    }
}
