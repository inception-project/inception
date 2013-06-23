/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model;

import static org.uimafit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.springframework.security.core.context.SecurityContextHolder;
import org.uimafit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationOption;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.CurationPanel;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

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

    private RepositoryService repository;

    private final static Log LOG = LogFactory.getLog(CurationPanel.class);
    int sentenceNumber;
    int begin, end;

    public CurationBuilder(RepositoryService repository)
    {
        this.repository = repository;
    }

    public CurationContainer buildCurationContainer(BratAnnotatorModel aBratAnnotatorModel)
    {
        CurationContainer curationContainer = new CurationContainer();
        // initialize Variables
        Project project = aBratAnnotatorModel.getProject();
        SourceDocument sourceDocument = aBratAnnotatorModel.getDocument();
        Map<Integer, Integer> segmentBeginEnd = new HashMap<Integer, Integer>();
        Map<Integer, Integer> segmentNumber = new HashMap<Integer, Integer>();
        Map<Integer, String> segmentText = new HashMap<Integer, String>();
        Map<String, Map<Integer, Integer>> segmentAdress = new HashMap<String, Map<Integer, Integer>>();
        // get annotation documents
        List<AnnotationDocument> annotationDocuments = repository.listAnnotationDocument(project,
                sourceDocument);

        Map<String, JCas> jCases = new HashMap<String, JCas>();
        AnnotationDocument randomAnnotationDocument = null;
        for (AnnotationDocument annotationDocument : annotationDocuments) {
            String username = annotationDocument.getUser();
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                try {
                    JCas jCas = repository.getAnnotationDocumentContent(annotationDocument);

                    if (randomAnnotationDocument == null) {
                        randomAnnotationDocument = annotationDocument;
                    }

                    int windowSize = aBratAnnotatorModel.getWindowSize();

                    begin = BratAjaxCasUtil.getAnnotationBeginOffset(jCas,
                            aBratAnnotatorModel.getSentenceAddress());
                    end = BratAjaxCasUtil.getAnnotationEndOffset(jCas, BratAjaxCasUtil
                            .getLastSentenceAddressInDisplayWindow(jCas,
                                    aBratAnnotatorModel.getSentenceAddress(), windowSize));
                    sentenceNumber = BratAjaxCasUtil.getSentenceNumber(jCas,
                            aBratAnnotatorModel.getSentenceAddress());
                    segmentAdress.put(username, new HashMap<Integer, Integer>());

                    int i = aBratAnnotatorModel.getSentenceAddress();
                    Sentence sentence = null;

                    for (int j = 0; j < aBratAnnotatorModel.getWindowSize(); j++) {
                        if (i >= aBratAnnotatorModel.getLastSentenceAddress()) {
                            sentence = (Sentence) jCas.getLowLevelCas().ll_getFSForRef(i);
                            sentenceNumber += 1;
                            segmentBeginEnd.put(sentence.getBegin(), sentence.getEnd());
                            segmentText.put(sentence.getBegin(), sentence.getCoveredText()
                                    .toString());
                            segmentNumber.put(sentence.getBegin(), sentenceNumber);
                            segmentAdress.get(username).put(sentence.getBegin(),
                                    sentence.getAddress());
                            break;
                        }
                        sentence = (Sentence) jCas.getLowLevelCas().ll_getFSForRef(i);
                        sentenceNumber += 1;
                        segmentBeginEnd.put(sentence.getBegin(), sentence.getEnd());
                        segmentText.put(sentence.getBegin(), sentence.getCoveredText().toString());
                        segmentNumber.put(sentence.getBegin(), sentenceNumber);
                        segmentAdress.get(username).put(sentence.getBegin(), sentence.getAddress());
                        i = BratAjaxCasUtil.getFollowingSentenceAddress(jCas, i);
                    }

                    jCases.put(username, jCas);
                }
                catch (Exception e) {
                    LOG.info("Skipping document due to exception [" + annotationDocument + "]", e);
                }
            }
        }

        // TODO Create pre-merged jcas if not exists for curation user

        JCas mergeJCas = null;
        try {
            mergeJCas = repository.getCurationDocumentContent(sourceDocument);
        }
        // Create jcas, if it could not be loaded from the file system
        catch (Exception e) {
            // reserve the begin/end offsets before creating re-merge
            int tempBegin = begin;
            int tempEnd = end;
            // re-merge JCAS for all sentences
            begin = BratAjaxCasUtil.getAnnotationBeginOffset((JCas)new ArrayList(jCases.values()).get(0),
                    aBratAnnotatorModel.getFirstSentenceAddress());
            end = BratAjaxCasUtil.getAnnotationEndOffset((JCas)new ArrayList(jCases.values()).get(0),
                    aBratAnnotatorModel.getLastSentenceAddress());
            mergeJCas = createMergeCas(mergeJCas, repository, randomAnnotationDocument, jCases,
                    aBratAnnotatorModel);
           // restore actual begin/end offsets
            begin = tempBegin;
            end = tempEnd;
        }

        int numUsers = jCases.size();

        List<Type> entryTypes = null;

        segmentAdress.put(CurationPanel.CURATION_USER, new HashMap<Integer, Integer>());
        for (Sentence sentence : selectCovered(mergeJCas, Sentence.class, begin, end)) {
            segmentAdress.get(CurationPanel.CURATION_USER).put(sentence.getBegin(),
                    sentence.getAddress());
        }

        if (entryTypes == null) {
            entryTypes = getEntryTypes(mergeJCas, aBratAnnotatorModel);
        }

        for (Integer begin : segmentBeginEnd.keySet()) {
            Integer end = segmentBeginEnd.get(begin);

            List<AnnotationOption> annotationOptions = null;
            try {
                annotationOptions = CasDiff.doDiff(entryTypes, jCases, begin, end);
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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

            CurationSegmentForSourceDocument curationSegment = new CurationSegmentForSourceDocument();
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
            curationContainer.getCurationSegmentByBegin().put(begin, curationSegment);
        }
        return curationContainer;
    }

    public static List<Type> getEntryTypes(JCas mergeJCas, BratAnnotatorModel aBratAnnotatorModel)
    {
        List<Type> entryTypes = new LinkedList<Type>();
        // entryTypes.add(CasUtil.getType(firstJCas.getCas(), Token.class));
        // entryTypes.add(CasUtil.getType(firstJCas.getCas(), Sentence.class));
        List<String> annotationTypeNames = new LinkedList<String>();
        for (TagSet tagSet : aBratAnnotatorModel.getAnnotationLayers()) {
        	annotationTypeNames.add(tagSet.getType().getName());
        }
        if (annotationTypeNames.contains(AnnotationTypeConstant.POS)) {
        	entryTypes.add(CasUtil.getType(mergeJCas.getCas(), POS.class));
        }
        if (annotationTypeNames.contains(AnnotationTypeConstant.DEPENDENCY)
        		&& annotationTypeNames.contains(AnnotationTypeConstant.POS)) {
        	entryTypes.add(CasUtil.getType(mergeJCas.getCas(), Dependency.class));
        }
        if (annotationTypeNames.contains(AnnotationTypeConstant.NAMEDENTITY)) {
        	entryTypes.add(CasUtil.getType(mergeJCas.getCas(), NamedEntity.class));
        }
        if (annotationTypeNames.contains(AnnotationTypeConstant.COREFRELTYPE)) {
        	// TODO CasDiff does not support coreference links so far
//        	entryTypes.add(CasUtil.getType(mergeJCas.getCas(), CoreferenceLink.class));
        }
        if (annotationTypeNames.contains(AnnotationTypeConstant.COREFERENCE)
        		&& annotationTypeNames.contains(AnnotationTypeConstant.COREFRELTYPE)) {
        	// TODO CasDiff does not support coreference chains so far
//        	entryTypes.add(CasUtil.getType(mergeJCas.getCas(), CoreferenceChain.class));
        }
        return entryTypes;
    }

    /**
     * For the first time a curation page is opened, create a MergeCas that contains only agreeing
     * annotations Using the CAS of the curator user.
     */
    public JCas createMergeCas(JCas mergeJCas, RepositoryService repository,
            AnnotationDocument randomAnnotationDocument, Map<String, JCas> jCases,
            BratAnnotatorModel aBratAnnotatorModel)
    {
        List<Type> entryTypes = null;
        int numUsers = jCases.size();
        try {
            mergeJCas = repository.getAnnotationDocumentContent(randomAnnotationDocument);
        }
        catch (UIMAException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        entryTypes = getEntryTypes(mergeJCas, aBratAnnotatorModel);
        jCases.put(CurationPanel.CURATION_USER, mergeJCas);

        List<AnnotationOption> annotationOptions = null;
        try {

            annotationOptions = CasDiff.doDiff(entryTypes, jCases, begin, end);
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
                            FeatureStructure fs = mergeJCas.getLowLevelCas()
                                    .ll_getFSForRef(address);
                            if (!(fs instanceof Token)) {
                                mergeJCas.getCas().removeFsFromIndexes(fs);
                            }
                        }
                    }
                }
            }
        }

        User userLoggedIn = repository.getUser(SecurityContextHolder.getContext()
                .getAuthentication().getName());
        try {
            repository.createCurationDocumentContent(mergeJCas,
                    randomAnnotationDocument.getDocument(), userLoggedIn);
        }
        catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return mergeJCas;
    }
}
