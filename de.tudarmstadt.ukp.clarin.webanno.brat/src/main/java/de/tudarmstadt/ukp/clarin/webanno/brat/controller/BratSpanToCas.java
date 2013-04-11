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

import static org.uimafit.util.JCasUtil.select;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorUIData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class BratSpanToCas
{

    /**
     * Update the CAS with new/modification of span annotations
     */
    public static void addSpanAnnotationToCas(BratAnnotatorModel aBratAnnotatorModel,
            String aValue, BratAnnotatorUIData aUIData, String annotationTypeName,
            String aFeatureName, boolean aMultipleSpan)
    {
        Map<Integer, Integer> offsets = offsets(aUIData.getjCas());
        int startAndEnd[] = getTokenStart(offsets, aBratAnnotatorModel.getAnnotationOffsetStart(),
                aBratAnnotatorModel.getAnnotationOffsetEnd());

        if (aMultipleSpan) {
            aBratAnnotatorModel.setAnnotationOffsetStart(startAndEnd[0]);
            aBratAnnotatorModel.setAnnotationOffsetEnd(startAndEnd[1]);
            updateCas(annotationTypeName, aUIData.getjCas().getCas(), aFeatureName,
                    aBratAnnotatorModel.getAnnotationOffsetStart(),
                    aBratAnnotatorModel.getAnnotationOffsetEnd(), aValue);
        }
        else {
            Map<Integer, Integer> splitedTokens = getSplitedTokens(offsets,
                    aBratAnnotatorModel.getAnnotationOffsetStart(),
                    aBratAnnotatorModel.getAnnotationOffsetEnd());
            for (Integer start : splitedTokens.keySet()) {
                updateCas(annotationTypeName, aUIData.getjCas().getCas(), aFeatureName, start,
                        splitedTokens.get(start), aValue);
            }
        }

    }

    /**
     * Update the CAS
     */
    private static void updateCas(String annotationTypeName, CAS aCas, String featureName,
            int aBegin, int aEnd, String aValue)
    {
        boolean duplicate = false;
        Type type = CasUtil.getType(aCas, annotationTypeName);
        Feature feature = type.getFeatureByBaseName(featureName);
        for (AnnotationFS fs : CasUtil.selectCovered(aCas, type, aBegin, aEnd)) {
            if (fs.getBegin() == aBegin && fs.getEnd() == aEnd) {
                if (!fs.getStringValue(feature).equals(aValue)) {
                    fs.setStringValue(feature, aValue);
                }
                duplicate = true;
            }
        }
        if (!duplicate) {
            AnnotationFS newAnnotation = aCas.createAnnotation(type, aBegin, aEnd);
            newAnnotation.setStringValue(feature, aValue);
            aCas.addFsToIndexes(newAnnotation);
        }
    }

    /**
     * Stores, for every tokens, the start and end position offsets : used fro multiple span
     * annotations
     *
     * @return map of tokens begin and end positions
     */
    private static Map<Integer, Integer> offsets(JCas aJcas)
    {
        Map<Integer, Integer> offsets = new HashMap<Integer, Integer>();
        for (Token token : select(aJcas, Token.class)) {
            offsets.put(token.getBegin(), token.getEnd());
        }
        return offsets;
    }

    /**
     * For multiple span, get the start and end offsets
     */
    private static int[] getTokenStart(Map<Integer, Integer> aOffset, int aStart, int aEnd)
    {
        Iterator<Integer> it = aOffset.keySet().iterator();
        boolean startFound = false;
        boolean endFound = false;
        while (it.hasNext()) {
            int tokenStart = it.next();
            if (aStart >= tokenStart && aStart <= aOffset.get(tokenStart)) {
                aStart = tokenStart;
                startFound = true;
                if (endFound) {
                    break;
                }
            }
            if (aEnd >= tokenStart && aEnd <= aOffset.get(tokenStart)) {
                aEnd = aOffset.get(tokenStart);
                endFound = true;
                if (startFound) {
                    break;
                }
            }
        }
        return new int[] { aStart, aEnd };
    }

    /**
     * If the annotation type is limited to only a single token, but brat sends multiple tokens,
     * split them up
     *
     * @return Map of start and end offsets for the multiple token span
     */

    private static Map<Integer, Integer> getSplitedTokens(Map<Integer, Integer> aOffset,
            int aStart, int aEnd)
    {
        Map<Integer, Integer> startAndEndOfSplitedTokens = new HashMap<Integer, Integer>();
        Iterator<Integer> it = aOffset.keySet().iterator();
        while (it.hasNext()) {
            int tokenStart = it.next();
            if (aStart <= tokenStart && tokenStart <= aEnd) {
                startAndEndOfSplitedTokens.put(tokenStart, aOffset.get(tokenStart));
            }
        }
        return startAndEndOfSplitedTokens;

    }
}
