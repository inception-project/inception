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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.NO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.RECREATE;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createCasCopy;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getRealCas;
import static java.lang.invoke.MethodHandles.lookup;
import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.slf4j.Logger;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationServiceAutoConfiguration#curationEditingService}.
 * </p>
 */
public class CurationEditingServiceImpl
    implements CurationEditingService
{
    private final static Logger LOG = getLogger(lookup().lookupClass());

    private final DocumentService documentService;
    private final CurationDocumentService curationDocumentService;
    private final CurationMergeService curationMergeService;

    public CurationEditingServiceImpl(DocumentService aDocumentService,
            CurationDocumentService aCurationDocumentService,
            CurationMergeService aCurationMergeService)
    {
        documentService = aDocumentService;
        curationDocumentService = aCurationDocumentService;
        curationMergeService = aCurationMergeService;
    }

    @Override
    public CAS readOrCreateCurationCas(SourceDocument aDocument, String aCurationUser,
            List<AnnotationLayer> aLayers, MergeStrategy aMergeStrategy,
            CurationMergeMode aMergeMode)
        throws CurationNotPossibleException, UIMAException, IOException
    {
        if (!curationDocumentService.isDocumentCuratable(aDocument)) {
            throw notCuratable(aDocument);
        }

        // If there is no curation CAS set and there are no users from whom a template could be
        // created we refuse curation.
        var curatableUsers = curationDocumentService.listCuratableUsers(aDocument);
        if (curatableUsers.isEmpty() && !curationDocumentService.existsCurationCas(aDocument)) {
            throw notCuratable(aDocument);
        }

        // A re-merge clears the existing curation CAS and rebuilds it from the annotation data of
        // the curatable users. Without any curatable users that would destroy the curator's work
        // without being able to produce anything in its place.
        if (aMergeMode == RECREATE && curatableUsers.isEmpty()) {
            throw new CurationNotPossibleException(aDocument,
                    "Cannot re-create the curation document for [" + aDocument.getName()
                            + "]: there are no finished annotation documents to merge "
                            + "from. Re-creating it would irrevocably discard the existing curation "
                            + "results. Set at least one annotation document back to "
                            + AnnotationDocumentState.FINISHED + " first.");
        }

        var casses = documentService.readAllCasesSharedNoUpgrade(aDocument, curatableUsers);

        // Another curator may have started curation since the checks above, so whether the curation
        // CAS exists is decided here, under the lock on it. The lock is kept until the request
        // ends, so only the request that creates the curation CAS merges into it - everybody else
        // gets the result and their requested merge mode.
        var created = new AtomicBoolean();
        documentService.readOrCreateAnnotationCas(aDocument, CURATION_SET, NO_CAS_UPGRADE,
                EXCLUSIVE_WRITE_ACCESS, () -> created.set(true));

        try {
            // A curation CAS deleted since the checks above has been re-created without anybody
            // to merge from
            if (created.get() && curatableUsers.isEmpty()) {
                throw notCuratable(aDocument);
            }

            // A curation CAS just created holds no more than the initial CAS, so it is always
            // merged into - regardless of the requested merge mode
            var curationCas = readCurationCas(aDocument, aCurationUser, casses, true, aLayers,
                    aMergeStrategy, created.get() ? RECREATE : aMergeMode);
            curationDocumentService.markCurationInProgress(aDocument);
            return curationCas;
        }
        catch (Exception e) {
            if (!created.get()) {
                throw e;
            }

            // The curation CAS was created above and nobody else can have written to it since, so
            // it holds no curation results. Remove it, so the document does not look merged and
            // the merge is retried when it is opened the next time.
            try {
                curationDocumentService.deleteCurationCas(aDocument);
            }
            catch (Exception ex) {
                LOG.error("Unable to remove the curation CAS of {} after the failed initial merge",
                        aDocument, ex);
            }

            throw e;
        }
    }

    @Override
    public CAS readCurationCas(SourceDocument aDocument, String aCurationUser,
            Map<String, CAS> aCasses, boolean aUpgrade, List<AnnotationLayer> aLayers,
            MergeStrategy aMergeStrategy, CurationMergeMode aMergeMode)
        throws UIMAException, IOException
    {
        CAS mergeCas;

        switch (aMergeMode) {
        case RECREATE: {
            // Replace the contents of the curation CAS instead of deleting it and writing a new
            // one, which would release the lock on it in between
            mergeCas = curationDocumentService.readCurationCas(aDocument);

            if (aUpgrade) {
                curationDocumentService.upgradeCurationCas(mergeCas, aDocument);
            }

            // Merge into a copy, so a failed merge leaves the curation CAS as it was
            var result = createCasCopy(mergeCas);
            curationMergeService.mergeCasses(aDocument, aCurationUser, result, aCasses,
                    aMergeStrategy, aLayers, true);
            deserializeCASComplete(serializeCASComplete((CASImpl) getRealCas(result)),
                    (CASImpl) getRealCas(mergeCas));

            curationDocumentService.writeCurationCas(mergeCas, aDocument, false);
            break;
        }
        case FILL_ONLY:
            // Merge into the existing curation CAS without clearing it first, so that annotations
            // already present in the curation document (e.g. curator decisions) are preserved. As
            // we mutate and persist the target CAS, it must be upgraded to the current type system
            // first - just like in the LOAD_ONLY case below.
            mergeCas = curationDocumentService.readCurationCas(aDocument);

            if (aUpgrade) {
                curationDocumentService.upgradeCurationCas(mergeCas, aDocument);
            }

            curationMergeService.mergeCasses(aDocument, aCurationUser, mergeCas, aCasses,
                    aMergeStrategy, aLayers, false);
            curationDocumentService.writeCurationCas(mergeCas, aDocument, true);
            break;
        case LOAD_ONLY:
            // Load the existing curation CAS as-is without merging anything into it.
            mergeCas = curationDocumentService.readCurationCas(aDocument);

            if (aUpgrade) {
                curationDocumentService.upgradeCurationCas(mergeCas, aDocument);
                // Only persisting the upgrade does not change the annotations, so there is
                // nothing to announce to listeners (search index, recommenders, ...)
                curationDocumentService.writeCurationCasSilently(mergeCas, aDocument, true);
            }
            break;
        default:
            throw new IllegalArgumentException(
                    "Unsupported curation merge mode [" + aMergeMode + "]");
        }

        return mergeCas;
    }

    @Override
    public boolean isOpenableForCuration(SourceDocument aDocument)
    {
        if (!curationDocumentService.isDocumentCuratable(aDocument)) {
            return false;
        }

        try {
            return !curationDocumentService.listCuratableUsers(aDocument).isEmpty()
                    || curationDocumentService.existsCurationCas(aDocument);
        }
        catch (IOException e) {
            LOG.warn("Unable to determine whether a curation CAS exists for {} - assuming it does",
                    aDocument, e);
            return true;
        }
    }

    @Override
    public Optional<SourceDocument> findNextCuratableDocument(List<SourceDocument> aDocuments,
            SourceDocument aDocument)
    {
        try {
            var index = aDocuments.indexOf(aDocument);
            if (index < 0) {
                return Optional.empty();
            }

            for (var candidate : aDocuments.subList(index + 1, aDocuments.size())) {
                if (isOpenableForCuration(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        catch (Exception e) {
            LOG.warn("Unable to determine the next curatable document after {}", aDocument, e);
        }

        return Optional.empty();
    }

    private DocumentNotCuratableException notCuratable(SourceDocument aDocument)
    {
        return new DocumentNotCuratableException(aDocument, "Document [" + aDocument.getName()
                + "] has the state " + aDocument.getState()
                + " and is not ready for curation. A document can "
                + "only be curated once annotation on it is complete (the document has reached the "
                + AnnotationDocumentState.FINISHED + " state) - depending on the workload regime, "
                + "this may require more than a single annotator to finish. This can also happen "
                + "when curation on a document was already started and afterwards all annotators "
                + "were removed from the project, disabled or put back into "
                + AnnotationDocumentState.IN_PROGRESS
                + " mode, or after importing a project without "
                + "importing/enabling its users. Use the monitoring page to reset the "
                + "curation state of this document.");
    }
}
