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
package de.tudarmstadt.ukp.clarin.webanno.automation.util;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getQualifiedLabel;
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
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Resource;
import javax.persistence.NoResultException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import edu.lium.mira.Mira;

/**
 * A utility class for the automation modules
 *
 * @author Seid Muhie Yimam
 *
 */
public class AutomationUtil
{

    @Resource(name = "documentRepository")
    private RepositoryService repository;

    @Resource(name = "annotationService")
    private static AnnotationService annotationService;

    public static void repeateAnnotation(BratAnnotatorModel aModel, RepositoryService aRepository,
            AnnotationService aAnnotationService, int aStart, int aEnd, AnnotationFeature aFeature)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {

        SourceDocument sourceDocument = aModel.getDocument();
        JCas jCas = aRepository.getCorrectionDocumentContent(sourceDocument);

        // get selected text, concatenations of tokens
        String selectedText = BratAjaxCasUtil.getSelectedText(jCas, aStart, aEnd);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);

        MiraTemplate template;
        try {
            template = aRepository.getMiraTemplate(aFeature);
        }
        catch (NoResultException e) {
            template = null;
        }

        int beginOffset = aModel.getSentenceBeginOffset();

        int endOffset;
        if (template != null && template.isPredictInThisPage()) {
            endOffset = BratAjaxCasUtil.getLastSentenceEndOffsetInDisplayWindow(jCas,
                    aModel.getSentenceAddress(), aModel.getWindowSize());
        }
        else {

            endOffset = BratAjaxCasUtil.selectByAddr(jCas, aModel.getLastSentenceAddress())
                    .getEnd();
        }
        for (Sentence sentence : selectCovered(jCas, Sentence.class, beginOffset, endOffset)) {
            String sentenceText = sentence.getCoveredText().toLowerCase();
            for (int i = -1; (i = sentenceText.indexOf(selectedText.toLowerCase(), i)) != -1; i = i
                    + selectedText.length()) {
                if (selectCovered(jCas, Token.class, sentence.getBegin() + i,
                        sentence.getBegin() + i + selectedText.length()).size() > 0) {

                    SpanAdapter adapter = (SpanAdapter) getAdapter(aFeature.getLayer(),
                            aAnnotationService);
                    adapter.add(jCas, sentence.getBegin() + i, sentence.getBegin() + i
                            + selectedText.length() - 1, aFeature, aFeature.getName());

                }
            }
        }
        aRepository.createCorrectionDocumentContent(jCas, aModel.getDocument(), user);
    }

    public static void deleteAnnotation(BratAnnotatorModel aModel, RepositoryService aRepository,
            AnnotationService aAnnotationService, int aStart, int aEnd, AnnotationFeature aFeature)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {

        SourceDocument sourceDocument = aModel.getDocument();
        JCas jCas = aRepository.getCorrectionDocumentContent(sourceDocument);

        // get selected text, concatenations of tokens
        String selectedText = BratAjaxCasUtil.getSelectedText(jCas, aStart, aEnd);

        TypeAdapter adapter = getAdapter(aFeature.getLayer(), annotationService);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);

        MiraTemplate template;
        try {
            template = aRepository.getMiraTemplate(aFeature);
        }
        catch (NoResultException e) {
            template = null;
        }

        int beginOffset = aModel.getSentenceBeginOffset();

        int endOffset;
        if (template != null && template.isPredictInThisPage()) {
            endOffset = BratAjaxCasUtil.getLastSentenceEndOffsetInDisplayWindow(jCas,
                    aModel.getSentenceAddress(), aModel.getWindowSize());
        }
        else {

            endOffset = BratAjaxCasUtil.selectByAddr(jCas, aModel.getLastSentenceAddress())
                    .getEnd();
        }
        for (Sentence sentence : selectCovered(jCas, Sentence.class, beginOffset, endOffset)) {
            String sentenceText = sentence.getCoveredText().toLowerCase();
            for (int i = -1; (i = sentenceText.indexOf(selectedText.toLowerCase(), i)) != -1; i = i
                    + selectedText.length()) {
                if (selectCovered(jCas, Token.class, sentence.getBegin() + i,
                        sentence.getBegin() + i + selectedText.length()).size() > 0) {
                    adapter.delete(jCas, aFeature, sentence.getBegin() + i, sentence.getBegin() + i
                            + selectedText.length() - 1, getQualifiedLabel(aFeature));
                }
            }
        }
        aRepository.createCorrectionDocumentContent(jCas, aModel.getDocument(), user);
    }

    public static void addOtherFeatureTrainDocument(MiraTemplate aTemplate,
            RepositoryService aRepository)
        throws IOException, UIMAException, ClassNotFoundException
    {
        File miraDir = aRepository.getMiraDir(aTemplate.getTrainFeature());
        if (!miraDir.exists()) {
            FileUtils.forceMkdir(miraDir);
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);
        for (AnnotationFeature feature : aTemplate.getOtherFeatures()) {
            if (feature.equals(aTemplate.getTrainFeature())) {
                continue;
            }
            File trainFile = new File(miraDir, feature.getId() + ".train");
            boolean documentChanged = false;
            for (SourceDocument document : aRepository.listSourceDocuments(feature.getProject())) {
                if (!document.isProcessed()
                        && (document.getFeature() != null && document.getFeature().equals(feature))) {
                    documentChanged = true;
                    break;
                }
            }
            if (!documentChanged && trainFile.exists()) {
                continue;
            }

            BufferedWriter trainOut = new BufferedWriter(new FileWriter(trainFile));
            for (SourceDocument sourceDocument : aRepository.listSourceDocuments(feature
                    .getProject())) {
                if ((sourceDocument.isTrainingDocument() && sourceDocument.getFeature() != null && sourceDocument
                        .getFeature().equals(feature))) {
                    JCas jCas = aRepository.readJCas(sourceDocument, sourceDocument.getProject(),
                            user);
                    for (Sentence sentence : select(jCas, Sentence.class)) {
                        trainOut.append(getMiraLine(sentence, feature, aTemplate).toString() + "\n");
                    }
                }
                sourceDocument.setProcessed(false);
            }
            trainOut.close();
        }

    }

    public static void generateTrainDocument(MiraTemplate aTemplate, RepositoryService aRepository,
            boolean aBase)
        throws IOException, UIMAException, ClassNotFoundException
    {
        File miraDir = aRepository.getMiraDir(aTemplate.getTrainFeature());
        if (!miraDir.exists()) {
            FileUtils.forceMkdir(miraDir);
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);
        AnnotationFeature feature = aTemplate.getTrainFeature();
        boolean documentChanged = false;
        for (SourceDocument document : aRepository.listSourceDocuments(feature.getProject())) {
            if (!document.isProcessed()
                    && (document.getFeature() != null && document.getFeature().equals(feature))) {
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
        BufferedWriter trainOut = new BufferedWriter(new FileWriter(trainFile));
        for (SourceDocument sourceDocument : aRepository.listSourceDocuments(feature.getProject())) {
            if ((sourceDocument.isTrainingDocument() && sourceDocument.getFeature() != null && sourceDocument
                    .getFeature().equals(feature))) {
                JCas jCas = aRepository.readJCas(sourceDocument, sourceDocument.getProject(), user);
                for (Sentence sentence : select(jCas, Sentence.class)) {
                    if (aBase) {// base training document
                        trainOut.append(getMiraLine(sentence, null, aTemplate).toString() + "\n");
                    }
                    else {// training document with other features
                        trainOut.append(getMiraLine(sentence, feature, aTemplate).toString() + "\n");
                    }
                }
                sourceDocument.setProcessed(!aBase);
            }
        }
        trainOut.close();
    }

    public static void generatePredictDocument(MiraTemplate aTemplate, RepositoryService aRepository)
        throws IOException, UIMAException, ClassNotFoundException
    {
        File miraDir = aRepository.getMiraDir(aTemplate.getTrainFeature());
        if (!miraDir.exists()) {
            FileUtils.forceMkdir(miraDir);
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);
        AnnotationFeature feature = aTemplate.getTrainFeature();
        boolean documentChanged = false;
        for (SourceDocument document : aRepository.listSourceDocuments(feature.getProject())) {
            if (!document.isProcessed() && !document.isTrainingDocument()) {
                documentChanged = true;
                break;
            }
        }
        if (!documentChanged) {
            return;
        }
        for (SourceDocument document : aRepository.listSourceDocuments(feature.getProject())) {
            if (!document.isProcessed() && !document.isTrainingDocument()) {
                File predFile = new File(miraDir, document.getId() + ".pred.ft");
                BufferedWriter predOut = new BufferedWriter(new FileWriter(predFile));
                JCas jCas = aRepository.readJCas(document, document.getProject(), user);
                BratAnnotatorUtility.clearJcas(jCas, document, user, aRepository);
                for (Sentence sentence : select(jCas, Sentence.class)) {
                    predOut.append(getMiraLine(sentence, null, aTemplate).toString() + "\n");
                }
                predOut.close();
            }
        }
    }

    private static StringBuffer getMiraLine(Sentence sentence, AnnotationFeature aLayerFeature,
            MiraTemplate aAModel)
        throws CASException
    {
        StringBuffer sb = new StringBuffer();

        for (Token token : selectCovered(sentence.getCAS().getJCas(), Token.class,
                sentence.getBegin(), sentence.getEnd())) {
            String word = token.getCoveredText();

            char[] words = word.toCharArray();

            String prefix1 = aAModel.isPrefix1() ? Character.toString(words[0]) + " " : "";
            String prefix2 = aAModel.isPrefix2() ? (words.length > 1 ? prefix1.trim()
                    + (Character.toString(words[1]).trim().equals("") ? "__nil__" : Character
                            .toString(words[1])) : "__nil__")
                    + " " : "";
            String prefix3 = aAModel.isPrefix3() ? (words.length > 2 ? prefix2.trim()
                    + (Character.toString(words[2]).trim().equals("") ? "__nil__" : Character
                            .toString(words[2])) : "__nil__")
                    + " " : "";
            String prefix4 = aAModel.isPrefix4() ? (words.length > 3 ? prefix3.trim()
                    + (Character.toString(words[3]).trim().equals("") ? "__nil__" : Character
                            .toString(words[3])) : "__nil__")
                    + " " : "";
            String suffix1 = aAModel.isSuffix1() ? Character.toString(words[words.length - 1])
                    + " " : "";
            String suffix2 = aAModel.isSuffix2() ? (words.length > 1 ? (Character
                    .toString(words[words.length - 2]).trim().equals("") ? "__nil__" : Character
                    .toString(words[words.length - 2])) + suffix1.trim() : "__nil__")
                    + " " : "";
            String suffix3 = aAModel.isSuffix3() ? (words.length > 2 ? (Character
                    .toString(words[words.length - 3]).trim().equals("") ? "__nil__" : Character
                    .toString(words[words.length - 3])) + suffix2.trim() : "__nil__")
                    + " " : "";
            String suffix4 = aAModel.isSuffix4() ? (words.length > 3 ? (Character
                    .toString(words[words.length - 4]).trim().equals("") ? "__nil__" : Character
                    .toString(words[words.length - 4])) + suffix3.trim() : "__nil__")
                    + " " : "";

            String nl = "\n";
            String tag = "";
            List<String> annotations = new ArrayList<String>();
            if (aLayerFeature != null) {
                TypeAdapter adapter = TypeUtil.getAdapter(aLayerFeature.getLayer(),
                        annotationService);
                if (aLayerFeature.getLayer().isMultipleTokens()) {
                    Map<Integer, String> multAnno = ((SpanAdapter) adapter).getMultipleAnnotation(
                            sentence, aLayerFeature);
                    tag = multAnno.get(token.getAddress()) == null ? "O" : multAnno.get(token
                            .getAddress());
                }
                else {
                    annotations = adapter.getAnnotation(sentence.getCAS().getJCas(), aLayerFeature,
                            token.getBegin(), token.getEnd());
                    tag = annotations.size() == 0 ? "__nill__" : annotations.get(0);
                }

            }
            sb.append(word + " " + prefix1 + prefix2 + prefix3 + prefix4 + suffix1 + suffix2
                    + suffix3 + suffix4 + tag + nl);
        }
        return sb;

    }

    /**
     * When additional layers are used as training feature, the training document should be
     * auto-predicted with the other layers. Example, if the train layer is Named Entity and POS
     * layer is used as additional feature, the training document should be predicted using the POS
     * layer documents for POS annotation
     */
    public static void otherFeatureClassifiers(MiraTemplate aTemplate, RepositoryService aRepository)
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

        for (AnnotationFeature feature : aTemplate.getOtherFeatures()) {
            for (SourceDocument document : aRepository.listSourceDocuments(aTemplate
                    .getTrainFeature().getProject())) {
                if (!document.isProcessed() && document.getFeature() != null
                        && document.getFeature().equals(feature)) {
                    documentChanged = true;
                    break;
                }
            }
        }
        if (!documentChanged) {
            return;
        }

        for (AnnotationFeature feature : aTemplate.getOtherFeatures()) {
            templateName = createOtherTemplate(aRepository, aTemplate,
                    getMiraTemplateFile(feature, aRepository), 0);

            File miraDir = aRepository.getMiraDir(aTemplate.getTrainFeature());
            File trainFile = new File(miraDir, feature.getId() + ".train");
            String initalModelName = "";
            String trainName = trainFile.getAbsolutePath();

            String modelName = aRepository.getMiraModel(feature, true).getAbsolutePath();

            boolean randomInit = false;

            if (!feature.getLayer().isLockToTokenOffset()) {
                mira.setIobScorer();
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

    public static String createOtherTemplate(RepositoryService aRepository, MiraTemplate aTemplate,
            File templateFile, int other)
        throws IOException
    {

        StringBuffer sb = new StringBuffer();
        int i = 1;
        i = setMorphoTemplate(aTemplate, sb, i);
        setNgramForLable(aTemplate, sb, i, other);

        sb.append("\n");
        sb.append("B\n");
        FileUtils.writeStringToFile(templateFile, sb.toString());
        return templateFile.getAbsolutePath();
    }

    private static void setNgramForLable(MiraTemplate aTemplate, StringBuffer sb, int i, int other)
    {
        int temp = i;
        sb.append("U" + String.format("%02d", i) + "%x[0,0]\n");
        i++;
        sb.append("U" + String.format("%02d", i) + "%x[-1,0]\n");
        i++;
        sb.append("U" + String.format("%02d", i) + "%x[1,0]\n");
        i++;
        sb.append("U" + String.format("%02d", i) + "%x[0,0]" + "%x[1,0]\n");
        i++;
        sb.append("U" + String.format("%02d", i) + "%x[-1,0]" + "%x[0,0]\n");
        i++;
        sb.append("U" + String.format("%02d", i) + "%x[-1,0]" + "%x[0,0]" + "%x[1,0]\n");
        i++;
        sb.append("\n");

        i = temp;
        sb.append("B" + String.format("%02d", i) + "%x[0,0]\n");
        i++;
        sb.append("B" + String.format("%02d", i) + "%x[-1,0]\n");
        i++;
        sb.append("B" + String.format("%02d", i) + "%x[1,0]\n");
        i++;
        sb.append("B" + String.format("%02d", i) + "%x[0,0]" + "%x[1,0]\n");
        i++;
        sb.append("B" + String.format("%02d", i) + "%x[-1,0]" + "%x[0,0]\n");
        i++;
        sb.append("B" + String.format("%02d", i) + "%x[-1,0]" + "%x[0,0]" + "%x[1,0]\n");
        i++;
        sb.append("\n");

        if (other > 0) {// consider other layer annotations as features
            while (other > 0) {
                other--;
                sb.append("B" + String.format("%02d", i) + "%x[0," + temp + "]\n");
                i++;
                sb.append("B" + String.format("%02d", i) + "%x[0,0] %x[0," + temp + "]\n");
                i++;
                sb.append("B" + String.format("%02d", i) + "%x[-1," + temp + "] %x[0," + temp
                        + "]\n");
                i++;
                temp++;
            }
        }
    }

    private static int setMorphoTemplate(MiraTemplate aTemplate, StringBuffer sb, int i)
    {

        sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
        i++;
        sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
        i++;
        sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
        i++;
        sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
        i++;
        sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
        i++;
        sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
        i++;
        sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
        i++;
        sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
        i++;
        sb.append("\n");

        return i;
    }

    public static File getMiraTemplateFile(AnnotationFeature aFeature, RepositoryService aRepository)
    {
        return new File(aRepository.getMiraDir(aFeature).getAbsolutePath(), aFeature.getId()
                + "-template");
    }

    /**
     * Based on the other layer, predict features for the training document
     */
    public static String generateFinalClassifier(MiraTemplate aTemplate,
            RepositoryService aRepository)
        throws CASException, UIMAException, ClassNotFoundException, IOException,
        BratAnnotationException
    {
        int frequency = 2;
        double sigma = 1;
        int iterations = 10;
        int beamSize = 0;
        boolean maxPosteriors = false;
        AnnotationFeature layerFeature = aTemplate.getTrainFeature();
        List<List<String>> predictions = new ArrayList<List<String>>();

        File miraDir = aRepository.getMiraDir(layerFeature);
        Mira mira = new Mira();
        File predFile = new File(miraDir, layerFeature.getLayer().getId() + "-"
                + layerFeature.getId() + ".train.ft");
        File predcitedFile = new File(predFile.getAbsolutePath() + "-pred");

        boolean documentChanged = false;
        for (SourceDocument document : aRepository.listSourceDocuments(layerFeature.getProject())) {
            if (!document.isProcessed()) {
                documentChanged = true;
                break;
            }
        }
        if (!documentChanged) {
            return aTemplate.getResult();
        }

        // if no other layer is used, use this as main train document, otherwise, add all the
        // predictions and modify template
        File baseTrainFile = new File(miraDir, layerFeature.getLayer().getId() + "-"
                + layerFeature.getId() + ".train.base");
        File trainFile = new File(miraDir, layerFeature.getLayer().getId() + "-"
                + layerFeature.getId() + ".train");

        // generate final classifier, using all features generated

        String trainName = trainFile.getAbsolutePath();
        String finalClassifierModelName = aRepository.getMiraModel(layerFeature, false).getAbsolutePath();
        if(new File(finalClassifierModelName).exists()){
            return aTemplate.getResult();
        }

        for (AnnotationFeature feature : aTemplate.getOtherFeatures()) {
            int shiftColumns = 0;
            int nbest = 1;
            String modelName = aRepository.getMiraModel(feature, true).getAbsolutePath();
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
            mira.test(input, stream);

            LineIterator it = IOUtils.lineIterator(new FileReader(predcitedFile));
            List<String> annotations = new ArrayList<String>();

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
        generateTrainDocument(aTemplate, aRepository, false);

        String trainTemplate;
        if (predictions.size() == 0) {
            trainTemplate = createOtherTemplate(aRepository, aTemplate,
                    getMiraTemplateFile(layerFeature, aRepository), 0);
            FileUtils.copyFile(baseTrainFile, trainFile);
        }
        else {
            trainTemplate = createOtherTemplate(aRepository, aTemplate,
                    getMiraTemplateFile(layerFeature, aRepository), predictions.size());
            buildTrainFile(baseTrainFile, trainFile, predictions);
        }

        boolean randomInit = false;
        if (!layerFeature.getLayer().isLockToTokenOffset()) {
            mira.setIobScorer();
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
        return trainResult;
    }

    /**
     * Based on the other layer, add features for the prediction document
     */
    public static void addOtherFeatureToPredictDocument(MiraTemplate aTemplate,
            RepositoryService aRepository)
        throws CASException, UIMAException, ClassNotFoundException, IOException,
        BratAnnotationException
    {
        AnnotationFeature layerFeature = aTemplate.getTrainFeature();

        File miraDir = aRepository.getMiraDir(layerFeature);
        for (SourceDocument document : aRepository.listSourceDocuments(layerFeature.getProject())) {
            List<List<String>> predictions = new ArrayList<List<String>>();
            if (!document.isProcessed() && !document.isTrainingDocument()) {
                File predFtFile = new File(miraDir, document.getId() + ".pred.ft");
                for (AnnotationFeature feature : aTemplate.getOtherFeatures()) {
                    Mira mira = new Mira();
                    int shiftColumns = 0;
                    int nbest = 1;
                    int beamSize = 0;
                    boolean maxPosteriors = false;
                    String modelName = aRepository.getMiraModel(feature, true).getAbsolutePath();
                    if (!new File(modelName).exists()) {
                        continue;
                    }
                    String testName = predFtFile.getAbsolutePath();
                    File predcitedFile = new File(predFtFile.getAbsolutePath() + "-pred");
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
                    List<String> annotations = new ArrayList<String>();

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
                File basePredFile = new File(miraDir, document.getId() + ".pred");
                if (predictions.size() == 0) {
                    createOtherTemplate(aRepository, aTemplate,
                            getMiraTemplateFile(layerFeature, aRepository), 0);
                    FileUtils.copyFile(predFtFile, basePredFile);
                }
                else {
                    createOtherTemplate(aRepository, aTemplate,
                            getMiraTemplateFile(layerFeature, aRepository), predictions.size());
                    buildPredictFile(predFtFile, basePredFile, predictions);
                }
            }
        }
    }

    private static void buildTrainFile(File aBaseFile, File aTrainFile,
            List<List<String>> aPredictions)
        throws IOException
    {
        LineIterator it = IOUtils.lineIterator(new FileReader(aBaseFile));
        StringBuffer trainBuffer = new StringBuffer();
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
                trainBuffer.append(label + " ");
                label = feature;

            }
            for (List<String> prediction : aPredictions) {
                trainBuffer.append(prediction.get(i) + " ");
            }
            // add its
            trainBuffer.append(label + "\n");
            i++;
        }
        IOUtils.write(trainBuffer.toString(), new FileOutputStream(aTrainFile));

    }

    private static void buildPredictFile(File apredFt, File aPredFile,
            List<List<String>> aPredictions)
        throws IOException
    {
        LineIterator it = IOUtils.lineIterator(new FileReader(apredFt));
        StringBuffer predBuffer = new StringBuffer();
        int i = 0;
        while (it.hasNext()) {
            String line = it.next();
            if (line.trim().equals("")) {
                predBuffer.append("\n");
                continue;
            }
            StringTokenizer st = new StringTokenizer(line, " ");
            while (st.hasMoreTokens()) {
                String feature = st.nextToken();
                predBuffer.append(feature + " ");
            }
            for (List<String> prediction : aPredictions) {
                predBuffer.append(prediction.get(i) + " ");
            }
            // add its
            predBuffer.append("\n");
            i++;
        }
        IOUtils.write(predBuffer.toString(), new FileOutputStream(aPredFile));

    }

    public static void predict(MiraTemplate aTemplate, RepositoryService aRepository)
        throws CASException, UIMAException, ClassNotFoundException, IOException,
        BratAnnotationException
    {
        AnnotationFeature layerFeature = aTemplate.getTrainFeature();

        File miraDir = aRepository.getMiraDir(layerFeature);
        for (SourceDocument document : aRepository.listSourceDocuments(layerFeature.getProject())) {
            if (!document.isProcessed() && !document.isTrainingDocument()) {
                File predFile = new File(miraDir, document.getId() + ".pred");
                Mira mira = new Mira();
                int shiftColumns = 0;
                int nbest = 1;
                int beamSize = 0;
                boolean maxPosteriors = false;
                String modelName = aRepository.getMiraModel(layerFeature, false).getAbsolutePath();
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

                LineIterator it = IOUtils.lineIterator(new FileReader(predcitedFile));
                List<String> annotations = new ArrayList<String>();

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

                JCas jCas = null;
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = aRepository.getUser(username);
                try {
                    AnnotationDocument annoDocument = aRepository.getAnnotationDocument(document,
                            user);
                    jCas = aRepository.getAnnotationDocumentContent(annoDocument);
                }
                catch (DataRetrievalFailureException e) {

                }

                TypeAdapter adapter = TypeUtil.getAdapter(layerFeature.getLayer(),
                        annotationService);
                adapter.automate(jCas, layerFeature, annotations);
                aRepository.createCorrectionDocumentContent(jCas, document, user);
                document.setProcessed(true);
            }
        }

    }
}
