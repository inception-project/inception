/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.updateDocumentTimestampAfterWrite;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.LinkCompareBehavior.LINK_ROLE_AS_LABEL;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.CorrectionDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.util.MergeCas;
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

    private final AnnotationSchemaService annotationService;
    private final DocumentService documentService;
    private final CorrectionDocumentService correctionDocumentService;
    private final CurationDocumentService curationDocumentService;
    private final UserDao userRepository;
    private final CasStorageService casStorageService;

    int diffRangeBegin, diffRangeEnd;
    boolean firstload = true;
    public static Map<Integer, Set<Integer>> crossSentenceLists;
    //
    Map<Integer, Integer> segmentBeginEnd = new HashMap<>();

    public SuggestionBuilder(CasStorageService aCasStorageService,
            DocumentService aDocumentService,
            CorrectionDocumentService aCorrectionDocumentService,
            CurationDocumentService aCurationDocumentService,
            AnnotationSchemaService aAnnotationService, UserDao aUserDao)
    {
        documentService = aDocumentService;
        correctionDocumentService = aCorrectionDocumentService;
        curationDocumentService = aCurationDocumentService;
        annotationService = aAnnotationService;
        userRepository = aUserDao;
        casStorageService = aCasStorageService;
    }

    public CurationContainer buildCurationContainer(AnnotatorState aBModel)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        CurationContainer curationContainer = new CurationContainer();
        // initialize Variables
        SourceDocument sourceDocument = aBModel.getDocument();
        Map<Integer, Integer> segmentBeginEnd = new HashMap<>();
        Map<Integer, Integer> segmentNumber = new HashMap<>();
        Map<String, Map<Integer, Integer>> segmentAdress = new HashMap<>();
        // get annotation documents

        List<AnnotationDocument> finishedAnnotationDocuments = new ArrayList<>();

        for (AnnotationDocument annotationDocument : documentService
                .listAnnotationDocuments(aBModel.getDocument())) {
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                finishedAnnotationDocuments.add(annotationDocument);
            }
        }

        Map<String, CAS> casses = new HashMap<>();

        AnnotationDocument randomAnnotationDocument = null;
        CAS mergeCas;

        // get the correction/automation CAS for the logged in user
        if (aBModel.getMode().equals(Mode.AUTOMATION)
                || aBModel.getMode().equals(Mode.CORRECTION)) {
            casses = listCasesforCorrection(randomAnnotationDocument, sourceDocument,
                    aBModel.getMode());
            mergeCas = getMergeCas(aBModel, sourceDocument, casses, randomAnnotationDocument,
                    false);
            String username = casses.keySet().iterator().next();
            updateSegment(aBModel, segmentBeginEnd, segmentNumber, segmentAdress,
                    casses.get(username), username, aBModel.getWindowBeginOffset(),
                    aBModel.getWindowEndOffset());
        }
        else {
            casses = listCassesforCuration(finishedAnnotationDocuments, randomAnnotationDocument,
                    aBModel.getMode());
            mergeCas = getMergeCas(aBModel, sourceDocument, casses, randomAnnotationDocument,
                    false);
            updateSegment(aBModel, segmentBeginEnd, segmentNumber, segmentAdress, mergeCas,
                    WebAnnoConst.CURATION_USER,
                    WebAnnoCasUtil.getFirstSentence(mergeCas).getBegin(),
                    mergeCas.getDocumentText().length());

        }

        List<Type> entryTypes = null;

        segmentAdress.put(WebAnnoConst.CURATION_USER, new HashMap<>());
        Type sentenceType = getType(mergeCas, Sentence.class);
        for (AnnotationFS sentence : selectCovered(mergeCas, sentenceType, diffRangeBegin,
                diffRangeEnd)) {
            segmentAdress.get(WebAnnoConst.CURATION_USER).put(sentence.getBegin(),
                    getAddr(sentence));
        }

        if (entryTypes == null) {
            entryTypes = getEntryTypes(mergeCas, aBModel.getAnnotationLayers(), annotationService);
        }

        // for cross-sentences annotation, update the end of the segment
        if (firstload) {
            long start = System.currentTimeMillis();
            log.debug("Updating cross sentence annotation list...");
            updateCrossSentAnnoList(segmentBeginEnd, segmentNumber, casses, entryTypes);
            firstload = false;
            log.debug("Cross sentence annotation list complete in {}ms",
                    (System.currentTimeMillis() - start));
        }

        List<DiffAdapter> adapters = CasDiff2.getAdapters(annotationService, aBModel.getProject());

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

            DiffResult diff = CasDiff2.doDiffSingle(entryTypes, adapters, LINK_ROLE_AS_LABEL,
                    casses, begin, end);

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
                    curationSegment.setSentenceState(SentenceState.DISAGREE);
                }
                else if (!diff.getIncompleteConfigurationSets().isEmpty()) {
                    curationSegment.setSentenceState(SentenceState.DISAGREE);
                }
                else {
                    curationSegment.setSentenceState(SentenceState.AGREE);
                }
            }
            else {
                curationSegment.setSentenceState(SentenceState.AGREE);
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

    private void updateCrossSentAnnoList(Map<Integer, Integer> aSegmentBeginEnd,
            Map<Integer, Integer> aSegmentNumber, Map<String, CAS> aCases, List<Type> aEntryTypes)
    {
        // FIXME Remove this side-effect and instead return this hashmap
        crossSentenceLists = new HashMap<>();

        // Extract the sentences for all the CASes
        Map<CAS, List<AnnotationFS>> idxSentences = new HashMap<>();
        for (CAS c : aCases.values()) {
            Type sentenceType = getType(c, Sentence.class);
            idxSentences.put(c, new ArrayList<>(select(c, sentenceType)));
        }

        Set<Integer> sentenceBegins = aSegmentBeginEnd.keySet();
        int count = 0;
        for (int sentBegin : sentenceBegins) {
            count++;

            if (count % 100 == 0) {
                log.debug("Updating cross-sentence annoations: {} of {} sentences...", count,
                        sentenceBegins.size());
            }

            int sentEnd = aSegmentBeginEnd.get(sentBegin);
            int currentSentenceNumber = -1;

            Set<Integer> crossSents = new HashSet<>();

            for (Type t : aEntryTypes) {
                for (CAS c : aCases.values()) {
                    // Determine sentence number for the current segment begin. This takes quite
                    // a while, so we only do it for the first CAS in the batch. Will be the
                    // same for all others anyway.
                    if (currentSentenceNumber == -1) {
                        currentSentenceNumber = aSegmentNumber.get(sentBegin);
                    }

                    // update cross-sentence annotation lists
                    for (AnnotationFS fs : selectCovered(c, t, diffRangeBegin, diffRangeEnd)) {
                        // CASE 1. annotation begins here
                        if (sentBegin <= fs.getBegin() && fs.getBegin() <= sentEnd) {
                            if (fs.getEnd() < sentBegin || sentEnd < fs.getEnd()) {
                                AnnotationFS s = getSentenceByAnnoEnd(idxSentences.get(c),
                                        fs.getEnd());
                                int thatSent = idxSentences.get(c).indexOf(s) + 1;
                                crossSents.add(thatSent);
                            }
                        }
                        // CASE 2. Annotation ends here
                        else if (sentBegin <= fs.getEnd() && fs.getEnd() <= sentEnd) {
                            if (fs.getBegin() < sentBegin || sentEnd < fs.getBegin()) {
                                int thatSent = WebAnnoCasUtil.getSentenceNumber(c, fs.getBegin());
                                crossSents.add(thatSent);
                            }
                        }
                    }

                    for (AnnotationFS fs : selectCovered(c, t, sentBegin, diffRangeEnd)) {
                        if (fs.getBegin() <= sentEnd && fs.getEnd() > sentEnd) {
                            AnnotationFS s = getSentenceByAnnoEnd(idxSentences.get(c), fs.getEnd());
                            aSegmentBeginEnd.put(sentBegin, s.getEnd());
                        }
                    }
                }
            }
            crossSentenceLists.put(currentSentenceNumber, crossSents);
        }
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

        // Upgrading should be an explicit action during the opening of a document at the end
        // of the open dialog - it must not happen during editing because the CAS addresses
        // are used as IDs in the UI
        // repository.upgradeCasAndSave(aDocument, aMode, user.getUsername());
        CAS cas = documentService.readAnnotationCas(randomAnnotationDocument);
        casses.put(user.getUsername(), cas);
        return casses;
    }

    public Map<String, CAS> listCassesforCuration(List<AnnotationDocument> annotationDocuments,
            AnnotationDocument randomAnnotationDocument, Mode aMode)
        throws UIMAException, ClassNotFoundException, IOException
    {
        Map<String, CAS> casses = new HashMap<>();
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            String username = annotationDocument.getUser();

            if (!annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                continue;
            }

            if (randomAnnotationDocument == null) {
                randomAnnotationDocument = annotationDocument;
            }

            // Upgrading should be an explicit action during the opening of a document at the end
            // of the open dialog - it must not happen during editing because the CAS addresses
            // are used as IDs in the UI
            // repository.upgradeCasAndSave(annotationDocument.getDocument(), aMode, username);
            CAS cas = documentService.readAnnotationCas(annotationDocument);
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
     * @param randomAnnotationDocument
     *            an annotation document.
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
            Map<String, CAS> aCasses, AnnotationDocument randomAnnotationDocument, boolean aUpgrade)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        CAS mergeCas = null;
        try {
            if (aState.getMode().equals(Mode.AUTOMATION)
                    || aState.getMode().equals(Mode.CORRECTION)) {
                // Upgrading should be an explicit action during the opening of a document at the
                // end of the open dialog - it must not happen during editing because the CAS 
                // addresses are used as IDs in the UI
                // repository.upgradeCasAndSave(aDocument, aBratAnnotatorModel.getMode(),
                // aBratAnnotatorModel.getUser().getUsername());
                mergeCas = correctionDocumentService.readCorrectionCas(aDocument);
                if (aUpgrade) {
                    correctionDocumentService.upgradeCorrectionCas(mergeCas, aDocument);
                    correctionDocumentService.writeCorrectionCas(mergeCas, aDocument);
                    updateDocumentTimestampAfterWrite(aState, correctionDocumentService
                            .getCorrectionCasTimestamp(aState.getDocument()));
                }
            }
            else {
                // Upgrading should be an explicit action during the opening of a document at the
                // end of the open dialog - it must not happen during editing because the CAS 
                // addresses are used as IDs in the UI
                // repository.upgradeCasAndSave(aDocument, aBratAnnotatorModel.getMode(),
                // aBratAnnotatorModel.getUser().getUsername());
                mergeCas = curationDocumentService.readCurationCas(aDocument);
                if (aUpgrade) {
                    curationDocumentService.upgradeCurationCas(mergeCas, aDocument);
                    curationDocumentService.writeCurationCas(mergeCas, aDocument, true);
                    updateDocumentTimestampAfterWrite(aState, curationDocumentService
                            .getCurationCasTimestamp(aState.getDocument()));
                }
            }
        }
        // Create JCas, if it could not be loaded from the file system
        catch (Exception e) {
            if (aState.getMode().equals(Mode.AUTOMATION)
                    || aState.getMode().equals(Mode.CORRECTION)) {
                mergeCas = createCorrectionCas(mergeCas, aState,
                        randomAnnotationDocument);
                updateDocumentTimestampAfterWrite(aState, correctionDocumentService
                        .getCorrectionCasTimestamp(aState.getDocument()));
            }
            else {
                mergeCas = createCurationCas(aState.getProject(),
                        randomAnnotationDocument, aCasses,
                        aState.getAnnotationLayers());
                updateDocumentTimestampAfterWrite(aState, curationDocumentService
                        .getCurationCasTimestamp(aState.getDocument()));
            }
        }
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
            entryTypes.add(
                    aAnnotationService.getAdapter(layer).getAnnotationType(aMergeCas));
        }
        return entryTypes;
    }

    /**
     * For the first time a curation page is opened, create a MergeCas that contains only agreeing
     * annotations Using the CAS of the curator user.
     *
     * @param aProject
     *            the project
     * @param randomAnnotationDocument
     *            an annotation document.
     * @param aCasses
     *            the CASes
     * @param aAnnotationLayers
     *            the layers.
     * @return the CAS.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public CAS createCurationCas(Project aProject, AnnotationDocument randomAnnotationDocument,
            Map<String, CAS> aCasses, List<AnnotationLayer> aAnnotationLayers)
        throws IOException
    {
        CAS mergeCas;
        boolean cacheEnabled = false;
        try {
            cacheEnabled = casStorageService.isCacheEnabled();
            casStorageService.disableCache();
            mergeCas = documentService.readAnnotationCas(randomAnnotationDocument);
        }
        finally {
            if (cacheEnabled) {
                casStorageService.enableCache();
            }
        }
        aCasses.put(WebAnnoConst.CURATION_USER, mergeCas);

        List<Type> entryTypes = getEntryTypes(mergeCas, aAnnotationLayers, annotationService);

        DiffResult diff = CasDiff2.doDiffSingle(annotationService, aProject, entryTypes,
                LinkCompareBehavior.LINK_ROLE_AS_LABEL, aCasses, 0,
                mergeCas.getDocumentText().length());

        mergeCas = MergeCas.reMergeCas(diff, aCasses);

        curationDocumentService.writeCurationCas(mergeCas, randomAnnotationDocument.getDocument(),
                false);
        
        return mergeCas;
    }

    private CAS createCorrectionCas(CAS aMergeCas, AnnotatorState aState,
            AnnotationDocument aRandomAnnotationDocument)
        throws UIMAException, ClassNotFoundException, IOException
    {
        User user = userRepository.getCurrentUser();
        aMergeCas = documentService.readAnnotationCas(aState.getDocument(), user);
        correctionDocumentService.writeCorrectionCas(aMergeCas,
                aRandomAnnotationDocument.getDocument());
        updateDocumentTimestampAfterWrite(aState, correctionDocumentService
                .getCorrectionCasTimestamp(aState.getDocument()));
        return aMergeCas;
    }
}
