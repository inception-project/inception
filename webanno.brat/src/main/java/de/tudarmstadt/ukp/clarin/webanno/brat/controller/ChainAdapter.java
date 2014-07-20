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
import static org.apache.uima.fit.util.CasUtil.selectFS;

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
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.TagColor;
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
    
    public static final String CHAIN = "Chain";
    public static final String LINK = "Link";

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
     * Add annotations from the CAS, which is controlled by the window size, to the brat response
     * {@link GetDocumentResponse}
     *
     * @param aJcas
     *            The JCAS object containing annotations
     * @param aResponse
     *            A brat response containing annotations in brat protocol
     * @param aBratAnnotatorModel
     *            Data model for brat annotations
     * @param aPreferredColor
     *            the preferred color to render this layer
     */
    @Override
    public void render(JCas aJcas, List<AnnotationFeature> aFeatures,
            GetDocumentResponse aResponse, BratAnnotatorModel aBratAnnotatorModel,
            String aPreferredColor)
    {
        // Get begin and end offsets of window content
        int windowBegin = BratAjaxCasUtil.selectByAddr(aJcas,
                Sentence.class, aBratAnnotatorModel.getSentenceAddress()).getBegin();
        int windowEnd = BratAjaxCasUtil.selectByAddr(aJcas, Sentence.class,
                BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(aJcas,
                        aBratAnnotatorModel.getSentenceAddress(),
                        aBratAnnotatorModel.getWindowSize())).getEnd();

        // Find the features for the arc and span labels
        AnnotationFeature spanLabelFeature = null;
        AnnotationFeature arcLabelFeature = null;
        for (AnnotationFeature f : aFeatures) {
            if (WebAnnoConst.COREFERENCE_TYPE_FEATURE.equals(f.getName())) {
                spanLabelFeature = f;
            }
            if (WebAnnoConst.COREFERENCE_RELATION_FEATURE.equals(f.getName())) {
                arcLabelFeature = f;
            }
        }
        // At this point arc and span feature labels must have been found! If not, the later code
        // will crash.
        
        Type chainType = getAnnotationType(aJcas.getCas());
        Feature chainFirst = chainType.getFeatureByBaseName(chainFirstFeatureName);
        
        int colorIndex = 0;
        // Iterate over the chains
        for (FeatureStructure chainFs : selectFS(aJcas.getCas(), chainType)) {
            AnnotationFS linkFs = (AnnotationFS) chainFs.getFeatureValue(chainFirst);
            AnnotationFS prevLinkFs = null;
            
            // Every chain is supposed to have a different color
            String color = TagColor.PALETTE_NORMAL[colorIndex % TagColor.PALETTE_NORMAL.length];
            // The color index is updated even for chains that have no visible links in the current
            // window because we would like the chain color to be independent of visibility. In
            // particular the color of a chain should not change when switching pages/scrolling.
            colorIndex++;

            // Iterate over the links of the chain
            while (linkFs != null) {
                Feature linkNext = linkFs.getType().getFeatureByBaseName(linkNextFeatureName);
                AnnotationFS nextLinkFs = (AnnotationFS) linkFs.getFeatureValue(linkNext);
                
                // Is link after window? If yes, we can skip the rest of the chain
                if (linkFs.getBegin() >= windowEnd) {
                    break; // Go to next chain
                }

                // Is link before window? We only need links that being within the window and that
                // end within the window
                if (!(linkFs.getBegin() >= windowBegin) && (linkFs.getEnd() <= windowEnd)) {
                    // prevLinkFs remains null until we enter the window
                    linkFs = nextLinkFs;
                    continue; // Go to next link
                }
                
                String bratTypeName = TypeUtil.getBratTypeName(this);

                // Render span
                {
                    String bratLabelText = TypeUtil.getBratLabelText(this, linkFs,
                            asList(spanLabelFeature));
                    Offsets offsets = new Offsets(linkFs.getBegin() - windowBegin, 
                            linkFs.getEnd() - windowBegin);
    
                    aResponse.addEntity(new Entity(((FeatureStructureImpl) linkFs).getAddress(),
                            bratTypeName, offsets, bratLabelText, color));
                }
                
                // Render arc (we do this on prevLinkFs because then we easily know that the current
                // and last link are within the window ;)
                if (prevLinkFs != null) {
                    String bratLabelText = TypeUtil.getBratLabelText(this, prevLinkFs,
                            asList(arcLabelFeature));
                    List<Argument> argumentList = asList(
                            new Argument("Arg1", ((FeatureStructureImpl) prevLinkFs).getAddress()), 
                            new Argument("Arg2", ((FeatureStructureImpl) linkFs).getAddress()));

                    aResponse.addRelation(new Relation(((FeatureStructureImpl) prevLinkFs).getAddress(),
                            bratTypeName, argumentList, bratLabelText, color));
                }

                prevLinkFs = linkFs;
                linkFs = nextLinkFs;
            }
        }        
    }
        
    /**
     * Update the CAS with new/modification of span annotations from brat
     *
     * @param aLabelValue
     *            the value of the annotation for the span
     */
    public void add(String aLabelValue, JCas aJCas, int aBegin, int aEnd, AnnotationFS aOriginFs,
            AnnotationFS aTargetFs, AnnotationFeature aFeature)
        throws MultipleSentenceCoveredException
    {
        boolean isChain = aFeature.getName().equals(WebAnnoConst.COREFERENCE_RELATION_FEATURE);
        if (isChain) {
            updateCoreferenceChainCas(aJCas, aOriginFs, aTargetFs, aLabelValue, aFeature);
        }
        else {
            // FIXME annotationTypeName should not be reset
            annotationTypeName = aFeature.getLayer().getName() + LINK;
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
    private void updateCoreferenceLinkCas(CAS aCas, int aBegin, int aEnd, String aValue,
            AnnotationFeature aFeature)
    {

        boolean duplicate = false;
        Type type = getAnnotationType(aCas);
        Feature feature = type.getFeatureByBaseName(aFeature.getName());
        // FIXME duplicate is always false?!
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
        // FIXME annotationTypeName should not be reset
        annotationTypeName = aFeature.getLayer().getName()+CHAIN;
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

            Type type = getAnnotationType(aJcas.getCas());
            Feature first = type.getFeatureByBaseName(chainFirstFeatureName);
            Feature next = targetLink.getType().getFeatureByBaseName(linkNextFeatureName);
            Feature labelFeature = targetLink.getType().getFeatureByBaseName(aFeature.getName());
            for (FeatureStructure fs : selectFS(aJcas.getCas(), type)) {

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

    private boolean mergeChain(JCas aJcas, AnnotationFS aOrigin, AnnotationFS aTarget,
            String aValue, AnnotationFeature aFeature)
    {
        boolean inThisChain = false;
        boolean inThatChain = false;
        FeatureStructure thatChain = null;
        FeatureStructure thisChain = null;

        Type type = getAnnotationType(aJcas.getCas());
        Feature first = type.getFeatureByBaseName(chainFirstFeatureName);
        Feature next = aOrigin.getType().getFeatureByBaseName(linkNextFeatureName);
        Feature labelFeature = aOrigin.getType().getFeatureByBaseName(aFeature.getName());

        for (FeatureStructure fs : selectFS(aJcas.getCas(), type)) {
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

        Type type = getAnnotationType(aJCas.getCas());
        Feature first = type.getFeatureByBaseName(chainFirstFeatureName);
        boolean found = false;
        AnnotationFS nextLink = null;
        FeatureStructure previousFS = null;
        for (FeatureStructure fs : selectFS(aJCas.getCas(), type)) {
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
        Type chainType = getAnnotationType(aJCas.getCas());
        Feature chainFirst = chainType.getFeatureByBaseName(chainFirstFeatureName);

        // Get the selected link. Since the rendered span and arc both refer to the same CAS
        // address, we do not have to handle deletes of spans or arcs differently here.
        AnnotationFS linkToDelete = (AnnotationFS) BratAjaxCasUtil.selectByAddr(aJCas,
                FeatureStructure.class, aAddress);
        
        // case 1 "removing first link": we keep the existing chain head and just remove the
        // first element
        //
        // case 2 "removing middle link": the new chain consists of the rest, the old chain head
        // remains
        //
        // case 3 "removing the last link": the old chain head remains and the last element of the
        // chain is removed.
        
        // To know which case we have, we first need to find the chain containing the element to
        // be deleted.
        FeatureStructure oldChainFs = null;
        FeatureStructure prevLinkFs = null;
        boolean found = false;
        chainLoop: for (FeatureStructure chainFs : selectFS(aJCas.getCas(), chainType)) {
            AnnotationFS linkFs = (AnnotationFS) chainFs.getFeatureValue(chainFirst);
            
            // Now we seek the link within the current chain
            while (linkFs != null) {
                Feature next = linkFs.getType().getFeatureByBaseName(linkNextFeatureName);
                if (((FeatureStructureImpl) linkFs).getAddress() == ((FeatureStructureImpl) linkToDelete)
                        .getAddress()) {
                    oldChainFs = chainFs;
                    break chainLoop;
                }
                prevLinkFs = linkFs;
                linkFs = (AnnotationFS) linkFs.getFeatureValue(next);
            }
        }
        
        // Did we find the chain?!
        if (oldChainFs == null) {
            throw new IllegalArgumentException("Chain link with address [" + aAddress
                    + "] not found in any chain!");
        }

        FeatureStructure followingLinkToDelete = linkToDelete.getFeatureValue(linkToDelete
                .getType().getFeatureByBaseName(linkNextFeatureName));

        if (prevLinkFs == null) {
            // case 1: first element removed
            oldChainFs.setFeatureValue(chainFirst, followingLinkToDelete);
            aJCas.removeFsFromIndexes(linkToDelete);
            
            // removed last element form chain?
            if (followingLinkToDelete == null) {
                aJCas.removeFsFromIndexes(oldChainFs);
            }
        }
        else if (followingLinkToDelete == null) {
            // case 3: removing the last link (but not leaving the chain empty)
            prevLinkFs.setFeatureValue(
                    prevLinkFs.getType().getFeatureByBaseName(linkNextFeatureName), null);
            
            aJCas.removeFsFromIndexes(linkToDelete);
        }
        else if (prevLinkFs != null && followingLinkToDelete != null) {
            // case 2: removing a middle link
            
            // Set up new chain for rest
            FeatureStructure newChain = aJCas.getCas().createFS(chainType);
            newChain.setFeatureValue(chainFirst, followingLinkToDelete);
            aJCas.addFsToIndexes(newChain);
            
            // Cut off from old chain
            prevLinkFs.setFeatureValue(
                    prevLinkFs.getType().getFeatureByBaseName(linkNextFeatureName), null);
        }
        else {
            throw new IllegalStateException(
                    "Unexpected situation while removing link. Please contact developers.");
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
        Type type = getAnnotationType(aCas);
        Feature first = type.getFeatureByBaseName(chainFirstFeatureName);
        for (FeatureStructure fs : selectFS(aCas, type)) {
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

    @Override
    public String getAttachTypeName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateFeature(JCas aJcas, AnnotationFeature aFeature,int aAddress, String aValue)
    {
        Type type = CasUtil.getType(aJcas.getCas(), aFeature.getLayer().getName()+LINK);
        Feature feature = type.getFeatureByBaseName(aFeature.getName());
        FeatureStructure fs = BratAjaxCasUtil.selectByAddr(aJcas, FeatureStructure.class, aAddress);
        fs.setFeatureValueFromString(feature, aValue);

    }
}
