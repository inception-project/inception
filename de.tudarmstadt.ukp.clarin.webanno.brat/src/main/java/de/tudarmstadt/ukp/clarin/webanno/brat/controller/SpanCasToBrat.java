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
import static org.uimafit.util.JCasUtil.selectCovered;

import java.util.HashSet;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * populate the {@link GetDocumentResponse} for span annotations ({@link POS}, {@link NamedEntity}
 * ...
 *
 * @author Seid Muhie Yimam
 *
 */
public class SpanCasToBrat
{

    public static <T extends Annotation> void addSpanAnnotationToResponse(JCas aJcas,
            GetDocumentResponse aResponse, int aBeginAddress, int aWindowSize, int aLastAddress,
            HashSet<String> annotationLayers, Class<T> aAnnotationClass, String aAnnotationType)
    {
        Sentence sentenceAddress = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(aBeginAddress);
        int current = sentenceAddress.getBegin();
        int i = aBeginAddress;
        Sentence sentence = null;

        for (int j = 0; j < aWindowSize; j++) {
            if (i >= aLastAddress) {
                sentence = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(i);
                updateResponse(aAnnotationClass, sentence, aAnnotationType, aResponse, current);
                break;
            }
            sentence = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(i);
            updateResponse(aAnnotationClass, sentence, aAnnotationType, aResponse, current);
            i = BratAjaxCasUtil.getFollowingSentenceAddress(aJcas, i);
        }
    }

    private static <T extends Annotation> void updateResponse(Class<T> aAnnotationClass, Sentence aSentence,
            String aAnnotationType, GetDocumentResponse aResponse, int aCurrent)
    {
        for (T annotation : selectCovered(aAnnotationClass, aSentence)) {
            String value = "";
            if (aAnnotationType.equals(AnnotationType.POS_PREFIX)) {
                value = ((POS) annotation).getPosValue();
            }
            else if (aAnnotationType.equals(AnnotationType.NAMEDENTITY_PREFIX)) {
                value = ((NamedEntity) annotation).getValue();
            }
            else if (aAnnotationType.equals(AnnotationType.LEMMA)) {
                value = ((Lemma) annotation).getValue();
            }
            aResponse.addEntity(new Entity(annotation.getAddress(), aAnnotationType + value,
                    asList(new Offsets(annotation.getBegin() - aCurrent, annotation.getEnd()
                            - aCurrent))));
        }
    }
}
