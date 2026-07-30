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
package de.tudarmstadt.ukp.inception.curation.service;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static java.lang.String.join;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.ConcurentCasModificationException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument_;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.config.CurationDocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.config.CurationProperties;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationDocumentServiceAutoConfiguration#curationDocumentService}.
 * </p>
 */
public class CurationDocumentServiceImpl
    implements CurationDocumentService
{
    private static final Logger LOG = LoggerFactory.getLogger(CurationDocumentServiceImpl.class);

    private final CurationProperties curationProperties;
    private final EntityManager entityManager;
    private final CasStorageService casStorageService;
    private final AnnotationSchemaService annotationService;
    private final DocumentService documentService;

    @Autowired
    public CurationDocumentServiceImpl(CasStorageService aCasStorageService,
            AnnotationSchemaService aAnnotationService, CurationProperties aCurationProperties,
            EntityManager aEntityManager, DocumentService aDocumentService)
    {
        casStorageService = aCasStorageService;
        annotationService = aAnnotationService;
        entityManager = aEntityManager;
        curationProperties = aCurationProperties;
        documentService = aDocumentService;
    }

    @Override
    @Transactional
    public void writeCurationCas(CAS aCas, SourceDocument aDocument, boolean aUpdateTimestamp)
        throws IOException
    {
        casStorageService.writeCas(aDocument, aCas, CURATION_SET);
        if (aUpdateTimestamp) {
            aDocument.setTimestamp(new Timestamp(new Date().getTime()));
            entityManager.merge(aDocument);
        }
    }

    @Override
    public CAS readCurationCas(SourceDocument aDocument) throws IOException
    {
        return casStorageService.readCas(aDocument, CURATION_SET);
    }

    @Override
    public void deleteCurationCas(SourceDocument aDocument) throws IOException
    {
        casStorageService.deleteCas(aDocument, CURATION_SET);
    }

    @Override
    public void upgradeCurationCas(CAS aCas, SourceDocument aDocument)
        throws UIMAException, IOException
    {
        annotationService.upgradeCas(aCas, aDocument, CURATION_USER);
    }

    @Override
    @Transactional
    public List<User> listCuratableUsers(SourceDocument aSourceDocument)
    {
        Validate.notNull(aSourceDocument, "Document must be specified");

        // We deliberately do not join ProjectPermission here: a user that left annotation data
        // behind but no longer holds the ANNOTATOR permission (e.g. removed from the project or
        // role changed) must remain curatable so their data stays accessible.
        // The User join still excludes the curation/initial-CAS pseudo users as well as users whose
        // account was deleted entirely (they have no User row).
        var query = join("\n", //
                "SELECT u, d FROM User u", //
                " JOIN AnnotationDocument as d", //
                "   ON d.user = u.username", //
                "WHERE u.username = d.user", //
                "  AND d.document = :document", //
                "  AND (d.state = :state or d.annotatorState = :ignore)", //
                "ORDER BY u.username ASC");

        var candidates = entityManager //
                .createQuery(query, Object[].class) //
                .setParameter("document", aSourceDocument) //
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .setParameter("ignore", AnnotationDocumentState.IGNORE) //
                .getResultList();

        var curatableUsers = new ArrayList<User>();
        for (var candidate : candidates) {
            var user = (User) candidate[0];
            var annDoc = (AnnotationDocument) candidate[1];

            // Each candidate either finished the document or set their own state to IGNORE (i.e.
            // the document should no longer be annotated by them - for whatever reason). A finished
            // document always has a CAS - it is created when the document is first opened - so its
            // owner is curatable without further checks. Any other candidate got here via the
            // IGNORE branch and only counts if a CAS was actually written, i.e. the annotator had
            // opened the document before it was set to IGNORE - an IGNORE document that was never
            // opened has no data to curate.
            if (annDoc.getState() == AnnotationDocumentState.FINISHED
                    || hasCas(aSourceDocument, user.getUsername())) {
                curatableUsers.add(user);
            }
        }

        return curatableUsers;
    }

    private boolean hasCas(SourceDocument aDocument, String aDataOwner)
    {
        try {
            return casStorageService.existsCas(aDocument, AnnotationSet.forUser(aDataOwner));
        }
        catch (IOException e) {
            LOG.warn("Unable to determine whether a CAS exists for [{}] on {} - assuming it does",
                    aDataOwner, aDocument, e);
            return true;
        }
    }

    @Override
    @Transactional
    public List<SourceDocument> listCuratableSourceDocuments(Project aProject)
    {
        if (curationProperties.isLegacyCuratableDocumentsStrategy()) {
            return listCuratableSourceDocuments_legacy(aProject);
        }
        else {
            return listCuratableSourceDocuments_new(aProject);
        }
    }

    /**
     * @deprecated To be removed when the legacy curatable document startegy is removed.
     */
    @Deprecated
    @Transactional
    List<SourceDocument> listCuratableSourceDocuments_legacy(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        // Curation that has already started may always be continued, matching isDocumentCuratable.
        // Once a document reached a curation state, the workload managers stop updating its state
        // (see e.g. MatrixWorkloadExtensionImpl#isInCuration), so resetting the annotators leaves
        // it in curation with no finished annotation document.
        //
        // Otherwise the legacy rule is "at least one annotator has finished". Documents that
        // annotators only set to IGNORE do not qualify, whether or not they left a CAS behind -
        // unlike listCuratableUsers, which collects all data worth merging once curation has
        // started.
        // A finished annotation document always has a CAS, so no CAS check is needed. Former
        // annotators count as well, so that their data stays accessible after they were removed
        // from
        // the project or lost the role.
        //
        // The query is rooted in SourceDocument rather than in AnnotationDocument because a
        // document
        // in a curation state need not have any annotation document at all.
        var query = join("\n", //
                "SELECT doc FROM SourceDocument AS doc", //
                "WHERE doc.project = :project", //
                "AND (doc.state IN (:curationStates)", //
                "     OR EXISTS (", //
                "       SELECT adoc FROM AnnotationDocument AS adoc", //
                "       WHERE adoc.document = doc", //
                "       AND adoc.user NOT IN (:pseudoUsers)", //
                "       AND adoc.state = :state))", //
                "ORDER BY doc.name ASC");

        return entityManager.createQuery(query, SourceDocument.class) //
                .setParameter("project", aProject) //
                .setParameter("curationStates", List.of(CURATION_IN_PROGRESS, CURATION_FINISHED)) //
                .setParameter("pseudoUsers", List.of(CURATION_USER, INITIAL_CAS_PSEUDO_USER)) //
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .getResultList();
    }

    @Transactional
    List<SourceDocument> listCuratableSourceDocuments_new(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(SourceDocument.class);
        var sd = cq.from(SourceDocument.class);

        cq.select(sd).distinct(true);

        var projectPredicate = cb.equal(sd.get(SourceDocument_.project), aProject);
        var statePredicate = sd.get(SourceDocument_.state).in(ANNOTATION_FINISHED,
                CURATION_IN_PROGRESS, CURATION_FINISHED);

        cq.where(cb.and(projectPredicate, statePredicate));
        cq.orderBy(cb.asc(sd.get(SourceDocument_.name)));

        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class, readOnly = true)
    public boolean isDocumentCuratable(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Document must be specified");

        // Make sure we know the latest state from the DB - just in case the given document is stale
        var state = getCurrentState(aDocument);

        // Curation that has already been started may always be continued, so a document in a
        // curation state or with an existing curation CAS bypasses the entry rules below.
        if (CURATION_IN_PROGRESS.equals(state) || CURATION_FINISHED.equals(state)) {
            return true;
        }

        try {
            if (existsCurationCas(aDocument)) {
                return true;
            }
        }
        catch (IOException e) {
            LOG.warn("Unable to determine whether a curation CAS exists for {} - assuming it does",
                    aDocument, e);
            return true;
        }

        if (curationProperties.isLegacyCuratableDocumentsStrategy()) {
            // Legacy rule: the document state does not matter, but at least one annotator must have
            // marked their annotation document as finished.
            return hasFinishedAnnotationDocument(aDocument);
        }

        // Default rule: the document itself must have reached the annotation-finished state.
        if (!ANNOTATION_FINISHED.equals(state)) {
            return false;
        }

        // We require at least one curatable user from whom we can obtain the curation CAS template
        return !listCuratableUsers(aDocument).isEmpty();
    }

    /**
     * @deprecated To be removed when the legacy curatable document startegy is removed.
     */
    @Deprecated
    private boolean hasFinishedAnnotationDocument(SourceDocument aDocument)
    {
        // Must stay in sync with the finished-annotation branch of
        // listCuratableSourceDocuments_legacy, otherwise a document offered in the curation list
        // cannot actually be opened, or vice versa. The caller has already checked the curation
        // state and the curation CAS, which is what the other branch of that query covers. Former
        // annotators count here as well.
        var query = join("\n", //
                "SELECT COUNT(*) FROM AnnotationDocument", //
                "WHERE document = :document", //
                "  AND user NOT IN (:pseudoUsers)", //
                "  AND state = :state");

        return entityManager.createQuery(query, Long.class) //
                .setParameter("document", aDocument) //
                .setParameter("pseudoUsers", List.of(CURATION_USER, INITIAL_CAS_PSEUDO_USER)) //
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .getSingleResult() > 0;
    }

    @Override
    public Optional<Long> getCurationCasTimestamp(SourceDocument aDocument) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");

        return casStorageService.getCasTimestamp(aDocument, CURATION_SET);
    }

    @Override
    public Optional<Long> verifyCurationCasTimestamp(SourceDocument aDocument, long aTimeStamp,
            String aContextAction)
        throws IOException, ConcurentCasModificationException
    {
        return casStorageService.verifyCasTimestamp(aDocument, CURATION_SET, aTimeStamp,
                aContextAction);
    }

    @Override
    @Transactional
    public List<SourceDocument> listCuratedDocuments(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        String query = join("\n", "FROM SourceDocument WHERE", "  project = :project AND",
                "  state = :state");

        return entityManager.createQuery(query, SourceDocument.class)
                .setParameter("project", aProject).setParameter("state", CURATION_FINISHED)
                .getResultList();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class, readOnly = true)
    public boolean isCurationFinished(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Source document must be specified");

        return CURATION_FINISHED.equals(getCurrentState(aDocument));
    }

    /**
     * @return the current state of the given document as recorded in the database, rather than the
     *         possibly stale state of the given entity - another curator may have finished curation
     *         in the meantime.
     * @throws NoResultException
     *             if the document does not exist.
     */
    @Transactional(readOnly = true)
    private SourceDocumentState getCurrentState(SourceDocument aDocument)
    {
        var query = join("\n", "SELECT state FROM SourceDocument WHERE", "  id = :id");

        return entityManager.createQuery(query, SourceDocumentState.class) //
                .setParameter("id", aDocument.getId()) //
                .getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public void markCurationInProgress(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Source document must be specified");

        // Avoid transition overhead when the documentis already in a curation state
        var state = getCurrentState(aDocument);
        if (CURATION_FINISHED.equals(state) || CURATION_IN_PROGRESS.equals(state)) {
            return;
        }

        documentService.transitionSourceDocumentState(aDocument,
                ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS);
    }

    @Override
    public boolean existsCurationCas(SourceDocument aDocument) throws IOException
    {
        return casStorageService.existsCas(aDocument, CURATION_SET);
    }
}
