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
import static org.uimafit.util.JCasUtil.select;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.uimafit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorUIData;
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
 * A class that is used to create Brat chain to CAS and vice-versa
 *
 * @author Seid Muhie Yimam
 */
public class ChainAdapter
    implements TypeAdapter
{
    private Log LOG = LogFactory.getLog(getClass());

    public static final String EXPLETIVE = "expletive";

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

    /**
     * The feature of an UIMA annotation for the first span in the chain
     */
    private String chainFirstFeatureName;

    /**
     * The feature of an UIMA annotation for the next span in the chain
     */
    private String linkNextFeatureName;

    private boolean singleTokenBehavior = false;

    private boolean isChain = false;

    public ChainAdapter(String aTypePrefix, String aTypeName, String aLabelFeatureName,
            String aFirstFeatureName, String aNextFeatureName)
    {
        typePrefix = aTypePrefix;
        labelFeatureName = aLabelFeatureName;
        annotationTypeName = aTypeName;
        chainFirstFeatureName = aFirstFeatureName;
        linkNextFeatureName = aNextFeatureName;
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
     * @see #setChain(boolean)
     */
    public boolean isChain()
    {
        return isChain;
    }

    /**
     * If true, It is drawing of arcs for coreference chains, otherwise it is a span annotation for
     * coreference links
     */
    public void setChain(boolean aIsChain)
    {
        isChain = aIsChain;
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
        Sentence firstSentence = (Sentence) BratAjaxCasUtil.selectAnnotationByAddress(aJcas,
                FeatureStructure.class, aBratAnnotatorModel.getSentenceAddress());

        // The last sentence address in the display window!
        Sentence lastSentence = (Sentence) BratAjaxCasUtil.selectAnnotationByAddress(
                aJcas,
                FeatureStructure.class,
                BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(aJcas,
                        aBratAnnotatorModel.getSentenceAddress(),
                        aBratAnnotatorModel.getWindowSize()));
        int i = aBratAnnotatorModel.getSentenceAddress();

        int windowBegin = firstSentence.getBegin();
        int windowEnd = lastSentence.getEnd();

        // Loop based on window size
        // j, controlling variable to display sentences based on window size
        // i, address of each sentences
        int j = 1;
        while (j <= aBratAnnotatorModel.getWindowSize()) {
            if (i >= aBratAnnotatorModel.getLastSentenceAddress()) {
                Sentence sentence = (Sentence) BratAjaxCasUtil.selectAnnotationByAddress(aJcas,
                        FeatureStructure.class, i);
                if (isChain) {
                    renderChains(sentence, aResponse, windowBegin, windowEnd);
                }
                else {
                    renderLinks(sentence, aResponse, windowBegin);
                }
                break;
            }
            else {
                Sentence sentence = (Sentence) BratAjaxCasUtil.selectAnnotationByAddress(aJcas,
                        FeatureStructure.class, i);
                if (isChain) {
                    renderChains(sentence, aResponse, windowBegin, windowEnd);
                }
                else {
                    renderLinks(sentence, aResponse, windowBegin);
                }
                i = BratAjaxCasUtil.getFollowingSentenceAddress(aJcas, i);
            }
            j++;
        }
    }

    /**
     * a helper method to the {@link #render(JCas, GetDocumentResponse, BratAnnotatorModel)}
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
    private void renderLinks(Sentence aSentence, GetDocumentResponse aResponse,
            int aFirstSentenceOffset)
    {
        Type type = CasUtil.getType(aSentence.getCAS(), annotationTypeName);
        Feature labelFeature = type.getFeatureByBaseName(labelFeatureName);
        for (AnnotationFS fs : CasUtil.selectCovered(type, aSentence)) {
            aResponse.addEntity(new Entity(((FeatureStructureImpl) fs).getAddress(), typePrefix
                    + fs.getStringValue(labelFeature), asList(new Offsets(fs.getBegin()
                    - aFirstSentenceOffset, fs.getEnd() - aFirstSentenceOffset))));
        }
    }

    /**
     *
     * @param aSentence
     * @param aResponse
     * @param aWindowBegin
     *            begin of the render window
     * @param aWindowEnd
     *            end of the render window The offset of the last sentence in the current display
     *            window
     * @param aReverse
     */
    private void renderChains(Sentence aSentence, GetDocumentResponse aResponse, int aWindowBegin,
            int aWindowEnd)
    {
        Type type = CasUtil.getType(aSentence.getCAS(), annotationTypeName);
        Feature first = type.getFeatureByBaseName(chainFirstFeatureName);

        int i = 1;// used for different display colors of chains
        for (FeatureStructure fs : CasUtil.selectFS(aSentence.getCAS(), type)) {
            // The first link in the
            AnnotationFS linkFs = (AnnotationFS) fs.getFeatureValue(first);

            Feature next = linkFs.getType().getFeatureByBaseName(linkNextFeatureName);
            Feature labelFeature = linkFs.getType().getFeatureByBaseName(labelFeatureName);
            while (linkFs != null) {
                // Render only links within the render window
                if (aWindowBegin <= linkFs.getBegin() && aWindowEnd >= linkFs.getEnd()) {
                    if (EXPLETIVE.equals(linkFs.getStringValue(labelFeature))) {
                        aResponse.addRelation(createLink(linkFs, linkFs, i));
                        linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
                        continue;
                    }
                    AnnotationFS nextLink = (AnnotationFS) linkFs.getFeatureValue(next);

                    if (nextLink != null) {
                        aResponse.addRelation(createLink(linkFs, nextLink, i));
                    }
                }
                linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
            }
            i++;
            if (i > 12) {
                i = 1;
            }
        }
    }

    /**
     * Create a link relation.
     *
     * @param aFrom
     *            source span
     * @param aTo
     *            target span
     * @param aColorIndex
     *            used for different display colors of chains
     */
    private Relation createLink(AnnotationFS aFrom, AnnotationFS aTo, int aColorIndex)
    {
        Feature labelFeature = aFrom.getType().getFeatureByBaseName(labelFeatureName);
        List<Argument> argumentList = getArgument(aFrom, aTo);
        return new Relation(((FeatureStructureImpl) aFrom).getAddress(), aColorIndex + typePrefix
                + aFrom.getStringValue(labelFeature), argumentList);
    }

    /**
     * Argument lists for the chain annotation
     *
     * @return
     */
    private static List<Argument> getArgument(FeatureStructure aOriginFs, FeatureStructure aTargetFs)
    {
        return asList(new Argument("Arg1", ((FeatureStructureImpl) aOriginFs).getAddress()),
                new Argument("Arg2", ((FeatureStructureImpl) aTargetFs).getAddress()));
    }

    /**
     * Update the CAS with new/modification of span annotations from brat
     *
     * @param aLabelValue
     *            the value of the annotation for the span
     */
    public void add(String aLabelValue, JCas aJCas, int aAnnotationOffsetStart,
            int aAnnotationOffsetEnd, int aOrigin, int aTarget)
    {
        Map<Integer, Integer> offsets = offsets(aJCas);

        if (singleTokenBehavior) {
            Map<Integer, Integer> splitedTokens = getSplitedTokens(offsets,
                    aAnnotationOffsetStart, aAnnotationOffsetEnd);
            if (isChain) {
                updateCoreferenceChainCas(aJCas, aOrigin,
                        aTarget, aLabelValue);
            }
            else {
                for (Integer start : splitedTokens.keySet()) {
                    updateCoreferenceLinkCas(aJCas.getCas(), start,
                            splitedTokens.get(start), aLabelValue);
                }
            }
        }
        else {
            if (isChain) {
                updateCoreferenceChainCas(aJCas, aOrigin,
                        aTarget, aLabelValue);
            }
            else {
                int startAndEnd[] = getTokenStart(offsets, aAnnotationOffsetStart, aAnnotationOffsetEnd);
                updateCoreferenceLinkCas(aJCas.getCas(),
                        startAndEnd[0], startAndEnd[1],
                        aLabelValue);
            }
        }
    }

    /**
     * A Helper method to {@link #add(String, BratAnnotatorUIData)}
     */
    private void updateCoreferenceLinkCas(CAS aCas, int aBegin, int aEnd, String aValue)
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
            aCas.addFsToIndexes(newAnnotation);
        }
    }

    /**
     * A Helper method to {@link #add(String, BratAnnotatorUIData)}
     */
    // Updating a coreference.
    // CASE 1: Chain does not exist yet
    // CASE 2: Add to the beginning of an existing chain
    // CASE 3: Add to the end of an existing chain
    // CASE 4: Replace a link in an existing chain
    // CASE 4a: we replace the link to the last link -> delete last link
    // CASE 4b: we replace the link to an intermediate link -> chain is cut in two,
    // create new CorefChain pointing to the first link in new chain
    // CASE 5: Add link at the same position as existing -> just update type
    private void updateCoreferenceChainCas(JCas aJcas, int aOrigin, int aTarget, String aValue)
    {
        boolean modify = false;

        AnnotationFS originLink = (AnnotationFS) BratAjaxCasUtil.selectAnnotationByAddress(aJcas,
                FeatureStructure.class, aOrigin);
        AnnotationFS targetLink = (AnnotationFS) BratAjaxCasUtil.selectAnnotationByAddress(aJcas,
                FeatureStructure.class, aTarget);

        // Currently support only anaphoric relation
        // Inverse direction
        if (originLink.getBegin() > targetLink.getBegin()) {
            AnnotationFS temp = originLink;
            originLink = targetLink;
            targetLink = temp;
        }

        AnnotationFS existingChain = null;
        boolean chainExist = false;
        boolean found = false;

        // If the two links are in different chain, merge them!!!
        boolean merge = mergeChain(aJcas, originLink, targetLink, aValue);

        if (!merge) {

            Type type = CasUtil.getType(aJcas.getCas(), annotationTypeName);
            Feature first = type.getFeatureByBaseName(chainFirstFeatureName);
            Feature next = targetLink.getType().getFeatureByBaseName(linkNextFeatureName);
            Feature labelFeature = targetLink.getType().getFeatureByBaseName(labelFeatureName);
            for (FeatureStructure fs : CasUtil.selectFS(aJcas.getCas(), type)) {

                AnnotationFS linkFs = (AnnotationFS) fs.getFeatureValue(first);

                // CASE 2
                if (fs.getFeatureValue(first) != null
                        && !found
                        && ((FeatureStructureImpl) fs.getFeatureValue(first)).getAddress() == ((FeatureStructureImpl) targetLink)
                                .getAddress()) {
                    fs.setFeatureValue(first, originLink);

                    originLink.setFeatureValue(next, targetLink);
                    originLink.setStringValue(labelFeature, aValue);
                    found = true;
                    break;
                }

                AnnotationFS lastLink = linkFs;

                while (linkFs != null && !found) {
                    // a-> c, b->c = a->b->c
                    if (linkFs.getFeatureValue(next) != null
                            && BratAjaxCasUtil.isAt((Annotation) linkFs.getFeatureValue(next),
                                    (Annotation) targetLink)) {
                        if (linkFs.getBegin() > originLink.getBegin()) {
                            originLink.setFeatureValue(next, linkFs);

                            if (lastLink == (AnnotationFS) fs.getFeatureValue(first)) {
                                fs.setFeatureValue(first, originLink);
                            }
                            else {
                                lastLink.setFeatureValue(next, originLink);
                            }
                        }
                        else {
                            linkFs.setFeatureValue(next, originLink);
                            originLink.setFeatureValue(next, targetLink);
                        }
                        originLink.setStringValue(labelFeature, aValue);
                        chainExist = true;
                        found = true;
                        break;
                    }
                    // CASE 4a/b
                    if (BratAjaxCasUtil.isAt((Annotation) linkFs, (Annotation) originLink)
                            && linkFs.getFeatureValue(next) != null
                            && !BratAjaxCasUtil.isAt((Annotation) linkFs.getFeatureValue(next),
                                    (Annotation) targetLink)
                            && targetLink.getBegin() < ((AnnotationFS) linkFs.getFeatureValue(next))
                                    .getBegin()) {
                        AnnotationFS tmpLink = (AnnotationFS) linkFs.getFeatureValue(next);
                        String tmpRel = linkFs.getStringValue(labelFeature);
                        linkFs.setFeatureValue(next, targetLink);
                        linkFs.setStringValue(labelFeature, aValue);
                        targetLink.setFeatureValue(next, tmpLink);
                        targetLink.setStringValue(labelFeature, tmpRel);
                        chainExist = true;
                        found = true;
                        break;
                    }
                    else if (BratAjaxCasUtil.isAt((Annotation) linkFs, (Annotation) originLink)
                            && linkFs.getFeatureValue(next) != null
                            && !BratAjaxCasUtil.isAt((Annotation) linkFs.getFeatureValue(next),
                                    (Annotation) targetLink)) {
                        linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
                        originLink = linkFs;
                        continue;
                    }
                    else if (BratAjaxCasUtil.isAt((Annotation) linkFs, (Annotation) originLink)
                            && linkFs.getFeatureValue(next) == null) {
                        linkFs.setFeatureValue(next, targetLink);
                        linkFs.setStringValue(labelFeature, aValue);
                        chainExist = true;
                        found = true;
                        break;
                    }
                    if (BratAjaxCasUtil.isAt((Annotation) linkFs, (Annotation) originLink)
                            && linkFs.getFeatureValue(next) != null
                            && BratAjaxCasUtil.isAt((Annotation) linkFs.getFeatureValue(next),
                                    (Annotation) targetLink)) {
                        modify = !linkFs.getStringValue(labelFeature).equals(aValue);
                        existingChain = linkFs;
                        chainExist = true;
                        break;
                    }

                    lastLink = linkFs;
                    linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
                }

                // CASE 3
                if (lastLink != null && lastLink.getBegin() == originLink.getBegin()) {
                    lastLink.setFeatureValue(next, targetLink);
                    lastLink.setStringValue(labelFeature, aValue);
                    chainExist = true;
                    break;
                }
            }

            if (existingChain == null) {

                // CASE 1
                if (!chainExist) {
                    FeatureStructure newChainFs = aJcas.getCas().createFS(type);
                    newChainFs.setFeatureValue(first, originLink);
                    originLink.setFeatureValue(next, targetLink);
                    originLink.setStringValue(labelFeature, aValue);
                    aJcas.addFsToIndexes(newChainFs);
                    aJcas.addFsToIndexes(originLink);
                }
            }
            // CASE 4: only change the relation type, everything same!!!
            else if (modify) {
                existingChain.setStringValue(labelFeature, aValue);
                aJcas.addFsToIndexes(existingChain);
            }
        }
        // clean unconnected coreference chains
        removeInvalidChain(aJcas.getCas());
    }

    private boolean mergeChain(JCas aJcas, AnnotationFS aOrigin, AnnotationFS aTarget, String aValue)
    {
        boolean inThisChain = false;
        boolean inThatChain = false;
        FeatureStructure thatChain = null;
        FeatureStructure thisChain = null;

        Type type = CasUtil.getType(aJcas.getCas(), annotationTypeName);
        Feature first = type.getFeatureByBaseName(chainFirstFeatureName);
        Feature next = aOrigin.getType().getFeatureByBaseName(linkNextFeatureName);
        Feature labelFeature = aOrigin.getType().getFeatureByBaseName(labelFeatureName);

        for (FeatureStructure fs : CasUtil.selectFS(aJcas.getCas(), type)) {
            AnnotationFS linkFs = (AnnotationFS) fs.getFeatureValue(first);
            boolean tempInThisChain = false;
            if (linkFs.getFeatureValue(next) != null) {
                while (linkFs != null) {
                    if (inThisChain) {
                        thatChain = fs;
                        if (BratAjaxCasUtil.isAt((Annotation) linkFs, (Annotation) aOrigin)) {
                            inThatChain = true;
                            linkFs = (AnnotationFS) linkFs.getFeatureValue(next);

                        }
                        else if (BratAjaxCasUtil.isAt((Annotation) linkFs, (Annotation) aTarget)) {
                            inThatChain = true;
                            linkFs = (AnnotationFS) linkFs.getFeatureValue(next);

                        }
                        else {
                            linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
                        }
                    }
                    else {
                        thisChain = fs;
                        if (BratAjaxCasUtil.isAt((Annotation) linkFs, (Annotation) aOrigin)) {
                            tempInThisChain = true;
                            linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
                        }
                        else if (BratAjaxCasUtil.isAt((Annotation) linkFs, (Annotation) aTarget)) {
                            tempInThisChain = true;
                            linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
                        }
                        else {
                            linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
                        }
                    }
                }
            }
            if (tempInThisChain) {
                inThisChain = true;
            }
        }
        if (inThatChain) {

            // |----------|
            // |---------------|

            // |----------------------------|
            // |------------|
            // OR
            // |-------|
            // |-------| ...
            // else{
            Map<Integer, AnnotationFS> beginRelationMaps = new TreeMap<Integer, AnnotationFS>();

            // All links in the first chain
            AnnotationFS linkFs = (AnnotationFS) thisChain.getFeatureValue(first);
            while (linkFs != null) {
                beginRelationMaps.put(linkFs.getBegin(), linkFs);
                linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
            }

            linkFs = (AnnotationFS) thatChain.getFeatureValue(first);
            while (linkFs != null) {
                beginRelationMaps.put(linkFs.getBegin(), linkFs);
                linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
            }

            aOrigin.setStringValue(labelFeature, aValue);
            beginRelationMaps.put(aOrigin.getBegin(), aOrigin);// update the relation

            Iterator<Integer> it = beginRelationMaps.keySet().iterator();

            FeatureStructure newChain = aJcas.getCas().createFS(type);
            newChain.setFeatureValue(first, beginRelationMaps.get(it.next()));
            AnnotationFS newLink = (AnnotationFS) newChain.getFeatureValue(first);

            while (it.hasNext()) {
                AnnotationFS link = beginRelationMaps.get(it.next());
                link.setFeatureValue(next, null);
                newLink.setFeatureValue(next, link);
                newLink.setStringValue(
                        labelFeature,
                        newLink.getStringValue(labelFeature) == null ? aValue : newLink
                                .getStringValue(labelFeature));
                newLink = (AnnotationFS) newLink.getFeatureValue(next);
            }

            aJcas.addFsToIndexes(newChain);

            aJcas.removeFsFromIndexes(thisChain);
            aJcas.removeFsFromIndexes(thatChain);
        }
        return inThatChain;
    }

    /**
     * Delete a chain annotation from CAS
     *
     * @param aJCas
     *            the CAS object
     * @param aId
     *            the low-level address of the span annotation.
     */
    public void deleteLinkFromCas(JCas aJCas, int aRef)
    {
        FeatureStructure fsToRemove = (FeatureStructure) BratAjaxCasUtil.selectAnnotationByAddress(
                aJCas, FeatureStructure.class, aRef);

        aJCas.removeFsFromIndexes(fsToRemove);
    }

    /**
     * Update the Cas before deleting a link. This way, if a link is deleted at the middle, The
     * chain will be splitted into two. If the first link is deleted, the <b>First</b> link will be
     * shifted to the next one.
     */
    public void updateCasBeforeDelete(JCas aJCas, int aRef)
    {
        FeatureStructure fsToRemove = (FeatureStructure) BratAjaxCasUtil.selectAnnotationByAddress(
                aJCas, FeatureStructure.class, aRef);

        Type type = CasUtil.getType(aJCas.getCas(), annotationTypeName);
        Feature first = type.getFeatureByBaseName(chainFirstFeatureName);
        boolean found = false;
        AnnotationFS nextLink = null;
        FeatureStructure previousFS = null;
        for (FeatureStructure fs : CasUtil.selectFS(aJCas.getCas(), type)) {
            if (found) {
                break;
            }

            AnnotationFS linkFs = (AnnotationFS) fs.getFeatureValue(first);
            Feature next = linkFs.getType().getFeatureByBaseName(linkNextFeatureName);

            if (((FeatureStructureImpl) fsToRemove).getAddress() == ((FeatureStructureImpl) linkFs)
                    .getAddress()) {

                // move first of the chain to the next one
                nextLink = (AnnotationFS) linkFs.getFeatureValue(next);
                linkFs.setFeatureValue(next, null);
                break;
            }

            while (linkFs != null) {
                if (((FeatureStructureImpl) fsToRemove).getAddress() == ((FeatureStructureImpl) linkFs)
                        .getAddress()) {

                    nextLink = (AnnotationFS) linkFs.getFeatureValue(next);
                    found = true;
                    linkFs.setFeatureValue(next, null);
                    previousFS.setFeatureValue(next, null);
                    break;
                }
                previousFS = linkFs;
                linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
            }
        }
        if (nextLink != null) {
            FeatureStructure newChainFs = aJCas.getCas().createFS(type);
            newChainFs.setFeatureValue(first, nextLink);
            aJCas.addFsToIndexes(newChainFs);
        }
    }

    /**
     * Remove an arc from a {@link CoreferenceChain}
     */
    public void delete(JCas aJCas, int aRef)
    {
        if (isChain) {
            Type type = CasUtil.getType(aJCas.getCas(), annotationTypeName);
            Feature first = type.getFeatureByBaseName(chainFirstFeatureName);

            FeatureStructure newChain = aJCas.getCas().createFS(type);
            boolean found = false;

            AnnotationFS originCorefType = (AnnotationFS) BratAjaxCasUtil
                    .selectAnnotationByAddress(aJCas, FeatureStructure.class, aRef);
            for (FeatureStructure fs : CasUtil.selectFS(aJCas.getCas(), type)) {
                AnnotationFS linkFs = (AnnotationFS) fs.getFeatureValue(first);
                Feature next = linkFs.getType().getFeatureByBaseName(linkNextFeatureName);
                if (found) {
                    break;
                }
                while (linkFs != null && !found) {
                    if (((FeatureStructureImpl) linkFs).getAddress() == ((FeatureStructureImpl) originCorefType)
                            .getAddress()) {
                        newChain.setFeatureValue(first, linkFs.getFeatureValue(next));
                        linkFs.setFeatureValue(next, null);
                        found = true;
                        break;
                    }
                    linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
                }
            }
            aJCas.addFsToIndexes(newChain);

            removeInvalidChain(aJCas.getCas());
        }
        else {
            ChainAdapter.getCoreferenceChainAdapter().updateCasBeforeDelete(aJCas, aRef);

            FeatureStructure fsToRemove = (FeatureStructure) BratAjaxCasUtil
                    .selectAnnotationByAddress(aJCas, FeatureStructure.class, aRef);

            aJCas.removeFsFromIndexes(fsToRemove);

            ChainAdapter.getCoreferenceChainAdapter().removeInvalidChain(aJCas.getCas());
        }
    }

    /**
     * Remove an invalid chain. A chain is invalid when its next link is null
     *
     * @param aCas
     */
    public void removeInvalidChain(CAS aCas)
    {
        // clean unconnected coreference chains
        List<FeatureStructure> orphanChains = new ArrayList<FeatureStructure>();
        Type type = CasUtil.getType(aCas, annotationTypeName);
        Feature first = type.getFeatureByBaseName(chainFirstFeatureName);
        for (FeatureStructure fs : CasUtil.selectFS(aCas, type)) {
            AnnotationFS linkFs = (AnnotationFS) fs.getFeatureValue(first);
            Feature next = linkFs.getType().getFeatureByBaseName(linkNextFeatureName);

            if (linkFs.getFeatureValue(next) == null) {
                orphanChains.add(fs);
            }
        }
        for (FeatureStructure chain : orphanChains) {
            aCas.removeFsFromIndexes(chain);
        }
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
     * Convenience method to get an adapter for coreference Link.
     *
     * NOTE: This is not meant to stay. It's just a convenience during refactoring!
     */
    public static final ChainAdapter getCoreferenceLinkAdapter()
    {
        ChainAdapter adapter = new ChainAdapter(AnnotationTypeConstant.COREFERENCE_PREFIX,
                CoreferenceLink.class.getName(),
                AnnotationTypeConstant.COREFERENCELINK_FEATURENAME,
                AnnotationTypeConstant.COREFERENCECHAIN_FIRST_FEATURENAME,
                AnnotationTypeConstant.COREFERENCELINK_NEXT_FEATURENAME);
        return adapter;
    }

    /**
     * Convenience method to get an adapter for coreference chain.
     *
     * NOTE: This is not meant to stay. It's just a convenience during refactoring!
     */
    public static final ChainAdapter getCoreferenceChainAdapter()
    {
        ChainAdapter adapter = new ChainAdapter(AnnotationTypeConstant.COREFERENCE_PREFIX,
                CoreferenceChain.class.getName(),
                AnnotationTypeConstant.COREFERENCECHAIN_FEATURENAME,
                AnnotationTypeConstant.COREFERENCECHAIN_FIRST_FEATURENAME,
                AnnotationTypeConstant.COREFERENCELINK_NEXT_FEATURENAME);
        adapter.setChain(true);
        return adapter;
    }

    @Override
    public String getLabelFeatureName()
    {
        return labelFeatureName;
    }
}
