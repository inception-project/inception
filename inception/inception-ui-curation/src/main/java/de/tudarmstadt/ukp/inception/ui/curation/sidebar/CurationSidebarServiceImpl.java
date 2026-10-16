/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.inception.curation.sidebar.CurationSidebarManagerPrefs.KEY_CURATION_SIDEBAR_MANAGER_PREFS;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.curation.service.DocumentNotCuratableException;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.config.CurationSidebarAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationSidebarAutoConfiguration#curationSidebarService}.
 * </p>
 */
public class CurationSidebarServiceImpl
    implements CurationSidebarService
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DocumentService documentService;
    private final CasStorageService casStorageService;
    private final CurationService curationService;
    private final CurationMergeService curationMergeService;
    private final CurationSessionService curationSessionService;
    private final CurationDocumentService curationDocumentService;
    private final PreferencesService preferencesService;

    public CurationSidebarServiceImpl(DocumentService aDocumentService,
            CasStorageService aCasStorageService, CurationService aCurationService,
            CurationMergeService aCurationMergeService,
            CurationSessionService aCurationSessionService,
            CurationDocumentService aCurationDocumentService,
            PreferencesService aPreferencesService)
    {
        documentService = aDocumentService;
        casStorageService = aCasStorageService;
        curationService = aCurationService;
        curationMergeService = aCurationMergeService;
        curationSessionService = aCurationSessionService;
        curationDocumentService = aCurationDocumentService;
        preferencesService = aPreferencesService;
    }

    /**
     * @return CAS associated with curation doc for the given user
     */
    @Transactional
    private CAS retrieveCurationCAS(SourceDocument aDoc) throws IOException
    {
        return documentService.readAnnotationCas(aDoc, CURATION_SET);
    }

    /**
     * Write to CAS associated with curation doc for the given user and update timestamp
     */
    @Transactional
    private void writeCurationCas(CAS aTargetCas, AnnotatorState aState) throws IOException
    {
        var doc = aState.getDocument();
        var annoDoc = documentService.createOrGetAnnotationDocument(doc, CURATION_SET);
        documentService.writeAnnotationCas(aTargetCas, annoDoc, EXPLICIT_ANNOTATOR_USER_ACTION);
        casStorageService.getCasTimestamp(doc, CURATION_SET)
                .ifPresent(aState::setAnnotationDocumentTimestamp);
    }

    @Transactional
    @Override
    public boolean isShowAll(String aSessionOwner, Long aProjectId)
    {
        return curationSessionService.isShowAll(aSessionOwner, aProjectId);
    }

    @Transactional
    @Override
    public void setShowAll(String aSessionOwner, Long aProjectId, boolean aValue)
    {
        curationSessionService.setShowAll(aSessionOwner, aProjectId, aValue);
    }

    @Transactional
    @Override
    public boolean isShowScore(String aSessionOwner, Long aProjectId)
    {
        return curationSessionService.isShowScore(aSessionOwner, aProjectId);
    }

    @Transactional
    @Override
    public void setShowScore(String aSessionOwner, Long aProjectId, boolean aValue)
    {
        curationSessionService.setShowScore(aSessionOwner, aProjectId, aValue);
    }

    @Override
    public boolean isCurationFinished(AnnotatorState aState)
    {
        var username = aState.getUser().getUsername();
        var sourceDoc = aState.getDocument();
        return username.equals(CURATION_USER) && sourceDoc.getState().equals(CURATION_FINISHED);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    @Transactional
    public MergeStrategyFactory<?> merge(AnnotatorState aState, CurationWorkflow aWorkflow,
            Collection<AnnotationSet> aDataOwners, boolean aClearTargetCas)
        throws IOException, UIMAException, NotEditableException
    {
        var doc = aState.getDocument();

        if (curationDocumentService.isCurationFinished(doc)) {
            throw new NotEditableException("Curation is already finished. You can put it back "
                    + "into progress via the monitoring page.");
        }

        MergeStrategyFactory factory = curationService.getMergeStrategyFactory(aWorkflow);
        var traits = factory.readTraits(aWorkflow);
        var mergeStrategy = factory.makeStrategy(traits);
        var aTargetCas = retrieveCurationCAS(doc);

        var userCases = documentService.readAllCasesSharedNoUpgrade(doc,
                aDataOwners.stream().map(AnnotationSet::id).toArray(String[]::new));

        // FIXME: should merging not overwrite the current users annos? (can result in
        // deleting the users annotations!!!), currently fixed by warn message to user
        // prepare merged CAS
        curationMergeService.mergeCasses(doc, aState.getUser().getUsername(), aTargetCas, userCases,
                mergeStrategy, aState.getAnnotationLayers(), aClearTargetCas);

        // write back and update timestamp
        writeCurationCas(aTargetCas, aState);

        curationDocumentService.markCurationInProgress(doc);

        LOG.debug("Merge done");
        return factory;
    }

    @Override
    @Transactional
    public Optional<MergeStrategyFactory<?>> performInitialMergeIfRequired(AnnotatorState aState)
        throws IOException, UIMAException, NotEditableException, DocumentNotCuratableException
    {
        if (!CURATION_SET.equals(aState.getDataOwner())) {
            return Optional.empty();
        }

        // Never merges over curation results, nor into a document whose curation is finished
        var doc = aState.getDocument();
        if (!curationDocumentService.isInitialMergeRequired(doc)) {
            return Optional.empty();
        }

        var project = aState.getProject();
        var prefs = preferencesService
                .loadDefaultTraitsForProject(KEY_CURATION_SIDEBAR_MANAGER_PREFS, project);
        if (!prefs.isAutoMergeCurationSidebar()) {
            // Curation starts from the document without annotations. Upgrade the curation CAS
            // when creating it - loading the document gets it from the CAS storage session and
            // does not upgrade it again.
            var created = new AtomicBoolean();
            documentService.readOrCreateAnnotationCas(doc, CURATION_SET, FORCE_CAS_UPGRADE,
                    EXCLUSIVE_WRITE_ACCESS, () -> created.set(true));
            if (!created.get()) {
                return Optional.empty();
            }

            try {
                // Like a merge, creating the curation CAS starts curation. Moving the document
                // into curation only when loading it would leave the curation CAS behind if the
                // loading fails, and an existing curation CAS is never merged into.
                curationDocumentService.markCurationInProgress(doc);
            }
            catch (RuntimeException e) {
                // Nobody else can have written to the curation CAS since it was created above
                try {
                    curationDocumentService.deleteCurationCas(doc);
                }
                catch (Exception ex) {
                    LOG.error("Unable to remove the curation CAS of {} after failing to start "
                            + "curation", doc, ex);
                }

                throw e;
            }

            return Optional.empty();
        }

        // Create the curation CAS from the initial CAS and upgrade it, so the merge can succeed.
        // Loading the document reads it again, but that duplication only happens when an initial
        // merge is required. Another curator may have started curation since the check above, so
        // whether the curation CAS exists is decided again here, under the lock on it. The lock is
        // kept until the request ends, so only the request that creates the CAS merges into it.
        var created = new AtomicBoolean();
        documentService.readOrCreateAnnotationCas(doc, CURATION_SET, FORCE_CAS_UPGRADE,
                EXCLUSIVE_WRITE_ACCESS, () -> created.set(true));
        if (!created.get()) {
            return Optional.empty();
        }

        try {
            // A curation CAS merged from nobody would look curated and never be merged again
            if (curationDocumentService.listCuratableUsers(doc).isEmpty()) {
                throw new DocumentNotCuratableException(doc, "Document [" + doc.getName()
                        + "] cannot be curated: there are no annotators whose annotations could "
                        + "be merged. This happens when all annotators were removed from the "
                        + "project, disabled or put back into "
                        + AnnotationDocumentState.IN_PROGRESS + " mode.");
            }

            // Always all curatable annotators: the curator's selection in the curation sidebar is
            // a view filter and is made on another document anyway. To merge from only some of
            // them, the curator re-merges.
            var dataOwners = curationSessionService.listCuratableDataOwners(doc);
            var workflow = curationService.readOrCreateCurationWorkflow(project);
            return Optional.of(merge(aState, workflow, dataOwners, true));
        }
        catch (Exception e) {
            // The curation CAS was created above and nobody else can have written to it since, so
            // it holds no curation results. Remove it, so the document does not look merged and
            // the merge is retried when it is opened the next time.
            try {
                curationDocumentService.deleteCurationCas(doc);
            }
            catch (Exception ex) {
                LOG.error("Unable to remove the curation CAS of {} after the failed initial merge",
                        doc, ex);
            }

            throw e;
        }
    }

}
