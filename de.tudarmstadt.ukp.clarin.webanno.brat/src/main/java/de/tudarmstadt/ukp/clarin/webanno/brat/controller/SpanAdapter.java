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
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import static java.util.Arrays.asList;
import static org.uimafit.util.CasUtil.getType;
import static org.uimafit.util.CasUtil.selectCovered;
import static org.uimafit.util.JCasUtil.selectCovered;

import java.util.List;

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
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
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
     * prefix will not stored in the CAS (striped away at {@link BratAjaxCasController#getType} )
     */
    private String labelPrefix;

    /**
     * A field that takes the name of the feature which should be set, e.e. "pos" or "lemma".
     */
    private String attachFeature;

    /**
     * A field that takes the name of the annotation to attach to, e.g.
     * "de.tudarmstadt...type.Token" (Token.class.getName())
     */
    private String attachType;

    /**
     * The UIMA type name.
     */
    private String annotationTypeName;

    /**
     * The feature of an UIMA annotation containing the label to be displayed in the UI.
     */
    private String labelFeatureName;

    private boolean singleTokenBehavior = false;

    public SpanAdapter(String aLabelPrefix, String aTypeName, String aLabelFeatureName,
            String aAttachFeature, String aAttachType)
    {
        labelPrefix = aLabelPrefix;
        labelFeatureName = aLabelFeatureName;
        annotationTypeName = aTypeName;
        attachFeature = aAttachFeature;
        attachType = aAttachType;
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
    public void render(JCas aJcas, GetDocumentResponse aResponse,
            BratAnnotatorModel aBratAnnotatorModel)
    {

        // The first sentence address in the display window!
        Sentence firstSentence = BratAjaxCasUtil.selectSentenceAt(aJcas,
                aBratAnnotatorModel.getSentenceBeginOffset(),
                aBratAnnotatorModel.getSentenceEndOffset());

        int lastAddressInPage = BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(aJcas,
                firstSentence.getAddress(), aBratAnnotatorModel.getWindowSize());

        // the last sentence address in the display window
        Sentence lastSentenceInPage;
        if (aBratAnnotatorModel.getMode().equals(Mode.CURATION)) {
            lastSentenceInPage = firstSentence;
        }
        else {
            lastSentenceInPage = (Sentence) BratAjaxCasUtil.selectByAddr(aJcas,
                    FeatureStructure.class, lastAddressInPage);
        }

        Type type = getType(aJcas.getCas(), annotationTypeName);
        int aFirstSentenceOffset = firstSentence.getBegin();
        for (AnnotationFS fs : selectCovered(aJcas.getCas(), type, firstSentence.getBegin(),
                lastSentenceInPage.getEnd())) {
            Feature labelFeature = fs.getType().getFeatureByBaseName(labelFeatureName);
            aResponse.addEntity(new Entity(((FeatureStructureImpl) fs).getAddress(), labelPrefix
                    + fs.getStringValue(labelFeature), asList(new Offsets(fs.getBegin()
                    - aFirstSentenceOffset, fs.getEnd() - aFirstSentenceOffset))));
        }
    }

    public static void renderTokenAndSentence(JCas aJcas, GetDocumentResponse aResponse,
            BratAnnotatorModel aBratAnnotatorModel)
    {

        // The first sentence address in the display window!
        Sentence firstSentence = BratAjaxCasUtil.selectSentenceAt(aJcas,
                aBratAnnotatorModel.getSentenceBeginOffset(),
                aBratAnnotatorModel.getSentenceEndOffset());

        int lastAddressInPage = BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(aJcas,
                firstSentence.getAddress(), aBratAnnotatorModel.getWindowSize());

        // the last sentence address in the display window
        Sentence lastSentenceInPage;
        if (aBratAnnotatorModel.getMode().equals(Mode.CURATION)) {
            lastSentenceInPage = firstSentence;
        }
        else {
            lastSentenceInPage = (Sentence) BratAjaxCasUtil.selectByAddr(aJcas,
                    FeatureStructure.class, lastAddressInPage);
        }

        int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(aJcas, firstSentence.getAddress());
        aResponse.setSentenceNumberOffset(sentenceNumber);

        int aFirstSentenceOffset = firstSentence.getBegin();

        // Render token + texts
        for (AnnotationFS fs : selectCovered(aJcas, Token.class, firstSentence.getBegin(),
                lastSentenceInPage.getEnd())) {
            aResponse.addToken(fs.getBegin() - aFirstSentenceOffset, fs.getEnd()
                    - aFirstSentenceOffset);
        }
        aResponse.setText(aJcas.getDocumentText().substring(aFirstSentenceOffset,
                lastSentenceInPage.getEnd()));

        // Render Sentence
        for (AnnotationFS fs : selectCovered(aJcas, Sentence.class, firstSentence.getBegin(),
                lastSentenceInPage.getEnd())) {
            aResponse.addSentence(fs.getBegin() - aFirstSentenceOffset, fs.getEnd()
                    - aFirstSentenceOffset);
        }

    }

    /**
     * Update the CAS with new/modification of span annotations from brat
     *
     * @param aLabelValue
     *            the value of the annotation for the span
     * @throws BratAnnotationException
     */
    public void add(JCas aJcas, int aBegin, int aEnd, String aLabelValue)
        throws BratAnnotationException
    {
        if (BratAjaxCasUtil.isSameSentence(aJcas, aBegin, aEnd)) {
            if (singleTokenBehavior) {
                List<Token> tokens = selectCovered(aJcas, Token.class, aBegin, aEnd);
                if (tokens.size() == 0) {
                    throw new SubTokenSelectedException(
                            "A minimum of one token should be selected!");
                }
                else {
                    for (Token token : tokens) {
                        updateCas(aJcas.getCas(), token.getBegin(), token.getEnd(), aLabelValue);
                    }
                }
            }
            else {
                List<Token> tokens = selectCovered(aJcas, Token.class, aBegin, aEnd);
                if (tokens.size() == 0) {
                    throw new SubTokenSelectedException(
                            "A minimum of one token should be selected!");
                }
                else {
                    // update the begin and ends (no sub token selection
                    aBegin = tokens.get(0).getBegin();
                    aEnd = tokens.get(tokens.size() - 1).getEnd();
                    updateCas(aJcas.getCas(), aBegin, aEnd, aLabelValue);
                }
            }
        }
        else {
            throw new MultipleSentenceCoveredException("Annotation coveres multiple sentences, "
                    + "limit your annotation to single sentence!");
        }
    }

    /**
     * A Helper method to {@link #add(String, BratAnnotatorUIData)}
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

            if (attachFeature != null) {
                Type theType = CasUtil.getType(aCas, attachType);
                Feature posFeature = theType.getFeatureByBaseName(attachFeature);
                CasUtil.selectCovered(aCas, theType, aBegin, aEnd).get(0)
                        .setFeatureValue(posFeature, newAnnotation);
            }
            aCas.addFsToIndexes(newAnnotation);
        }
    }

    @Override
    public void delete(JCas aJCas, int aAddress)
    {
        FeatureStructure fs = (FeatureStructure) BratAjaxCasUtil.selectByAddr(aJCas,
                FeatureStructure.class, aAddress);
        aJCas.removeFsFromIndexes(fs);
    }

    /**
     * Convenience method to get an adapter for part-of-speech.
     *
     * NOTE: This is not meant to stay. It's just a convenience during refactoring!
     */
    public static final SpanAdapter getPosAdapter()
    {
        SpanAdapter adapter = new SpanAdapter(AnnotationTypeConstant.POS_PREFIX,
                POS.class.getName(), "PosValue", "pos",
                Token.class.getName());
        adapter.setSingleTokenBehavior(true);
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
                "value", "lemma", Token.class.getName());
        adapter.setSingleTokenBehavior(true);
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
                NamedEntity.class.getName(), "value", null,
                null);
        adapter.setSingleTokenBehavior(false);
        return adapter;
    }

    @Override
    public String getLabelFeatureName()
    {
        return labelFeatureName;
    }

    @Override
    public String getLabelPrefix()
    {
        return labelPrefix;
    }

    @Override
    public Type getAnnotationType(CAS cas)
    {
        return CasUtil.getType(cas, annotationTypeName);
    }
    
    @Override
    public String getAnnotationTypeName()
    {
        return annotationTypeName;
    }
}
