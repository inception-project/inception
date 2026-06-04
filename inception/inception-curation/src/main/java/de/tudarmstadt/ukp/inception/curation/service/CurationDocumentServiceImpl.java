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
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static java.util.Comparator.comparing;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument_;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.config.CurationDocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.config.CurationProperties;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import jakarta.persistence.EntityManager;

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

    @Autowired
    public CurationDocumentServiceImpl(CasStorageService aCasStorageService,
            AnnotationSchemaService aAnnotationService, CurationProperties aCurationProperties,
            EntityManager aEntityManager)
    {
        casStorageService = aCasStorageService;
        annotationService = aAnnotationService;
        entityManager = aEntityManager;
        curationProperties = aCurationProperties;
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
        var query = String.join("\n", //
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

    @Transactional
    List<SourceDocument> listCuratableSourceDocuments_legacy(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        // We deliberately do not restrict to current annotators: a document that only has finished
        // or IGNORE annotation data from a former annotator (removed from the project or role
        // changed) must still surface for curation so that data stays accessible. The pseudo users
        // for the curation and initial CASes are excluded.
        var query = String.join("\n", //
                "SELECT adoc", //
                "FROM AnnotationDocument AS adoc", //
                "WHERE adoc.project = :project", //
                "AND adoc.user NOT IN (:pseudoUsers)", //
                "AND (adoc.state = :state or adoc.annotatorState = :ignore)");

        var candidates = entityManager.createQuery(query, AnnotationDocument.class) //
                .setParameter("project", aProject) //
                .setParameter("pseudoUsers", List.of(CURATION_USER, INITIAL_CAS_PSEUDO_USER)) //
                .setParameter("state", AnnotationDocumentState.FINISHED) //
                .setParameter("ignore", AnnotationDocumentState.IGNORE) //
                .getResultList();

        // A document is curatable only if at least one annotator actually produced data on it: a
        // finished document always has a CAS, while a document the annotator set to IGNORE counts
        // only if a CAS was actually written (same reasoning as in listCuratableUsers).
        var curatableDocuments = new HashMap<Long, SourceDocument>();
        for (var adoc : candidates) {
            var document = adoc.getDocument();
            if (curatableDocuments.containsKey(document.getId())) {
                continue;
            }

            if (adoc.getState() == AnnotationDocumentState.FINISHED
                    || hasCas(document, adoc.getAnnotationSet().id())) {
                curatableDocuments.put(document.getId(), document);
            }
        }

        var result = new ArrayList<>(curatableDocuments.values());
        result.sort(comparing(SourceDocument::getName));
        return result;
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

        String query = String.join("\n", "FROM SourceDocument WHERE", "  project = :project AND",
                "  state = :state");

        return entityManager.createQuery(query, SourceDocument.class)
                .setParameter("project", aProject).setParameter("state", CURATION_FINISHED)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCurationFinished(SourceDocument aDocument)
    {
        Validate.notNull(aDocument, "Source document must be specified");

        var query = String.join("\n", "FROM SourceDocument WHERE", "  id = :id");

        var d = entityManager.createQuery(query, SourceDocument.class) //
                .setParameter("id", aDocument.getId()) //
                .getSingleResult();

        return CURATION_FINISHED.equals(d.getState());
    }

    @Override
    public boolean existsCurationCas(SourceDocument aDocument) throws IOException
    {
        return casStorageService.existsCas(aDocument, CURATION_SET);
    }
}
