/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.brat.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getFeature;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getFirstSentenceNumber;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.isSameSentence;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.selectOverlapping;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.selectSentenceAt;
import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratAjaxCasUtil.setFeature;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.ObjectUtils;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.action.ActionContext;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.component.AnnotationDetailEditorPanel.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.exception.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.exception.MultipleSentenceCoveredException;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A class that is used to create Brat Span to CAS and vice-versa
 *
 */
public class SpanAdapter
    implements TypeAdapter, AutomationTypeAdapter
{
    /**
     * The minimum offset of the annotation is on token, and the annotation can't span multiple
     * tokens too
     */
    private boolean lockToTokenOffsets;

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

    private boolean deletable;

    private AnnotationLayer layer;

    private Map<String, AnnotationFeature> features;

    // value NILL for a token when the training file do not have annotations provided
    private final static String NILL = "__nill__";

    public SpanAdapter(AnnotationLayer aLayer, Collection<AnnotationFeature> aFeatures)
    {
        layer = aLayer;

        // Using a sorted map here so we have reliable positions in the map when iterating. We use
        // these positions to remember the armed slots!
        features = new TreeMap<String, AnnotationFeature>();
        for (AnnotationFeature f : aFeatures) {
            features.put(f.getName(), f);
        }
    }

    /**
     * Span can only be made on a single token (not multiple tokens), e.g. for POS or Lemma
     * annotations. If this is set and a span is made across multiple tokens, then one annotation of
     * the specified type will be created for each token. If this is not set, a single annotation
     * covering all tokens is created.
     *
     * @param aSingleTokenBehavior
     *            whether to enable the behavior.
     */
    public void setLockToTokenOffsets(boolean aSingleTokenBehavior)
    {
        lockToTokenOffsets = aSingleTokenBehavior;
    }

    /**
     * @return whether the behavior is enabled.
     * @see #setLockToTokenOffsets(boolean)
     */
    public boolean isLockToTokenOffsets()
    {
        return lockToTokenOffsets;
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
     * Add new span annotation into the CAS and return the the id of the span annotation
     *
     * @param aJcas
     *            the JCas.
     * @param aBegin
     *            the begin offset.
     * @param aEnd
     *            the end offset.
     * @param aFeature
     *            the feature.
     * @param aValue
     *            the value of the annotation for the span
     * @return the ID.
     * @throws BratAnnotationException
     *             if the annotation cannot be created/updated.
     */
    public Integer add(JCas aJcas, int aBegin, int aEnd, AnnotationFeature aFeature, Object aValue)
        throws BratAnnotationException
    {
        // if zero-offset annotation is requested
        if (aBegin == aEnd) {
            return updateCas(aJcas.getCas(), aBegin, aEnd, aFeature, aValue);
        }
        if (crossMultipleSentence || isSameSentence(aJcas, aBegin, aEnd)) {
            if (lockToTokenOffsets) {
                List<Token> tokens = selectOverlapping(aJcas, Token.class, aBegin, aEnd);

                if (tokens.isEmpty()) {
                    throw new BratAnnotationException("No token is found to annotate");
                }
                return updateCas(aJcas.getCas(), tokens.get(0).getBegin(), tokens.get(0).getEnd(),
                        aFeature, aValue);

            }
            else if (allowMultipleToken) {
                List<Token> tokens = selectOverlapping(aJcas, Token.class, aBegin, aEnd);
                // update the begin and ends (no sub token selection
                aBegin = tokens.get(0).getBegin();
                aEnd = tokens.get(tokens.size() - 1).getEnd();
                return updateCas(aJcas.getCas(), aBegin, aEnd, aFeature, aValue);
            }
            else {
                return updateCas(aJcas.getCas(), aBegin, aEnd, aFeature, aValue);
            }
        }
        else {
            throw new MultipleSentenceCoveredException("Annotation coveres multiple sentences, "
                    + "limit your annotation to single sentence!");
        }
    }
    
    // get feature Value of existing span annotation 
    public Serializable getSpan(JCas aJCas, int aBegin, int aEnd, AnnotationFeature aFeature,
            String aLabelValue)
    {
        if(allowStacking){
            return null;
        }
        int begin;
        int end;
        // update the begin and ends (no sub token selection)
        if (lockToTokenOffsets) {
            List<Token> tokens = selectOverlapping(aJCas, Token.class, aBegin, aEnd);
            begin = tokens.get(0).getBegin();
            end = tokens.get(tokens.size() - 1).getEnd();
        }
        else if (allowMultipleToken) {
            List<Token> tokens = selectOverlapping(aJCas, Token.class, aBegin, aEnd);
            begin = tokens.get(0).getBegin();
            end = tokens.get(tokens.size() - 1).getEnd();
        }
        else {
            begin = aBegin;
            end = aEnd;
        }
        Type type = CasUtil.getType(aJCas.getCas(), getAnnotationTypeName());

        for (AnnotationFS fs : CasUtil.selectCovered(aJCas.getCas(), type, begin, end)) {
            if (fs.getBegin() == aBegin && fs.getEnd() == aEnd) {
                return getFeatureValue(fs, aFeature);
            }
        }
        return null;
    }

    public static Serializable getFeatureValue(FeatureStructure aFs, AnnotationFeature aFeature)
       {
           Feature uimaFeature = aFs.getType().getFeatureByBaseName(aFeature.getName());
           switch (aFeature.getType()) {
           case CAS.TYPE_NAME_STRING:
               return aFs.getFeatureValueAsString(uimaFeature);
           case CAS.TYPE_NAME_BOOLEAN:
               return aFs.getBooleanValue(uimaFeature);
           case CAS.TYPE_NAME_FLOAT:
               return aFs.getFloatValue(uimaFeature);
           case CAS.TYPE_NAME_INTEGER:
               return aFs.getIntValue(uimaFeature);
           default:
               return aFs.getFeatureValueAsString(uimaFeature);
           }
       }
    /**
     * A Helper method to add annotation to CAS
     */
    private Integer updateCas(CAS aCas, int aBegin, int aEnd, AnnotationFeature aFeature,
            Object aValue)
        throws BratAnnotationException
    {
        Type type = CasUtil.getType(aCas, getAnnotationTypeName());
        for (AnnotationFS fs : CasUtil.selectCovered(aCas, type, aBegin, aEnd)) {
            if (fs.getBegin() == aBegin && fs.getEnd() == aEnd) {
                if (!allowStacking) {
                    setFeature(fs, aFeature, aValue);
                    return getAddr(fs);
                }
            }
        }
        AnnotationFS newAnnotation = createAnnotation(aCas, aBegin, aEnd, aFeature, aValue, type);
        return getAddr(newAnnotation);
    }

    private AnnotationFS createAnnotation(CAS aCas, int aBegin, int aEnd,
            AnnotationFeature aFeature, Object aValue, Type aType)
        throws BratAnnotationException
    {
        AnnotationFS newAnnotation = aCas.createAnnotation(aType, aBegin, aEnd);
        setFeature(newAnnotation, aFeature, aValue);

        if (getAttachFeatureName() != null) {
            Type theType = CasUtil.getType(aCas, getAttachTypeName());
            Feature attachFeature = theType.getFeatureByBaseName(getAttachFeatureName());
            if (CasUtil.selectCovered(aCas, theType, aBegin, aEnd).isEmpty()) {
                throw new BratAnnotationException("No annotation of type [" + getAttachTypeName()
                        + "] to attach to at location [" + aBegin + "-" + aEnd + "].");
            }
            CasUtil.selectCovered(aCas, theType, aBegin, aEnd).get(0)
                    .setFeatureValue(attachFeature, newAnnotation);
        }
        aCas.addFsToIndexes(newAnnotation);
        return newAnnotation;
    }

    /**
     * A Helper method to add annotation to a Curation CAS
     * @throws BratAnnotationException 
     */
    public AnnotationFS updateCurationCas(CAS aCas, int aBegin, int aEnd,
            AnnotationFeature aFeature, Object aValue, AnnotationFS aClickedFs, boolean aIsSlot)
        throws BratAnnotationException
    {
        Type type = CasUtil.getType(aCas, getAnnotationTypeName());
        AnnotationFS newAnnotation = null;
        int countAnno = 0;
        for (AnnotationFS fs : CasUtil.selectCovered(aCas, type, aBegin, aEnd)) {
            countAnno++;
            newAnnotation = fs;
            if (fs.getBegin() == aBegin && fs.getEnd() == aEnd) {
                if (!allowStacking) {
                    setFeature(fs, aFeature, aValue);
                    return fs;
                }
                // if stacking, get other existing feature values before updating with the new
                // feature
                StringBuilder clickedFtValues = new StringBuilder();
                StringBuilder curationFtValues = new StringBuilder();
                for (Feature feat : type.getFeatures()) {
                    switch (feat.getRange().getName()) {
                    case CAS.TYPE_NAME_STRING:
                    case CAS.TYPE_NAME_BOOLEAN:
                    case CAS.TYPE_NAME_FLOAT:
                    case CAS.TYPE_NAME_INTEGER:
                        clickedFtValues.append(aClickedFs.getFeatureValueAsString(feat));
                        curationFtValues.append(fs.getFeatureValueAsString(feat));
                    default:
                        continue;
                    }
                }
                if (clickedFtValues.toString().equals(curationFtValues.toString())) {
                    return fs;
                }
            }
        }

        if (!aIsSlot) {
            newAnnotation = createAnnotation(aCas, aBegin, aEnd, aFeature, aValue, type);
        }
        if (aIsSlot && countAnno > 1) {
            throw new BratAnnotationException(
                    "There are different stacking annotation on curation panel, cannot copy the slot feature");
        }
        return newAnnotation;
    }

    @Override
    public void delete(JCas aJCas, VID aVid)
    {
        FeatureStructure fs = selectByAddr(aJCas, FeatureStructure.class, aVid.getId());
        aJCas.removeFsFromIndexes(fs);

        // delete associated attachFeature
        if (getAttachTypeName() == null) {
            return;
        }
        Type theType = CasUtil.getType(aJCas.getCas(), getAttachTypeName());
        Feature attachFeature = theType.getFeatureByBaseName(getAttachFeatureName());
        if (attachFeature == null) {
            return;
        }
        CasUtil.selectCovered(aJCas.getCas(), theType, ((AnnotationFS) fs).getBegin(),
                ((AnnotationFS) fs).getEnd()).get(0).setFeatureValue(attachFeature, null);

    }

    @Override
    public void delete(JCas aJCas, AnnotationFeature aFeature, int aBegin, int aEnd, Object aValue)
    {
        Type type = CasUtil.getType(aJCas.getCas(), getAnnotationTypeName());
        for (AnnotationFS fs : CasUtil.selectCovered(aJCas.getCas(), type, aBegin, aEnd)) {

            if (fs.getBegin() == aBegin && fs.getEnd() == aEnd) {
                if (ObjectUtils.equals(getFeature(fs, aFeature), aValue)) {
                    delete(aJCas, new VID(getAddr(fs)));
                }
            }
        }
    }

    @Override
    public long getTypeId()
    {
        return layer.getId();
    }

    @Override
    public Type getAnnotationType(CAS cas)
    {
        return CasUtil.getType(cas, getAnnotationTypeName());
    }

    /**
     * The UIMA type name.
     */
    @Override
    public String getAnnotationTypeName()
    {
        return layer.getName();
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
        return layer.getAttachFeature() == null ? null : layer.getAttachFeature().getName();
    }

    @Override
    public List<String> getAnnotation(JCas aJcas, AnnotationFeature aFeature, int begin, int end)
    {
        Type type = getType(aJcas.getCas(), getAnnotationTypeName());
        List<String> annotations = new ArrayList<String>();

        for (Token token : selectCovered(aJcas, Token.class, begin, end)) {
            if (selectCovered(aJcas.getCas(), type, token.getBegin(), token.getEnd()).size() > 0) {
                AnnotationFS anno = selectCovered(aJcas.getCas(), type, token.getBegin(),
                        token.getEnd()).get(0);
                Feature labelFeature = anno.getType().getFeatureByBaseName(aFeature.getName());
                annotations.add(anno.getFeatureValueAsString(labelFeature));
            }
            else {
                annotations.add(NILL);
            }
        }
        return annotations;
    }

    public Map<Integer, String> getMultipleAnnotation(Sentence sentence, AnnotationFeature aFeature)
        throws CASException
    {
        Map<Integer, String> multAnno = new HashMap<Integer, String>();
        Type type = getType(sentence.getCAS(), getAnnotationTypeName());
        for (AnnotationFS fs : selectCovered(sentence.getCAS(), type, sentence.getBegin(),
                sentence.getEnd())) {
            boolean isBegin = true;
            Feature labelFeature = fs.getType().getFeatureByBaseName(aFeature.getName());
            for (Token token : selectCovered(sentence.getCAS().getJCas(), Token.class,
                    fs.getBegin(), fs.getEnd())) {
                if (multAnno.get(getAddr(token)) == null) {
                    if (isBegin) {
                        multAnno.put(getAddr(token),
                                "B-" + fs.getFeatureValueAsString(labelFeature));
                        isBegin = false;
                    }
                    else {
                        multAnno.put(getAddr(token),
                                "I-" + fs.getFeatureValueAsString(labelFeature));
                    }
                }
            }
        }
        return multAnno;
    }

    /**
     * A field that takes the name of the annotation to attach to, e.g.
     * "de.tudarmstadt...type.Token" (Token.class.getName())
     */
    @Override
    public String getAttachTypeName()
    {
        return layer.getAttachType() == null ? null : layer.getAttachType().getName();
    }

    @Override
    public void updateFeature(JCas aJcas, AnnotationFeature aFeature, int aAddress, Object aValue)
    {
        FeatureStructure fs = selectByAddr(aJcas, FeatureStructure.class, aAddress);
        setFeature(fs, aFeature, aValue);
    }

    @Override
    public AnnotationLayer getLayer()
    {
        return layer;
    }

    @Override
    public Collection<AnnotationFeature> listFeatures()
    {
        return features.values();
    }
}
