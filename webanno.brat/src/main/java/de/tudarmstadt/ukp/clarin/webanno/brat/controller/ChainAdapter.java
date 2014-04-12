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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
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

    public static final String EXPLETIVE = "expletive";

    /**
     * Prefix of the label value for Brat to make sure that different annotation types can use the
     * same label, e.g. a POS tag "N" and a named entity type "N".
     *
     * This is used to differentiate the different types in the brat annotation/visualization. The
     * prefix will not stored in the CAS(striped away at {@link BratAjaxCasController#getType} )
     */
    private final long layerId;

    /**
     * The UIMA type name.
     */
    private String annotationTypeName;

    /**
     * The feature of an UIMA annotation for the first span in the chain
     */
    private final String chainFirstFeatureName;

    /**
     * The feature of an UIMA annotation for the next span in the chain
     */
    private final String linkNextFeatureName;

    // private boolean singleTokenBehavior = false;

    private boolean isChain = false;

    private boolean deletable;

    public ChainAdapter(long aLayerId, String aTypeName, String aLabelFeatureName,
            String aFirstFeatureName, String aNextFeatureName)
    {
        layerId = aLayerId;
        annotationTypeName = aTypeName;
        chainFirstFeatureName = aFirstFeatureName;
        linkNextFeatureName = aNextFeatureName;
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
    public void render(JCas aJcas, List<AnnotationFeature> aFeatures,
            GetDocumentResponse aResponse, BratAnnotatorModel aBratAnnotatorModel)
    {
        // The first sentence address in the display window!
        Sentence firstSentence = (Sentence) BratAjaxCasUtil.selectByAddr(aJcas,
                FeatureStructure.class, aBratAnnotatorModel.getSentenceAddress());

        // The last sentence address in the display window!
        Sentence lastSentence = (Sentence) BratAjaxCasUtil.selectByAddr(
                aJcas,
                FeatureStructure.class,
                BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(aJcas,
                        aBratAnnotatorModel.getSentenceAddress(),
                        aBratAnnotatorModel.getWindowSize()));
       
        int windowBegin = firstSentence.getBegin();
        int windowEnd = lastSentence.getEnd();

        // Loop based on window size
        // j, controlling variable to display sentences based on window size
        // i, address of each sentences

        for (AnnotationFeature feature : aFeatures) {
            isChain = feature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE);
            int j = 1;
            int i = aBratAnnotatorModel.getSentenceAddress();
            while (j <= aBratAnnotatorModel.getWindowSize()) {
                if (i >= aBratAnnotatorModel.getLastSentenceAddress()) {
                    Sentence sentence = (Sentence) BratAjaxCasUtil.selectByAddr(aJcas,
                            FeatureStructure.class, i);
                    if (isChain) {
                        renderChains(sentence, aResponse, feature, windowBegin, windowEnd);
                    }
                    else {
                        renderLinks(sentence, aResponse, feature, windowBegin);
                    }
                    break;
                }
                else {
                    Sentence sentence = (Sentence) BratAjaxCasUtil.selectByAddr(aJcas,
                            FeatureStructure.class, i);
                    if (isChain) {
                        renderChains(sentence, aResponse, feature, windowBegin, windowEnd);
                    }
                    else {
                        renderLinks(sentence, aResponse, feature, windowBegin);
                    }
                    i = BratAjaxCasUtil.getFollowingSentenceAddress(aJcas, i);
                }
                j++;
            }
        }
    }

    /**
     * a helper method to the
     * {@link #renderAnnotation(JCas, GetDocumentResponse, BratAnnotatorModel)}
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
            AnnotationFeature aFeature, int aFirstSentenceOffset)
    {
        Type type = CasUtil.getType(aSentence.getCAS(), aFeature.getLayer().getName()+"Link");
        Feature labelFeature = type.getFeatureByBaseName(aFeature.getName());
        for (AnnotationFS fs : CasUtil.selectCovered(type, aSentence)) {
            aResponse.addEntity(new Entity(((FeatureStructureImpl) fs).getAddress() ,
                    layerId + "_" + fs.getStringValue(labelFeature),
                    asList(new Offsets(fs.getBegin() - aFirstSentenceOffset, fs.getEnd()
                            - aFirstSentenceOffset))));
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
    private void renderChains(Sentence aSentence, GetDocumentResponse aResponse,
            AnnotationFeature aFeature, int aWindowBegin, int aWindowEnd)
    {
        Type type = CasUtil.getType(aSentence.getCAS(), annotationTypeName);
        Feature first = type.getFeatureByBaseName(chainFirstFeatureName);

        int i = 1;// used for different display colors of chains
        for (FeatureStructure fs : CasUtil.selectFS(aSentence.getCAS(), type)) {
            // The first link in the
            AnnotationFS linkFs = (AnnotationFS) fs.getFeatureValue(first);

            Feature next = linkFs.getType().getFeatureByBaseName(linkNextFeatureName);
            Feature labelFeature = linkFs.getType().getFeatureByBaseName(aFeature.getName());
            while (linkFs != null) {
                // Render only links within the render window
                if (aWindowBegin <= linkFs.getBegin() && aWindowEnd >= linkFs.getEnd()) {
                    if (EXPLETIVE.equals(linkFs.getStringValue(labelFeature))) {
                        aResponse.addRelation(createLink(linkFs, linkFs,aFeature, i));
                        linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
                        continue;
                    }
                    AnnotationFS nextLink = (AnnotationFS) linkFs.getFeatureValue(next);

                    if (nextLink != null) {
                        aResponse.addRelation(createLink(linkFs, nextLink,aFeature, i));
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
    private Relation createLink(AnnotationFS aFrom,  AnnotationFS aTo,AnnotationFeature aFeature,
            int aColorIndex)
    {
        Feature labelFeature = aFrom.getType().getFeatureByBaseName(aFeature.getName());
        List<Argument> argumentList = getArgument(aFrom, aTo);
        return new Relation(((FeatureStructureImpl) aFrom).getAddress(), aColorIndex + "_"
                + layerId + "_" + aFrom.getStringValue(labelFeature), argumentList);
    }

    /**
     * Argument lists for the chain annotation
     *
     * @return
     */
    private static List<Argument> getArgument(FeatureStructure aOriginFs, FeatureStructure aTargetFs)
    {
        return asList(new Argument("Arg1", ((FeatureStructureImpl) aOriginFs).getAddress() + ""),
                new Argument("Arg2", ((FeatureStructureImpl) aTargetFs).getAddress() + ""));
    }

    /**
     * Update the CAS with new/modification of span annotations from brat
     *
     * @param aLabelValue
     *            the value of the annotation for the span
     * @throws MultipleSentenceCoveredException
     * @throws SubTokenSelectedException
     */
    public void add(String aLabelValue, JCas aJCas, int aBegin, int aEnd, AnnotationFS aOriginFs,
            AnnotationFS aTargetFs, AnnotationFeature aFeature)
        throws BratAnnotationException
    {
    	 isChain = aFeature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE);
        if (isChain) {
            updateCoreferenceChainCas(aJCas, aOriginFs, aTargetFs, aLabelValue, aFeature);
        }
        else {
        	annotationTypeName = aFeature.getLayer().getName()+"Link";
            List<Token> tokens = BratAjaxCasUtil
                    .selectOverlapping(aJCas, Token.class, aBegin, aEnd);
            if (!BratAjaxCasUtil.isSameSentence(aJCas, aBegin, aEnd)) {
                throw new MultipleSentenceCoveredException(
                        "Annotation coveres multiple sentences, "
                                + "limit your annotation to single sentence!");
            }
            else {
                // update the begin and ends (no sub token selection
                aBegin = tokens.get(0).getBegin();
                aEnd = tokens.get(tokens.size() - 1).getEnd();

                updateCoreferenceLinkCas(aJCas.getCas(), aBegin, aEnd, aLabelValue, aFeature);
            }
        }
    }

    /**
     * A Helper method to {@link #add(String, BratAnnotatorUIData)}
     */
    private void updateCoreferenceLinkCas(CAS aCas, int aBegin, int aEnd, String aValue, AnnotationFeature aFeature)
    {

        boolean duplicate = false;
        Type type = CasUtil.getType(aCas, annotationTypeName);
        Feature feature = type.getFeatureByBaseName(aFeature.getName());
        if (!duplicate) {
            AnnotationFS newAnnotation = aCas.createAnnotation(type, aBegin, aEnd);
            newAnnotation.setFeatureValueFromString(feature, aValue);
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
    private void updateCoreferenceChainCas(JCas aJcas, AnnotationFS aOriginFs,
            AnnotationFS aTargetFs, String aValue, AnnotationFeature aFeature)
    {
        boolean modify = false;
        annotationTypeName = aFeature.getLayer().getName()+"Chain";
        // Variables used for swapping
        AnnotationFS originLink = aOriginFs;
        AnnotationFS targetLink = aTargetFs;

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
        boolean merge = mergeChain(aJcas, originLink, targetLink, aValue, aFeature);

        if (!merge) {

            Type type = CasUtil.getType(aJcas.getCas(), annotationTypeName);
            Feature first = type.getFeatureByBaseName(chainFirstFeatureName);
            Feature next = targetLink.getType().getFeatureByBaseName(linkNextFeatureName);
            Feature labelFeature = targetLink.getType().getFeatureByBaseName(aFeature.getName());
            for (FeatureStructure fs : CasUtil.selectFS(aJcas.getCas(), type)) {

                AnnotationFS linkFs = (AnnotationFS) fs.getFeatureValue(first);

                // CASE 2
                if (fs.getFeatureValue(first) != null
                        && !found
                        && ((FeatureStructureImpl) fs.getFeatureValue(first)).getAddress() == ((FeatureStructureImpl) targetLink)
                                .getAddress()) {
                    fs.setFeatureValue(first, originLink);

                    originLink.setFeatureValue(next, targetLink);
                    originLink.setFeatureValueFromString(labelFeature, aValue);
                    found = true;
                    break;
                }

                AnnotationFS lastLink = linkFs;

                while (linkFs != null && !found) {
                    // a-> c, b->c = a->b->c
                    if (linkFs.getFeatureValue(next) != null
                            && BratAjaxCasUtil.isSame((Annotation) linkFs.getFeatureValue(next),
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
                        originLink.setFeatureValueFromString(labelFeature, aValue);
                        chainExist = true;
                        found = true;
                        break;
                    }
                    // CASE 4a/b
                    if (BratAjaxCasUtil.isSame((Annotation) linkFs, (Annotation) originLink)
                            && linkFs.getFeatureValue(next) != null
                            && !BratAjaxCasUtil.isSame((Annotation) linkFs.getFeatureValue(next),
                                    (Annotation) targetLink)
                            && targetLink.getBegin() < ((AnnotationFS) linkFs.getFeatureValue(next))
                                    .getBegin()) {
                        AnnotationFS tmpLink = (AnnotationFS) linkFs.getFeatureValue(next);
                        String tmpRel = linkFs.getStringValue(labelFeature);
                        linkFs.setFeatureValue(next, targetLink);
                        linkFs.setFeatureValueFromString(labelFeature, aValue);
                        targetLink.setFeatureValue(next, tmpLink);
                        targetLink.setFeatureValueFromString(labelFeature, tmpRel);
                        chainExist = true;
                        found = true;
                        break;
                    }
                    else if (BratAjaxCasUtil.isSame((Annotation) linkFs, (Annotation) originLink)
                            && linkFs.getFeatureValue(next) != null
                            && !BratAjaxCasUtil.isSame((Annotation) linkFs.getFeatureValue(next),
                                    (Annotation) targetLink)) {
                        linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
                        originLink = linkFs;
                        continue;
                    }
                    else if (BratAjaxCasUtil.isSame((Annotation) linkFs, (Annotation) originLink)
                            && linkFs.getFeatureValue(next) == null) {
                        linkFs.setFeatureValue(next, targetLink);
                        linkFs.setFeatureValueFromString(labelFeature, aValue);
                        chainExist = true;
                        found = true;
                        break;
                    }
                    if (BratAjaxCasUtil.isSame((Annotation) linkFs, (Annotation) originLink)
                            && linkFs.getFeatureValue(next) != null
                            && BratAjaxCasUtil.isSame((Annotation) linkFs.getFeatureValue(next),
                                    (Annotation) targetLink)) {
                        modify = !linkFs.getStringValue(labelFeature).equals(aValue);
                        existingChain = linkFs;
                        chainExist = true;
                        break;
                    }

                    lastLink = linkFs;
                    linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
                }

                lastLink = linkFs;
                // CASE 3
                if (lastLink != null && lastLink.getBegin() == originLink.getBegin()) {
                    lastLink.setFeatureValue(next, targetLink);
                    lastLink.setFeatureValueFromString(labelFeature, aValue);
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
                    originLink.setFeatureValueFromString(labelFeature, aValue);
                    aJcas.addFsToIndexes(newChainFs);
                    aJcas.addFsToIndexes(originLink);
                }
            }
            // CASE 4: only change the relation type, everything same!!!
            else if (modify) {
                existingChain.setFeatureValueFromString(labelFeature, aValue);
                aJcas.addFsToIndexes(existingChain);
            }
        }
        // clean unconnected coreference chains
        removeInvalidChain(aJcas.getCas());
    }

    private boolean mergeChain(JCas aJcas, AnnotationFS aOrigin, AnnotationFS aTarget, String aValue, AnnotationFeature aFeature)
    {
        boolean inThisChain = false;
        boolean inThatChain = false;
        FeatureStructure thatChain = null;
        FeatureStructure thisChain = null;

        Type type = CasUtil.getType(aJcas.getCas(), annotationTypeName);
        Feature first = type.getFeatureByBaseName(chainFirstFeatureName);
        Feature next = aOrigin.getType().getFeatureByBaseName(linkNextFeatureName);
        Feature labelFeature = aOrigin.getType().getFeatureByBaseName(aFeature.getName());

        for (FeatureStructure fs : CasUtil.selectFS(aJcas.getCas(), type)) {
            AnnotationFS linkFs = (AnnotationFS) fs.getFeatureValue(first);
            boolean tempInThisChain = false;
            if (linkFs.getFeatureValue(next) != null) {
                while (linkFs != null) {
                    if (inThisChain) {
                        thatChain = fs;
                        if (BratAjaxCasUtil.isSame((Annotation) linkFs, (Annotation) aOrigin)) {
                            inThatChain = true;
                            linkFs = (AnnotationFS) linkFs.getFeatureValue(next);

                        }
                        else if (BratAjaxCasUtil.isSame((Annotation) linkFs, (Annotation) aTarget)) {
                            inThatChain = true;
                            linkFs = (AnnotationFS) linkFs.getFeatureValue(next);

                        }
                        else {
                            linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
                        }
                    }
                    else {
                        thisChain = fs;
                        if (BratAjaxCasUtil.isSame((Annotation) linkFs, (Annotation) aOrigin)) {
                            tempInThisChain = true;
                            linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
                        }
                        else if (BratAjaxCasUtil.isSame((Annotation) linkFs, (Annotation) aTarget)) {
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

            aOrigin.setFeatureValueFromString(labelFeature, aValue);
            beginRelationMaps.put(aOrigin.getBegin(), aOrigin);// update the relation

            Iterator<Integer> it = beginRelationMaps.keySet().iterator();

            FeatureStructure newChain = aJcas.getCas().createFS(type);
            newChain.setFeatureValue(first, beginRelationMaps.get(it.next()));
            AnnotationFS newLink = (AnnotationFS) newChain.getFeatureValue(first);

            while (it.hasNext()) {
                AnnotationFS link = beginRelationMaps.get(it.next());
                link.setFeatureValue(next, null);
                newLink.setFeatureValue(next, link);
                newLink.setFeatureValueFromString(
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
     * Update the Cas before deleting a link. This way, if a link is deleted at the middle, The
     * chain will be splitted into two. If the first link is deleted, the <b>First</b> link will be
     * shifted to the next one.
     */
    public void updateCasBeforeDelete(JCas aJCas, int aRef)
    {
        FeatureStructure fsToRemove = BratAjaxCasUtil.selectByAddr(aJCas, FeatureStructure.class,
                aRef);

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

    @Override
    public void delete(JCas aJCas, int aAddress)
    {
        if (isChain) {
            Type type = CasUtil.getType(aJCas.getCas(), annotationTypeName);
            Feature first = type.getFeatureByBaseName(chainFirstFeatureName);

            FeatureStructure newChain = aJCas.getCas().createFS(type);
            boolean found = false;

            AnnotationFS originCorefType = (AnnotationFS) BratAjaxCasUtil.selectByAddr(aJCas,
                    FeatureStructure.class, aAddress);
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

            /* updateCasBeforeDelete(aJCas, aAddress); */

            FeatureStructure fsToRemove = BratAjaxCasUtil.selectByAddr(aJCas,
                    FeatureStructure.class, aAddress);

            aJCas.removeFsFromIndexes(fsToRemove);

            /* removeInvalidChain(aJCas.getCas()); */
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

    @Override
    public long getTypeId()
    {
        return layerId;
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

    public void setDeletable(boolean deletable)
    {
        this.deletable = deletable;
    }

    @Override
    public boolean isDeletable()
    {
        return deletable;
    }

    @Override
    public String getAttachFeatureName()
    {
        return null;
    }

    @Override
    public void deleteBySpan(JCas aJCas, AnnotationFS fs, int aBegin, int aEnd)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public List<String> getAnnotation(JCas aJcas, AnnotationFeature aFeature, int begin, int end)
    {
        return new ArrayList<String>();
    }

    @Override
    public void automate(JCas aJcas, AnnotationFeature aFeature, List<String> labelValues)
        throws BratAnnotationException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(JCas aJCas, AnnotationFeature aFeature, int aBegin, int aEnd, String aValue)
    {
        // TODO Auto-generated method stub

    }

}
