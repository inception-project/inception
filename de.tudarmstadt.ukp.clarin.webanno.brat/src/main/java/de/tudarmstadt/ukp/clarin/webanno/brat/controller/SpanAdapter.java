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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorUIData;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A class that is used to create Brat Span to CAS and vice-versa
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 */
public class SpanAdapter
    implements TypeAdapter
{
    private Log LOG = LogFactory.getLog(getClass());
    /**
     * Prefix of the label value for Brat to make sure that different annotation types can use the
     * same label, e.g. a POS tag "N" and a named entity type "N".
     *
     * This is used to differentiate the different types in the brat annotation/visualization. The
     * prefix will not stored in the CAS(striped away at {@link BratAjaxCasController#getType} )
     */
    private String typePrefix;

    /**
     * The UIMA type name.
     */
    private String annotationTypeName;

    /**
     * The feature of an UIMA annotation containing the label to be displayed in the UI.
     */
    private String labelFeatureName;

    private boolean singleTokenBehavior = false;
    private boolean addPosToToken;
    private boolean addLemmaToToken;

    public SpanAdapter(String aTypePrefix, String aTypeName, String aLabelFeatureName)
    {
        typePrefix = aTypePrefix;
        labelFeatureName = aLabelFeatureName;
        annotationTypeName = aTypeName;
    }

    /**
     * Span can only be made on a single token (not multiple tokens), e.g. for POS or Lemma
     * annotations. If this is set and a span is made across multiple tokens, then one annotation of
     * the specified type will be created for each token. If this is not set, a single annotation
     * covering all tokens is created.
     */
    public void setSingleTokenBehavior(boolean aSingleTokenBehavior)
    {
        singleTokenBehavior = aSingleTokenBehavior;
    }

    /**
     * @see #setSingleTokenBehavior(boolean)
     */
    public boolean isSingleTokenBehavior()
    {
        return singleTokenBehavior;
    }

    /**
     *
     * @see #setAddPosToToken(boolean)
     */
    public boolean isAddPosToToken()
    {
        return addPosToToken;
    }

    /**
     * Attach POS to a Token layer
     *
     * @param aAddPosToToken
     */
    public void setAddPosToToken(boolean aAddPosToToken)
    {
        addPosToToken = aAddPosToToken;
    }

    /**
     *
     * @see #addLemmaToToken
     */
    public boolean isAddLemmaToToken()
    {
        return addLemmaToToken;
    }

    /**
     * Attach Lemma to a Token layer
     *
     * @param aAddLemmaToToken
     */
    public void setAddLemmaToToken(boolean aAddLemmaToToken)
    {
        addLemmaToToken = aAddLemmaToToken;
    }

    /**
     * Add annotations from the CAS, which is controlled by the window size, to the brat response
     * {@link GetDocumentResponse}
     *
     * @param aJcas
     *            The JCAS object containing annotations
     * @param aResponse
     *            A brat response containing annotations in brat protocol
     * @param aBratAnnotatorModel
     *            Data model for brat annotations
     */
    @Override
    public void addToBrat(JCas aJcas, GetDocumentResponse aResponse,
            BratAnnotatorModel aBratAnnotatorModel)
    {
        // The first sentence address in the display window!
        Sentence firstSentence = (Sentence) BratAjaxCasUtil.selectAnnotationByAddress(aJcas,
                FeatureStructure.class, aBratAnnotatorModel.getSentenceAddress());
        int i = aBratAnnotatorModel.getSentenceAddress();

        // Loop based on window size
        // j, controlling variable to display sentences based on window size
        // i, address of each sentences
        int j = 1;
        while (j <= aBratAnnotatorModel.getWindowSize()) {
            if (i >= aBratAnnotatorModel.getLastSentenceAddress()) {
                Sentence sentence = (Sentence) BratAjaxCasUtil.selectAnnotationByAddress(aJcas,
                        FeatureStructure.class, i);
                updateResponse(sentence, aResponse, firstSentence.getBegin());
                break;
            }
            else {
                Sentence sentence = (Sentence) BratAjaxCasUtil.selectAnnotationByAddress(aJcas,
                        FeatureStructure.class, i);
                updateResponse(sentence, aResponse, firstSentence.getBegin());
                i = BratAjaxCasUtil.getFollowingSentenceAddress(aJcas, i);
            }
            j++;
        }
    }

    /**
     * a helper method to the {@link #addToBrat(JCas, GetDocumentResponse, BratAnnotatorModel)}
     *
     * @param aSentence
     *            The current sentence in the CAS annotation, with annotations
     * @param aResponse
     *            The {@link GetDocumentResponse} object with the annotation from CAS annotation
     * @param aFirstSentenceOffset
     *            The first sentence offset. The actual offset in the brat visualization window will
     *            be <b>X-Y</b>, where <b>X</b> is the offset of the annotated span and <b>Y</b> is
     *            aFirstSentenceOffset
     */
    private void updateResponse(Sentence aSentence, GetDocumentResponse aResponse,
            int aFirstSentenceOffset)
    {
        Type type = CasUtil.getType(aSentence.getCAS(), annotationTypeName);
        for (AnnotationFS fs : CasUtil.selectCovered(type, aSentence)) {

            Feature labelFeature = fs.getType().getFeatureByBaseName(labelFeatureName);
            aResponse.addEntity(new Entity(((FeatureStructureImpl) fs).getAddress(), typePrefix
                    + fs.getStringValue(labelFeature), asList(new Offsets(fs.getBegin()
                    - aFirstSentenceOffset, fs.getEnd() - aFirstSentenceOffset))));
        }
    }

    /**
     * Update the CAS with new/modification of span annotations from brat
     *
     * @param aLabelValue
     *            the value of the annotation for the span
     * @param aUIData
     *            Other information obtained from brat such as the start and end offsets
     */
    public void addToCas(String aLabelValue, BratAnnotatorUIData aUIData)
    {
        Map<Integer, Integer> offsets = offsets(aUIData.getjCas());

        if (singleTokenBehavior) {
            Map<Integer, Integer> splitedTokens = getSplitedTokens(offsets,
                    aUIData.getAnnotationOffsetStart(), aUIData.getAnnotationOffsetEnd());
            for (Integer start : splitedTokens.keySet()) {
                updateCas(aUIData.getjCas().getCas(), start, splitedTokens.get(start), aLabelValue);
            }
        }
        else {
            int startAndEnd[] = getTokenStart(offsets, aUIData.getAnnotationOffsetStart(),
                    aUIData.getAnnotationOffsetEnd());
            aUIData.setAnnotationOffsetStart(startAndEnd[0]);
            aUIData.setAnnotationOffsetEnd(startAndEnd[1]);
            updateCas(aUIData.getjCas().getCas(), aUIData.getAnnotationOffsetStart(),
                    aUIData.getAnnotationOffsetEnd(), aLabelValue);
        }
    }

    /**
     * A Helper method to {@link #addToCas(String, BratAnnotatorUIData)}
     */
    private void updateCas(CAS aCas, int aBegin, int aEnd, String aValue)
    {

        boolean duplicate = false;
        Type type = CasUtil.getType(aCas, annotationTypeName);
        Feature feature = type.getFeatureByBaseName(labelFeatureName);
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
            // Attach the POS to a Token
            if (addPosToToken) {
                Type tokenType = CasUtil.getType(aCas, Token.class.getName());
                Feature posFeature = tokenType.getFeatureByBaseName("pos");
                CasUtil.selectCovered(aCas, tokenType, aBegin, aEnd).get(0)
                        .setFeatureValue(posFeature, newAnnotation);
            }
         // Attach the Lemma to a Token
            if (addLemmaToToken) {
                Type tokenType = CasUtil.getType(aCas, Token.class.getName());
                Feature lemmaFeature = tokenType.getFeatureByBaseName("lemma");
                CasUtil.selectCovered(aCas, tokenType, aBegin, aEnd).get(0)
                        .setFeatureValue(lemmaFeature, newAnnotation);
            }
            aCas.addFsToIndexes(newAnnotation);
        }
    }

    /**
     * Delete a span annotation from CAS
     *
     * @param aJCas
     *            the CAS object
     * @param aId
     *            the low-level address of the span annotation.
     */
    public void deleteFromCas(JCas aJCas, int aRef)
    {
        FeatureStructure fs = (FeatureStructure) BratAjaxCasUtil.selectAnnotationByAddress(aJCas,
                FeatureStructure.class, aRef);
        aJCas.removeFsFromIndexes(fs);
    }

    /**
     * Stores, for every tokens, the start and end position offsets : used for multiple span
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

    /**
     * Convenience method to get an adapter for part-of-speech.
     *
     * NOTE: This is not meant to stay. It's just a convenience during refactoring!
     */
    public static final SpanAdapter getPosAdapter()
    {
        SpanAdapter adapter = new SpanAdapter(AnnotationTypeConstant.POS_PREFIX,
                POS.class.getName(), AnnotationTypeConstant.POS_FEATURENAME);
        adapter.setSingleTokenBehavior(true);
        adapter.setAddPosToToken(true);
        return adapter;
    }

    /**
     * Convenience method to get an adapter for lemma.
     *
     * NOTE: This is not meant to stay. It's just a convenience during refactoring!
     */
    public static final SpanAdapter getLemmaAdapter()
    {
        SpanAdapter adapter = new SpanAdapter("", Lemma.class.getName(),
                AnnotationTypeConstant.LEMMA_FEATURENAME);
        adapter.setSingleTokenBehavior(true);
        adapter.setAddLemmaToToken(true);
        return adapter;
    }

    /**
     * Convenience method to get an adapter for named entity.
     *
     * NOTE: This is not meant to stay. It's just a convenience during refactoring!
     */
    public static final SpanAdapter getNamedEntityAdapter()
    {
        SpanAdapter adapter = new SpanAdapter(AnnotationTypeConstant.NAMEDENTITY_PREFIX,
                NamedEntity.class.getName(), AnnotationTypeConstant.NAMEDENTITY_FEATURENAME);
        adapter.setSingleTokenBehavior(false);
        return adapter;
    }

	@Override
	public String getLabelFeatureName() {
		// TODO Auto-generated method stub
		return labelFeatureName;
	}
}
