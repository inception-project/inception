/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getFirstSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectSentenceAt;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.Position;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
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
 *
 * @author Andreas Straninger
 * @author Seid Muhie Yimam
 */
public class SuggestionBuilder
{
    private final AnnotationService annotationService;
    private final RepositoryService repository;
    private final UserDao userRepository;

    int sentenceNumber;
    int begin, end;
    boolean firstload = true;
    public static Map<Integer, Set<Integer>> crossSentenceLists;
    //
    Map<Integer, Integer> segmentBeginEnd = new HashMap<Integer, Integer>();

    public SuggestionBuilder(RepositoryService repository, AnnotationService aAnnotationService,
            UserDao aUserDao)
    {
        this.repository = repository;
        this.annotationService = aAnnotationService;
        userRepository = aUserDao;
    }

    public CurationContainer buildCurationContainer(BratAnnotatorModel aBModel)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        CurationContainer curationContainer = new CurationContainer();
        // initialize Variables
        SourceDocument sourceDocument = aBModel.getDocument();
        Map<Integer, Integer> segmentBeginEnd = new HashMap<Integer, Integer>();
        Map<Integer, Integer> segmentNumber = new HashMap<Integer, Integer>();
        Map<String, Map<Integer, Integer>> segmentAdress = new HashMap<String, Map<Integer, Integer>>();
        // get annotation documents

        List<AnnotationDocument> finishedAnnotationDocuments = new ArrayList<AnnotationDocument>();

