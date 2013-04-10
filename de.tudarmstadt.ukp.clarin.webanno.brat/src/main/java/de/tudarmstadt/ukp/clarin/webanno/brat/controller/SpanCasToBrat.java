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

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
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

    public static void addSpanAnnotationToResponse(JCas aJcas, GetDocumentResponse aResponse,
            int aBeginAddress, int aWindowSize, int aLastAddress, String annotationTypeName,
            String aAnnotationTypePrefix, String featureName)
    {
        Sentence sentenceAddress = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(aBeginAddress);
        int current = sentenceAddress.getBegin();
        int i = aBeginAddress;
        Sentence sentence = null;

        for (int j = 0; j < aWindowSize; j++) {
            if (i >= aLastAddress) {
                sentence = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(i);
                updateResponse(annotationTypeName, sentence, aAnnotationTypePrefix, aResponse,
                        current, featureName);
                break;
            }
            sentence = (Sentence) aJcas.getLowLevelCas().ll_getFSForRef(i);
            updateResponse(annotationTypeName, sentence, aAnnotationTypePrefix, aResponse, current,
                    featureName);
            i = BratAjaxCasUtil.getFollowingSentenceAddress(aJcas, i);
        }
    }

    private static void updateResponse(String annotationTypeName, Sentence aSentence,
            String aAnnotationTypePrefix, GetDocumentResponse aResponse, int aCurrent,
            String featureName)
    {
        Type type = CasUtil.getType(aSentence.getCAS(), annotationTypeName);
        for (AnnotationFS fs : CasUtil.selectCovered(type, aSentence)) {

            Feature feature = fs.getType().getFeatureByBaseName(featureName);
            aResponse.addEntity(new Entity(((FeatureStructureImpl) fs).getAddress(),
                    aAnnotationTypePrefix + fs.getStringValue(feature), asList(new Offsets(fs
                            .getBegin() - aCurrent, fs.getEnd() - aCurrent))));
        }
    }
}
