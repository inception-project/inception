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
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.AutomationModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
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

    public static void predict(BratAnnotatorModel aModel, AutomationModel aAutomationModel,
            RepositoryService aRepository, AnnotationService aAnnotationService, int aStart,
            int aEnd, Tag aTag)
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
        User logedInUser = aRepository.getUser(username);

        int beginOffset = aModel.getSentenceBeginOffset();

        int endOffset;
        if (aAutomationModel.isPredictInThisPage()) {
            endOffset = BratAjaxCasUtil.getLastSentenceEndOffsetInDisplayWindow(jCas,
                    aModel.getSentenceAddress(), aModel.getWindowSize());
        }
        else {

            endOffset = BratAjaxCasUtil.selectByAddr(jCas, aModel.getLastSentenceAddress())
                    .getEnd();
        }
        for (Sentence sentence : selectCovered(jCas, Sentence.class, beginOffset, endOffset)) {
            String sentenceText = sentence.getCoveredText().toLowerCase();
            for (int i = -1; (i = sentenceText.indexOf(selectedText.toLowerCase(), i + 1)) != -1
                    && selectCovered(jCas, Token.class, sentence.getBegin() + i,
                            sentence.getBegin() + i + selectedText.length()).size() > 0;) {
                bratAjaxCasController.createSpanAnnotation(jCas, sentence.getBegin() + i,
                        sentence.getBegin() + i + selectedText.length(), getQualifiedLabel(aTag),
                        null, null);
            }
        }
        aRepository.createCorrectionDocumentContent(jCas, aModel.getDocument(), logedInUser);
    }

    public static boolean isTemplateConfigured(AutomationModel aModel)
    {
        if (aModel.isCapitalized() || aModel.isContainsNumber() || aModel.isPrefix1()
                || aModel.isPrefix2() || aModel.isPrefix3() || aModel.isPrefix4()
                || aModel.isPrefix5() || aModel.isSuffix1() || aModel.isSuffix2()
                || aModel.isSuffix3() || aModel.isSuffix4() || aModel.isSuffix5()) {
            return true;
        }
        return false;

    }

    public static void casToMiraTrainData(Project aProject, TagSet aTagSet, TagSet aFeatureTagSet,
            AutomationModel aAModel, RepositoryService aRepository)
        throws IOException, UIMAException, ClassNotFoundException
    {
        File miraDir = aRepository.getMiraDir(aProject);
        FileUtils.forceMkdir(miraDir);
        File trainFile = new File(miraDir, "train");
        File testFile = new File(miraDir, "test");

        BufferedWriter trainOut = new BufferedWriter(new FileWriter(trainFile));
        BufferedWriter testOut = new BufferedWriter(new FileWriter(testFile));

        for (SourceDocument sourceDocument : aRepository.listSourceDocuments(aProject)) {

            if (sourceDocument.getState().equals(SourceDocumentState.CURATION_FINISHED)) {
                JCas jCas = aRepository.getCurationDocumentContent(sourceDocument);
                int sentCount = select(jCas, Sentence.class).size();
                int i = 0;
                for (Sentence sentence : select(jCas, Sentence.class)) {
                    if (((double) i / sentCount) * 100 < 90) {

                        trainOut.append(getMiraLines(sentence, false, aTagSet, aFeatureTagSet,
                                aAModel).toString()
                                + "\n");
                    }
                    else {
                        testOut.append(getMiraLines(sentence, false, aTagSet, aFeatureTagSet,
                                aAModel).toString()
                                + "\n");
                    }
                    i++;
                }
            }

        }
        trainOut.close();
        testOut.close();

    }

    private static StringBuffer getMiraLines(Sentence sentence, boolean aPredict, TagSet aTagSet,
            TagSet aFeatureTagSet, AutomationModel aAModel)
        throws CASException
    {
        StringBuffer sb = new StringBuffer();
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
            TypeAdapter adapter = TypeUtil.getAdapter(aTagSet);
            // TODO: when free annotation layers defined, check if tagset is on multiple token or
            // not
            if (adapter.getLabelPrefix().equals(AnnotationTypeConstant.NAMEDENTITY_PREFIX)) {
                TypeAdapter featureAdapter = TypeUtil.getAdapter(aFeatureTagSet);
                List<String> featureAnnotations = featureAdapter.getAnnotation(sentence.getCAS()
                        .getJCas(), token.getBegin(), token.getEnd());
                featureTagSet = featureAnnotations.size() == 0 ? "" : featureAnnotations.get(0)
                        + " ";

            }
            List<String> annotations = adapter.getAnnotation(sentence.getCAS().getJCas(),
                    token.getBegin(), token.getEnd());
            String tag = aPredict == true ? "" : annotations.size() == 0 ? "__nill__" : annotations
                    .get(0);
            sb.append(word + " " + capitalized

            /* + getVowels(word, FileUtils.readFileToString(new File(argv[1]))) + " " */

            + containsNUmber + prefix1 + prefix2 + prefix3 + prefix4 + prefix5 + suffix1 + suffix2
                    + suffix3 + suffix4 + suffix5 + featureTagSet + tag + nl);
        }
        return sb;

    }

    public static String train(Project aProject, TagSet aTagset, AutomationModel aAModel,
            RepositoryService aRepository)
    {
        String trainResult = "";
        String testResult = "";
        try {
            Mira mira = new Mira();
            int frequency = 2;
            double sigma = 1;
            int iterations = 10;
            int beamSize = 0;
            boolean maxPosteriors = false;
            String templateName = null;

            templateName = createMiraTemplate(aProject, aRepository, aAModel, aTagset,
                    getMiraTemplateFile(aProject, aRepository));
            File miraDir = aRepository.getMiraDir(aProject);
            File trainFile = new File(miraDir, "train");
            File testFile = new File(miraDir, "test");
            String trainName = trainFile.getAbsolutePath();
            String modelName = aRepository.getMiraModel(aProject).getAbsolutePath();
            String testName = testFile.getAbsolutePath();
            new Vector<String>();
            boolean randomInit = false;
            mira.loadTemplates(templateName);
            mira.setClip(sigma);
            mira.maxPosteriors = maxPosteriors;
            mira.beamSize = beamSize;
            int numExamples = mira.count(trainName, frequency);
            mira.initModel(randomInit);
            for (int i = 0; i < iterations; i++) {
                trainResult = mira.train(trainName, iterations, numExamples, i);
                mira.averageWeights(iterations * numExamples);
                if (testName != null) {
                    BufferedReader input = new BufferedReader(new FileReader(testName));
                    testResult = mira.test(input, null);
                }
            }
            mira.saveModel(modelName);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "TRAIN:" + trainResult + "~~~" + "TEST:" + testResult;
    }

    public static String createMiraTemplate(Project aProject, RepositoryService aRepository,
            AutomationModel aAModel, TagSet aTagSet, File templateFile)
        throws IOException
    {

        StringBuffer sb = new StringBuffer();
        TypeAdapter adapter = TypeUtil.getAdapter(aTagSet);
        if (adapter.getLabelPrefix().equals(AnnotationTypeConstant.POS_PREFIX)) {
            setMorphoTemplate(aAModel, sb);
        }
        else {
        setNgramTemplate(aAModel, sb);
        }

        sb.append("\n");
        sb.append("B\n");
        FileUtils.writeStringToFile(templateFile, sb.toString());
        return templateFile.getAbsolutePath();
    }

    private static void setNgramTemplate(AutomationModel aAModel, StringBuffer sb)
    {
        int i = 1;
        if (aAModel.getNgram() == 1) {
            sb.append("U" + String.format("%02d", i) + "%x[0,0]\n");
            i++;
            sb.append("U" + String.format("%02d", i) + "%x[0,1]\n");
            i++;
        }
        else if (aAModel.getNgram() == 2) {
            sb.append("U" + String.format("%02d", i) + "%x[0,0]\n");
            i++;
            sb.append("U" + String.format("%02d", i) + "%x[0,1]\n");
            i++;
            sb.append("U" + String.format("%02d", i) + "%x[0,0] %x[0,1]\n");
            i++;
            sb.append("U" + String.format("%02d", i) + "%x[-1,0] %x[0,0]\n");
            i++;
            sb.append("U" + String.format("%02d", i) + "%x[-1,1] %x[0,1]\n");
            i++;
        }
        
        sb.append("\n");
        i = 1;
        if (aAModel.getBigram() == 1) {
            sb.append("B" + String.format("%02d", i) + "%x[0,0]\n");
            i++;
            sb.append("B" + String.format("%02d", i) + "%x[0,1]\n");
            i++;
        }
        else if (aAModel.getNgram() == 2) {
            sb.append("B" + String.format("%02d", i) + "%x[0,0]\n");
            i++;
            sb.append("B" + String.format("%02d", i) + "%x[0,1]\n");
            i++;
            sb.append("B" + String.format("%02d", i) + "%x[0,0] %x[0,1]\n");
            i++;
            sb.append("B" + String.format("%02d", i) + "%x[-1,0] %x[0,0]\n");
            i++;
            sb.append("B" + String.format("%02d", i) + "%x[-1,1] %x[0,1]\n");
            i++;
        }

    }

    private static void setMorphoTemplate(AutomationModel aAModel, StringBuffer sb)
    {
        int i = 1;
        if (aAModel.isCapitalized()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aAModel.isContainsNumber()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aAModel.isPrefix1()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aAModel.isPrefix2()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aAModel.isPrefix3()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aAModel.isPrefix4()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aAModel.isPrefix5()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aAModel.isSuffix1()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aAModel.isSuffix2()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }

        if (aAModel.isSuffix3()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aAModel.isSuffix4()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }
        if (aAModel.isSuffix5()) {
            sb.append("U" + String.format("%02d", i) + "%x[0," + i + "]\n");
            i++;
        }

        sb.append("\n");

        if (aAModel.getNgram() == 1) {
            sb.append("U" + String.format("%02d", i) + "%x[0,0]\n");
            i++;
        }
        else if (aAModel.getNgram() == 2) {
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

    public static File getMiraTemplateFile(Project aProject, RepositoryService aRepository)
    {
        return new File(aRepository.getMiraDir(aProject).getAbsolutePath(), aProject.getName()
                + "-template");
    }

    public static void predict(SourceDocument aDocument, String aUsername, TagSet aTagSet,
            TagSet aFeatureTagSet, int aBegin, int aEnd, AutomationModel aAModel,
            RepositoryService aRepository, AnnotationService aAnnotationService,
            boolean aPredictAnnotator, boolean aPredicrAutomator)
    {
        try {

            File predFile = casToMiraFile(aDocument, aUsername, aTagSet, aFeatureTagSet, aBegin,
                    aEnd, aAModel, aRepository);
            Mira mira = new Mira();
            int shiftColumns = 0;
            int nbest = 1;
            int beamSize = 0;
            boolean maxPosteriors = false;
            String modelName = aRepository.getMiraModel(aDocument.getProject()).getAbsolutePath();
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

            if (aPredictAnnotator) {
                JCas annotatorJCas = aRepository.readJCas(aDocument, aDocument.getProject(),
                        aRepository.getUser(aUsername));

                int i = 0;
                for (Token token : selectCovered(annotatorJCas, Token.class, aBegin, aEnd)) {
                    Tag tag = aAnnotationService.getTag(tags.get(i), aTagSet);
                    String annotationType = TypeUtil.getQualifiedLabel(tag);
                    BratAjaxCasController controller = new BratAjaxCasController(aRepository,
                            aAnnotationService);
                    controller.createSpanAnnotation(annotatorJCas, token.getBegin(),
                            token.getEnd(), annotationType, null, null);
                    i++;
                }
                aRepository.createAnnotationDocumentContent(annotatorJCas, aDocument,
                        aRepository.getUser(aUsername));
            }
            if (aPredicrAutomator) {
                JCas automateJCas = aRepository.getCorrectionDocumentContent(aDocument);
                int i = 0;
                for (Token token : selectCovered(automateJCas, Token.class, aBegin, aEnd)) {
                    Tag tag = aAnnotationService.getTag(tags.get(i), aTagSet);
                    String annotationType = TypeUtil.getQualifiedLabel(tag);
                    BratAjaxCasController controller = new BratAjaxCasController(aRepository,
                            aAnnotationService);
                    controller.createSpanAnnotation(automateJCas, token.getBegin(), token.getEnd(),
                            annotationType, null, null);
                    i++;
                }
                aRepository.createCorrectionDocumentContent(automateJCas, aDocument,
                        aRepository.getUser(aUsername));
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static File casToMiraFile(SourceDocument aDocument, String aUsername, TagSet aTagSet,
            TagSet aFeatureTagSet, int aBegin, int aEnd, AutomationModel aAModel,
            RepositoryService aRepository)
        throws UIMAException, IOException, ClassNotFoundException, CASException
    {
        File predFile;
        File miraDir = aRepository.getMiraDir(aDocument.getProject());
        predFile = new File(miraDir, "predFile");
        if (predFile.exists()) {
            predFile.delete();
            predFile.createNewFile();
        }
        OutputStream stream = new FileOutputStream(predFile);
        JCas jCas = aRepository.readJCas(aDocument, aDocument.getProject(),
                aRepository.getUser(aUsername));
        for (Sentence sentence : selectCovered(jCas, Sentence.class, aBegin, aEnd)) {
            IOUtils.write(getMiraLines(sentence, true, aTagSet, aFeatureTagSet, aAModel) + "\n",
                    stream, "UTF-8");
        }

        return predFile;
    }
}
