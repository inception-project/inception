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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.service;

import static de.tudarmstadt.ukp.clarin.webanno.brat.adapter.TypeUtil.getAdapter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.core.JsonGenerator;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.brat.adapter.TypeRenderer;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratRenderer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.SuggestionViewPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SourceListView;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.service.CasDiff2.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.service.CasDiff2.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.service.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.service.CasDiff2.LinkCompareBehavior;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A utility class for the curation AND Correction modules
 *
 */
public class CuratorUtil
{
    private static final Log LOG = LogFactory.getLog(CuratorUtil.class);

    public final static String CURATION_USER = "CURATION_USER";

    /**
     * Get JCAS objects of annotator where diff will run on it
     *
     * @param aJCases
     *            the JCases.
     * @param aAnnotationDocuments
     *            the annotation documents.
     * @param aRepository
     *            the repository.
     * @param annotationSelectionByUsernameAndAddress
     *            selections by user.
     * @throws UIMAException
     *             hum?
     * @throws ClassNotFoundException
     *             hum?
     * @throws IOException
     *             if an I/O error occurs.
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
                JCas jCas = aRepository.readAnnotationCas(annotationDocument);
                aJCases.put(username, jCas);

                // cleanup annotationSelections
                annotationSelectionByUsernameAndAddress.put(username,
                        new HashMap<Integer, AnnotationSelection>());
            }
        }
    }

    public static void populateCurationSentences(
            Map<String, JCas> aJCases,
            List<CurationUserSegmentForAnnotationDocument> aSentences,
            AnnotatorStateImpl aBratAnnotatorModel,
            final List<AnnotationOption> aAnnotationOptions,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            AnnotationService aAnnotationService, CurationContainer aCurationContainer,
            final Map<String, AnnotationState> aStates)
        throws IOException
    {
        List<String> usernamesSorted = new ArrayList<String>(aJCases.keySet());
        Collections.sort(usernamesSorted);

        final Mode mode = aBratAnnotatorModel.getMode();
        boolean isAutomationMode = mode.equals(Mode.AUTOMATION);
        boolean isCorrectionMode = mode.equals(Mode.CORRECTION);
        boolean isCurationMode = mode.equals(Mode.CURATION);

        String annotatorCasUser;
        switch (mode) {
        case AUTOMATION: // fall-through
        case CORRECTION:
            annotatorCasUser = SecurityContextHolder.getContext().getAuthentication().getName();
            break;
        case CURATION:
            annotatorCasUser = CURATION_USER;
            break;
        default:
            throw new IllegalStateException("Illegal mode [" + mode + "]");
        }

        LOG.debug("mode = [" + mode + "]");
        LOG.debug("all users is  " + usernamesSorted);
        LOG.debug("annotator CAS is for user [" + annotatorCasUser + "]");

        for (String username : usernamesSorted) {
            if ((!username.equals(CURATION_USER) && isCurationMode)
                    || (username.equals(CURATION_USER) && (isAutomationMode || isCorrectionMode))) {

                JCas jCas = aJCases.get(username);
                // Set up coloring strategy
                ColoringStrategy curationColoringStrategy = new ColoringStrategy()
                {
                    @Override
                    public String getColor(Object aObj, String aLabel)
                    {
                        if (aStates.get(aObj.toString())==null){
                            return AnnotationState.NOT_SUPPORTED.getColorCode();
                        }
                        return aStates.get(aObj.toString()).getColorCode();
                    }
                };

                // Create curation view for the current user
                CurationUserSegmentForAnnotationDocument curationUserSegment2 = new CurationUserSegmentForAnnotationDocument();
                curationUserSegment2.setCollectionData(getCollectionInformation(aAnnotationService,
                        aCurationContainer));
                curationUserSegment2.setDocumentResponse(render(jCas, aAnnotationService,
                        aBratAnnotatorModel, curationColoringStrategy));
                curationUserSegment2.setUsername(username);
                curationUserSegment2.setBratAnnotatorModel(aBratAnnotatorModel);
                curationUserSegment2
                        .setAnnotationSelectionByUsernameAndAddress(aAnnotationSelectionByUsernameAndAddress);
                aSentences.add(curationUserSegment2);
            }
        }
    }

    private static String render(JCas aJcas, AnnotationService aAnnotationService,
            AnnotatorStateImpl aBratAnnotatorModel, ColoringStrategy aCurationColoringStrategy)
        throws IOException
    {
        GetDocumentResponse response = new GetDocumentResponse();
        response.setRtlMode(ScriptDirection.RTL.equals(aBratAnnotatorModel.getScriptDirection()));

        // Render invisible baseline annotations (sentence, tokens)
        BratRenderer.renderTokenAndSentence(aJcas, response, aBratAnnotatorModel);

        // Render visible (custom) layers
        for (AnnotationLayer layer : aBratAnnotatorModel.getAnnotationLayers()) {
            if (layer.getName().equals(Token.class.getName())
                    || layer.getName().equals(Sentence.class.getName())
                    || WebAnnoConst.CHAIN_TYPE.equals(layer.getType())) {
                continue;
            }

            List<AnnotationFeature> features = aAnnotationService.listAnnotationFeature(layer);
            List<AnnotationFeature> invisibleFeatures = new ArrayList<AnnotationFeature>();
            for (AnnotationFeature feature : features) {
                if (!feature.isVisible()) {
                    invisibleFeatures.add(feature);
                }
            }
            features.removeAll(invisibleFeatures);
            TypeAdapter adapter = getAdapter(aAnnotationService, layer);
            TypeRenderer renderer = BratRenderer.getRenderer(adapter);
            renderer.render(aJcas, features, response, aBratAnnotatorModel,
                    aCurationColoringStrategy);
        }

        return JSONUtil.toInterpretableJsonString(response);
    }

    private static String getCollectionInformation(AnnotationService aAnnotationService,
            CurationContainer aCurationContainer)
        throws IOException
    {
        GetCollectionInformationResponse info = new GetCollectionInformationResponse();
        info.setEntityTypes(BratRenderer.buildEntityTypes(aCurationContainer
                .getBratAnnotatorModel().getAnnotationLayers(), aAnnotationService));

        StringWriter out = new StringWriter();
        JsonGenerator jsonGenerator = JSONUtil.getJsonConverter().getObjectMapper()
                .getFactory().createGenerator(out);
        jsonGenerator.writeObject(info);
        return out.toString();
    }

    /**
     * @param aTarget
     *            the AJAX target.
     * @param aParent
     *            the parent.
     * @param aCurationContainer
     *            the container.
     * @param aMergeVisualizer
     *            the annotator component.
     * @param aRepository
     *            the repository.
     * @param aAnnotationSelectionByUsernameAndAddress
     *            selections by user.
     * @param aCurationSegment
     *            the segment.
     * @param aAnnotationService
     *            the annotation service.
     * @throws UIMAException
     *             hum?
     * @throws ClassNotFoundException
     *             hum?
     * @throws IOException
     *             hum?
     * @throws AnnotationException
     *             hum?
     */
    public static void updatePanel(
            AjaxRequestTarget aTarget,
            SuggestionViewPanel aParent,
            CurationContainer aCurationContainer,
            BratAnnotator aMergeVisualizer,
            RepositoryService aRepository,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            SourceListView aCurationSegment, AnnotationService aAnnotationService, UserDao aUserDao)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        AnnotatorStateImpl bModel = aCurationContainer.getBratAnnotatorModel();
        SourceDocument sourceDocument = bModel.getDocument();
        Map<String, JCas> jCases = new HashMap<String, JCas>();

