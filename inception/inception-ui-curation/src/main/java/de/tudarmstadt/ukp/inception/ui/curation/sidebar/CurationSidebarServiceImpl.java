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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Optional.empty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.curation.api.CurationSessionService;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
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
    private final UserDao userRegistry;
    private final CasStorageService casStorageService;
    private final CurationService curationService;
    private final CurationMergeService curationMergeService;
    private final CurationSessionService curationSessionService;

    public CurationSidebarServiceImpl(DocumentService aDocumentService, UserDao aUserRegistry,
            CasStorageService aCasStorageService, CurationService aCurationService,
            CurationMergeService aCurationMergeService,
            CurationSessionService aCurationSessionService)
    {
        documentService = aDocumentService;
        userRegistry = aUserRegistry;
        casStorageService = aCasStorageService;
        curationService = aCurationService;
        curationMergeService = aCurationMergeService;
        curationSessionService = aCurationSessionService;
    }

    /**
     * @return CAS associated with curation doc for the given user
     */
    // REC: Do we really needs this? Why not save via the AnnotationPage facilities? Or at least
    // the curation target should already be set in the annotator state, so why not rely on that?
    @Transactional
    private Optional<CAS> retrieveCurationCAS(String aSessionOwner, long aProjectId,
            SourceDocument aDoc)
        throws IOException
    {
        var curationUser = curationSessionService.getCurationTarget(aSessionOwner, aProjectId);
        if (curationUser == null) {
            return empty();
        }

        return Optional
                .of(documentService.readAnnotationCas(aDoc, AnnotationSet.forUser(curationUser)));
    }

    /**
     * Write to CAS associated with curation doc for the given user and update timestamp
     */
    // REC: Do we really needs this? Why not save via the AnnotationPage facilities? Or at least
    // the curation target should already be set in the annotator state, so why not rely on that?
    @Transactional
    private void writeCurationCas(CAS aTargetCas, AnnotatorState aState, long aProjectId)
        throws IOException
    {
        var curatorName = curationSessionService.getCurationTarget(aState.getUser().getUsername(),
                aProjectId);
        User curator = userRegistry.getUserOrCurationUser(curatorName);

        var doc = aState.getDocument();
        var annoDoc = documentService.createOrGetAnnotationDocument(doc, curator);
        documentService.writeAnnotationCas(aTargetCas, annoDoc, EXPLICIT_ANNOTATOR_USER_ACTION);
        casStorageService.getCasTimestamp(doc, AnnotationSet.forUser(curator.getUsername()))
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
    public boolean isCurationFinished(AnnotatorState aState, String aSessionOwner)
    {
        var username = aState.getUser().getUsername();
        var sourceDoc = aState.getDocument();
        return (username.equals(aSessionOwner)
                && documentService.isAnnotationFinished(sourceDoc, aState.getUser()))
                || (username.equals(CURATION_USER)
                        && sourceDoc.getState().equals(CURATION_FINISHED));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    @Transactional
    public MergeStrategyFactory<?> merge(AnnotatorState aState, CurationWorkflow aWorkflow,
            String aCurator, Collection<User> aUsers, boolean aClearTargetCas)
        throws IOException, UIMAException
    {
        MergeStrategyFactory factory = curationService.getMergeStrategyFactory(aWorkflow);
        var traits = factory.readTraits(aWorkflow);
        var mergeStrategy = factory.makeStrategy(traits);
        var doc = aState.getDocument();
        var aTargetCas = retrieveCurationCAS(aCurator, doc.getProject().getId(), doc).orElseThrow(
                () -> new IllegalArgumentException("No target CAS configured in curation state"));

        var userCases = documentService.readAllCasesSharedNoUpgrade(doc, aUsers);

        // FIXME: should merging not overwrite the current users annos? (can result in
        // deleting the users annotations!!!), currently fixed by warn message to user
        // prepare merged CAS
        curationMergeService.mergeCasses(doc, aState.getUser().getUsername(), aTargetCas, userCases,
                mergeStrategy, aState.getAnnotationLayers(), aClearTargetCas);

        // write back and update timestamp
        writeCurationCas(aTargetCas, aState, doc.getProject().getId());

        LOG.debug("Merge done");
        return factory;
    }

}
