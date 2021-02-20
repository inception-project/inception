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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model;

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.updateDocumentTimestampAfterWrite;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getFirstSentence;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiffSingle;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.getDiffAdapters;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_ROLE_AS_LABEL;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SentenceState.AGREE;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SentenceState.DISAGREE;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.CasMerge;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.StopWatch;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * This class is responsible for two things. Firstly, it creates a pre-merged cas, which contains
 * all annotations, where all annotators agree on. This is done by copying a random cas and removing
 * all differing annotations.
 *
 * Secondly, the class creates an instance of {@link CurationContainer}, which is the wicket model
 * for the curation panel. The {@link CurationContainer} contains the text for all sentences, which
 * are displayed at a specific page.
 */
public class SuggestionBuilder
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AnnotationSchemaService schemaService;
    private final DocumentService documentService;
    private final CurationDocumentService curationDocumentService;
    private final UserDao userRepository;

    private int diffRangeBegin;
    private int diffRangeEnd;

    public SuggestionBuilder(DocumentService aDocumentService,
            CurationDocumentService aCurationDocumentService,
            AnnotationSchemaService aAnnotationService, UserDao aUserDao)
    {
        documentService = aDocumentService;
        curationDocumentService = aCurationDocumentService;
        schemaService = aAnnotationService;
        userRepository = aUserDao;
    }

    public CurationContainer buildCurationContainer(AnnotatorState aState)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        CurationContainer curationContainer = new CurationContainer();
        // initialize Variables
        SourceDocument sourceDocument = aState.getDocument();
        Map<Integer, Integer> segmentBeginEnd = new HashMap<>();
        Map<Integer, Integer> segmentNumber = new HashMap<>();
        Map<String, Map<Integer, Integer>> segmentAdress = new HashMap<>();

        // get annotation documents
        List<AnnotationDocument> finishedAnnotationDocuments = new ArrayList<>();
        for (AnnotationDocument annotationDocument : documentService
                .listAnnotationDocuments(aState.getDocument())) {
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                finishedAnnotationDocuments.add(annotationDocument);
            }
        }

        Map<String, CAS> casses = listCassesforCuration(finishedAnnotationDocuments,
                aState.getMode());
        CAS mergeCas = getMergeCas(aState, sourceDocument, casses, null, false, false, false);
        updateSegment(aState, segmentBeginEnd, segmentNumber, segmentAdress, mergeCas,
                CURATION_USER, getFirstSentence(mergeCas).getBegin(),
                mergeCas.getDocumentText().length());

        segmentAdress.put(CURATION_USER, new HashMap<>());
        Type sentenceType = getType(mergeCas, Sentence.class);
        for (AnnotationFS s : selectCovered(mergeCas, sentenceType, diffRangeBegin, diffRangeEnd)) {
            segmentAdress.get(CURATION_USER).put(s.getBegin(), getAddr(s));
        }

        List<DiffAdapter> adapters = getDiffAdapters(schemaService, aState.getAnnotationLayers());

        long diffStart = System.currentTimeMillis();
        log.debug("Calculating differences...");
        int count = 0;
        for (Integer begin : segmentBeginEnd.keySet()) {
            Integer end = segmentBeginEnd.get(begin);

            count++;
            if (count % 100 == 0) {
                log.debug("Processing differences: {} of {} sentences...", count,
                        segmentBeginEnd.size());
            }

            DiffResult diff = doDiffSingle(adapters, LINK_ROLE_AS_LABEL, casses, begin, end)
                    .toResult();

            SourceListView curationSegment = new SourceListView();
            curationSegment.setBegin(begin);
            curationSegment.setEnd(end);
            curationSegment.setSentenceNumber(segmentNumber.get(begin));
            if (diff.hasDifferences() || !diff.getIncompleteConfigurationSets().isEmpty()) {
                // Is this confSet a diff due to stacked annotations (with same configuration)?
                boolean stackedDiff = false;

                stackedDiffSet: for (ConfigurationSet d : diff.getDifferingConfigurationSets()
                        .values()) {
                    for (Configuration c : d.getConfigurations()) {
                        if (c.getCasGroupIds().size() != d.getCasGroupIds().size()) {
                            stackedDiff = true;
                            break stackedDiffSet;
                        }
                    }
                }

                if (stackedDiff) {
                    curationSegment.setSentenceState(DISAGREE);
                }
                else if (!diff.getIncompleteConfigurationSets().isEmpty()) {
                    curationSegment.setSentenceState(DISAGREE);
                }
                else {
                    curationSegment.setSentenceState(AGREE);
                }
            }
            else {
                curationSegment.setSentenceState(AGREE);
            }

            for (String username : segmentAdress.keySet()) {
                curationSegment.getSentenceAddress().put(username,
                        segmentAdress.get(username).get(begin));
            }
            curationContainer.getCurationViewByBegin().put(begin, curationSegment);
        }
        log.debug("Difference calculation completed in {}ms",
                (System.currentTimeMillis() - diffStart));

        return curationContainer;
    }

    /**
     * Get a sentence at the end of an annotation
     */
    private static AnnotationFS getSentenceByAnnoEnd(List<AnnotationFS> aSentences, int aEnd)
    {
        int prevEnd = 0;
        AnnotationFS sent = null;
        for (AnnotationFS sentence : aSentences) {
            if (prevEnd >= aEnd) {
                return sent;
            }
            sent = sentence;
            prevEnd = sent.getEnd();
        }
        return sent;
    }

    private Map<String, CAS> listCasesforCorrection(AnnotationDocument randomAnnotationDocument,
            SourceDocument aDocument, Mode aMode)
        throws UIMAException, ClassNotFoundException, IOException
    {
        Map<String, CAS> casses = new HashMap<>();
        User user = userRepository
                .get(SecurityContextHolder.getContext().getAuthentication().getName());
        randomAnnotationDocument = documentService.getAnnotationDocument(aDocument, user);

        CAS cas = documentService.readAnnotationCas(randomAnnotationDocument.getDocument(),
                randomAnnotationDocument.getUser(), AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
        casses.put(user.getUsername(), cas);
        return casses;
    }

    public Map<String, CAS> listCassesforCuration(List<AnnotationDocument> annotationDocuments,
            Mode aMode)
        throws UIMAException, ClassNotFoundException, IOException
    {
        Map<String, CAS> casses = new HashMap<>();
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            String username = annotationDocument.getUser();

            if (!annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                continue;
            }

            CAS cas = documentService.readAnnotationCas(annotationDocument.getDocument(),
                    annotationDocument.getUser(), AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
            casses.put(username, cas);
        }
        return casses;
    }

    /**
     * Fetches the CAS that the user will be able to edit. In AUTOMATION/CORRECTION mode, this is
     * the CAS for the CORRECTION_USER and in CURATION mode it is the CAS for the CURATION user.
     *
     * @param aState
     *            the model.
     * @param aDocument
     *            the source document.
     * @param aCasses
     *            the CASes.
     * @param aTemplate
     *            an annotation document which is used as a template for the new merge CAS.
     * @return the CAS.
     * @throws UIMAException
     *             hum?
     * @throws ClassNotFoundException
     *             hum?
     * @throws IOException
     *             if an I/O error occurs.
     * @throws AnnotationException
     *             hum?
     */
    public CAS getMergeCas(AnnotatorState aState, SourceDocument aDocument,
            Map<String, CAS> aCasses, AnnotationDocument aTemplate, boolean aUpgrade,
            boolean aMergeIncompleteAnnotations, boolean aForceRecreateCas)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        if (aForceRecreateCas) {
            return initializeMergeCas(aState, aCasses, aTemplate, aMergeIncompleteAnnotations);
        }

        if (!curationDocumentService.existsCurationCas(aDocument)) {
            return initializeMergeCas(aState, aCasses, aTemplate, aMergeIncompleteAnnotations);
        }

        CAS mergeCas = curationDocumentService.readCurationCas(aDocument);
        if (aUpgrade) {
            curationDocumentService.upgradeCurationCas(mergeCas, aDocument);
            curationDocumentService.writeCurationCas(mergeCas, aDocument, true);
            updateDocumentTimestampAfterWrite(aState,
                    curationDocumentService.getCurationCasTimestamp(aState.getDocument()));
        }

        return mergeCas;
    }

    public CAS initializeMergeCas(AnnotatorState aState, Map<String, CAS> aCasses,
            AnnotationDocument aTemplate, boolean aMergeIncompleteAnnotations)
        throws ClassNotFoundException, UIMAException, IOException, AnnotationException
    {
        CAS mergeCas = createCurationCas(aState, aTemplate, aCasses, aState.getAnnotationLayers(),
                aMergeIncompleteAnnotations);
        updateDocumentTimestampAfterWrite(aState,
                curationDocumentService.getCurationCasTimestamp(aState.getDocument()));

        return mergeCas;
    }

    /**
     * Puts CASes into a list and get a random annotation document that will be used as a base for
     * the diff.
     */
    private void updateSegment(AnnotatorState aBratAnnotatorModel,
            Map<Integer, Integer> aIdxSentenceBeginEnd,
            Map<Integer, Integer> aIdxSentenceBeginNumber,
            Map<String, Map<Integer, Integer>> aSegmentAdress, CAS aCas, String aUsername,
            int aWindowStart, int aWindowEnd)
    {
        diffRangeBegin = aWindowStart;
        diffRangeEnd = aWindowEnd;

        // Get the number of the first sentence - instead of fetching the number over and over
        // we can just increment this one.
        int sentenceNumber = WebAnnoCasUtil.getSentenceNumber(aCas, diffRangeBegin);

        aSegmentAdress.put(aUsername, new HashMap<>());
        Type sentenceType = CasUtil.getType(aCas, Sentence.class);
        for (AnnotationFS sentence : selectCovered(aCas, sentenceType, diffRangeBegin,
                diffRangeEnd)) {
            aIdxSentenceBeginEnd.put(sentence.getBegin(), sentence.getEnd());
            aIdxSentenceBeginNumber.put(sentence.getBegin(), sentenceNumber);
            aSegmentAdress.get(aUsername).put(sentence.getBegin(), getAddr(sentence));
            sentenceNumber += 1;
        }
    }

    @Deprecated
    public static List<Type> getEntryTypes(CAS aMergeCas, List<AnnotationLayer> aLayers,
            AnnotationSchemaService aAnnotationService)
    {
        List<Type> entryTypes = new LinkedList<>();

        for (AnnotationLayer layer : aLayers) {
            if (layer.getName().equals(Token.class.getName())) {
                continue;
            }
            if (layer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                continue;
            }
            entryTypes.add(aAnnotationService.getAdapter(layer).getAnnotationType(aMergeCas));
        }
        return entryTypes;
    }

    /**
     * For the first time a curation page is opened, create a MergeCas that contains only agreeing
     * annotations Using the CAS of the curator user.
     *
     * @param aState
     *            the annotator state
     * @param aRandomAnnotationDocument
     *            an annotation document.
     * @param aCasses
     *            the CASes
     * @param aAnnotationLayers
     *            the layers.
     * @return the CAS.
     * @throws IOException
     *             if an I/O error occurs.
     */
    private CAS createCurationCas(AnnotatorState aState,
            AnnotationDocument aRandomAnnotationDocument, Map<String, CAS> aCasses,
            List<AnnotationLayer> aAnnotationLayers, boolean aMergeIncompleteAnnotations)
        throws IOException, UIMAException, AnnotationException
    {
        Validate.notNull(aState, "State must be specified");
        Validate.notNull(aRandomAnnotationDocument, "Annotation document must be specified");

        // We need a modifiable copy of some annotation document which we can use to initialize
        // the curation CAS. This is an exceptional case where BYPASS is the correct choice
        CAS mergeCas = documentService.readAnnotationCas(aRandomAnnotationDocument,
                UNMANAGED_ACCESS);

        List<DiffAdapter> adapters = getDiffAdapters(schemaService, aState.getAnnotationLayers());

        DiffResult diff;
        try (StopWatch watch = new StopWatch(log, "CasDiff")) {
            diff = doDiffSingle(adapters, LINK_ROLE_AS_LABEL, aCasses, 0,
                    mergeCas.getDocumentText().length()).toResult();
        }

        try (StopWatch watch = new StopWatch(log, "CasMerge")) {
            CasMerge casMerge = new CasMerge(schemaService);
            casMerge.setMergeIncompleteAnnotations(aMergeIncompleteAnnotations);
            casMerge.reMergeCas(diff, aState.getDocument(), aState.getUser().getUsername(),
                    mergeCas, aCasses);
        }

        curationDocumentService.writeCurationCas(mergeCas, aRandomAnnotationDocument.getDocument(),
                false);

        return mergeCas;
    }
}
