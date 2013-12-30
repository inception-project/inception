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
import static org.uimafit.util.JCasUtil.selectCovered;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

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
        User logedInUser = aRepository.getUser(username);

        int beginOffset = aModel.getSentenceBeginOffset();

        int endOffset;
        if (aModel.isPredictInThisPage()) {
            endOffset = BratAjaxCasUtil.getLastSentenceEndOffsetInDisplayWindow(jCas,
                    aModel.getSentenceAddress(), aModel.getWindowSize());
        }
        else {

            endOffset = BratAjaxCasUtil.selectByAddr(jCas, aModel.getLastSentenceAddress()).getEnd();
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
}
