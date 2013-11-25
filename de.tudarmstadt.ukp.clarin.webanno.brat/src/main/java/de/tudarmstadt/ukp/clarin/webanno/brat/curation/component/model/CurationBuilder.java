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

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;
import static org.uimafit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.AnnotationOption;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.CasDiffException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
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
public class CurationBuilder
{

    private final RepositoryService repository;
    int sentenceNumber;
    int begin, end;

    public CurationBuilder(RepositoryService repository)
    {
        this.repository = repository;
    }

    public CurationContainer buildCurationContainer(BratAnnotatorModel aBratAnnotatorModel)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        CurationContainer curationContainer = new CurationContainer();
        // initialize Variables
        SourceDocument sourceDocument = aBratAnnotatorModel.getDocument();
        Map<Integer, Integer> segmentBeginEnd = new HashMap<Integer, Integer>();
        Map<Integer, Integer> segmentNumber = new HashMap<Integer, Integer>();
        Map<Integer, String> segmentText = new HashMap<Integer, String>();
        Map<String, Map<Integer, Integer>> segmentAdress = new HashMap<String, Map<Integer, Integer>>();
        // get annotation documents
        List<AnnotationDocument> annotationDocuments = repository
                .listAnnotationDocuments(sourceDocument);

        Map<String, JCas> jCases = new HashMap<String, JCas>();

        AnnotationDocument randomAnnotationDocument = null;

        // get the correction JCas for the logged in user
        if (aBratAnnotatorModel.getMode().equals(Mode.CORRECTION)) {
            jCases = listJcasesforCorrection(randomAnnotationDocument, sourceDocument);
            String username = jCases.keySet().iterator().next();
            updateSegment(aBratAnnotatorModel, segmentBeginEnd, segmentNumber, segmentText,
                    segmentAdress, jCases.get(username), username);

        }
        else {

            jCases = listJcasesforCuration(annotationDocuments, randomAnnotationDocument);
            for (String username : jCases.keySet()) {
                JCas jCas = jCases.get(username);
                updateSegment(aBratAnnotatorModel, segmentBeginEnd, segmentNumber, segmentText,
                        segmentAdress, jCas, username);
            }
        }

        JCas mergeJCas = getMergeCas(aBratAnnotatorModel, sourceDocument, jCases,
                randomAnnotationDocument);

        int numUsers = jCases.size();

        List<Type> entryTypes = null;

        segmentAdress.put(CurationPanel.CURATION_USER, new HashMap<Integer, Integer>());
        for (Sentence sentence : selectCovered(mergeJCas, Sentence.class, begin, end)) {
            segmentAdress.get(CurationPanel.CURATION_USER).put(sentence.getBegin(),
                    sentence.getAddress());
        }

        if (entryTypes == null) {
            entryTypes = getEntryTypes(mergeJCas, aBratAnnotatorModel.getAnnotationLayers());
        }

