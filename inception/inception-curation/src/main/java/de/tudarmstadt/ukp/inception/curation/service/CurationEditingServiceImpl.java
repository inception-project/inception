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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.RECREATE;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
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
        var curatableUsers = curationDocumentService.listCuratableUsers(aDocument);

        if (!curationDocumentService.isDocumentCuratable(aDocument)) {
            throw notCuratable(aDocument);
        }

        // If there is no curation CAS set and there are no users from whom a template could be
        // created we refuse curation.
        if (curatableUsers.isEmpty() && !curationDocumentService.existsCurationCas(aDocument)) {
            throw notCuratable(aDocument);
        }

        // A re-merge deletes the existing curation CAS and rebuilds it from the annotation data of
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

        var templateUser = curatableUsers.isEmpty() ? null : curatableUsers.get(0).getUsername();
        var curationCas = readCurationCas(aDocument, aCurationUser, casses, templateUser, true,
                aLayers, aMergeStrategy, aMergeMode);

        curationDocumentService.markCurationInProgress(aDocument);

        return curationCas;
    }

    @Override
    public CAS readCurationCas(SourceDocument aDocument, String aCurationUser,
            Map<String, CAS> aCasses, String aTemplateUser, boolean aUpgrade,
            List<AnnotationLayer> aLayers, MergeStrategy aMergeStrategy,
            CurationMergeMode aMergeMode)
        throws UIMAException, IOException
    {
        CAS mergeCas;

        var curationCasExists = curationDocumentService.existsCurationCas(aDocument);

        // If no curation CAS exists yet, there is nothing to load or merge into, so we always have
        // to recreate it from the annotators - regardless of the requested merge mode.
        var effectiveMergeMode = curationCasExists ? aMergeMode : RECREATE;

        switch (effectiveMergeMode) {
        case RECREATE:
            if (aTemplateUser == null) {
                throw new IllegalStateException("Cannot create a curation document for ["
                        + aDocument.getName() + "] without a template annotation document");
            }

            // We need a modifiable copy of some annotation document which we can use to initialize
            // the curation CAS. This is an exceptional case where UNMANAGED_ACCESS is the correct
            // choice
            mergeCas = documentService.readAnnotationCas(aDocument,
                    AnnotationSet.forUser(aTemplateUser), FORCE_CAS_UPGRADE, UNMANAGED_ACCESS);
            curationMergeService.mergeCasses(aDocument, aCurationUser, mergeCas, aCasses,
                    aMergeStrategy, aLayers, true);
            curationDocumentService.deleteCurationCas(aDocument);
            curationDocumentService.writeCurationCas(mergeCas, aDocument, false);
            break;
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
                curationDocumentService.writeCurationCas(mergeCas, aDocument, true);
            }
            break;
        default:
            throw new IllegalArgumentException(
                    "Unsupported curation merge mode [" + effectiveMergeMode + "]");
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
                + " and is not ready for curation. By default, a document can "
                + "only be curated once annotation on it is complete (the document has reached the "
                + AnnotationDocumentState.FINISHED + " state) - depending on the workload regime, "
                + "this may require more than a single annotator to finish. This can also happen "
                + "when curation on a document was already started and afterwards all annotators "
                + "were removed from the project, disabled or put back into "
                + AnnotationDocumentState.IN_PROGRESS
                + " mode, or after importing a project without "
                + "importing/enabling its users. To curate incomplete data on purpose, an "
                + "administrator can enable the legacy curatable-documents strategy in the "
                + "application configuration. Otherwise, use the monitoring page to reset the "
                + "curation state of this document.");
    }
}