        for (AnnotationDocument annotationDocument : repository.listAnnotationDocuments(aBModel
                .getDocument())) {
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                finishedAnnotationDocuments.add(annotationDocument);
            }
        }

        Map<String, JCas> jCases = new HashMap<String, JCas>();

        AnnotationDocument randomAnnotationDocument = null;
        JCas mergeJCas;

        // get the correction/automation JCas for the logged in user
        if (aBModel.getMode().equals(Mode.AUTOMATION) || aBModel.getMode().equals(Mode.CORRECTION)) {
            jCases = listJcasesforCorrection(randomAnnotationDocument, sourceDocument,
                    aBModel.getMode());
            mergeJCas = getMergeCas(aBModel, sourceDocument, jCases, randomAnnotationDocument);
            String username = jCases.keySet().iterator().next();
            updateSegment(aBModel, segmentBeginEnd, segmentNumber, segmentAdress,
                    jCases.get(username), username, aBModel.getPreferences().getWindowSize());

        }
        else {

            jCases = listJcasesforCuration(finishedAnnotationDocuments, randomAnnotationDocument,
                    aBModel.getMode());
            mergeJCas = getMergeCas(aBModel, sourceDocument, jCases, randomAnnotationDocument);
            updateSegment(aBModel, segmentBeginEnd, segmentNumber, segmentAdress, mergeJCas,
                    CurationPanel.CURATION_USER, aBModel.getPreferences().getCurationWindowSize());

        }

        List<Type> entryTypes = null;

        segmentAdress.put(CurationPanel.CURATION_USER, new HashMap<Integer, Integer>());
        for (Sentence sentence : selectCovered(mergeJCas, Sentence.class, begin, end)) {
            segmentAdress.get(CurationPanel.CURATION_USER).put(sentence.getBegin(),
                    getAddr(sentence));
        }

        if (entryTypes == null) {
            entryTypes = getEntryTypes(mergeJCas, aBModel.getAnnotationLayers(), annotationService);
        }

        // for cross-sentences annotation, update the end of the segment
        if (firstload) {
            updateCrossSentAnnoList(segmentBeginEnd, jCases, entryTypes);
            firstload = false;
        }

        for (Integer begin : segmentBeginEnd.keySet()) {
            Integer end = segmentBeginEnd.get(begin);

            DiffResult diff = CasDiff2.doDiffSingle(annotationService, aBModel.getProject(),
                    entryTypes, LinkCompareBehavior.LINK_TARGET_AS_LABEL, jCases, begin, end);
            SourceListView curationSegment = new SourceListView();
            curationSegment.setBegin(begin);
            curationSegment.setEnd(end);
            if (diff.hasDifferences() || !diff.getIncompleteConfigurationSets().isEmpty()) {
                curationSegment.setSentenceState(SentenceState.DISAGREE);
            }
            else {
                curationSegment.setSentenceState(SentenceState.AGREE);
            }
            curationSegment.setSentenceNumber(segmentNumber.get(begin));

            for (String username : segmentAdress.keySet()) {
                curationSegment.getSentenceAddress().put(username,
                        segmentAdress.get(username).get(begin));
            }
            curationContainer.getCurationViewByBegin().put(begin, curationSegment);
        }
        return curationContainer;
    }

    private void updateCrossSentAnnoList(Map<Integer, Integer> segmentBeginEnd,
            Map<String, JCas> jCases, List<Type> entryTypes)
    {
        crossSentenceLists = new HashMap<>();
        for (Integer begin : segmentBeginEnd.keySet()) {
            int thisSent = -1;
            Set<Integer> crossSents = new HashSet<>();
            for (Type t : entryTypes) {
                for (JCas c : jCases.values()) {
                    if (thisSent == -1) {
                        thisSent = BratAjaxCasUtil.getSentenceNumber(c, begin);
                    }
                    // update cross-sentence annotation lists
                    for (AnnotationFS fs : selectCovered(c.getCas(), t, this.begin, end)) {
                        // CASE 1. annotation begins here
                        if (fs.getBegin() >= begin && fs.getBegin() <= segmentBeginEnd.get(begin)) {
                            if (fs.getEnd() > segmentBeginEnd.get(begin) || fs.getEnd() < begin) {
                                Sentence s = BratAjaxCasUtil.getSentenceByAnnoEnd(c, fs.getEnd());
                                int thatSent = BratAjaxCasUtil.getSentenceNumber(c, s.getBegin());
                                crossSents.add(thatSent);
                            }
                        }
                        // CASE 2. Annotation ends here
                        else if (fs.getEnd() >= begin && fs.getEnd() <= segmentBeginEnd.get(begin)) {
                            if (fs.getBegin() > segmentBeginEnd.get(begin) || fs.getBegin() < begin) {
                                int thatSent = BratAjaxCasUtil.getSentenceNumber(c, fs.getBegin());
                                crossSents.add(thatSent);
                            }
                        }
                    }

                    for (AnnotationFS fs : selectCovered(c.getCas(), t, begin, end)) {
                        if (fs.getBegin() <= segmentBeginEnd.get(begin)
                                && fs.getEnd() > segmentBeginEnd.get(begin)) {
                            Sentence s = BratAjaxCasUtil.getSentenceByAnnoEnd(c, fs.getEnd());
                            segmentBeginEnd.put(begin, s.getEnd());
                        }
                    }
                }
            }
            crossSentenceLists.put(thisSent, crossSents);
        }
    }

    public Map<String, JCas> listJcasesforCorrection(AnnotationDocument randomAnnotationDocument,
            SourceDocument aDocument, Mode aMode)
        throws UIMAException, ClassNotFoundException, IOException
    {
        Map<String, JCas> jCases = new HashMap<String, JCas>();
        User user = userRepository.get(SecurityContextHolder.getContext().getAuthentication()
                .getName());
        randomAnnotationDocument = repository.getAnnotationDocument(aDocument, user);

        // Upgrading should be an explicit action during the opening of a document at the end
        // of the open dialog - it must not happen during editing because the CAS addresses
        // are used as IDs in the UI
        // repository.upgradeCasAndSave(aDocument, aMode, user.getUsername());
        JCas jCas = repository.readAnnotationCas(randomAnnotationDocument);
        jCases.put(user.getUsername(), jCas);
        return jCases;
    }

    public Map<String, JCas> listJcasesforCuration(List<AnnotationDocument> annotationDocuments,
            AnnotationDocument randomAnnotationDocument, Mode aMode)
        throws UIMAException, ClassNotFoundException, IOException
    {
        Map<String, JCas> jCases = new HashMap<String, JCas>();
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
            JCas jCas = repository.readAnnotationCas(annotationDocument);
            jCases.put(username, jCas);
        }
        return jCases;
    }

    /**
     * Fetches the CAS that the user will be able to edit. In AUTOMATION/CORRECTION mode, this is
     * the CAS for the CORRECTION_USER and in CURATION mode it is the CAS for the CURATION user.
     *
     * @param aBratAnnotatorModel
     *            the model.
     * @param aDocument
     *            the source document.
     * @param jCases
     *            the JCases.
     * @param randomAnnotationDocument
     *            an annotation document.
     * @return the JCas.
     * @throws UIMAException
     *             hum?
     * @throws ClassNotFoundException
     *             hum?
     * @throws IOException
     *             if an I/O error occurs.
     * @throws BratAnnotationException
     *             hum?
     */
    public JCas getMergeCas(BratAnnotatorModel aBratAnnotatorModel, SourceDocument aDocument,
            Map<String, JCas> jCases, AnnotationDocument randomAnnotationDocument)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        JCas mergeJCas = null;
        try {
            if (aBratAnnotatorModel.getMode().equals(Mode.AUTOMATION)
                    || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION)) {
                // Upgrading should be an explicit action during the opening of a document at the
                // end
                // of the open dialog - it must not happen during editing because the CAS addresses
                // are used as IDs in the UI
                // repository.upgradeCasAndSave(aDocument, aBratAnnotatorModel.getMode(),
                // aBratAnnotatorModel.getUser().getUsername());
                mergeJCas = repository.readCorrectionCas(aDocument);
            }
            else {
                // Upgrading should be an explicit action during the opening of a document at the
                // end
                // of the open dialog - it must not happen during editing because the CAS addresses
                // are used as IDs in the UI
                // repository.upgradeCasAndSave(aDocument, aBratAnnotatorModel.getMode(),
                // aBratAnnotatorModel.getUser().getUsername());
                mergeJCas = repository.readCurationCas(aDocument);
            }
        }
        // Create jcas, if it could not be loaded from the file system
        catch (Exception e) {

            if (aBratAnnotatorModel.getMode().equals(Mode.AUTOMATION)
                    || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION)) {
                mergeJCas = createCorrectionCas(mergeJCas, aBratAnnotatorModel,
                        randomAnnotationDocument);
            }
            else {
                mergeJCas = createCurationCas(aBratAnnotatorModel.getProject(),
                        randomAnnotationDocument, jCases, aBratAnnotatorModel.getAnnotationLayers());
            }
        }
        return mergeJCas;
    }

    /**
     * Puts JCases into a list and get a random annotation document that will be used as a base for
     * the diff.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws UIMAException
     */
    private void updateSegment(BratAnnotatorModel aBratAnnotatorModel,
            Map<Integer, Integer> segmentBeginEnd, Map<Integer, Integer> segmentNumber,
            Map<String, Map<Integer, Integer>> segmentAdress, JCas jCas, String username,
            int aWinSize)
        throws UIMAException, ClassNotFoundException, IOException
    {
        Sentence firstSentence = selectSentenceAt(jCas,
                aBratAnnotatorModel.getSentenceBeginOffset(),
                aBratAnnotatorModel.getSentenceEndOffset());
        Sentence lastSentence = selectByAddr(jCas, Sentence.class,
                getLastSentenceAddressInDisplayWindow(jCas, getAddr(firstSentence), aWinSize));

        begin = firstSentence.getBegin();
        end = lastSentence.getEnd();
        sentenceNumber = getFirstSentenceNumber(jCas, getAddr(firstSentence));
        segmentAdress.put(username, new HashMap<Integer, Integer>());

        for (Sentence sentence : selectCovered(jCas, Sentence.class, begin, end)) {
            sentenceNumber += 1;
            segmentBeginEnd.put(sentence.getBegin(), sentence.getEnd());
            segmentNumber.put(sentence.getBegin(), sentenceNumber);
            segmentAdress.get(username).put(sentence.getBegin(), getAddr(sentence));
        }

        /*
         * if (segmentBeginEnd.isEmpty()) { for (Sentence sentence : selectCovered(mergeJCas,
         * Sentence.class, begin, end)) {
         *
         * } }
         */
    }

    public static List<Type> getEntryTypes(JCas mergeJCas, List<AnnotationLayer> aLayers,
            AnnotationService aAnnotationService)
    {
        List<Type> entryTypes = new LinkedList<Type>();

        for (AnnotationLayer layer : aLayers) {
            if (layer.getName().equals(Token.class.getName())) {
                continue;
            }
            if (layer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                continue;
            }
            entryTypes.add(getAdapter(aAnnotationService, layer).getAnnotationType(
                    mergeJCas.getCas()));
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
     * @param jCases
     *            the JCases
     * @param aAnnotationLayers
     *            the layers.
     * @return the JCas.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public JCas createCurationCas(Project aProject, AnnotationDocument randomAnnotationDocument,
            Map<String, JCas> jCases, List<AnnotationLayer> aAnnotationLayers)
        throws IOException
    {
        User userLoggedIn = userRepository.get(SecurityContextHolder.getContext()
                .getAuthentication().getName());

        JCas mergeJCas = repository.readAnnotationCas(randomAnnotationDocument);
        jCases.put(CurationPanel.CURATION_USER, mergeJCas);

        List<Type> entryTypes = getEntryTypes(mergeJCas, aAnnotationLayers, annotationService);

        DiffResult diff = CasDiff2.doDiffSingle(annotationService, aProject, entryTypes,
                LinkCompareBehavior.LINK_ROLE_AS_LABEL, jCases, 0, mergeJCas.getDocumentText()
                        .length());

        for (Entry<Position, ConfigurationSet> diffEntry : diff.getDifferingConfigurationSets()
                .entrySet()) {
            // Remove FSes with differences from the merge CAS
            List<Configuration> cfgsForCurationUser = diffEntry.getValue().getConfigurations(
                    CurationPanel.CURATION_USER);
            for (Configuration cfg : cfgsForCurationUser) {
                FeatureStructure fs = cfg.getFs(CurationPanel.CURATION_USER, jCases);
                mergeJCas.removeFsFromIndexes(fs);
            }
        }

        repository
                .writeCurationCas(mergeJCas, randomAnnotationDocument.getDocument(), userLoggedIn);
        return mergeJCas;
    }

    private JCas createCorrectionCas(JCas mergeJCas, BratAnnotatorModel aBratAnnotatorModel,
            AnnotationDocument randomAnnotationDocument)
        throws UIMAException, ClassNotFoundException, IOException
    {
        User userLoggedIn = userRepository.get(SecurityContextHolder.getContext()
                .getAuthentication().getName());
        mergeJCas = repository.readAnnotationCas(aBratAnnotatorModel.getDocument(), userLoggedIn);
        repository.writeCorrectionCas(mergeJCas, randomAnnotationDocument.getDocument(),
                userLoggedIn);
        return mergeJCas;
    }
}
