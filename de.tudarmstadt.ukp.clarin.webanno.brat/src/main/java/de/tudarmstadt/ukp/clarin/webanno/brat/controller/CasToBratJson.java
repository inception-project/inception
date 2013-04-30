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
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import static java.util.Arrays.asList;
import static org.uimafit.util.JCasUtil.select;
import static org.uimafit.util.JCasUtil.selectCovered;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.jcas.JCas;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Convert the CAS object to BRAT JSON format
 * Will be replaced sooner with SpanAdapter and ArcAdapter
 * @author Seid Muhie Yimam
 *
 */
@Deprecated
public class CasToBratJson
{

    public CasToBratJson()
    {
        // Nothing to Do.
    }

    private MappingJacksonHttpMessageConverter jsonConverter;

    public void setJsonConverter(MappingJacksonHttpMessageConverter aJsonConverter)
    {
        jsonConverter = aJsonConverter;
    }

    public void addTokenToResponse(JCas aJcas, GetDocumentResponse aResponse,
            BratAnnotatorModel aBratAnnotatorModel)
    {

        Sentence sentenceAddress = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(
                aBratAnnotatorModel.getSentenceAddress());
        int current = sentenceAddress.getBegin();
        int i = aBratAnnotatorModel.getSentenceAddress();
        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(aJcas, i);
        aResponse.setSentenceNumberOffset(sentenceNumber);
        Sentence sentence = null;

        for (int j = 0; j < aBratAnnotatorModel.getWindowSize(); j++) {
            if (i >= aBratAnnotatorModel.getLastSentenceAddress()) {
                sentence = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(i);
                for (Token coveredToken : selectCovered(Token.class, sentence)) {
                    aResponse.addToken(coveredToken.getBegin() - current, coveredToken.getEnd()
                            - current);
                }
                break;
            }
            sentence = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(i);
            for (Token coveredToken : selectCovered(Token.class, sentence)) {
                aResponse.addToken(coveredToken.getBegin() - current, coveredToken.getEnd()
                        - current);
            }
            i = BratAjaxCasUtil.getFollowingSentenceAddress(aJcas, i);
        }
        if (sentence != null) {
            aResponse.setText(aJcas.getDocumentText().substring(current, sentence.getEnd()));
        }
    }

    public void addSentenceToResponse(JCas aJcas, GetDocumentResponse aResponse,
            BratAnnotatorModel aBratAnnotatorModel)
    {

        Sentence sentenceAddress = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(
                aBratAnnotatorModel.getSentenceAddress());
        int current = sentenceAddress.getBegin();
        int i = aBratAnnotatorModel.getSentenceAddress();
        Sentence sentence = null;

        for (int j = 0; j < aBratAnnotatorModel.getWindowSize(); j++) {
            if (i >= aBratAnnotatorModel.getLastSentenceAddress()) {
                sentence = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(i);
                aResponse.addSentence(sentence.getBegin() - current, sentence.getEnd() - current);
                break;
            }
            sentence = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(i);
            aResponse.addSentence(sentence.getBegin() - current, sentence.getEnd() - current);
            i = BratAjaxCasUtil.getFollowingSentenceAddress(aJcas, i);
        }
    }

    public void addCorefTypeToResponse(JCas aJcas, GetDocumentResponse aResponse,
            BratAnnotatorModel aBratAnnotatorModel)
    {

        Sentence sentenceAddress = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(
                aBratAnnotatorModel.getSentenceAddress());
        int current = sentenceAddress.getBegin();
        int i = aBratAnnotatorModel.getSentenceAddress();

        Sentence sentence = null;
        for (int j = 0; j < aBratAnnotatorModel.getWindowSize(); j++) {
            if (i >= aBratAnnotatorModel.getLastSentenceAddress()) {
                sentence = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(i);
                for (CoreferenceLink link : selectCovered(CoreferenceLink.class, sentence)) {

                    aResponse
                            .addEntity(new Entity(link.getAddress(),
                                    AnnotationTypeConstant.COREFERENCE_PREFIX + link.getReferenceType(),
                                    asList(new Offsets(link.getBegin() - current, link.getEnd()
                                            - current))));
                }
                break;
            }
            sentence = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(i);
            for (CoreferenceLink link : selectCovered(CoreferenceLink.class, sentence)) {
                int begin = link.getBegin();
                int end = link.getEnd();
                if (sentence.getBegin() <= begin && sentence.getEnd() >= end) {
                    aResponse
                            .addEntity(new Entity(link.getAddress(),
                                    AnnotationTypeConstant.COREFERENCE_PREFIX + link.getReferenceType(),
                                    asList(new Offsets(link.getBegin() - current, link.getEnd()
                                            - current))));
                }
            }
            i = BratAjaxCasUtil.getFollowingSentenceAddress(aJcas, i);
        }
    }

    public void addCoreferenceToResponse(JCas aJcas, GetDocumentResponse aResponse,
            BratAnnotatorModel aBratAnnotatorModel)
    {
        int beginOffset = BratAjaxCasUtil.selectAnnotationByAddress(aJcas, Sentence.class,
                aBratAnnotatorModel.getSentenceAddress()).getBegin();
        int endOffset = BratAjaxCasUtil.selectAnnotationByAddress(
                aJcas,
                Sentence.class,
                BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(aJcas,
                        aBratAnnotatorModel.getSentenceAddress(),
                        aBratAnnotatorModel.getWindowSize())).getEnd();

        // Every 12 Co-reference chains will have different color
        int i = 1;

        for (CoreferenceChain chain : select(aJcas, CoreferenceChain.class)) {
            CoreferenceLink link = chain.getFirst();

            while (link != null) {
                int begin = link.getBegin();
                int end = link.getEnd();

                if (beginOffset <= begin && endOffset >= end) {
                    if (link.getReferenceRelation() != null
                            && link.getReferenceRelation().equals("expletive")) {
                        List<Argument> argumentList = new ArrayList<Argument>();
                        argumentList.add(new Argument("Arg1", link.getAddress()));
                        argumentList.add(new Argument("Arg2", +link.getAddress()));

                        aResponse.addRelation(new Relation(link.getAddress(), i
                                + AnnotationTypeConstant.COREFERENCE_PREFIX + link.getReferenceRelation(),
                                argumentList));
                        link = link.getNext();
                        continue;
                    }
                    CoreferenceLink nextLink = link.getNext();

                    if (nextLink != null) {
                        List<Argument> argumentList = new ArrayList<Argument>();

                        argumentList.add(new Argument("Arg1", link.getAddress()));
                        argumentList.add(new Argument("Arg2", +nextLink.getAddress()));

                        aResponse.addRelation(new Relation(link.getAddress(), i
                                + AnnotationTypeConstant.COREFERENCE_PREFIX + link.getReferenceRelation(),
                                argumentList));
                    }
                }
                link = link.getNext();
            }
            i++;
            if (i > 12) {
                i = 1;
            }
        }
    }

    public void generateBratJson(Object aResponse, File aFile)
        throws IOException
    {
        StringWriter out = new StringWriter();

        JsonGenerator jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
                .createJsonGenerator(out);

        jsonGenerator.writeObject(aResponse);
        FileUtils.writeStringToFile(aFile, out.toString());
    }
}