        // This is the CAS that the user can actively edit
        JCas annotatorCas = getAnnotatorCase(bModel, aRepository, aUserDao,
                aAnnotationSelectionByUsernameAndAddress, sourceDocument, jCases);

        // We store the CAS that the user will edit as the "CURATION USER"
        jCases.put(CURATION_USER, annotatorCas);

        // get differing feature structures
        List<Type> entryTypes = SuggestionBuilder.getEntryTypes(annotatorCas,
                bModel.getAnnotationLayers(), aAnnotationService);
        List<AnnotationOption> annotationOptions = null;

        Map<String, AnnotationState> annoStates = new HashMap<>();

        DiffResult diff;

        if (bModel.getMode().equals(Mode.CURATION)) {
            diff = CasDiff2.doDiffSingle(aAnnotationService, bModel.getProject(), entryTypes,
                    LinkCompareBehavior.LINK_ROLE_AS_LABEL, jCases,
                    aCurationSegment.getCurationBegin(), aCurationSegment.getCurationEnd());
        }
        else {
            diff = CasDiff2.doDiffSingle(aAnnotationService, bModel.getProject(), entryTypes,
                    LinkCompareBehavior.LINK_ROLE_AS_LABEL, jCases, aCurationSegment.getBegin(),
                    aCurationSegment.getEnd());
        }

        Collection<ConfigurationSet> d = diff.getDifferingConfigurationSets().values();

        Collection<ConfigurationSet> i = diff.getIncompleteConfigurationSets().values();
        for (ConfigurationSet cfgSet : d) {
            if (i.contains(cfgSet)) {
                i.remove(cfgSet);
            }
        }

        addSuggestionColor(aAnnotationService, bModel.getMode(), jCases, annoStates, d, false,
                false);
        addSuggestionColor(aAnnotationService, bModel.getMode(), jCases, annoStates, i, true, false);

        List<ConfigurationSet> all = new ArrayList<>();

        for (ConfigurationSet a : diff.getConfigurationSets()) {
            all.add(a);
        }
        for (ConfigurationSet cfgSet : d) {
            all.remove(cfgSet);
        }
        for (ConfigurationSet cfgSet : i) {
            all.remove(cfgSet);
        }

        addSuggestionColor(aAnnotationService, bModel.getMode(), jCases, annoStates, all, false,
                true);

        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();

        CuratorUtil.populateCurationSentences(jCases, sentences, bModel, annotationOptions,
                aAnnotationSelectionByUsernameAndAddress, aAnnotationService, aCurationContainer,
                annoStates);

        // update sentence list on the right side
        aParent.setModelObject(sentences);