        for (Integer begin : segmentBeginEnd.keySet()) {
            Integer end = segmentBeginEnd.get(begin);

            List<AnnotationOption> annotationOptions = null;
            try {
                annotationOptions = CasDiff.doDiff(entryTypes, jCases, begin, end);
            }
            catch (Exception e) {
                throw new CasDiffException(e.getMessage());
            }

            Boolean hasDiff = false;
            for (AnnotationOption annotationOption : annotationOptions) {
                List<AnnotationSelection> annotationSelections = annotationOption
                        .getAnnotationSelections();
                if (annotationSelections.size() > 1) {
                    hasDiff = true;
                }
                else if (annotationSelections.size() == 1) {
                    AnnotationSelection annotationSelection = annotationSelections.get(0);
                    if (annotationSelection.getAddressByUsername().size() < numUsers) {
                        hasDiff = true;
                    }
                }
            }

            CurationViewForSourceDocument curationSegment = new CurationViewForSourceDocument();
            curationSegment.setBegin(begin);
            curationSegment.setEnd(end);
            if (hasDiff) {
                curationSegment.setSentenceState(SentenceState.DISAGREE);
            }
            else {
                curationSegment.setSentenceState(SentenceState.AGREE);
            }
            curationSegment.setText(segmentText.get(begin));
            curationSegment.setSentenceNumber(segmentNumber.get(begin));

            for (String username : segmentAdress.keySet()) {
                curationSegment.getSentenceAddress().put(username,
                        segmentAdress.get(username).get(begin));
            }
            curationContainer.getCurationViewByBegin().put(begin, curationSegment);
        }
        return curationContainer;
    }

    public Map<String, JCas> listJcasesforCorrection(AnnotationDocument randomAnnotationDocument,
            SourceDocument aDocument)
        throws UIMAException, ClassNotFoundException, IOException
    {
        Map<String, JCas> jCases = new HashMap<String, JCas>();
        User user = repository.getUser(SecurityContextHolder.getContext().getAuthentication()
                .getName());
        randomAnnotationDocument = repository.getAnnotationDocument(aDocument, user);

        JCas jCas = repository.getAnnotationDocumentContent(randomAnnotationDocument);
        jCases.put(user.getUsername(), jCas);
        return jCases;
    }

    public Map<String, JCas> listJcasesforCuration(List<AnnotationDocument> annotationDocuments,
            AnnotationDocument randomAnnotationDocument)
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
            JCas jCas = repository.getAnnotationDocumentContent(annotationDocument);
            jCases.put(username, jCas);
        }
        return jCases;
    }

    public JCas getMergeCas(BratAnnotatorModel aBratAnnotatorModel, SourceDocument sourceDocument,
            Map<String, JCas> jCases, AnnotationDocument randomAnnotationDocument)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        JCas mergeJCas = null;
        try {
            if (aBratAnnotatorModel.getMode().equals(Mode.CORRECTION)) {
                mergeJCas = repository.getCorrectionDocumentContent(sourceDocument);
            }
            else {
                mergeJCas = repository.getCurationDocumentContent(sourceDocument);
            }
        }
        // Create jcas, if it could not be loaded from the file system
        catch (Exception e) {

            if (aBratAnnotatorModel.getMode().equals(Mode.CORRECTION)) {
                mergeJCas = createCorrectionCas(mergeJCas, aBratAnnotatorModel,
                        randomAnnotationDocument);
            }
            else {
                mergeJCas = createMergeCas(mergeJCas, randomAnnotationDocument, jCases,
                        aBratAnnotatorModel);
            }
        }
        return mergeJCas;
    }

    /**
     * Puts JCases into a list and get a random annotation document that will be used as a base for
     * the {@link CasDiff}
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws UIMAException
     */
    private void updateSegment(BratAnnotatorModel aBratAnnotatorModel,
            Map<Integer, Integer> segmentBeginEnd, Map<Integer, Integer> segmentNumber,
            Map<Integer, String> segmentText, Map<String, Map<Integer, Integer>> segmentAdress,
            JCas jCas, String username)
        throws UIMAException, ClassNotFoundException, IOException
    {

        int windowSize = aBratAnnotatorModel.getWindowSize();

        Sentence firstSentence = BratAjaxCasUtil.selectSentenceAt(jCas,
                aBratAnnotatorModel.getSentenceBeginOffset(),
                aBratAnnotatorModel.getSentenceEndOffset());
        Sentence lastSentence = selectByAddr(
                jCas,
                Sentence.class,
                BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(jCas,
                        firstSentence.getAddress(), windowSize));

        begin = firstSentence.getBegin();
        end = lastSentence.getEnd();
        sentenceNumber = BratAjaxCasUtil.getFirstSentenceNumber(jCas, firstSentence.getAddress());
        segmentAdress.put(username, new HashMap<Integer, Integer>());

        for (Sentence sentence : selectCovered(jCas, Sentence.class, begin, end)) {
            sentenceNumber += 1;
            segmentBeginEnd.put(sentence.getBegin(), sentence.getEnd());
            segmentText.put(sentence.getBegin(), sentence.getCoveredText().toString());
            segmentNumber.put(sentence.getBegin(), sentenceNumber);
            segmentAdress.get(username).put(sentence.getBegin(), sentence.getAddress());
        }
    }

    public static List<Type> getEntryTypes(JCas mergeJCas, Set<TagSet> aTagSets)
    {
        List<Type> entryTypes = new LinkedList<Type>();

        for (TagSet tagSet : aTagSets) {
            if (tagSet.getType().getName().equals(AnnotationTypeConstant.COREFERENCE)
                    || tagSet.getType().getName().equals(AnnotationTypeConstant.COREFRELTYPE)) {
                continue;
            }
            entryTypes.add(getAdapter(tagSet.getType()).getAnnotationType(mergeJCas.getCas()));
        }
        return entryTypes;
    }

    /**
     * For the first time a curation page is opened, create a MergeCas that contains only agreeing
     * annotations Using the CAS of the curator user.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws UIMAException
     * @throws BratAnnotationException
     */
    public JCas createMergeCas(JCas mergeJCas, AnnotationDocument randomAnnotationDocument,
            Map<String, JCas> jCases, BratAnnotatorModel aBratAnnotatorModel)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        User userLoggedIn = repository.getUser(SecurityContextHolder.getContext()
                .getAuthentication().getName());

        List<Type> entryTypes = null;
        int numUsers = jCases.size();
        mergeJCas = repository.getAnnotationDocumentContent(randomAnnotationDocument);

        entryTypes = getEntryTypes(mergeJCas, aBratAnnotatorModel.getAnnotationLayers());
        jCases.put(CurationPanel.CURATION_USER, mergeJCas);

        List<AnnotationOption> annotationOptions = null;

        annotationOptions = CasDiff.doDiff(entryTypes, jCases, begin, end);
        for (AnnotationOption annotationOption : annotationOptions) {
            // remove the featureStructure if more than 1 annotationSelection exists per
            // annotationOption
            boolean removeFS = annotationOption.getAnnotationSelections().size() > 1;
            if (annotationOption.getAnnotationSelections().size() == 1) {
                removeFS = annotationOption.getAnnotationSelections().get(0).getAddressByUsername()
                        .size() <= numUsers;
            }
            for (AnnotationSelection annotationSelection : annotationOption
                    .getAnnotationSelections()) {
                for (String username : annotationSelection.getAddressByUsername().keySet()) {
                    if (username.equals(CurationPanel.CURATION_USER)) {
                        Integer address = annotationSelection.getAddressByUsername().get(username);

                        // removing disagreeing feature structures in mergeJCas
                        if (removeFS && address != null) {
                            FeatureStructure fs = selectByAddr(mergeJCas, address);
                            if (!(fs instanceof Token)) {
                                mergeJCas.getCas().removeFsFromIndexes(fs);
                            }
                        }
                    }
                }
            }
        }

        repository.createCurationDocumentContent(mergeJCas, randomAnnotationDocument.getDocument(),
                userLoggedIn);
        return mergeJCas;
    }

    private JCas createCorrectionCas(JCas mergeJCas, BratAnnotatorModel aBratAnnotatorModel,
            AnnotationDocument randomAnnotationDocument)
        throws UIMAException, ClassNotFoundException, IOException
    {
        User userLoggedIn = repository.getUser(SecurityContextHolder.getContext()
                .getAuthentication().getName());
        mergeJCas = repository.readJCas(aBratAnnotatorModel.getDocument(), aBratAnnotatorModel
                .getDocument().getProject(), userLoggedIn);
        repository.createCorrectionDocumentContent(mergeJCas,
                randomAnnotationDocument.getDocument(), userLoggedIn);
        return mergeJCas;
    }
}
