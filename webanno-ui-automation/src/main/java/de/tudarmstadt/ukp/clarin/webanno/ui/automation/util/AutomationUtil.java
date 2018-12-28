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
package de.tudarmstadt.ukp.clarin.webanno.ui.automation.util;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.persistence.NoResultException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CorrectionDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.AutomationStatus;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import edu.lium.mira.Mira;

/**
 * A utility class for the automation modules
 */
public class AutomationUtil
{
    private static Logger LOG = LoggerFactory.getLogger(AutomationUtil.class);
    private static final String NILL = "__nill__";

    public static void repeateSpanAnnotation(AnnotatorState aState,
            DocumentService aDocumentService, CorrectionDocumentService aCorrectionDocumentService,
            AnnotationSchemaService aAnnotationService, int aStart, int aEnd,
            AnnotationFeature aFeature, String aValue)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        AnnotationDocument annoDoc = aDocumentService.getAnnotationDocument(aState.getDocument(),
                aState.getUser());
        JCas annoCas = aDocumentService.readAnnotationCas(annoDoc);

        // get selected text, concatenations of tokens
        String selectedText = WebAnnoCasUtil.getSelectedText(annoCas, aStart, aEnd);
        SpanAdapter adapter = (SpanAdapter) aAnnotationService.getAdapter(aFeature.getLayer());
        for (SourceDocument d : aDocumentService.listSourceDocuments(aState.getProject())) {
            loadDocument(d, aAnnotationService, aDocumentService, aCorrectionDocumentService,
                    aState.getUser());
            JCas jCas = aCorrectionDocumentService.readCorrectionCas(d);

            for (Sentence sentence : select(jCas, Sentence.class)) {
                String sentenceText = sentence.getCoveredText().toLowerCase();
                for (int i = -1; (i = sentenceText.indexOf(selectedText.toLowerCase(),
                        i)) != -1; i = i + selectedText.length()) {
                    if (selectCovered(jCas, Token.class, sentence.getBegin() + i,
                            sentence.getBegin() + i + selectedText.length()).size() > 0) {
                        int addr = adapter.add(aState.getDocument(), aState.getUser().getUsername(),
                                jCas, sentence.getBegin() + i,
                                sentence.getBegin() + i + selectedText.length() - 1);
                        adapter.setFeatureValue(aState.getDocument(),
                                aState.getUser().getUsername(), jCas, addr, aFeature, aValue);
                    }
                }
            }
            aCorrectionDocumentService.writeCorrectionCas(jCas, d);
        }
    }

    public static void repeateRelationAnnotation(AnnotatorState aState,
            DocumentService aDocumentService, CorrectionDocumentService aCorrectionDocumentService,
            AnnotationSchemaService aAnnotationService, AnnotationFS fs, AnnotationFeature aFeature,
            String aValue)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        for (SourceDocument d : aDocumentService.listSourceDocuments(aState.getProject())) {
            loadDocument(d, aAnnotationService, aDocumentService, aCorrectionDocumentService,
                    aState.getUser());
            JCas jCas = aCorrectionDocumentService.readCorrectionCas(d);

            ArcAdapter adapter = (ArcAdapter) aAnnotationService.getAdapter(aFeature.getLayer());
            String sourceFName = adapter.getSourceFeatureName();
            String targetFName = adapter.getTargetFeatureName();

            Type type = getType(jCas.getCas(), aFeature.getLayer().getName());
            Type spanType = getType(jCas.getCas(), adapter.getAttachTypeName());
            Feature arcSpanFeature = spanType.getFeatureByBaseName(adapter.getAttachFeatureName());

            Feature dependentFeature = type.getFeatureByBaseName(targetFName);
            Feature governorFeature = type.getFeatureByBaseName(sourceFName);

            AnnotationFS dependentFs = null;
            AnnotationFS governorFs = null;
    
            if (adapter.getAttachFeatureName() != null) {
                dependentFs = (AnnotationFS) fs.getFeatureValue(dependentFeature)
                        .getFeatureValue(arcSpanFeature);
                governorFs = (AnnotationFS) fs.getFeatureValue(governorFeature)
                        .getFeatureValue(arcSpanFeature);

            }
            else {
                dependentFs = (AnnotationFS) fs.getFeatureValue(dependentFeature);
                governorFs = (AnnotationFS) fs.getFeatureValue(governorFeature);
            }

            if (adapter.isCrossMultipleSentence()) {
                List<AnnotationFS> mSpanAnnos = new ArrayList<>(
                        getAllAnnoFss(jCas, governorFs.getType()));
                repeatRelation(aState, 0, jCas.getDocumentText().length() - 1, aFeature, aValue,
                        jCas, adapter, dependentFs, governorFs, mSpanAnnos);
            }
            else {
                for (Sentence sent : select(jCas, Sentence.class)) {
                    List<AnnotationFS> spanAnnos = selectCovered(governorFs.getType(), sent);
                    repeatRelation(aState, sent.getBegin(), sent.getEnd(), aFeature, aValue, jCas,
                            adapter, dependentFs, governorFs, spanAnnos);
                }

            }

            aCorrectionDocumentService.writeCorrectionCas(jCas, d);
        }
    }

    private static void repeatRelation(AnnotatorState aState, int aStart, int aEnd,
            AnnotationFeature aFeature, String aValue, JCas jCas, ArcAdapter adapter,
            AnnotationFS aDepFS, AnnotationFS aGovFS, List<AnnotationFS> aSpanAnnos)
        throws AnnotationException
    {
        String dCoveredText = aDepFS.getCoveredText();
        String gCoveredText = aGovFS.getCoveredText();
        AnnotationFS d = null, g = null;
        Type attachSpanType = aDepFS.getType();

        for (AnnotationFS fs : aSpanAnnos) {
            if (dCoveredText.equals(fs.getCoveredText())) {
                if (g != null && isSamAnno(attachSpanType, fs, aDepFS)) {
                    AnnotationFS arc = adapter.add(g, fs, jCas, aStart, aEnd);
                    adapter.setFeatureValue(aState.getDocument(), aState.getUser().getUsername(),
                            jCas, getAddr(arc), aFeature, aValue);
                    g = null;
                    d = null;
                    continue;// so we don't go to the other if
                }
                else if (d == null && isSamAnno(attachSpanType, fs, aDepFS)) {
                    d = fs;
                    continue; // so we don't go to the other if
                }
            }
            // we don't use else, in case gov and dep are the same
            if (gCoveredText.equals(fs.getCoveredText())  ) {
                if (d != null && isSamAnno(attachSpanType, fs, aGovFS)) {
                    AnnotationFS arc = adapter.add(fs, d, jCas, aStart, aEnd);
                    adapter.setFeatureValue(aState.getDocument(), aState.getUser().getUsername(),
                            jCas, getAddr(arc), aFeature, aValue);
                    g = null;
                    d = null;
                }
                else if (g == null && isSamAnno(attachSpanType, fs, aGovFS)) {
                    g = fs;
                }
            }
        }
    }

    private static Collection<AnnotationFS> getAllAnnoFss(JCas aJcas, Type aType)
    {
        Collection<AnnotationFS> spanAnnos = select(aJcas.getCas(), aType);
        new ArrayList<>(spanAnnos).sort(Comparator.comparingInt(AnnotationFS::getBegin));
        return spanAnnos;
    }

    private static boolean isSamAnno(Type aType, AnnotationFS aMFs, AnnotationFS aFs)
    {
        for (Feature f : aType.getFeatures()) {
            // anywhere is ok
            if (f.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN)) {
                continue;
            }
            // anywhere is ok
            if (f.getName().equals(CAS.FEATURE_FULL_NAME_END)) {
                continue;
            }
            if (!f.getRange().isPrimitive() && aMFs.getFeatureValue(f) instanceof SofaFS) {
                continue;
            }
            // do not attach relation on empty span annotations
            if (aMFs.getFeatureValueAsString(f) == null) {
                continue;
            }
            if (aFs.getFeatureValueAsString(f) == null) {
                continue;
            }
            if (!aMFs.getFeatureValueAsString(f).equals(aFs.getFeatureValueAsString(f))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Repeat annotation will repeat annotations of same pattern to all documents on the project
     * load CAS from document in case no initial CORRECTION_CAS is not created before
     */
    public static void loadDocument(SourceDocument aDocument,
            AnnotationSchemaService annotationService, DocumentService aDocumentService,
            CorrectionDocumentService aCorrectionDocumentService, User logedInUser)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        JCas jCas = null;
        if (!aCorrectionDocumentService.existsCorrectionCas(aDocument)) {
            try {
                AnnotationDocument logedInUserAnnotationDocument = aDocumentService
                        .getAnnotationDocument(aDocument, logedInUser);
                jCas = aDocumentService.readAnnotationCas(logedInUserAnnotationDocument);
                annotationService.upgradeCas(jCas.getCas(), logedInUserAnnotationDocument);
                aCorrectionDocumentService.writeCorrectionCas(jCas, aDocument);
            }
            catch (DataRetrievalFailureException | NoResultException e) {
                jCas = aDocumentService.readAnnotationCas(
                        aDocumentService.createOrGetAnnotationDocument(aDocument, logedInUser));
                // upgrade this cas
                annotationService.upgradeCas(jCas.getCas(),
                        aDocumentService.createOrGetAnnotationDocument(aDocument, logedInUser));
                aCorrectionDocumentService.writeCorrectionCas(jCas, aDocument);
            }
        }
        else {
            jCas = aCorrectionDocumentService.readCorrectionCas(aDocument);
            // upgrade this automation cas
            aCorrectionDocumentService.upgradeCorrectionCas(jCas.getCas(), aDocument);
        }
    }

    public static void deleteSpanAnnotation(AnnotatorState aBModel,
            DocumentService aDocumentService, CorrectionDocumentService aCorrectionDocumentService,
            AnnotationSchemaService aAnnotationService, int aStart, int aEnd,
            AnnotationFeature aFeature, String aValue)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        AnnotationDocument annoDoc = aDocumentService.getAnnotationDocument(aBModel.getDocument(),
                aBModel.getUser());
        JCas annoCas = aDocumentService.readAnnotationCas(annoDoc);
        // get selected text, concatenations of tokens
        String selectedText = WebAnnoCasUtil.getSelectedText(annoCas, aStart, aEnd);

        for (SourceDocument d : aDocumentService.listSourceDocuments(aBModel.getProject())) {
            loadDocument(d, aAnnotationService, aDocumentService, aCorrectionDocumentService,
                    aBModel.getUser());
            JCas jCas = aCorrectionDocumentService.readCorrectionCas(d);

            TypeAdapter adapter = aAnnotationService.getAdapter(aFeature.getLayer());

            for (Sentence sentence : select(jCas, Sentence.class)) {
                String sentenceText = sentence.getCoveredText().toLowerCase();
                for (int i = -1; (i = sentenceText.indexOf(selectedText.toLowerCase(),
                        i)) != -1; i = i + selectedText.length()) {
                    if (selectCovered(jCas, Token.class, sentence.getBegin() + i,
                            sentence.getBegin() + i + selectedText.length()).size() > 0) {

                        deleteSpanAnnotation(adapter, aBModel, jCas, aFeature,
                                sentence.getBegin() + i,
                                sentence.getBegin() + i + selectedText.length() - 1, aValue);
                    }
                }
            }
            aCorrectionDocumentService.writeCorrectionCas(jCas,d);
        }
    }

    @Deprecated
    private static void deleteSpanAnnotation(TypeAdapter aAdapter, AnnotatorState aState,
            JCas aJCas, AnnotationFeature aFeature, int aBegin, int aEnd, Object aValue)
    {
        Type type = CasUtil.getType(aJCas.getCas(), aAdapter.getAnnotationTypeName());
        for (AnnotationFS fs : CasUtil.selectCovered(aJCas.getCas(), type, aBegin, aEnd)) {
            if (fs.getBegin() == aBegin && fs.getEnd() == aEnd) {
                if (ObjectUtils.equals(aAdapter.getFeatureValue(aFeature, fs), aValue)) {
                    aAdapter.delete(aState.getDocument(), aState.getUser().getUsername(), aJCas,
                            new VID(getAddr(fs)));
                }
            }
        }
    }
    
    public static void deleteRelationAnnotation(AnnotatorState aBModel,
            DocumentService aDocumentService, CorrectionDocumentService aCorrectionDocumentService,
            AnnotationSchemaService aAnnotationService, AnnotationFS fs, AnnotationFeature aFeature,
            String aValue)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        for (SourceDocument d : aDocumentService.listSourceDocuments(aBModel.getProject())) {
            loadDocument(d, aAnnotationService, aDocumentService, aCorrectionDocumentService,
                    aBModel.getUser());
            JCas jCas = aCorrectionDocumentService.readCorrectionCas(d);
            ArcAdapter adapter = (ArcAdapter) aAnnotationService.getAdapter(aFeature.getLayer());
            String sourceFName = adapter.getSourceFeatureName();
            String targetFName = adapter.getTargetFeatureName();

            Type type = getType(jCas.getCas(), aFeature.getLayer().getName());
            Type spanType = getType(jCas.getCas(), adapter.getAttachTypeName());
            Feature arcSpanFeature = spanType.getFeatureByBaseName(adapter.getAttachFeatureName());

            Feature dependentFeature = type.getFeatureByBaseName(targetFName);
            Feature governorFeature = type.getFeatureByBaseName(sourceFName);

            AnnotationFS dependentFs = null;
            AnnotationFS governorFs = null;

            if (adapter.getAttachFeatureName() != null) {
                dependentFs = (AnnotationFS) fs.getFeatureValue(dependentFeature)
                        .getFeatureValue(arcSpanFeature);
                governorFs = (AnnotationFS) fs.getFeatureValue(governorFeature)
                        .getFeatureValue(arcSpanFeature);

            }
            else {
                dependentFs = (AnnotationFS) fs.getFeatureValue(dependentFeature);
                governorFs = (AnnotationFS) fs.getFeatureValue(governorFeature);
            }

            int beginOffset = 0;
            int endOffset = jCas.getDocumentText().length() - 1;

            String depCoveredText = dependentFs.getCoveredText();
            String govCoveredText = governorFs.getCoveredText();

            adapter.delete(aBModel.getDocument(), aBModel.getUser().getUsername(), jCas, aFeature,
                    beginOffset, endOffset, depCoveredText, govCoveredText, aValue);
            aCorrectionDocumentService.writeCorrectionCas(jCas, d);
        }
    }

    // generates training document that will be used to predict the training document
    // to add extra features, for example add POS tag as a feature for NE classifier
    public static void addOtherFeatureTrainDocument(MiraTemplate aTemplate,
            AnnotationSchemaService aAnnotationService, AutomationService aAutomationService,
            UserDao aUserDao)
        throws IOException, UIMAException, ClassNotFoundException
    {
        File miraDir = aAutomationService.getMiraDir(aTemplate.getTrainFeature());
        if (!miraDir.exists()) {
            FileUtils.forceMkdir(miraDir);
        }

        AutomationStatus status = aAutomationService.getAutomationStatus(aTemplate);
        for (AnnotationFeature feature : aTemplate.getOtherFeatures()) {
            File trainFile = new File(miraDir, feature.getId() + ".train");
            boolean documentChanged = false;
            for (TrainingDocument document : aAutomationService
                    .listTrainingDocuments(feature.getProject())) {
                if (!document.isProcessed() && (document.getFeature() != null
                        && document.getFeature().equals(feature))) {
                    documentChanged = true;
                    break;
                }
            }
            if (!documentChanged && trainFile.exists()) {
                continue;
            }

            BufferedWriter trainOut = new BufferedWriter(new FileWriter(trainFile));
            TypeAdapter adapter = aAnnotationService.getAdapter(feature.getLayer());
            for (TrainingDocument trainingDocument : aAutomationService
                    .listTrainingDocuments(feature.getProject())) {
                if ((trainingDocument.getFeature() != null
                        && trainingDocument.getFeature().equals(feature))) {
                    JCas jCas = aAutomationService.readTrainingAnnotationCas(trainingDocument);
                    for (Sentence sentence : select(jCas, Sentence.class)) {
                        trainOut.append(getMiraLine(aAnnotationService, sentence, feature, adapter)
                                .toString()).append("\n");
                    }
                    trainingDocument.setProcessed(false);
                    status.setTrainDocs(status.getTrainDocs() - 1);
                }
            }
            trainOut.close();
        }
    }

    /**
     * If the training file or the test file already contain the "Other layer" annotations, get the
     * UIMA annotation and add it as a feature - no need to train and predict for this "other layer"
     */
    private static void addOtherFeatureFromAnnotation(AnnotationFeature aFeature,
            DocumentService aRepository, AutomationService aAutomationServic,
            AnnotationSchemaService aAnnotationService, UserDao aUserDao,
            List<List<String>> aPredictions, SourceDocument aSourceDocument)
        throws UIMAException, ClassNotFoundException, IOException
    {
        TypeAdapter adapter = aAnnotationService.getAdapter(aFeature.getLayer());
        List<String> annotations = new ArrayList<>();
        // this is training - all training documents will be converted to a single training file
        if (aSourceDocument == null) {
            for (TrainingDocument trainingDocument : aAutomationServic
                    .listTrainingDocuments(aFeature.getProject())) {

                JCas jCas = aAutomationServic.readTrainingAnnotationCas(trainingDocument);
                for (Sentence sentence : select(jCas, Sentence.class)) {
                    switch (aFeature.getLayer().getAnchoringMode()) {
                    case TOKENS:
                        annotations.addAll(
                                getMultipleAnnotation(aAnnotationService, sentence, aFeature)
                                        .values());
                        break;
                    case SINGLE_TOKEN:
                        annotations.addAll(getAnnotation(adapter, sentence, aFeature));
                    default:
                        throw new IllegalStateException("Unsupported anchoring mode: ["
                                + aFeature.getLayer().getAnchoringMode() + "]");
                    }
                }
            }
            aPredictions.add(annotations);
        }
        // This is SourceDocument to predict (in the suggestion pane)
        else {
            User user = aUserDao.getCurrentUser();
            AnnotationDocument annodoc = aRepository.createOrGetAnnotationDocument(aSourceDocument,
                    user);
            JCas jCas = aRepository.readAnnotationCas(annodoc);
            for (Sentence sentence : select(jCas, Sentence.class)) {
                switch (aFeature.getLayer().getAnchoringMode()) {
                case TOKENS:
                    annotations.addAll(
                            getMultipleAnnotation(aAnnotationService, sentence, aFeature).values());
                    break;
                case SINGLE_TOKEN:
                    annotations.addAll(getAnnotation(adapter, sentence, aFeature));
                    break;
                default:
                    throw new IllegalStateException("Unsupported anchoring mode: ["
                            + aFeature.getLayer().getAnchoringMode() + "]");
                }
            }
            aPredictions.add(annotations);
        }
    }

    public static void addTabSepTrainDocument(MiraTemplate aTemplate,
            AutomationService aAutomationService)
        throws IOException, UIMAException, ClassNotFoundException, AutomationException
    {
        File miraDir = aAutomationService.getMiraDir(aTemplate.getTrainFeature());
        if (!miraDir.exists()) {
            FileUtils.forceMkdir(miraDir);
        }

        AutomationStatus status = aAutomationService.getAutomationStatus(aTemplate);

        boolean documentChanged = false;
        for (TrainingDocument document : aAutomationService
                .listTabSepDocuments(aTemplate.getTrainFeature().getProject())) {
            if (!document.isProcessed()) {
                documentChanged = true;
                break;
            }
        }
        if (!documentChanged) {
            return;
        }

        for (TrainingDocument trainingDocument : aAutomationService.listTabSepDocuments(aTemplate
                .getTrainFeature().getProject())) {
            if (trainingDocument.getFeature() != null) { // This is a target layer train document
                continue;
            }
            File trainFile = new File(miraDir, trainingDocument.getId()
                    + trainingDocument.getProject().getId() + ".train");
            BufferedWriter trainOut = new BufferedWriter(new FileWriter(trainFile));
            File tabSepFile = new File(aAutomationService.getDocumentFolder(trainingDocument),
                    trainingDocument.getName());
            LineIterator it = IOUtils.lineIterator(new FileReader(tabSepFile));
            while (it.hasNext()) {
                String line = it.next();
                if (line.trim().equals("")) {
                    trainOut.append("\n");
                }
                else {
                    StringTokenizer st = new StringTokenizer(line, "\t");
                    if (st.countTokens() != 2) {
                        trainOut.close();
                        throw new AutomationException("This is not a valid TAB-SEP document");
                    }
                    trainOut.append(getMiraLineForTabSep(st.nextToken(), st.nextToken()));
                }
            }
            trainingDocument.setProcessed(false);
            status.setTrainDocs(status.getTrainDocs() - 1);
            trainOut.close();
        }

    }

    public static void generateTrainDocument(MiraTemplate aTemplate, DocumentService aRepository,
            CurationDocumentService aCurationDocumentService,
            AnnotationSchemaService aAnnotationService, AutomationService aAutomationService,
            UserDao aUserDao, boolean aBase)
        throws IOException, UIMAException, ClassNotFoundException, AutomationException
    {
        LOG.info("Starting to generate training document");
        
        File miraDir = aAutomationService.getMiraDir(aTemplate.getTrainFeature());
        if (!miraDir.exists()) {
            FileUtils.forceMkdir(miraDir);
        }
        AnnotationFeature feature = aTemplate.getTrainFeature();
        boolean documentChanged = false;
        // A. training document for other train layers were changed
        for (AnnotationFeature otherrFeature : aTemplate.getOtherFeatures()) {
            for (TrainingDocument document : aAutomationService.listTrainingDocuments(aTemplate
                    .getTrainFeature().getProject())) {
                if (!document.isProcessed() && document.getFeature() != null
                        && document.getFeature().equals(otherrFeature)) {
                    documentChanged = true;
                    break;
                }
            }
        }
        // B. Training document for the main training layer were changed
        for (TrainingDocument document : aAutomationService
                .listTrainingDocuments(feature.getProject())) {
            if (!document.isProcessed()
                    && (document.getFeature() != null && document.getFeature().equals(feature))) {
                documentChanged = true;
                break;
            }
        }
        // C. New Curation document arrives
        if (aRepository.listSourceDocuments(feature.getProject()).size() > 0) {
            documentChanged = true;
        }

        // D. tab-sep training documents
        for (TrainingDocument document : aAutomationService
                .listTabSepDocuments(aTemplate.getTrainFeature().getProject())) {
            if (!document.isProcessed() && document.getFeature() != null
                    && document.getFeature().equals(feature)) {
                documentChanged = true;
                break;
            }
        }
        if (!documentChanged) {
            return;
        }
        File trainFile;
        if (aBase) {
            trainFile = new File(miraDir, feature.getLayer().getId() + "-" + feature.getId()
                    + ".train.ft");
        }
        else {
            trainFile = new File(miraDir, feature.getLayer().getId() + "-" + feature.getId()
                    + ".train.base");
        }

        AutomationStatus status = aAutomationService.getAutomationStatus(aTemplate);

        BufferedWriter trainOut = new BufferedWriter(new FileWriter(trainFile));
        TypeAdapter adapter = aAnnotationService.getAdapter(feature.getLayer());
        // Training documents (Curated or webanno-compatible imported ones - read using UIMA)
        List<TrainingDocument> trainingDocuments = aAutomationService
                .listTrainingDocuments(feature.getProject());
        int trainingDocsCount = 0;
        for (TrainingDocument trainingDocument : trainingDocuments) {
            if ((trainingDocument.getFeature() != null 
                    && trainingDocument.getFeature().equals(feature)) 
                    && !trainingDocument.getFormat().equals(WebAnnoConst.TAB_SEP)) {
                JCas jCas = aAutomationService.readTrainingAnnotationCas(trainingDocument);
                for (Sentence sentence : select(jCas, Sentence.class)) {
                    if (aBase) { // base training document
                        trainOut.append(getMiraLine(aAnnotationService, sentence, null, adapter)
                            .toString()).append("\n");
                    }
                    else { // training document with other features
                        trainOut.append(getMiraLine(aAnnotationService, sentence, feature, adapter)
                            .toString()).append("\n");
                    }
                }
                trainingDocument.setProcessed(!aBase);
                if (!aBase) {
                    status.setTrainDocs(status.getTrainDocs() - 1);
                }
            }
        }
        // for curated docuemnts
        List<SourceDocument> sourceDocuments = aRepository
                .listSourceDocuments(feature.getProject());
        for (SourceDocument sourceDocument : sourceDocuments) {
            if (sourceDocument.getState().equals(SourceDocumentState.CURATION_FINISHED)) {
                JCas jCas = aCurationDocumentService.readCurationCas(sourceDocument);
                for (Sentence sentence : select(jCas, Sentence.class)) {
                    if (aBase) { // base training document
                        trainOut.append(
                                getMiraLine(aAnnotationService, sentence, null, adapter).toString())
                                .append("\n");
                    }
                    else { // training document with other features
                        trainOut.append(getMiraLine(aAnnotationService, sentence, feature, adapter)
                                .toString()).append("\n");
                    }
                }
                if (!aBase) {
                    status.setTrainDocs(status.getTrainDocs() - 1);
                }
            }
            trainingDocsCount++;
            LOG.info("Processed source document " + trainingDocsCount + " of "
                    + trainingDocuments.size());
        }
        // Tab-sep documents to be used as a target layer train document
        int goldStandardDocsCounter = 0;
        List<TrainingDocument> goldStandardDocs = aAutomationService
                .listTabSepDocuments(feature.getProject());
        for (TrainingDocument document : goldStandardDocs) {
            if (document.getFormat().equals(WebAnnoConst.TAB_SEP) && document.getFeature() != null
                    && document.getFeature().equals(feature)) {
                File tabSepFile = new File(aAutomationService.getDocumentFolder(document),
                        document.getName());
                LineIterator it = IOUtils.lineIterator(new FileReader(tabSepFile));
                while (it.hasNext()) {
                    String line = it.next();
                    if (line.trim().equals("")) {
                        trainOut.append("\n");
                    }
                    else {
                        StringTokenizer st = new StringTokenizer(line, "\t");
                        if (st.countTokens() != 2) {
                            trainOut.close();
                            throw new AutomationException("This is not a valid TAB-SEP document");
                        }
                        if (aBase) {
                            trainOut.append(getMiraLineForTabSep(st.nextToken(), ""));
                        }
                        else {
                            trainOut.append(getMiraLineForTabSep(st.nextToken(), st.nextToken()));
                        }
                    }
                }
            }
            goldStandardDocsCounter++;
            LOG.info("Processed gold standard document " + goldStandardDocsCounter + " of "
                    + goldStandardDocs.size());
        }
        trainOut.close();
        
        LOG.info("Completed generating training document");
    }

    //TODO: rename to predictDocument
    public static void generatePredictDocument(MiraTemplate aTemplate, DocumentService aRepository,
            CorrectionDocumentService aCorrectionDocumentService,
            AnnotationSchemaService aAnnotationService, AutomationService aAutomationService,
            UserDao aUserDao)
        throws IOException, UIMAException, ClassNotFoundException
    {
        File miraDir = aAutomationService.getMiraDir(aTemplate.getTrainFeature());
        if (!miraDir.exists()) {
            FileUtils.forceMkdir(miraDir);
        }

        User user = aUserDao.getCurrentUser();
        AnnotationFeature feature = aTemplate.getTrainFeature();
        TypeAdapter adapter = aAnnotationService.getAdapter(feature.getLayer());
        for (SourceDocument document : aRepository.listSourceDocuments(feature.getProject())) {
            File predFile = new File(miraDir, document.getId() + ".pred.ft");
            BufferedWriter predOut = new BufferedWriter(new FileWriter(predFile));
            JCas jCas;
            try {
                jCas = aCorrectionDocumentService.readCorrectionCas(document);
            }
            catch (Exception e) {
                AnnotationDocument annoDoc = aRepository.createOrGetAnnotationDocument(document,
                        user);
                jCas = aRepository.readAnnotationCas(annoDoc);
            }

            for (Sentence sentence : select(jCas, Sentence.class)) {
                predOut.append(getMiraLine(aAnnotationService, sentence, null, adapter).toString())
                        .append("\n");
            }
            predOut.close();
        }
    }

    private static StringBuffer getMiraLine(AnnotationSchemaService aAnnotationService,
            Sentence sentence, AnnotationFeature aLayerFeature, TypeAdapter aAdapter)
        throws CASException
    {
        StringBuffer sb = new StringBuffer();

        String tag = "";
        List<String> annotations = new ArrayList<>();
        Map<Integer, String> multAnno = null;
        if (aLayerFeature != null) {
            switch (aLayerFeature.getLayer().getAnchoringMode()) {
            case TOKENS:
                multAnno = getMultipleAnnotation(aAnnotationService, sentence, aLayerFeature);
            case SINGLE_TOKEN:
                annotations = getAnnotation(aAdapter, sentence, aLayerFeature);
            default:
                throw new IllegalStateException("Unsupported anchoring mode: ["
                        + aLayerFeature.getLayer().getAnchoringMode() + "]");
            }
        }

        int i = 0;
        for (Token token : selectCovered(Token.class, sentence)) {
            String word = token.getCoveredText();

            char[] words = word.toCharArray();

            String prefix1 = "", prefix2 = "", prefix3 = "", prefix4 = "", suffix1 = "", suffix2 = "", suffix3 = "", suffix4 = "";
            if (
                    aLayerFeature == null || 
                    AnchoringMode.SINGLE_TOKEN.equals(aLayerFeature.getLayer().getAnchoringMode())
            ) {
                prefix1 = Character.toString(words[0]) + " ";
                prefix2 = (words.length > 1 ? prefix1.trim()
                        + (Character.toString(words[1]).trim().equals("") ? "__nil__" : Character
                                .toString(words[1])) : "__nil__")
                        + " ";
                prefix3 = (words.length > 2 ? prefix2.trim()
                        + (Character.toString(words[2]).trim().equals("") ? "__nil__" : Character
                                .toString(words[2])) : "__nil__")
                        + " ";
                prefix4 = (words.length > 3 ? prefix3.trim()
                        + (Character.toString(words[3]).trim().equals("") ? "__nil__" : Character
                                .toString(words[3])) : "__nil__")
                        + " ";
                suffix1 = Character.toString(words[words.length - 1]) + " ";
                suffix2 = (words.length > 1 ? (Character.toString(words[words.length - 2]).trim()
                        .equals("") ? "__nil__" : Character.toString(words[words.length - 2]))
                        + suffix1.trim() : "__nil__")
                        + " ";
                suffix3 = (words.length > 2 ? (Character.toString(words[words.length - 3]).trim()
                        .equals("") ? "__nil__" : Character.toString(words[words.length - 3]))
                        + suffix2.trim() : "__nil__")
                        + " ";
                suffix4 = (words.length > 3 ? (Character.toString(words[words.length - 4]).trim()
                        .equals("") ? "__nil__" : Character.toString(words[words.length - 4]))
                        + suffix3.trim() : "__nil__")
                        + " ";
            }
            String nl = "\n";

            if (aLayerFeature != null) {
                if (AnchoringMode.TOKENS.equals(aLayerFeature.getLayer().getAnchoringMode())) {
                    tag = multAnno.get(getAddr(token)) == null ? "O" : multAnno.get(getAddr(token));
                }
                else {
                    tag = annotations.size() == 0 ? NILL : annotations.get(i);
                    i++;
                }

            }
            sb.append(word).append(" ").append(prefix1).append(prefix2).append(prefix3)
                .append(prefix4).append(suffix1).append(suffix2).append(suffix3).append(suffix4)
                .append(tag).append(nl);
        }
        return sb;

    }

    private static StringBuffer getMiraLineForTabSep(String aToken, String aFeature)
        throws CASException
    {
        StringBuffer sb = new StringBuffer();
        char[] words = aToken.toCharArray();
        String prefix1 = Character.toString(words[0]) + " ";
        String prefix2 = (words.length > 1 ? prefix1.trim()
                + (Character.toString(words[1]).trim().equals("") ? "__nil__" : Character
                        .toString(words[1])) : "__nil__")
                + " ";
        String prefix3 = (words.length > 2 ? prefix2.trim()
                + (Character.toString(words[2]).trim().equals("") ? "__nil__" : Character
                        .toString(words[2])) : "__nil__")
                + " ";
        String prefix4 = (words.length > 3 ? prefix3.trim()
                + (Character.toString(words[3]).trim().equals("") ? "__nil__" : Character
                        .toString(words[3])) : "__nil__")
                + " ";
        String suffix1 = Character.toString(words[words.length - 1]) + " ";
        String suffix2 = (words.length > 1 ? (Character.toString(words[words.length - 2]).trim()
                .equals("") ? "__nil__" : Character.toString(words[words.length - 2]))
                + suffix1.trim() : "__nil__")
                + " ";
        String suffix3 = (words.length > 2 ? (Character.toString(words[words.length - 3]).trim()
                .equals("") ? "__nil__" : Character.toString(words[words.length - 3]))
                + suffix2.trim() : "__nil__")
                + " ";
        String suffix4 = (words.length > 3 ? (Character.toString(words[words.length - 4]).trim()
                .equals("") ? "__nil__" : Character.toString(words[words.length - 4]))
                + suffix3.trim() : "__nil__")
                + " ";

        String nl = "\n";
        sb.append(aToken).append(" ").append(prefix1).append(prefix2).append(prefix3)
            .append(prefix4).append(suffix1).append(suffix2).append(suffix3).append(suffix4)
            .append(aFeature).append(nl);
        return sb;

    }

    /**
     * When additional layers are used as training feature, the training document should be
     * auto-predicted with the other layers. Example, if the train layer is Named Entity and POS
     * layer is used as additional feature, the training document should be predicted using the POS
     * layer documents for POS annotation
     *
     * @param aTemplate
     *            the template.
     * @param aRepository
     *            the repository.
     * @throws IOException
     *             hum?
     * @throws ClassNotFoundException
     *             hum?
     */
    public static void otherFeatureClassifiers(MiraTemplate aTemplate,
            DocumentService aRepository, AutomationService aAutomationService)
        throws IOException, ClassNotFoundException
    {
        Mira mira = new Mira();
        int frequency = 2;
        double sigma = 1;
        int iterations = 10;
        int beamSize = 0;
        boolean maxPosteriors = false;
        String templateName = null;

        for (AnnotationFeature feature : aTemplate.getOtherFeatures()) {
            templateName = createTemplate(feature, getMiraTemplateFile(feature, aAutomationService),
                    0);

            File miraDir = aAutomationService.getMiraDir(aTemplate.getTrainFeature());
            File trainFile = new File(miraDir, feature.getId() + ".train");
            String initalModelName = "";
            String trainName = trainFile.getAbsolutePath();

            String modelName = aAutomationService.getMiraModel(feature, true, null)
                    .getAbsolutePath();

            boolean randomInit = false;

            switch (feature.getLayer().getAnchoringMode()) {
            case SINGLE_TOKEN:
                // Nothing extra to do
                break;
            case TOKENS:
                mira.setIobScorer();
                break;
            default:
                throw new IllegalStateException("Unsupported anchoring mode: ["
                        + feature.getLayer().getAnchoringMode() + "]");
            }
            mira.loadTemplates(templateName);
            mira.setClip(sigma);
            mira.maxPosteriors = maxPosteriors;
            mira.beamSize = beamSize;
            int numExamples = mira.count(trainName, frequency);
            mira.initModel(randomInit);
            if (!initalModelName.equals("")) {
                mira.loadModel(initalModelName);
            }
            for (int i = 0; i < iterations; i++) {
                mira.train(trainName, iterations, numExamples, i);
                mira.averageWeights(iterations * numExamples);
            }
            mira.saveModel(modelName);
        }
    }

    /**
     * Classifier for an external tab-sep file (token TAB feature)
     *
     * @param aTemplate
     *            the template.
     * @throws IOException
     *             hum?
     * @throws ClassNotFoundException
     *             hum?
     */
    public static void tabSepClassifiers(MiraTemplate aTemplate,
            AutomationService aAutomationService)
        throws IOException, ClassNotFoundException
    {
        Mira mira = new Mira();
        int frequency = 2;
        double sigma = 1;
        int iterations = 10;
        int beamSize = 0;
        boolean maxPosteriors = false;
        String templateName = null;

        boolean documentChanged = false;
        for (TrainingDocument document : aAutomationService
                .listTabSepDocuments(aTemplate.getTrainFeature().getProject())) {
            if (!document.isProcessed()) {
                documentChanged = true;
                break;
            }
        }
        if (!documentChanged) {
            return;
        }

        for (TrainingDocument trainingDocument : aAutomationService.listTabSepDocuments(aTemplate
                .getTrainFeature().getProject())) {
            if (trainingDocument.getFeature() != null) { // This is a target layer train document
                continue;
            }
            File miraDir = aAutomationService.getMiraDir(aTemplate.getTrainFeature());
            File trainFile = new File(miraDir, trainingDocument.getId()
                    + trainingDocument.getProject().getId() + ".train");
            templateName = createTemplate(null,
                    getMiraTemplateFile(aTemplate.getTrainFeature(), aAutomationService), 0);

            String initalModelName = "";
            String trainName = trainFile.getAbsolutePath();
            String modelName = aAutomationService.getMiraModel(aTemplate.getTrainFeature(), true,
                    trainingDocument).getAbsolutePath();
            boolean randomInit = false;

            mira.loadTemplates(templateName);
            mira.setClip(sigma);
            mira.maxPosteriors = maxPosteriors;
            mira.beamSize = beamSize;
            int numExamples = mira.count(trainName, frequency);
            mira.initModel(randomInit);
            if (!initalModelName.equals("")) {
                mira.loadModel(initalModelName);
            }
            for (int i = 0; i < iterations; i++) {
                mira.train(trainName, iterations, numExamples, i);
                mira.averageWeights(iterations * numExamples);
            }
            mira.saveModel(modelName);
        }
    }

    public static String createTemplate(AnnotationFeature aFeature, File templateFile, int aOther)
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        if (aFeature == null) {
            setMorphoTemplate(sb, aOther);
        }
        else {
            switch (aFeature.getLayer().getAnchoringMode()) {
            case SINGLE_TOKEN:
                setMorphoTemplate(sb, aOther);
                break;
            case TOKENS:
                setNgramForLable(sb, aOther);
                break;
            default:
                throw new IllegalStateException("Unsupported anchoring mode: ["
                        + aFeature.getLayer().getAnchoringMode() + "]");
            }
        }
        
        sb.append("\n");
        sb.append("B\n");
        FileUtils.writeStringToFile(templateFile, sb.toString());
        return templateFile.getAbsolutePath();
    }

    private static void setNgramForLable(StringBuffer aSb, int aOther)
    {
        int i = 1;
        aSb.append("U").append(String.format("%02d", i)).append("%x[0,0]\n");
        i++;
        /*
         * aSb.append("U" + String.format("%02d", i) + "%x[0,1]\n"); i++; aSb.append("U" +
         * String.format("%02d", i) + "%x[0,0]" + "%x[0,1]\n"); i++;
         */
        aSb.append("U").append(String.format("%02d", i)).append("%x[-1,0]").append("%x[0,0]\n");
        i++;
        /*
         * aSb.append("U" + String.format("%02d", i) + "%x[-1,1]" + "%x[0,1]\n"); i++;
         */

        int temp = 1;
        int tempOther = aOther;
        if (aOther > 0) { // consider other layer annotations as features
            while (aOther > 0) {
                aOther--;
                aSb.append("U").append(String.format("%02d", i)).append("%x[0,").append(temp)
                    .append("]\n");
                i++;
                aSb.append("U").append(String.format("%02d", i)).append("%x[0,0] %x[0,")
                    .append(temp).append("]\n");
                i++;
                aSb.append("U").append(String.format("%02d", i)).append("%x[-1,").append(temp)
                    .append("] %x[0,").append(temp).append("]\n");
                i++;
                temp++;
            }
        }
        aSb.append("\n");

        i = 1;
        aSb.append("B").append(String.format("%02d", i)).append("%x[0,0]\n");
        i++;
        /*
         * aSb.append("B" + String.format("%02d", i) + "%x[0,1]\n"); i++; aSb.append("B" +
         * String.format("%02d", i) + "%x[0,0]" + "%x[0,1]\n"); i++;
         */
        aSb.append("B").append(String.format("%02d", i)).append("%x[-1,0]").append("%x[0,0]\n");
        i++;
        /*
         * aSb.append("B" + String.format("%02d", i) + "%x[-1,1]" + "%x[0,1]\n"); i++;
         */
        aSb.append("\n");
        temp = 1;
        if (tempOther > 0) { // consider other layer annotations as features
            while (aOther > 0) {
                aOther--;
                aSb.append("B").append(String.format("%02d", i)).append("%x[0,").append(temp)
                    .append("]\n");
                i++;
                aSb.append("B").append(String.format("%02d", i)).append("%x[0,0] %x[0,")
                    .append(temp).append("]\n");
                i++;
                aSb.append("B").append(String.format("%02d", i)).append("%x[-1,").append(temp)
                    .append("] %x[0,").append(temp).append("]\n");
                i++;
                temp++;
            }
        }
    }

    // only for token based automation, we need morphological features.
    private static void setMorphoTemplate(StringBuffer aSb, int aOther)
    {
        int i = 1;
        aSb.append("U").append(String.format("%02d", i)).append("%x[0,").append(i).append("]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[0,").append(i).append("]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[0,").append(i).append("]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[0,").append(i).append("]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[0,").append(i).append("]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[0,").append(i).append("]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[0,").append(i).append("]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[0,").append(i).append("]\n");
        i++;
        aSb.append("\n");

        aSb.append("U").append(String.format("%02d", i)).append("%x[0,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[-1,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[1,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[-2,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[2,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[-2,0]").append("%x[-1,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[-1,0]").append("%x[0,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[0,0]").append("%x[1,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[1,0]").append("%x[2,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[-2,0]").append("%x[-1,0]")
            .append("%x[0,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[-1,0]").append("%x[0,0]")
            .append("%x[1,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[0,0]").append("%x[1,0]")
            .append("%x[2,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[-2,0]").append("%x[-1,0]")
            .append("%x[0,0]").append("%x[1,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[-1,0]").append("%x[0,0]")
            .append("%x[1,0]").append("%x[2,0]\n");
        i++;
        aSb.append("U").append(String.format("%02d", i)).append("%x[-2,0]").append("%x[-1,0]")
            .append("%x[0,0").append("%x[1,0]").append("%x[2,0]]\n");
        aSb.append("\n");
        int temp = 1;
        if (aOther > 0) { // consider other layer annotations as features
            while (aOther > 0) {
                aOther--;
                aSb.append("U").append(String.format("%02d", i)).append("%x[0,").append(temp)
                    .append("]\n");
                i++;
                aSb.append("U").append(String.format("%02d", i)).append("%x[0,0] %x[0,")
                    .append(temp).append("]\n");
                i++;
                aSb.append("U").append(String.format("%02d", i)).append("%x[-1,").append(temp)
                    .append("] %x[0,").append(temp).append("]\n");
                i++;
                temp++;
            }
        }
        aSb.append("\n");
    }

    public static File getMiraTemplateFile(AnnotationFeature aFeature,
            AutomationService aAutomationService)
    {
        return new File(aAutomationService.getMiraDir(aFeature).getAbsolutePath(), aFeature.getId()
                + "-template");
    }

    /**
     * Based on the other layer, predict features for the training document
     *
     * @param aTemplate
     *            the template.
     * @param aRepository
     *            the repository.
     * @return the prediction.
     * @throws UIMAException
     *             hum?
     * @throws ClassNotFoundException
     *             hum?
     * @throws IOException
     *             hum?
     * @throws AnnotationException
     *             hum?
     *
     * @throws AutomationException
     *             if an error occurs.
     */
    public static String generateFinalClassifier(MiraTemplate aTemplate,
            DocumentService aRepository, CurationDocumentService aCurationDocumentService,
            AnnotationSchemaService aAnnotationService, AutomationService aAutomationService,
            UserDao aUserDao)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException,
        AutomationException
    {
        int frequency = 2;
        double sigma = 1;
        int iterations = 10;
        int beamSize = 0;
        boolean maxPosteriors = false;
        AnnotationFeature layerFeature = aTemplate.getTrainFeature();
        List<List<String>> predictions = new ArrayList<>();

        File miraDir = aAutomationService.getMiraDir(layerFeature);
        Mira mira = new Mira();
        File predFile = new File(miraDir, layerFeature.getLayer().getId() + "-"
                + layerFeature.getId() + ".train.ft");
        File predcitedFile = new File(predFile.getAbsolutePath() + "-pred");

        boolean trainingDocumentUpdated = false;

        // A. training document for other train layers were changed
        for (AnnotationFeature feature : aTemplate.getOtherFeatures()) {
            for (TrainingDocument document : aAutomationService.listTrainingDocuments(aTemplate
                    .getTrainFeature().getProject())) {
                if (!document.isProcessed() && document.getFeature() != null
                        && document.getFeature().equals(feature)) {
                    trainingDocumentUpdated = true;
                    break;
                }
            }
        }
        // B. Training document for the main training layer were changed
        for (TrainingDocument document : aAutomationService
                .listTrainingDocuments(layerFeature.getProject())) {
            if (!document.isProcessed() && (document.getFeature() != null
                    && document.getFeature().equals(layerFeature))) {
                trainingDocumentUpdated = true;
                break;
            }
        }

        // C. New Curation document arrives
        for (SourceDocument document : aRepository.listSourceDocuments(layerFeature.getProject())) {
            if (document.getState().equals(SourceDocumentState.CURATION_FINISHED)) {
                trainingDocumentUpdated = true;
                break;
            }
        }
        // D. tab-sep training documents
        for (TrainingDocument document : aAutomationService
                .listTabSepDocuments(aTemplate.getTrainFeature().getProject())) {
            if (!document.isProcessed() && document.getFeature() != null
                    && document.getFeature().equals(layerFeature)) {
                trainingDocumentUpdated = true;
                break;
            }
        }
        if (!trainingDocumentUpdated) {
            return aTemplate.getResult();
        }

        // if no other layer is used, use this as main train document,
        // otherwise, add all the
        // predictions and modify template
        File baseTrainFile = new File(miraDir, layerFeature.getLayer().getId() + "-"
                + layerFeature.getId() + ".train.base");
        File trainFile = new File(miraDir, layerFeature.getLayer().getId() + "-"
                + layerFeature.getId() + ".train");

        // generate final classifier, using all features generated

        String trainName = trainFile.getAbsolutePath();
        String finalClassifierModelName = aAutomationService.getMiraModel(layerFeature, false, null)
                .getAbsolutePath();
        getFeatureOtherLayer(aTemplate, aRepository, aAutomationService, aAnnotationService,
                aUserDao, beamSize, maxPosteriors, predictions, mira, predFile, predcitedFile,
                null);

        getFeaturesTabSep(aTemplate, aAutomationService, beamSize, maxPosteriors,
                layerFeature, predictions, mira, predFile, predcitedFile);

        generateTrainDocument(aTemplate, aRepository, aCurationDocumentService, aAnnotationService,
                aAutomationService, aUserDao, false);

        String trainTemplate;
        if (predictions.size() == 0) {
            trainTemplate = createTemplate(aTemplate.getTrainFeature(),
                    getMiraTemplateFile(layerFeature, aAutomationService), 0);
            FileUtils.copyFile(baseTrainFile, trainFile);
        }
        else {
            trainTemplate = createTemplate(aTemplate.getTrainFeature(),
                    getMiraTemplateFile(layerFeature, aAutomationService), predictions.size());
            buildTrainFile(baseTrainFile, trainFile, predictions);
        }

        boolean randomInit = false;
        
        switch (layerFeature.getLayer().getAnchoringMode()) {
        case SINGLE_TOKEN:
            // Nothing extra to do
            break;
        case TOKENS:
            mira.setIobScorer();
            break;
        default:
            throw new IllegalStateException("Unsupported anchoring mode: ["
                    + layerFeature.getLayer().getAnchoringMode() + "]");
        }
        mira.loadTemplates(trainTemplate);
        mira.setClip(sigma);
        mira.maxPosteriors = maxPosteriors;
        mira.beamSize = beamSize;
        int numExamples = mira.count(trainName, frequency);
        mira.initModel(randomInit);
        String trainResult = "";
        for (int i = 0; i < iterations; i++) {
            trainResult = mira.train(trainName, iterations, numExamples, i);
            mira.averageWeights(iterations * numExamples);
        }
        mira.saveModel(finalClassifierModelName);

        // all training documents are processed by now
        for (TrainingDocument document : aAutomationService
                .listTrainingDocuments(layerFeature.getProject())) {
            document.setProcessed(true);
        }
        for (TrainingDocument document : aAutomationService.listTabSepDocuments(layerFeature
                .getProject())) {
            document.setProcessed(true);
        }
        return trainResult;
    }

    private static void getFeatureOtherLayer(MiraTemplate aTemplate, DocumentService aRepository,
            AutomationService aAutomationService, AnnotationSchemaService aAnnotationService,
            UserDao aUserDao, int beamSize, boolean maxPosteriors, List<List<String>> predictions,
            Mira mira, File predFtFile, File predcitedFile, SourceDocument document)
        throws IOException, ClassNotFoundException, UIMAException
    {
        // other layers as training document
        for (AnnotationFeature feature : aTemplate.getOtherFeatures()) {
            int shiftColumns = 0;
            int nbest = 1;
            String modelName = aAutomationService.getMiraModel(feature, true, null)
                    .getAbsolutePath();
            if (!new File(modelName).exists()) {
                addOtherFeatureFromAnnotation(feature, aRepository, aAutomationService,
                        aAnnotationService, aUserDao, predictions, document);
                continue;
            }
            String testName = predFtFile.getAbsolutePath();

            PrintStream stream = new PrintStream(predcitedFile);
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            if (testName != null) {
                input = new BufferedReader(new FileReader(testName));
            }
            mira.loadModel(modelName);
            mira.setShiftColumns(shiftColumns);
            mira.nbest = nbest;
            mira.beamSize = beamSize;
            mira.maxPosteriors = maxPosteriors;
            mira.test(input, stream);

            LineIterator it = IOUtils.lineIterator(new FileReader(predcitedFile));
            List<String> annotations = new ArrayList<>();

            while (it.hasNext()) {
                String line = it.next();
                if (line.trim().equals("")) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(line, " ");
                String tag = "";
                while (st.hasMoreTokens()) {
                    tag = st.nextToken();
                }
                annotations.add(tag);
            }
            predictions.add(annotations);
        }
    }

    private static void getFeaturesTabSep(MiraTemplate aTemplate,
            AutomationService aAutomationService, int beamSize, boolean maxPosteriors,
            AnnotationFeature layerFeature, List<List<String>> predictions, Mira mira,
            File predFile, File predcitedFile)
        throws IOException, ClassNotFoundException, AutomationException
    {
        for (TrainingDocument document : aAutomationService
                .listTabSepDocuments(aTemplate.getTrainFeature().getProject())) {
            int shiftColumns = 0;
            int nbest = 1;
            String modelName = aAutomationService.getMiraModel(layerFeature, true, document)
                    .getAbsolutePath();
            if (!new File(modelName).exists()) {
                continue;
            }
            String testName = predFile.getAbsolutePath();

            PrintStream stream = new PrintStream(predcitedFile);
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            if (testName != null) {
                input = new BufferedReader(new FileReader(testName));
            }
            mira.loadModel(modelName);
            mira.setShiftColumns(shiftColumns);
            mira.nbest = nbest;
            mira.beamSize = beamSize;
            mira.maxPosteriors = maxPosteriors;
            try {
                mira.test(input, stream);
            }
            catch (Exception e) {
                throw new AutomationException(document.getName() + " is Invalid TAB-SEP file!");
            }

            LineIterator it = IOUtils.lineIterator(new FileReader(predcitedFile));
            List<String> annotations = new ArrayList<>();

            while (it.hasNext()) {
                String line = it.next();
                if (line.trim().equals("")) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(line, " ");
                String tag = "";
                while (st.hasMoreTokens()) {
                    tag = st.nextToken();
                }
                annotations.add(tag);
            }
            predictions.add(annotations);
        }
    }

    /**
     * Based on the other layer, add features for the prediction document
     *
     * @param aTemplate
     *            the template.
     * @param aRepository
     *            the repository.
     * @throws UIMAException
     *             hum?
     * @throws ClassNotFoundException
     *             hum?
     * @throws IOException
     *             hum?
     * @throws AnnotationException
     *             hum?
     * @throws AutomationException
     *             hum?
     */
    public static void addOtherFeatureToPredictDocument(MiraTemplate aTemplate,
            DocumentService aRepository, AnnotationSchemaService aAnnotationService,
            AutomationService aAutomationService, UserDao aUserDao)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException,
        AutomationException
    {
        AnnotationFeature layerFeature = aTemplate.getTrainFeature();

        File miraDir = aAutomationService.getMiraDir(layerFeature);
        for (SourceDocument document : aRepository.listSourceDocuments(layerFeature.getProject())) {
            List<List<String>> predictions = new ArrayList<>();
            File predFtFile = new File(miraDir, document.getId() + ".pred.ft");
            Mira mira = new Mira();
            int beamSize = 0;
            boolean maxPosteriors = false;
            File predcitedFile = new File(predFtFile.getAbsolutePath() + "-pred");

            getFeatureOtherLayer(aTemplate, aRepository, aAutomationService, aAnnotationService,
                    aUserDao, beamSize, maxPosteriors, predictions, mira, predFtFile, predcitedFile,
                    document);

            getFeaturesTabSep(aTemplate, aAutomationService, beamSize, maxPosteriors, layerFeature,
                    predictions, mira, predFtFile, predcitedFile);

            File basePredFile = new File(miraDir, document.getId() + ".pred");
            if (predictions.size() == 0) {
                createTemplate(aTemplate.getTrainFeature(),
                        getMiraTemplateFile(layerFeature, aAutomationService), 0);
                FileUtils.copyFile(predFtFile, basePredFile);
            }
            else {
                createTemplate(aTemplate.getTrainFeature(),
                        getMiraTemplateFile(layerFeature, aAutomationService), predictions.size());
                buildPredictFile(predFtFile, basePredFile, predictions,
                        aTemplate.getTrainFeature());
            }
        }
    }

    // add all predicted features and its own label at the end, to train a classifier.
    private static void buildTrainFile(File aBaseFile, File aTrainFile,
            List<List<String>> aPredictions)
        throws IOException
    {
        LineIterator it = IOUtils.lineIterator(new FileReader(aBaseFile));
        StringBuilder trainBuffer = new StringBuilder();
        int i = 0;
        while (it.hasNext()) {
            String line = it.next();
            if (line.trim().equals("")) {
                trainBuffer.append("\n");
                continue;
            }
            StringTokenizer st = new StringTokenizer(line, " ");
            String label = "";
            String feature = "";
            // Except the last token, which is the label, maintain the line
            while (st.hasMoreTokens()) {
                feature = st.nextToken();
                if (label.equals("")) { // first time
                    label = feature;
                    continue;
                }
                trainBuffer.append(label).append(" ");
                label = feature;

            }
            for (List<String> prediction : aPredictions) {
                trainBuffer.append(prediction.get(i)).append(" ");
            }
            // add its own label
            trainBuffer.append(label).append("\n");
            i++;
        }
        IOUtils.write(trainBuffer.toString(), new FileOutputStream(aTrainFile));

    }

    // add additional features predicted so that it will have the same number of features as the
    // classifier
    private static void buildPredictFile(File apredFt, File aPredFile,
            List<List<String>> aPredictions, AnnotationFeature aFeature)
        throws IOException
    {
        LineIterator it = IOUtils.lineIterator(new FileReader(apredFt));
        StringBuilder predBuffer = new StringBuilder();
        int i = 0;
        while (it.hasNext()) {
            String line = it.next();
            if (line.trim().equals("")) {
                predBuffer.append("\n");
                continue;
            }
            StringTokenizer st = new StringTokenizer(line, " ");
            // if the target feature is on multiple token, we do not need the morphological features
            // in the prediction file
            switch (aFeature.getLayer().getAnchoringMode()) {
            case TOKENS:
                predBuffer.append(st.nextToken()).append(" ");
                break;
            case SINGLE_TOKEN:
                while (st.hasMoreTokens()) {
                    predBuffer.append(st.nextToken()).append(" ");
                }
                break;
            default:
                throw new IllegalStateException("Unsupported anchoring mode: ["
                        + aFeature.getLayer().getAnchoringMode() + "]");
            }
            
            for (List<String> prediction : aPredictions) {
                predBuffer.append(prediction.get(i)).append(" ");
            }
            // add its
            predBuffer.append("\n");
            i++;
        }
        
        IOUtils.write(predBuffer.toString(), new FileOutputStream(aPredFile));
    }

    /**
     * Add new annotation to the CAS using the MIRA prediction. This is different from the add
     * methods in the {@link TypeAdapter}s in such a way that the begin and end offsets are always
     * exact so that no need to re-compute
     *
     * @param aJcas
     *            the JCas.
     * @param aFeature
     *            the feature.
     * @param aLabelValues
     *            the values.
     * @throws AnnotationException
     *             if the annotations could not be created/updated.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public static void automate(JCas aJcas, AnnotationFeature aFeature, List<String> aLabelValues)
        throws AnnotationException, IOException
    {

        String typeName = aFeature.getLayer().getName();
        String attachTypeName = aFeature.getLayer().getAttachType() == null ? null : aFeature
                .getLayer().getAttachType().getName();
        Type type = CasUtil.getType(aJcas.getCas(), typeName);
        Feature feature = type.getFeatureByBaseName(aFeature.getName());

        int i = 0;
        String prevNe = "O";
        int begin = 0;
        int end = 0;
        // remove existing annotations of this type, after all it is an
        // automation, no care
        clearAnnotations(aJcas, type);

        switch (aFeature.getLayer().getAnchoringMode()) {
        case TOKENS:
            for (Token token : select(aJcas, Token.class)) {
                String value = aLabelValues.get(i);
                AnnotationFS newAnnotation;
                if (value.equals("O") && prevNe.equals("O")) {
                    i++;
                    continue;
                }
                else if (value.equals("O") && !prevNe.equals("O")) {
                    newAnnotation = aJcas.getCas().createAnnotation(type, begin, end);
                    newAnnotation.setFeatureValueFromString(feature, prevNe.replace("B-", ""));
                    prevNe = "O";
                    aJcas.getCas().addFsToIndexes(newAnnotation);
                }
                else if (!value.equals("O") && prevNe.equals("O")) {
                    begin = token.getBegin();
                    end = token.getEnd();
                    prevNe = value;

                }
                else if (!value.equals("O") && !prevNe.equals("O")) {
                    if (value.replace("B-", "").replace("I-", "").equals(
                            prevNe.replace("B-", "").replace("I-", "")) && value.startsWith("B-")) {
                        newAnnotation = aJcas.getCas().createAnnotation(type, begin, end);
                        newAnnotation.setFeatureValueFromString(feature,
                                prevNe.replace("B-", "").replace("I-", ""));
                        prevNe = value;
                        begin = token.getBegin();
                        end = token.getEnd();
                        aJcas.getCas().addFsToIndexes(newAnnotation);

                    }
                    else if (value.replace("B-", "").replace("I-", "")
                            .equals(prevNe.replace("B-", "").replace("I-", ""))) {
                        i++;
                        end = token.getEnd();
                        continue;

                    }
                    else {
                        newAnnotation = aJcas.getCas().createAnnotation(type, begin, end);
                        newAnnotation.setFeatureValueFromString(feature,
                                prevNe.replace("B-", "").replace("I-", ""));
                        prevNe = value;
                        begin = token.getBegin();
                        end = token.getEnd();
                        aJcas.getCas().addFsToIndexes(newAnnotation);

                    }
                }

                i++;
            }
        case SINGLE_TOKEN: {
            // check if annotation is on an AttachType
            Feature attachFeature = null;
            Type attachType;
            if (attachTypeName != null) {
                attachType = CasUtil.getType(aJcas.getCas(), attachTypeName);
                attachFeature = attachType.getFeatureByBaseName(attachTypeName);
            }

            for (Token token : select(aJcas, Token.class)) {
                AnnotationFS newAnnotation = aJcas.getCas().createAnnotation(type, token.getBegin(),
                        token.getEnd());
                newAnnotation.setFeatureValueFromString(feature, aLabelValues.get(i));
                i++;
                if (attachFeature != null) {
                    token.setFeatureValue(attachFeature, newAnnotation);
                }
                aJcas.getCas().addFsToIndexes(newAnnotation);
            }
            break;
        }
        default:
            throw new IllegalStateException(
                    "Unsupported anchoring mode: [" + aFeature.getLayer().getAnchoringMode() + "]");
        }
    }

    public static void predict(MiraTemplate aTemplate, DocumentService aRepository,
            CorrectionDocumentService aCorrectionDocumentService,
            AutomationService aAutomationService, UserDao aUserDao)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        AnnotationFeature layerFeature = aTemplate.getTrainFeature();

        File miraDir = aAutomationService.getMiraDir(layerFeature);
        AutomationStatus status = aAutomationService.getAutomationStatus(aTemplate);
        for (SourceDocument document : aRepository.listSourceDocuments(layerFeature.getProject())) {
            File predFile = new File(miraDir, document.getId() + ".pred");
            Mira mira = new Mira();
            int shiftColumns = 0;
            int nbest = 1;
            int beamSize = 0;
            boolean maxPosteriors = false;
            String modelName = aAutomationService.getMiraModel(layerFeature, false, null)
                    .getAbsolutePath();
            String testName = predFile.getAbsolutePath();
            File predcitedFile = new File(predFile.getAbsolutePath() + "-pred");
            PrintStream stream = new PrintStream(predcitedFile);
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            if (testName != null) {
                input = new BufferedReader(new FileReader(testName));
            }
            mira.loadModel(modelName);
            mira.setShiftColumns(shiftColumns);
            mira.nbest = nbest;
            mira.beamSize = beamSize;
            mira.maxPosteriors = maxPosteriors;
            mira.test(input, stream);

            LOG.info("Prediction is wrtten to a MIRA File. To be done is writing back to the CAS");
            LineIterator it = IOUtils.lineIterator(new FileReader(predcitedFile));
            List<String> annotations = new ArrayList<>();

            while (it.hasNext()) {
                String line = it.next();
                if (line.trim().equals("")) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(line, " ");
                String tag = "";
                while (st.hasMoreTokens()) {
                    tag = st.nextToken();

                }
                annotations.add(tag);
            }

            LOG.info(annotations.size() + " Predictions found to be written to the CAS");
            JCas jCas = null;
            User user = aUserDao.getCurrentUser();
            try {
                AnnotationDocument annoDocument = aRepository.getAnnotationDocument(document,
                        user);
                jCas = aRepository.readAnnotationCas(annoDocument);
                automate(jCas, layerFeature, annotations);
            }
            catch (DataRetrievalFailureException e) {
                automate(jCas, layerFeature, annotations);
                LOG.info("Predictions found are written to the CAS");
                aCorrectionDocumentService.writeCorrectionCas(jCas, document);
                status.setAnnoDocs(status.getAnnoDocs() - 1);
            }
            automate(jCas, layerFeature, annotations);
            LOG.info("Predictions found are written to the CAS");
            aCorrectionDocumentService.writeCorrectionCas(jCas, document);
            status.setAnnoDocs(status.getAnnoDocs() - 1);
        }
    }
    
    public static void clearAnnotations(JCas aJCas, Type aType)
        throws IOException
    {
        List<AnnotationFS> annotationsToRemove = new ArrayList<>();
        annotationsToRemove.addAll(select(aJCas.getCas(), aType));
        for (AnnotationFS annotation : annotationsToRemove) {
            aJCas.removeFsFromIndexes(annotation);
        }
    }
    
    public static Map<Integer, String> getMultipleAnnotation(
            AnnotationSchemaService aAnnotationService, Sentence sentence,
            AnnotationFeature aFeature)
        throws CASException
    {
        SpanAdapter adapter = (SpanAdapter) aAnnotationService.getAdapter(aFeature.getLayer());
        Map<Integer, String> multAnno = new HashMap<>();
        Type type = getType(sentence.getCAS(), adapter.getAnnotationTypeName());
        for (AnnotationFS fs : selectCovered(type, sentence)) {
            boolean isBegin = true;
            Feature labelFeature = fs.getType().getFeatureByBaseName(aFeature.getName());
            for (Token token : selectCovered(Token.class, fs)) {
                if (multAnno.get(getAddr(token)) == null) {
                    if (isBegin) {
                        multAnno.put(getAddr(token),
                                "B-" + fs.getFeatureValueAsString(labelFeature));
                        isBegin = false;
                    }
                    else {
                        multAnno.put(getAddr(token),
                                "I-" + fs.getFeatureValueAsString(labelFeature));
                    }
                }
            }
        }
        return multAnno;
    }
    
    private static List<String> getAnnotation(TypeAdapter aAdapter, Sentence aSentence,
            AnnotationFeature aFeature)
    {
        CAS cas = aSentence.getCAS();
        
        Type type = getType(cas, aAdapter.getAnnotationTypeName());
        List<String> annotations = new ArrayList<>();

        for (Token token : selectCovered(Token.class, aSentence)) {
            List<AnnotationFS> tokenLevelAnnotations = selectCovered(type, token);
            if (tokenLevelAnnotations.size() > 0) {
                AnnotationFS anno = tokenLevelAnnotations.get(0);
                Feature labelFeature = anno.getType().getFeatureByBaseName(aFeature.getName());
                annotations.add(anno.getFeatureValueAsString(labelFeature));
            }
            else {
                annotations.add(NILL);
            }
        }
        return annotations;
    }
}
