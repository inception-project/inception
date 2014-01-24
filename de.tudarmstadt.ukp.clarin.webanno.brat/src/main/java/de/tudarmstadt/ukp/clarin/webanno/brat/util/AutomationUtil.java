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
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
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

    public static void predict(BratAnnotatorModel aModel, RepositoryService aRepository,
            AnnotationService aAnnotationService, int aStart, int aEnd, Tag aTag)
        throws UIMAException, ClassNotFoundException, IOException, BratAnnotationException
    {

        SourceDocument sourceDocument = aModel.getDocument();
        JCas jCas = aRepository.getCorrectionDocumentContent(sourceDocument);

        // get selected text, concatenations of tokens
        String selectedText = "";
        for (Token coveredToken : selectCovered(jCas, Token.class, aStart, aEnd)) {
            selectedText = selectedText + " " + coveredToken.getCoveredText();
        }
        selectedText = selectedText.trim();

        BratAjaxCasController bratAjaxCasController = new BratAjaxCasController(aRepository,
                aAnnotationService);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);

        MiraTemplate template;
        try {
            template = aRepository.getMiraTemplate(aTag.getTagSet());
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
            for (int i = -1; (i = sentenceText.indexOf(selectedText.toLowerCase(), i)) != -1; i++) {
                if (selectCovered(jCas, Token.class, sentence.getBegin() + i,
                        sentence.getBegin() + i + selectedText.length()).size() > 0) {
                    bratAjaxCasController.createSpanAnnotation(jCas, sentence.getBegin() + i,
                            sentence.getBegin() + i + selectedText.length(),
                            getQualifiedLabel(aTag), null, null);
                }
            }
        }
        aRepository.createCorrectionDocumentContent(jCas, aModel.getDocument(), user);
    }

    public static boolean casToMiraTrainData(MiraTemplate aTemplate, RepositoryService aRepository)
        throws IOException, UIMAException, ClassNotFoundException
    {

        TagSet layer = aTemplate.getTrainTagSet();
        TagSet fLayer = aTemplate.getFeatureTagSets().size() > 0 ? aTemplate.getFeatureTagSets()
                .iterator().next() : null;

        File miraDir = aRepository.getMiraDir(layer);
        if (!miraDir.exists()) {
            FileUtils.forceMkdir(miraDir);
        }
        File trainFile = new File(miraDir, "train");

        BufferedWriter trainOut = new BufferedWriter(new FileWriter(trainFile));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);

        boolean existsTemplate = getMiraTemplateFile(layer, aRepository).exists();
        boolean templateChanged = false;
        if (existsTemplate) {
            templateChanged = isTemplateChanged(aTemplate, aRepository, layer, fLayer);
        }

        // We have a problem merging MIRA models currently
        // the option is, if at least one new training file is added, it will re-create the model
        // using all documents
        boolean processed = true;
        for (SourceDocument sourceDocument : aRepository.listSourceDocuments(layer.getProject())) {
            if ((sourceDocument.isTrainingDocument() || sourceDocument.getState().equals(
                    SourceDocumentState.CURATION_FINISHED))
                    && (!sourceDocument.isProcessed() || templateChanged)) {
                processed = false;
                break;
            }
        }
        if (processed) {
            trainOut.close();
            return processed;
        }
        for (SourceDocument sourceDocument : aRepository.listSourceDocuments(layer.getProject())) {
            // train documents per layer (those for different layer should be ignored
            if (sourceDocument.getTemplate() != null
                    && sourceDocument.getTemplate().equals(aTemplate)
                    && (sourceDocument.isTrainingDocument() || sourceDocument.getState().equals(
                            SourceDocumentState.CURATION_FINISHED))) {
                JCas jCas = aRepository.readJCas(sourceDocument, sourceDocument.getProject(), user);
                for (Sentence sentence : select(jCas, Sentence.class)) {

                    trainOut.append(getMiraLines(sentence, false, layer, fLayer, aTemplate)
                            .toString() + "\n");
                }
                sourceDocument.setProcessed(true);
            }

        }
        trainOut.close();
        return processed;

    }

    private static boolean isTemplateChanged(MiraTemplate aTemplate, RepositoryService aRepository,
            TagSet aLayer, TagSet aFLayer)
        throws IOException
    {
        boolean templateChanged = false;
        File existingTemplateFile = AutomationUtil.getMiraTemplateFile(aLayer, aRepository);
        File thisTemplateFile = new File(existingTemplateFile.getAbsolutePath() + "bkp");
        if (!FileUtils.contentEquals(
                existingTemplateFile,
                new File(AutomationUtil.createMiraTemplate(aLayer, aRepository, aTemplate, aLayer,
                        aFLayer, thisTemplateFile)))) {
            templateChanged = true;
        }
        return templateChanged;
    }

    private static StringBuffer getMiraLines(Sentence sentence, boolean aPredict, TagSet aTagSet,
            TagSet aFeatureTagSet, MiraTemplate aAModel)
        throws CASException
    {
        StringBuffer sb = new StringBuffer();

        TypeAdapter adapter = TypeUtil.getAdapter(aTagSet);
        Map<Integer, String> neTags = new HashMap<Integer, String>();
        getNeTags(sentence, adapter, neTags);

        for (Token token : selectCovered(sentence.getCAS().getJCas(), Token.class,
                sentence.getBegin(), sentence.getEnd())) {
            String word = token.getCoveredText();
            String capitalized = aAModel.isCapitalized() ? (Character.isUpperCase(word
                    .codePointAt(0)) ? "Y " : "N ") : "";
            String containsNUmber = aAModel.isContainsNumber() ? (word.matches(".*\\d.*") ? "Y "
                    : "N ") : "";

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
            String prefix5 = aAModel.isPrefix5() ? (words.length > 4 ? prefix4.trim()
                    + (Character.toString(words[4]).trim().equals("") ? "__nil__" : Character
                            .toString(words[4])) : "__nil__")
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
            String suffix5 = aAModel.isSuffix5() ? (words.length > 4 ? (Character
                    .toString(words[words.length - 5]).trim().equals("") ? "__nil__" : Character
                    .toString(words[words.length - 5])) + suffix4.trim() : "__nil__")
                    + " " : "";

            String nl = "\n";
            String featureTagSet = "";
            // TODO: when free annotation layers defined, check if tagset is on multiple token or
            // not
            if (adapter.getLabelPrefix().equals(AnnotationTypeConstant.NAMEDENTITY_PREFIX)
                    && aFeatureTagSet != null && aFeatureTagSet.getId() > 0) {
                TypeAdapter featureAdapter = TypeUtil.getAdapter(aFeatureTagSet);
                List<String> featureAnnotations = featureAdapter.getAnnotation(sentence.getCAS()
                        .getJCas(), token.getBegin(), token.getEnd());
                featureTagSet = featureAnnotations.size() == 0 ? "" : featureAnnotations.get(0)
                        + " ";

            }
            // if the another layer is used as a feature when building the template
            // we should provide NILL so that MIRA will not fail
            featureTagSet = aFeatureTagSet == null ? "" : featureTagSet.equals("") ? "__nil__"
                    : featureTagSet;

            List<String> annotations = adapter.getAnnotation(sentence.getCAS().getJCas(),
                    token.getBegin(), token.getEnd());
            String tag = "";
            if (adapter.getLabelPrefix().equals(AnnotationTypeConstant.NAMEDENTITY_PREFIX)
                    && !aPredict) {
                tag = neTags.get(token.getAddress()) == null ? "O" : neTags.get(token.getAddress());
            }
            else {
                tag = aPredict == true ? "" : annotations.size() == 0 ? "__nill__" : annotations
                        .get(0);
            }
            sb.append(word + " " + capitalized

            /* + getVowels(word, FileUtils.readFileToString(new File(argv[1]))) + " " */

            + containsNUmber + prefix1 + prefix2 + prefix3 + prefix4 + prefix5 + suffix1 + suffix2
                    + suffix3 + suffix4 + suffix5 + featureTagSet + tag + nl);
        }
        return sb;

    }

    private static void getNeTags(Sentence sentence, TypeAdapter adapter,
            Map<Integer, String> neTags)
        throws CASException
    {
        for (NamedEntity namedEntity : selectCovered(NamedEntity.class, sentence)) {
            boolean isBegin = true;
            for (Token token : selectCovered(sentence.getCAS().getJCas(), Token.class,
                    namedEntity.getBegin(), namedEntity.getEnd())) {
                if (neTags.get(token.getAddress()) == null) {
                    if (isBegin) {
                        neTags.put(token.getAddress(), "B-" + namedEntity.getValue());
                        isBegin = false;
                    }
                    else {
                        neTags.put(token.getAddress(), "I-" + namedEntity.getValue());
                    }
                }
            }
        }
    }

    public static String train(MiraTemplate aTemplate, RepositoryService aRepository)
        throws IOException, ClassNotFoundException
    {
        String trainResult = "";
        Mira mira = new Mira();
        int frequency = 2;
        double sigma = 1;
        int iterations = 10;
        int beamSize = 0;
        boolean maxPosteriors = false;
        String templateName = null;

        TagSet layer = aTemplate.getTrainTagSet();
        TagSet fLayer = aTemplate.getFeatureTagSets().size() > 0 ? aTemplate.getFeatureTagSets()
                .iterator().next() : null;

        templateName = createMiraTemplate(layer, aRepository, aTemplate, layer, fLayer,
                getMiraTemplateFile(layer, aRepository));

        File miraDir = aRepository.getMiraDir(layer);
        File trainFile = new File(miraDir, "train");
        String initalModelName = "";
        String trainName = trainFile.getAbsolutePath();

        String modelName = aRepository.getMiraModel(layer).getAbsolutePath();

        boolean randomInit = false;
        TypeAdapter adapter = TypeUtil.getAdapter(layer);
        if (adapter.getLabelPrefix().equals(AnnotationTypeConstant.NAMEDENTITY_PREFIX)) {
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
            trainResult = mira.train(trainName, iterations, numExamples, i);
            mira.averageWeights(iterations * numExamples);
            /*
             * if (testName != null) { BufferedReader input = new BufferedReader(new
             * FileReader(testName)); testResult = mira.test(input, null); }
             */
        }
        mira.saveModel(modelName);

        return trainResult;
    }

    public static String createMiraTemplate(TagSet aProject, RepositoryService aRepository,
            MiraTemplate aTemplate, TagSet aTagSet, TagSet aFeatureTagSet, File templateFile)
        throws IOException
    {

        StringBuffer sb = new StringBuffer();
        TypeAdapter adapter = TypeUtil.getAdapter(aTagSet);
        int i = 1;
        if (adapter.getLabelPrefix().equals(AnnotationTypeConstant.POS_PREFIX)) {
            i = setMorphoTemplate(aTemplate, sb, i);
            setNgramForLable(aTemplate, sb, i);
        }
        else {
            i = setMorphoTemplate(aTemplate, sb, i);
            setNgramTemplate(aTemplate, sb, i, aFeatureTagSet);
        }

        sb.append("\n");
        sb.append("B\n");
        FileUtils.writeStringToFile(templateFile, sb.toString());
        return templateFile.getAbsolutePath();
    }

    private static void setNgramForLable(MiraTemplate aTemplate, StringBuffer sb, int i)
    {
        if (aTemplate.getNgram() == 1) {
            sb.append("U" + String.format("%02d", i) + "%x[0,0]\n");
            i++;
        }
        else if (aTemplate.getNgram() == 2) {
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
        }
        else {

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

            sb.append("U" + String.format("%02d", i) + "%x[-2,0]\n");
            i++;
            sb.append("U" + String.format("%02d", i) + "%x[2,0]\n");
            i++;
            sb.append("U" + String.format("%02d", i) + "%x[-2,0]" + "%x[-1,0]\n");
            i++;
            sb.append("U" + String.format("%02d", i) + "%x[1,0]" + "%x[2,0]\n");
            i++;
            sb.append("U" + String.format("%02d", i) + "%x[-2,0]" + "%x[-1,0]" + "%x[0,0]\n");
            i++;
            sb.append("U" + String.format("%02d", i) + "%x[0,0]" + "%x[1,0]" + "%x[2,0]\n");
            i++;
            sb.append("U" + String.format("%02d", i) + "%x[-2,0]" + "%x[-1,0]" + "%x[0,0]"
                    + "%x[1,0]\n");
            i++;
            sb.append("U" + String.format("%02d", i) + "%x[-1,0]" + "%x[0,0]" + "%x[1,0]"
                    + "%x[2,0]\n");
            i++;
            sb.append("U" + String.format("%02d", i) + "%x[-2,0]" + "%x[-1,0]" + "%x[0,0]"
                    + "%x[1,0]" + "%x[2,0]\n");
            i++;
        }
    }

    private static void setNgramTemplate(MiraTemplate aTemplate, StringBuffer sb, int i,
            TagSet aFeatureTagSet)
    {

        int featureTag = i;
        if (aTemplate.getNgram() == 1) {
            sb.append("U" + String.format("%02d", i) + "%x[0,0]\n");
            i++;
            if (aFeatureTagSet != null) {
                sb.append("U" + String.format("%02d", i) + "%x[0," + featureTag + "]\n");
                i++;
            }
        }
        else if (aTemplate.getNgram() == 2) {
            sb.append("U" + String.format("%02d", i) + "%x[0,0]\n");
            i++;
            if (aFeatureTagSet != null) {
                sb.append("U" + String.format("%02d", i) + "%x[0," + featureTag + "]\n");
                i++;
                sb.append("U" + String.format("%02d", i) + "%x[0,0] %x[0," + featureTag + "]\n");
                i++;
            }
            sb.append("U" + String.format("%02d", i) + "%x[-1,0] %x[0,0]\n");
            i++;
            if (aFeatureTagSet != null) {
                sb.append("U" + String.format("%02d", i) + "%x[-1," + featureTag + "] %x[0,"
                        + featureTag + "]\n");
                i++;
            }
        }

        sb.append("\n");
        i = 1;
        if (aTemplate.getBigram() == 1) {
            sb.append("B" + String.format("%02d", i) + "%x[0,0]\n");
            i++;
            if (aFeatureTagSet != null) {
                sb.append("B" + String.format("%02d", i) + "%x[0," + featureTag + "]\n");
                i++;
            }
        }
        else if (aTemplate.getNgram() == 2) {
            sb.append("B" + String.format("%02d", i) + "%x[0,0]\n");
            i++;
            if (aFeatureTagSet != null) {
                sb.append("B" + String.format("%02d", i) + "%x[0," + featureTag + "]\n");
                i++;
                sb.append("B" + String.format("%02d", i) + "%x[0,0] %x[0," + featureTag + "]\n");
                i++;
            }
            sb.append("B" + String.format("%02d", i) + "%x[-1,0] %x[0,0]\n");
            i++;
            if (aFeatureTagSet != null) {
                sb.append("B" + String.format("%02d", i) + "%x[-1," + featureTag + "] %x[0,"
                        + featureTag + "]\n");
                i++;
            }
        }

    }

    private static int setMorphoTemplate(MiraTemplate aTemplate, StringBuffer sb, int i)
    {
        if (aTemplate.isCapitalized()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aTemplate.isContainsNumber()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aTemplate.isPrefix1()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aTemplate.isPrefix2()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aTemplate.isPrefix3()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aTemplate.isPrefix4()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aTemplate.isPrefix5()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aTemplate.isSuffix1()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aTemplate.isSuffix2()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }

        if (aTemplate.isSuffix3()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aTemplate.isSuffix4()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aTemplate.isSuffix5()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }

        sb.append("\n");

        return i;
    }

    public static File getMiraTemplateFile(TagSet TagSet, RepositoryService aRepository)
    {
        return new File(aRepository.getMiraDir(TagSet).getAbsolutePath(), TagSet.getName()
                + "-template");
    }

    public static void predict(MiraTemplate aTemplate, RepositoryService aRepository,
            AnnotationService aAnnotationService)
        throws CASException, UIMAException, ClassNotFoundException, IOException,
        BratAnnotationException
    {
        TagSet layer = aTemplate.getTrainTagSet();
        TagSet fLayer = aTemplate.getFeatureTagSets().size() > 0 ? aTemplate.getFeatureTagSets()
                .iterator().next() : null;

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aRepository.getUser(username);

        for (SourceDocument sourceDocument : aRepository.listSourceDocuments(layer.getProject())) {

            if ((!sourceDocument.isTrainingDocument() && !sourceDocument.getState().equals(
                    SourceDocumentState.CURATION_FINISHED))) {

                JCas jCas;
                try {
                    jCas = aRepository.getCorrectionDocumentContent(sourceDocument);
                }
                catch (DataRetrievalFailureException e) {// this was first time automation is done, read from source!
                    jCas = aRepository.readJCas(sourceDocument, sourceDocument.getProject(), user);

                }
                File predFile = casToMiraFile(jCas, username, layer, fLayer, -1, -1, aTemplate,
                        aRepository);
                Mira mira = new Mira();
                int shiftColumns = 0;
                int nbest = 1;
                int beamSize = 0;
                boolean maxPosteriors = false;
                String modelName = aRepository.getMiraModel(layer).getAbsolutePath();
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
                List<String> tags = new ArrayList<String>();

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
                    tags.add(tag);
                }

                TypeAdapter adapter = TypeUtil.getAdapter(layer);
                adapter.automate(jCas, tags);
                aRepository.createCorrectionDocumentContent(jCas, sourceDocument, user);
                sourceDocument.setProcessed(true);
            }
        }
    }

    private static File casToMiraFile(JCas jCas, String aUsername, TagSet aTagSet,
            TagSet aFeatureTagSet, int aBegin, int aEnd, MiraTemplate aAModel,
            RepositoryService aRepository)
        throws UIMAException, IOException, ClassNotFoundException, CASException
    {
        File predFile;
        File miraDir = aRepository.getMiraDir(aTagSet);
        predFile = new File(miraDir, "predFile");
        if (predFile.exists()) {
            predFile.delete();
            predFile.createNewFile();
        }
        OutputStream stream = new FileOutputStream(predFile);

        if (aBegin == -1) {
            for (Sentence sentence : select(jCas, Sentence.class)) {
                IOUtils.write(
                        getMiraLines(sentence, true, aTagSet, aFeatureTagSet, aAModel) + "\n",
                        stream, "UTF-8");
            }
        }
        else {
            for (Sentence sentence : selectCovered(jCas, Sentence.class, aBegin, aEnd)) {
                IOUtils.write(
                        getMiraLines(sentence, true, aTagSet, aFeatureTagSet, aAModel) + "\n",
                        stream, "UTF-8");
            }
        }

        return predFile;
    }
}
