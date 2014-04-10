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
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
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
    /**
     * Prefix of the label value for Brat to make sure that different annotation types can use the
     * same label, e.g. a POS tag "N" and a named entity type "N".
     *
     * This is used to differentiate the different types in the brat annotation/visualization. The
     * prefix will not stored in the CAS (striped away at {@link BratAjaxCasController#getType} )
     *
     * It is a short unique numeric identifier for the type (primary key in the DB). This identifier
     * is only transiently used when communicating with the UI. It is not persisted long term other
     * than in the type registry (e.g. in the database).
     */
    private final long typeId;

    /**
     * A field that takes the name of the feature which should be set, e.e. "pos" or "lemma".
     */
    private final String attachFeatureName;

    /**
     * A field that takes the name of the annotation to attach to, e.g.
     * "de.tudarmstadt...type.Token" (Token.class.getName())
     */
    private final String attachType;

    /**
     * The UIMA type name.
     */
    private final String annotationTypeName;

    /**
     * The minimum offset of the annotation is on token, and the annotation can't span multiple
     * tokens too
     */
    private boolean lockeToTokenOffsets;

    /**
     * The minimum offset of the annotation is on token, and the annotation can span multiple token
     * too
     */
    private boolean allowMultipleToken;

    /**
     * Allow multiple annotations of the same layer (only when the type value is different)
     */
    private boolean allowStacking;

    private boolean crossMultipleSentence;

    boolean deletable;

    public SpanAdapter(long aTypeId, String aTypeName, String aAttachFeature, String aAttachType)
    {

        typeId = aTypeId;
        annotationTypeName = aTypeName;
        attachFeatureName = aAttachFeature;
        attachType = aAttachType;
    }

    /**
     * Span can only be made on a single token (not multiple tokens), e.g. for POS or Lemma
     * annotations. If this is set and a span is made across multiple tokens, then one annotation of
     * the specified type will be created for each token. If this is not set, a single annotation
     * covering all tokens is created.
     */
    public void setLockToTokenOffsets(boolean aSingleTokenBehavior)
    {
        lockeToTokenOffsets = aSingleTokenBehavior;
    }

    /**
     * @see #setLockToTokenOffsets(boolean)
     */
    public boolean isLockToTokenOffsets()
    {
        return lockeToTokenOffsets;
    }

    public boolean isAllowMultipleToken()
    {
        return allowMultipleToken;
    }

    public void setAllowMultipleToken(boolean allowMultipleToken)
    {
        this.allowMultipleToken = allowMultipleToken;
    }

    public boolean isAllowStacking()
    {
        return allowStacking;
    }

    public void setAllowStacking(boolean allowStacking)
    {
        this.allowStacking = allowStacking;
    }

    public boolean isCrossMultipleSentence()
    {
        return crossMultipleSentence;
    }

    public void setCrossMultipleSentence(boolean crossMultipleSentence)
    {
        this.crossMultipleSentence = crossMultipleSentence;
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
    public void render(JCas aJcas, List<AnnotationFeature> aFeatures,
            GetDocumentResponse aResponse, BratAnnotatorModel aBratAnnotatorModel)
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

            String annotations = "";
            for (AnnotationFeature feature : aFeatures) {
                Feature labelFeature = fs.getType().getFeatureByBaseName(feature.getName());
                if (annotations.equals("")) {
                    annotations = typeId
                            + "_"
                            + (fs.getFeatureValueAsString(labelFeature) == null ? " " : fs
                                    .getFeatureValueAsString(labelFeature));
                }
                else {
                    annotations = annotations
                            + " | "
                            + (fs.getFeatureValueAsString(labelFeature) == null ? " " : fs
                                    .getFeatureValueAsString(labelFeature));
                }
            }

            aResponse.addEntity(new Entity(((FeatureStructureImpl) fs).getAddress() + "",
                    annotations, asList(new Offsets(fs.getBegin() - aFirstSentenceOffset, fs
                            .getEnd() - aFirstSentenceOffset))));
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

        int sentenceNumber = BratAjaxCasUtil.getFirstSentenceNumber(aJcas,
                firstSentence.getAddress());
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

    public void add(JCas aJcas, int aBegin, int aEnd, AnnotationFeature aFeature, String aValue)
        throws BratAnnotationException
    {
        if (crossMultipleSentence || BratAjaxCasUtil.isSameSentence(aJcas, aBegin, aEnd)) {
            if (lockeToTokenOffsets) {
                List<Token> tokens = BratAjaxCasUtil.selectOverlapping(aJcas, Token.class, aBegin,
                        aEnd);

                for (Token token : tokens) {
                    updateCas(aJcas.getCas(), token.getBegin(), token.getEnd(), aFeature, aValue);
                }
            }
            else if (allowMultipleToken) {
                List<Token> tokens = BratAjaxCasUtil.selectOverlapping(aJcas, Token.class, aBegin,
                        aEnd);
                // update the begin and ends (no sub token selection
                aBegin = tokens.get(0).getBegin();
                aEnd = tokens.get(tokens.size() - 1).getEnd();
                updateCas(aJcas.getCas(), aBegin, aEnd, aFeature, aValue);
            }
            else {
                updateCas(aJcas.getCas(), aBegin, aEnd, aFeature, aValue);
            }
        }
        else {
            throw new MultipleSentenceCoveredException("Annotation coveres multiple sentences, "
                    + "limit your annotation to single sentence!");
        }
    }

    /**
     * A Helper method to add annotation to CAS
     */
    private void updateCas(CAS aCas, int aBegin, int aEnd, AnnotationFeature aFeature, String aValue)
    {
        boolean duplicate = false;
        Type type = CasUtil.getType(aCas, annotationTypeName);

        Feature feature = type.getFeatureByBaseName(aFeature.getName());
        for (AnnotationFS fs : CasUtil.selectCovered(aCas, type, aBegin, aEnd)) {

            if (fs.getBegin() == aBegin && fs.getEnd() == aEnd) {
                if (!allowStacking) {
                    fs.setStringValue(feature, aValue);
                    duplicate = true;
                    continue;
                }
                if (fs.getStringValue(feature).equals(aValue)) {
                    duplicate = true;
                    // break;
                    // if (!fs.getStringValue(feature).equals(aValue)) {
                    // fs.setStringValue(feature, aValue);
                }
            }
        }
        if (!duplicate) {
            AnnotationFS newAnnotation = aCas.createAnnotation(type, aBegin, aEnd);
            newAnnotation.setStringValue(feature, aValue);

            if (attachFeatureName != null) {
                Type theType = CasUtil.getType(aCas, attachType);
                Feature attachFeature = theType.getFeatureByBaseName(attachFeatureName);
                // if the attache type feature structure is not in place
                // (for
                // custom annotation),
                // create it
                if (CasUtil.selectCovered(aCas, theType, aBegin, aEnd).size() == 0) {
                    AnnotationFS attachTypeAnnotation = aCas
                            .createAnnotation(theType, aBegin, aEnd);
                    aCas.addFsToIndexes(attachTypeAnnotation);

                }
                CasUtil.selectCovered(aCas, theType, aBegin, aEnd).get(0)
                        .setFeatureValue(attachFeature, newAnnotation);
            }
            aCas.addFsToIndexes(newAnnotation);
        }
    }

    @Override
    public void delete(JCas aJCas, int aAddress)
    {
        FeatureStructure fs = BratAjaxCasUtil.selectByAddr(aJCas, FeatureStructure.class, aAddress);
        aJCas.removeFsFromIndexes(fs);

        // delete associated attachFeature
        if (attachType == null) {
            return;
        }
        Type theType = CasUtil.getType(aJCas.getCas(), attachType);
        Feature attachFeature = theType.getFeatureByBaseName(attachFeatureName);
        if (attachFeature == null) {
            return;
        }
        CasUtil.selectCovered(aJCas.getCas(), theType, ((AnnotationFS) fs).getBegin(),
                ((AnnotationFS) fs).getEnd()).get(0).setFeatureValue(attachFeature, null);

    }

    @Override
    public void delete(JCas aJCas, AnnotationFeature aFeature, int aBegin, int aEnd, String aValue)
    {
        Type type = CasUtil.getType(aJCas.getCas(), annotationTypeName);
        Feature feature = type.getFeatureByBaseName(aFeature.getName());
        for (AnnotationFS fs : CasUtil.selectCovered(aJCas.getCas(), type, aBegin, aEnd)) {

            if (fs.getBegin() == aBegin && fs.getEnd() == aEnd) {
                if (fs.getStringValue(feature).equals(aValue)) {
                    delete(aJCas, ((FeatureStructureImpl) fs).getAddress());

                }
            }
        }
    }

    @Override
    public long getTypeId()
    {
        return typeId;
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

    public void setDeletable(boolean aDeletable)
    {
        this.deletable = aDeletable;
    }

    @Override
    public boolean isDeletable()
    {
        return deletable;
    }

    @Override
    public String getAttachFeatureName()
    {
        return attachFeatureName;
    }

    @Override
    public void deleteBySpan(JCas aJCas, AnnotationFS fs, int aBegin, int aEnd)
    {

    }

    @Override
    public List<String> getAnnotation(JCas aJcas, AnnotationFeature aFeature, int begin, int end)
    {
        Type type = getType(aJcas.getCas(), annotationTypeName);
        List<String> annotations = new ArrayList<String>();
        for (AnnotationFS fs : selectCovered(aJcas.getCas(), type, begin, end)) {
            Feature labelFeature = fs.getType().getFeatureByBaseName(aFeature.getName());
            annotations.add(fs.getStringValue(labelFeature));
        }
        return annotations;
    }

    @Override
    public void automate(JCas aJcas, AnnotationFeature aFeature, List<String> aLabelValues)
        throws BratAnnotationException, IOException
    {

        Type type = CasUtil.getType(aJcas.getCas(), annotationTypeName);
        Feature feature = type.getFeatureByBaseName(aFeature.getName());

        int i = 0;
        String prevNe = "O";
        int begin = 0;
        int end = 0;
        // remove existing annotations of this type, after all it is an
        // automation, no care
        BratAnnotatorUtility.clearAnnotations(aJcas, type);

        if (type.getName().equals(NamedEntity.class.getName())) {
            for (Token token : select(aJcas, Token.class)) {
                String value = aLabelValues.get(i);
                AnnotationFS newAnnotation;
                if (value.equals("O") && prevNe.equals("O")) {
                    i++;
                    continue;
                }
                else if (value.equals("O") && !prevNe.equals("O")) {
                    newAnnotation = aJcas.getCas().createAnnotation(type, begin, end);
                    newAnnotation.setStringValue(feature, prevNe.replace("B-", ""));
                    prevNe = "O";
                    aJcas.getCas().addFsToIndexes(newAnnotation);
                }
                else if (!value.equals("O") && prevNe.equals("O")) {
                    begin = token.getBegin();
                    end = token.getEnd();
                    prevNe = value;

                }
                else if (!value.equals("O") && !prevNe.equals("O")) {
                    if (value.replace("B-", "").replace("I-", "")
                            .equals(prevNe.replace("B-", "").replace("I-", ""))
                            && value.startsWith("B-")) {
                        newAnnotation = aJcas.getCas().createAnnotation(type, begin, end);
                        newAnnotation.setStringValue(feature,
                                prevNe.replace("B-", "").replace("I-", ""));
                        prevNe = value;
                        begin = token.getBegin();
                        end = token.getEnd();
                        aJcas.getCas().addFsToIndexes(newAnnotation);

                    }
                    else if (value.replace("B-", "").replace("I-", "")
                            .equals(prevNe.replace("B-", "").replace("I-", ""))) {
                        i++;
                        end = token.getEnd();
                        continue;

                    }
                    else {
                        newAnnotation = aJcas.getCas().createAnnotation(type, begin, end);
                        newAnnotation.setStringValue(feature,
                                prevNe.replace("B-", "").replace("I-", ""));
                        prevNe = value;
                        begin = token.getBegin();
                        end = token.getEnd();
                        aJcas.getCas().addFsToIndexes(newAnnotation);

                    }
                }

                i++;

            }
        }
        else {
            for (Token token : select(aJcas, Token.class)) {

                AnnotationFS newAnnotation = aJcas.getCas().createAnnotation(type,
                        token.getBegin(), token.getEnd());
                newAnnotation.setStringValue(feature, aLabelValues.get(i));
                i++;
                if (attachFeatureName != null) {
                    Type theType = CasUtil.getType(aJcas.getCas(), attachType);
                    Feature posFeature = theType.getFeatureByBaseName(attachFeatureName);
                    CasUtil.selectCovered(aJcas.getCas(), theType, token.getBegin(), token.getEnd())
                            .get(0).setFeatureValue(posFeature, newAnnotation);
                }
                aJcas.getCas().addFsToIndexes(newAnnotation);
            }
        }
    }

}
