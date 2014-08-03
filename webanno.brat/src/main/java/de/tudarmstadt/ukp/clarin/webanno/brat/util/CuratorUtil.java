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
package de.tudarmstadt.ukp.clarin.webanno.brat.util;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.beans.BeansException;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.AnnotationOption;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.CurationViewPanel;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.CurationBuilder;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model.CurationViewForSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.project.ProjectUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A utility class for the curation AND Correction modules
 *
 * @author Seid Muhie Yimam
 */
public class CuratorUtil
{
    public final static String CURATION_USER = "CURATION_USER";

    /**
     * Get JCAS objects of annotator where {@link CasDiff} will run on it
     */
    public static void getCases(Map<String, JCas> aJCases,
            List<AnnotationDocument> aAnnotationDocuments, RepositoryService aRepository,
            Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress)
        throws UIMAException, ClassNotFoundException, IOException
    {
        for (AnnotationDocument annotationDocument : aAnnotationDocuments) {
            String username = annotationDocument.getUser();
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)
                    || username.equals(CURATION_USER)) {
                JCas jCas = aRepository.getAnnotationDocumentContent(annotationDocument);
                aJCases.put(username, jCas);

                // cleanup annotationSelections
                annotationSelectionByUsernameAndAddress.put(username,
                        new HashMap<Integer, AnnotationSelection>());
            }
        }
    }

    /**
     * Set different attributes for {@link BratAnnotatorModel} that will be used for the
     * {@link CurationViewForSourceDocument}
     */
    public static BratAnnotatorModel setBratAnnotatorModel(SourceDocument aSourceDocument,
            RepositoryService aRepository, CurationViewForSourceDocument aCurationSegment,
            AnnotationService aAnnotationService)
        throws BeansException, FileNotFoundException, IOException
    {
        User userLoggedIn = aRepository.getUser(SecurityContextHolder.getContext()
                .getAuthentication().getName());
        BratAnnotatorModel bratAnnotatorModel = new BratAnnotatorModel();// .getModelObject();
        bratAnnotatorModel.setDocument(aSourceDocument);
        bratAnnotatorModel.setProject(aSourceDocument.getProject());
        bratAnnotatorModel.setUser(userLoggedIn);
        bratAnnotatorModel.setFirstSentenceAddress(aCurationSegment.getSentenceAddress().get(
                CURATION_USER));
        bratAnnotatorModel.setLastSentenceAddress(aCurationSegment.getSentenceAddress().get(
                CURATION_USER));
        bratAnnotatorModel.setSentenceAddress(aCurationSegment.getSentenceAddress().get(
                CURATION_USER));

        bratAnnotatorModel.setSentenceBeginOffset(aCurationSegment.getBegin());
        bratAnnotatorModel.setSentenceEndOffset(aCurationSegment.getEnd());

        bratAnnotatorModel.setMode(Mode.CURATION);
        ProjectUtil.setAnnotationPreference(userLoggedIn.getUsername(), aRepository,
                aAnnotationService, bratAnnotatorModel, Mode.CURATION);
        return bratAnnotatorModel;
    }

    public static void fillLookupVariables(
            List<AnnotationOption> aAnnotationOptions,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            BratAnnotatorModel bratAnnotatorModel)
    {
        // fill lookup variable for annotation selections
        for (AnnotationOption annotationOption : aAnnotationOptions) {
            for (AnnotationSelection annotationSelection : annotationOption
                    .getAnnotationSelections()) {
                for (String username : annotationSelection.getAddressByUsername().keySet()) {
                    if ((!username.equals(CURATION_USER) && bratAnnotatorModel.getMode().equals(
                            Mode.CURATION))
                            || (username.equals(CURATION_USER) && (bratAnnotatorModel.getMode()
                                    .equals(Mode.AUTOMATION) || bratAnnotatorModel.getMode()
                                    .equals(Mode.CORRECTION)))) {
                        Integer address = annotationSelection.getAddressByUsername().get(username);
                        // aAnnotationSelectionByUsernameAndAddress.put(username,
                        // new
                        // HashMap<Integer, AnnotationSelection>());
                        aAnnotationSelectionByUsernameAndAddress.get(username).put(address,
                                annotationSelection);
                    }
                }
            }
        }
    }

    public static void populateCurationSentences(
            Map<String, JCas> aJCases,
            List<CurationUserSegmentForAnnotationDocument> aSentences,
            BratAnnotatorModel aBratAnnotatorModel,
            final List<AnnotationOption> aAnnotationOptions,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            MappingJacksonHttpMessageConverter aJsonConverter,
            AnnotationService aAnnotationService, CurationContainer aCurationContainer)
        throws IOException
    {
        List<String> usernamesSorted = new ArrayList<String>(aJCases.keySet());
        Collections.sort(usernamesSorted);
        final int numUsers = usernamesSorted.size();

        final Mode mode = aBratAnnotatorModel.getMode();
        boolean isAutomationMode = mode.equals(Mode.AUTOMATION);
        boolean isCorrectionMode = mode.equals(Mode.CORRECTION);
        boolean isCurationMode = mode.equals(Mode.CURATION);

        for (String username : usernamesSorted) {
            if (
                    (!username.equals(CURATION_USER) && isCurationMode) ||
                    (username.equals(CURATION_USER) && (isAutomationMode || isCorrectionMode))
            ) {
                // WTF?
                final Map<Integer, AnnotationSelection> annotationSelectionByAddress = new HashMap<Integer, AnnotationSelection>();
                for (AnnotationOption annotationOption : aAnnotationOptions) {
                    for (AnnotationSelection annotationSelection : annotationOption
                            .getAnnotationSelections()) {
                        if (annotationSelection.getAddressByUsername().containsKey(username)) {
                            Integer address = annotationSelection.getAddressByUsername().get(
                                    username);
                            annotationSelectionByAddress.put(address, annotationSelection);
                        }
                    }
                }

                JCas jCas = aJCases.get(username);

                JCas userJCas = null;
                if (isAutomationMode || isCorrectionMode) {
                    userJCas = aJCases.get(SecurityContextHolder.getContext().getAuthentication()
                            .getName());
                }
                else if (isCurationMode) {
                    userJCas = aJCases.get(CURATION_USER);
                }

                // Save window location (WTF?!)
                int sentenceAddress = aBratAnnotatorModel.getSentenceAddress();
                int lastSentenceAddress = aBratAnnotatorModel.getLastSentenceAddress();

                // Override window location
                aBratAnnotatorModel.setSentenceAddress(getSentenceAddress(aBratAnnotatorModel,
                        jCas, userJCas));
                aBratAnnotatorModel.setLastSentenceAddress(getLastSentenceAddress(
                        aBratAnnotatorModel, jCas, userJCas));

                // Set up coloring strategy
                ColoringStrategy curationColoringStrategy = new ColoringStrategy()
                {
                    @Override
                    public String getColor(FeatureStructure aFS, String aLabel)
                    {
                          int address = BratAjaxCasUtil.getAddr(aFS);
                          AnnotationSelection annotationSelection = annotationSelectionByAddress.get(address);
                          AnnotationState newState = null;
                          if (mode.equals(Mode.AUTOMATION) || mode.equals(Mode.CORRECTION)) {
                              newState = getCorrectionState(annotationSelection, aAnnotationOptions, numUsers,
                                      address);
                          }
                          else {
                              newState = getCurationState(numUsers, annotationSelection);
                          }

                        return newState.getColorCode();
                    }
                };

                // Create curation view for the current user
                CurationUserSegmentForAnnotationDocument curationUserSegment2 = new CurationUserSegmentForAnnotationDocument();
                curationUserSegment2.setCollectionData(getCollectionInformation(aJsonConverter,
                        aAnnotationService, aCurationContainer));
                curationUserSegment2.setDocumentResponse(render(jCas, aAnnotationService,
                        aBratAnnotatorModel, aJsonConverter, curationColoringStrategy));
                curationUserSegment2.setUsername(username);
                curationUserSegment2.setBratAnnotatorModel(aBratAnnotatorModel);
                curationUserSegment2
                        .setAnnotationSelectionByUsernameAndAddress(aAnnotationSelectionByUsernameAndAddress);
                aSentences.add(curationUserSegment2);

                // Restore window location
                aBratAnnotatorModel.setSentenceAddress(sentenceAddress);
                aBratAnnotatorModel.setLastSentenceAddress(lastSentenceAddress);
            }
        }
    }

    /**
     * Get the sentence address for jCas from userJCas.
     */
    private static int getSentenceAddress(BratAnnotatorModel aBratAnnotatorModel, JCas jCas,
            JCas userJCas)
    {
        int sentenceAddress = BratAjaxCasUtil.selectSentenceAt(userJCas,
                aBratAnnotatorModel.getSentenceBeginOffset(),
                aBratAnnotatorModel.getSentenceEndOffset()).getAddress();
        Sentence sentence = selectByAddr(userJCas, Sentence.class, sentenceAddress);
        List<Sentence> sentences = JCasUtil.selectCovered(jCas, Sentence.class,
                sentence.getBegin(), sentence.getEnd());
        return sentences.get(0).getAddress();
    }

    private static int getLastSentenceAddress(BratAnnotatorModel aBratAnnotatorModel, JCas jCas,
            JCas userJCas)
    {
        Sentence sentence = selectByAddr(userJCas, Sentence.class,
                aBratAnnotatorModel.getLastSentenceAddress());
        List<Sentence> sentences = JCasUtil.selectCovered(jCas, Sentence.class,
                sentence.getBegin(), sentence.getEnd());
        return sentences.get(0).getAddress();
    }

    private static String render(JCas aJcas,
            AnnotationService aAnnotationService,
            BratAnnotatorModel aBratAnnotatorModel,
            MappingJacksonHttpMessageConverter aJsonConverter,
            ColoringStrategy aCurationColoringStrategy)
        throws IOException
    {
        GetDocumentResponse response = new GetDocumentResponse();

        // Render invisible baseline annotations (sentence, tokens)
        SpanAdapter.renderTokenAndSentence(aJcas, response, aBratAnnotatorModel);

        // Render visible (custom) layers
        for (AnnotationLayer layer : aBratAnnotatorModel.getAnnotationLayers()) {
            if (
                    layer.getName().equals(Token.class.getName()) ||
                    layer.getName().equals(Sentence.class.getName())
            ) {
                continue;
            }

            List<AnnotationFeature> features = aAnnotationService.listAnnotationFeature(layer);
            List<AnnotationFeature> invisibleFeatures = new ArrayList<AnnotationFeature>();
            for(AnnotationFeature feature:features){
                if(!feature.isVisible()){
                    invisibleFeatures.add(feature);
                }
            }
            features.removeAll(invisibleFeatures);
            TypeAdapter adapter = getAdapter(layer);
            adapter.render(aJcas, features, response, aBratAnnotatorModel,
                    aCurationColoringStrategy);
        }

        StringWriter out = new StringWriter();
        JsonGenerator jsonGenerator = aJsonConverter.getObjectMapper().getJsonFactory()
                .createJsonGenerator(out);
        jsonGenerator.writeObject(response);
        return out.toString();
    }

    private static String getCollectionInformation(
            MappingJacksonHttpMessageConverter aJsonConverter,
            AnnotationService aAnnotationService, CurationContainer aCurationContainer)
        throws IOException
    {
        GetCollectionInformationResponse info = new GetCollectionInformationResponse();
        info.setEntityTypes(BratAjaxConfiguration.buildEntityTypes(aCurationContainer
                .getBratAnnotatorModel().getAnnotationLayers(), aAnnotationService));

        StringWriter out = new StringWriter();
        JsonGenerator jsonGenerator = aJsonConverter.getObjectMapper().getJsonFactory()
                .createJsonGenerator(out);
        jsonGenerator.writeObject(info);
        return out.toString();
    }

    private static AnnotationState getCurationState(int numUsers,
            AnnotationSelection annotationSelection)
    {
        AnnotationState newState;
        if (annotationSelection == null) {
            newState = AnnotationState.AGREE;

        }
        else if (annotationSelection.getAddressByUsername().size() == numUsers) {
            newState = AnnotationState.AGREE;

        }
        else if (annotationSelection.getAddressByUsername().containsKey(CURATION_USER)) {
            newState = AnnotationState.USE;

        }
        else {
            boolean doNotUse = false;
            for (AnnotationSelection otherAnnotationSelection : annotationSelection
                    .getAnnotationOption().getAnnotationSelections()) {
                if (otherAnnotationSelection.getAddressByUsername().containsKey(CURATION_USER)) {
                    doNotUse = true;
                    break;
                }
            }
            if (doNotUse) {
                newState = AnnotationState.DO_NOT_USE;
            }
            else {
                newState = AnnotationState.DISAGREE;

            }
        }
        return newState;
    }

    private static AnnotationState getCorrectionState(AnnotationSelection annotationSelection,
            List<AnnotationOption> aAnnotationOptions, int numUsers, int address)
    {
        AnnotationOption annotationOption = null;

        for (AnnotationOption annotationOption2 : aAnnotationOptions) {
            for (AnnotationSelection annotationSelection2 : annotationOption2
                    .getAnnotationSelections()) {
                if (annotationSelection2.getAddressByUsername().containsKey(CURATION_USER)
                        && annotationSelection2.getAddressByUsername().get(CURATION_USER).equals(address)) {
                    annotationOption = annotationOption2;
                    break;
                }

            }
        }
        AnnotationState newState = null;
        if (annotationSelection == null) {
            newState = AnnotationState.NOT_SUPPORTED;

        }
        else if (annotationSelection.getAddressByUsername().size() == numUsers) {
            newState = AnnotationState.AGREE;

        }
        else if (annotationOption.getAnnotationSelections().size() == 1) {
            newState = AnnotationState.DISAGREE;
        }
        else {
            newState = AnnotationState.DO_NOT_USE;
        }
        return newState;
    }

    public static void updatePanel(
            AjaxRequestTarget aTarget,
            CurationViewPanel aParent,
            CurationContainer aCurationContainer,
            BratAnnotator aMergeVisualizer,
            RepositoryService aRepository,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            CurationViewForSourceDocument aCurationSegment, AnnotationService aAnnotationService,
            MappingJacksonHttpMessageConverter aJsonConverter)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {
        SourceDocument sourceDocument = aCurationContainer.getBratAnnotatorModel().getDocument();
        List<AnnotationDocument> annotationDocuments = aRepository
                .listAnnotationDocuments(sourceDocument);
        Map<String, JCas> jCases = new HashMap<String, JCas>();
        JCas annotatorCas = null;
        // this is a CORRECTION project
        if (aCurationContainer.getBratAnnotatorModel().getMode().equals(Mode.AUTOMATION)
                || aCurationContainer.getBratAnnotatorModel().getMode().equals(Mode.CORRECTION)) {
            annotatorCas = aRepository.getCorrectionDocumentContent(sourceDocument);

            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            User user = aRepository.getUser(username);
            AnnotationDocument annotationDocument = aRepository.getAnnotationDocument(
                    sourceDocument, user);
            jCases.put(user.getUsername(),
                    aRepository.getAnnotationDocumentContent(annotationDocument));
            aAnnotationSelectionByUsernameAndAddress.put(CURATION_USER,
                    new HashMap<Integer, AnnotationSelection>());
        }
        else {
            annotatorCas = aRepository.getCurationDocumentContent(sourceDocument);
            // get cases from repository
            CuratorUtil.getCases(jCases, annotationDocuments, aRepository,
                    aAnnotationSelectionByUsernameAndAddress);
        }
        // add mergeJCas separately
        jCases.put(CURATION_USER, annotatorCas);

        // get differing feature structures
        List<Type> entryTypes = CurationBuilder.getEntryTypes(annotatorCas, aCurationContainer
                .getBratAnnotatorModel().getAnnotationLayers());
        List<AnnotationOption> annotationOptions = null;
        try {
            annotationOptions = CasDiff.doDiff(entryTypes, jCases, aCurationSegment.getBegin(),
                    aCurationSegment.getEnd());
        }
        catch (Exception e) {
            throw new CasDiffException(e.getMessage());
        }

        // fill lookup variable for annotation selections
        CuratorUtil.fillLookupVariables(annotationOptions,
                aAnnotationSelectionByUsernameAndAddress,
                aCurationContainer.getBratAnnotatorModel());

        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();

        BratAnnotatorModel bratAnnotatorModel = null;
        if (!(aCurationContainer.getBratAnnotatorModel().getMode().equals(Mode.AUTOMATION) || aCurationContainer
                .getBratAnnotatorModel().getMode().equals(Mode.CORRECTION))) {
            // update sentence address, offsets,... per sentence/per user in the curation view
            bratAnnotatorModel = CuratorUtil.setBratAnnotatorModel(sourceDocument,
                    aRepository, aCurationSegment, aAnnotationService);
        }
        else {
            bratAnnotatorModel = aCurationContainer.getBratAnnotatorModel();
        }

        CuratorUtil.populateCurationSentences(jCases, sentences, bratAnnotatorModel,
                annotationOptions, aAnnotationSelectionByUsernameAndAddress, aJsonConverter, aAnnotationService, aCurationContainer);
        // update sentence list on the right side
        aParent.setModelObject(sentences);
        if (aCurationContainer.getBratAnnotatorModel().getMode().equals(Mode.CURATION)) {
            aMergeVisualizer.setModelObject(bratAnnotatorModel);
            aMergeVisualizer.reloadContent(aTarget);
        }
        aTarget.add(aParent);
    }
}
