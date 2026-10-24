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
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.FILL_ONLY;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.LOAD_ONLY;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.RECREATE;
import static de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarManagerPrefs.KEY_CURATION_SIDEBAR_MANAGER_PREFS;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createCasCopy;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getRealCas;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Collections.emptyMap;
import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.slf4j.Logger;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;

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
    private final PreferencesService preferencesService;

    public CurationEditingServiceImpl(DocumentService aDocumentService,
            CurationDocumentService aCurationDocumentService,
            CurationMergeService aCurationMergeService, PreferencesService aPreferencesService)
    {
        documentService = aDocumentService;
        curationDocumentService = aCurationDocumentService;
        curationMergeService = aCurationMergeService;
        preferencesService = aPreferencesService;
    }

    @Override
    public CAS readOrCreateCurationCas(SourceDocument aDocument, String aCurationUser,
            List<AnnotationLayer> aLayers, MergeStrategy aMergeStrategy,
            CurationMergeMode aMergeMode)
        throws CurationNotPossibleException, UIMAException, IOException
    {
        return readOrCreateCurationCas(aDocument, aCurationUser, aLayers, aMergeStrategy,
                aMergeMode, () -> {
                    // Nothing to report
                });
    }

    @Override
    public CAS readOrCreateCurationCas(SourceDocument aDocument, String aCurationUser,
            List<AnnotationLayer> aLayers, MergeStrategy aMergeStrategy,
            CurationMergeMode aMergeMode, Runnable aInitialMergeListener)
        throws CurationNotPossibleException, UIMAException, IOException
    {
        return readOrCreateCurationCas(aDocument, aCurationUser, aLayers, aMergeStrategy,
                aMergeMode, user -> true, aInitialMergeListener);
    }

    @Override
    public CAS readOrCreateCurationCas(SourceDocument aDocument, String aCurationUser,
            List<AnnotationLayer> aLayers, MergeStrategy aMergeStrategy,
            CurationMergeMode aMergeMode, Collection<AnnotationSet> aDataOwners)
        throws CurationNotPossibleException, UIMAException, IOException
    {
        return readOrCreateCurationCas(aDocument, aCurationUser, aLayers, aMergeStrategy,
                aMergeMode, user -> aDataOwners.contains(AnnotationSet.forUser(user)), () -> {
                    // Nothing to report
                });
    }

    private CAS readOrCreateCurationCas(SourceDocument aDocument, String aCurationUser,
            List<AnnotationLayer> aLayers, MergeStrategy aMergeStrategy,
            CurationMergeMode aMergeMode, Predicate<User> aMergeSourceFilter,
            Runnable aInitialMergeListener)
        throws CurationNotPossibleException, UIMAException, IOException
    {
        if (!curationDocumentService.isDocumentCuratable(aDocument)) {
            throw notCuratable(aDocument);
        }

        // Creating an empty or a merged curation CAS in place of the missing one would present a
        // finished curation nobody has reviewed - and rotate out the backups of the lost one
        if (isCuratedVersionMissing(aDocument)) {
            throw curatedVersionMissing(aDocument);
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

        var mergeSourceUsers = curatableUsers.stream().filter(aMergeSourceFilter).toList();

        // The same holds when none of the annotators to merge from is curatable - because none
        // were selected, or because the selected ones stopped being curatable in the meantime
        if (aMergeMode == RECREATE && mergeSourceUsers.isEmpty()) {
            throw new CurationNotPossibleException(aDocument,
                    "Cannot re-create the curation document for [" + aDocument.getName()
                            + "]: none of the annotators to merge from has annotations that can "
                            + "be curated. Re-creating it would irrevocably discard the existing "
                            + "curation results.");
        }

        // Filling in without anybody to merge from would report a merge that did nothing. A
        // selection may be stale, e.g. when the selected annotators were reset in the meantime.
        if (aMergeMode == FILL_ONLY && mergeSourceUsers.isEmpty()) {
            throw new CurationNotPossibleException(aDocument,
                    "Cannot merge into the curation document for [" + aDocument.getName()
                            + "]: none of the annotators to merge from has annotations that can be "
                            + "curated.");
        }

        // Another curator may have started curation since the checks above, so whether the curation
        // CAS exists is decided here, under the lock on it. The lock is kept until the request
        // ends, so only the request that creates the curation CAS merges into it - everybody else
        // gets the result and their requested merge mode.
        var created = new AtomicBoolean();
        documentService.readOrCreateAnnotationCas(aDocument, CURATION_SET, NO_CAS_UPGRADE,
                EXCLUSIVE_WRITE_ACCESS, () -> created.set(true));

        CurationMergeMode mergeMode;
        CAS curationCas;
        try {
            // A curation CAS deleted since the checks above has been re-created without anybody
            // to merge from - or for a curation that has been finished in the meantime
            if (created.get() && mergeSourceUsers.isEmpty()) {
                throw notCuratable(aDocument);
            }

            if (created.get() && curationDocumentService.isCurationFinished(aDocument)) {
                throw curatedVersionMissing(aDocument);
            }

            if (created.get()) {
                // Writing the curation CAS only notifies listeners (search index, recommenders,
                // ...) about it if its annotation document exists - and when a document is opened
                // for curation, the page only creates that once the curation CAS has been provided
                documentService.createOrGetAnnotationDocument(aDocument, CURATION_SET);
            }

            mergeMode = created.get() ? initialMergeMode(aDocument, aMergeMode) : aMergeMode;

            // The annotators' CASes are only needed for a merge - and whether there is one is only
            // known now that it is decided whether this request created the curation CAS
            Map<String, CAS> casses = mergeMode == LOAD_ONLY ? emptyMap()
                    : documentService.readAllCasesSharedNoUpgrade(aDocument, mergeSourceUsers);

            // The curation CAS is now held by the request, so the page reads it from there and
            // cannot upgrade it itself - it has to be upgraded here. Only a finished curation that
            // is merely viewed is handed over as it is: its stored version and backups must stay as
            // they are.
            var upgrade = created.get() || mergeMode != LOAD_ONLY
                    || !curationDocumentService.isCurationFinished(aDocument);
            curationCas = readCurationCas(aDocument, aCurationUser, casses, upgrade, aLayers,
                    aMergeStrategy, mergeMode);
            curationDocumentService.markCurationInProgress(aDocument);
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

        // Only once curation has been started successfully - and outside of the cleanup above, so
        // that whatever the listener does cannot undo it
        if (created.get() && mergeMode != LOAD_ONLY) {
            aInitialMergeListener.run();
        }

        return curationCas;
    }

    /**
     * A curation CAS just created holds no more than the initial CAS, so it is merged into
     * regardless of the requested merge mode - unless the document is only being opened and the
     * project starts curation without the automatic merge. Then curation starts from the document
     * without annotations. An explicitly requested merge always merges.
     */
    private CurationMergeMode initialMergeMode(SourceDocument aDocument,
            CurationMergeMode aRequestedMergeMode)
    {
        if (aRequestedMergeMode == LOAD_ONLY && !isAutoMergeEnabled(aDocument)) {
            return LOAD_ONLY;
        }

        return RECREATE;
    }

    private boolean isAutoMergeEnabled(SourceDocument aDocument)
    {
        var prefs = preferencesService.loadDefaultTraitsForProject(
                KEY_CURATION_SIDEBAR_MANAGER_PREFS, aDocument.getProject());
        return prefs.isAutoMergeCurationSidebar();
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

        if (isCuratedVersionMissing(aDocument)) {
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

    /**
     * CURATION_FINISHED is only ever reached through an explicit curator action, so a missing
     * curation CAS there means the curation results are missing.
     */
    private boolean isCuratedVersionMissing(SourceDocument aDocument)
    {
        if (!curationDocumentService.isCurationFinished(aDocument)) {
            return false;
        }

        try {
            return !curationDocumentService.existsCurationCas(aDocument);
        }
        catch (IOException e) {
            // If we cannot tell, assume it is there - opening the document fails if it is not
            LOG.warn("Unable to determine whether a curation CAS exists for {} - assuming it does",
                    aDocument, e);
            return false;
        }
    }

    private CurationNotPossibleException curatedVersionMissing(SourceDocument aDocument)
    {
        return new CurationNotPossibleException(aDocument,
                "The curated version of document [" + aDocument.getName()
                        + "] is missing. A project manager can put its curation "
                        + "back into progress to curate it again.");
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