        aTarget.add(aParent);
    }

    /**
     * For each {@link ConfigurationSet}, where there are some differences in users annotation and
     * the curation annotation.
     *
     * @param aAnnotationService
     * @param aProj
     * @param aCasMap
     * @param aSuggestionColors
     * @param aCfgSet
     */
    private static void addSuggestionColor(AnnotationService aAnnotationService, Mode aMode,
            Map<String, JCas> aCasMap, Map<String, AnnotationState> aSuggestionColors,
            Collection<ConfigurationSet> aCfgSet, boolean aI, boolean aAgree)
    {
        for (ConfigurationSet cs : aCfgSet) {
            boolean use = false;
            for (String u : cs.getCasGroupIds()) {
                for (Configuration c : cs.getConfigurations(u)) {

                    FeatureStructure fs = c.getFs(u, aCasMap);
                    Object key = fs;
                    // link FS
                    if (c.getPosition().getFeature() != null) {
                        ArrayFS links = (ArrayFS) fs.getFeatureValue(fs.getType()
                                .getFeatureByBaseName(c.getPosition().getFeature()));
                        FeatureStructure link = links.get(c.getAID(u).index);
                        fs = (AnnotationFS) link.getFeatureValue(link.getType()
                                .getFeatureByBaseName("target"));
                        key = key + "-" + fs + "-" + link;
                    }
                    if (aAgree) {
                        aSuggestionColors.put(key.toString(), AnnotationState.AGREE);
                        continue;
                    }
                    // automation and correction projects
                    if (!aMode.equals(Mode.CURATION) && !aAgree) {
                        if (cs.getCasGroupIds().size() == 2) {
                            aSuggestionColors.put(key.toString(), AnnotationState.DO_NOT_USE);
                        }
                        else {
                            aSuggestionColors.put(key.toString(), AnnotationState.DISAGREE);
                        }
                        continue;
                    }

                    // this set agree with the curation annotation
                    if (c.getCasGroupIds().contains(CURATION_USER)) {
                        use = true;
                    }
                    else {
                        use = false;
                    }
                    // this curation view
                    if (u.equals(CURATION_USER)) {
                        continue;
                    }

                    if (aAgree) {
                        aSuggestionColors.put(key.toString(), AnnotationState.AGREE);
                    }
                    else if (use) {
                        aSuggestionColors.put(key.toString(), AnnotationState.USE);
                    }
                    else if (aI) {
                        aSuggestionColors.put(key.toString(), AnnotationState.DISAGREE);
                    }
                    else if (!cs.getCasGroupIds().contains(CURATION_USER)) {
                        aSuggestionColors.put(key.toString(), AnnotationState.DISAGREE);
                    }
                    else {
                        aSuggestionColors.put(key.toString(), AnnotationState.DO_NOT_USE);
                    }
                }
            }
        }
    }

    public static JCas getAnnotatorCase(
            AnnotatorStateImpl aBModel,
            RepositoryService aRepository,
            UserDao aUserDao,
            Map<String, Map<Integer, AnnotationSelection>> aAnnotationSelectionByUsernameAndAddress,
            SourceDocument sourceDocument, Map<String, JCas> jCases)
        throws UIMAException, IOException, ClassNotFoundException
    {

        JCas annotatorCas;
        if (aBModel.getMode().equals(Mode.AUTOMATION) || aBModel.getMode().equals(Mode.CORRECTION)) {
            // If this is a CORRECTION or AUTOMATION project, then we get the CORRECTION document
            // and put it in as the single document to compare with. Basically what we do is that
            // we treat consider this scenario as a curation scenario where the CORRECTION document
            // is the only document we compare with.

            // The CAS the user can edit is the one from the virtual CORRECTION USER
            annotatorCas = aRepository.readCorrectionCas(sourceDocument);

            User user = aUserDao.get(SecurityContextHolder.getContext().getAuthentication()
                    .getName());
            AnnotationDocument annotationDocument = aRepository.getAnnotationDocument(
                    sourceDocument, user);
            jCases.put(user.getUsername(), aRepository.readAnnotationCas(annotationDocument));
            aAnnotationSelectionByUsernameAndAddress.put(CURATION_USER,
                    new HashMap<Integer, AnnotationSelection>());
        }
        else {
            // If this is a true CURATION then we get all the annotation documents from all the
            // active users.

            // The CAS the user can edit is the one from the virtual CURATION USER
            annotatorCas = aRepository.readCurationCas(sourceDocument);

            // Now we get all the other CASes from the repository
            List<AnnotationDocument> annotationDocuments = aRepository
                    .listAnnotationDocuments(sourceDocument);
            for (AnnotationDocument annotationDocument : annotationDocuments) {
                String username = annotationDocument.getUser();
                if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)
                        || username.equals(CuratorUtil.CURATION_USER)) {
                    JCas jCas = aRepository.readAnnotationCas(annotationDocument);
                    jCases.put(username, jCas);

                    // cleanup annotationSelections
                    aAnnotationSelectionByUsernameAndAddress.put(username,
                            new HashMap<Integer, AnnotationSelection>());
                }
            }
        }
        return annotatorCas;
    }
}
