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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFERENCE_TYPE_FEATURE;
import static org.apache.uima.fit.util.CasUtil.selectFS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * A class that is used to create Brat chain to CAS and vice-versa
 */
public class ChainAdapter
    extends TypeAdapter_ImplBase
{
    public static final String CHAIN = "Chain";
    public static final String LINK = "Link";
    public static final String FEAT_FIRST = "first";
    public static final String FEAT_NEXT = "next";

    /**
     * The UIMA type name.
     */
    private final String annotationTypeName;

    public ChainAdapter(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer, String aTypeName,
            Collection<AnnotationFeature> aFeatures)
    {
        super(aFeatureSupportRegistry, aEventPublisher, aLayer, aFeatures);

        annotationTypeName = aTypeName;
    }

    public AnnotationFS addSpan(SourceDocument aDocument, String aUsername, JCas aJCas, int aBegin,
            int aEnd)
        throws AnnotationException
    {
        return handle(new CreateSpanAnnotationRequest(aDocument, aUsername, aJCas, aBegin, aEnd));
    }

    public AnnotationFS handle(CreateSpanAnnotationRequest aRequest)
        throws AnnotationException
    {
        CreateSpanAnnotationRequest request = aRequest;
        
        request = new SpanCrossSentenceBehavior().apply(this, request);
        
        request = new SpanAnchoringModeBehavior().apply(this, request);
        
        request = new ChainStackingBehavior().apply(this, request);
        
        return createChainElementAnnotation(request);
    }
    
    private AnnotationFS createChainElementAnnotation(CreateSpanAnnotationRequest aRequest)
    {
        // Add the link annotation on the span
        AnnotationFS newLink = newLink(aRequest.getJcas(), aRequest.getBegin(), aRequest.getEnd());

        // The added link is a new chain on its own - add the chain head FS
        newChain(aRequest.getJcas(), newLink);

        return newLink;
    }
    
    public int addArc(SourceDocument aDocument, String aUsername, JCas aJCas,
            AnnotationFS aOriginFs, AnnotationFS aTargetFs)
    {
        // Determine if the links are adjacent. If so, just update the arc label
        AnnotationFS originNext = getNextLink(aOriginFs);
        AnnotationFS targetNext = getNextLink(aTargetFs);

        // adjacent - origin links to target
        if (WebAnnoCasUtil.isSame(originNext, aTargetFs)) {
        }
        // adjacent - target links to origin
        else if (WebAnnoCasUtil.isSame(targetNext, aOriginFs)) {
            if (isLinkedListBehavior()) {
                throw new IllegalStateException("Cannot change direction of a link within a chain");
            }
            else {
                // in set mode there are no arc labels anyway
            }
        }
        // if origin and target are not adjacent
        else {
            FeatureStructure originChain = getChainForLink(aJCas, aOriginFs);
            FeatureStructure targetChain = getChainForLink(aJCas, aTargetFs);

            AnnotationFS targetPrev = getPrevLink(targetChain, aTargetFs);

            if (!WebAnnoCasUtil.isSame(originChain, targetChain)) {
                if (isLinkedListBehavior()) {
                    // if the two links are in different chains then split the chains up at the
                    // origin point and target point and create a new link between origin and target
                    // the tail of the origin chain becomes a new chain

                    // if originFs has a next, then split of the origin chain up
                    // the rest becomes its own chain
                    if (originNext != null) {
                        newChain(aJCas, originNext);
                        // we set originNext below
                        // we set the arc label below
                    }

                    // if targetFs has a prev, then split it off
                    if (targetPrev != null) {
                        setNextLink(targetPrev, null);
                    }
                    // if it has no prev then we fully append the target chain to the origin chain
                    // and we can remove the target chain head
                    else {
                        aJCas.removeFsFromIndexes(targetChain);
                    }

                    // connect the rest of the target chain to the origin chain
                    setNextLink(aOriginFs, aTargetFs);
                }
                else {
                    // collect all the links
                    List<AnnotationFS> links = new ArrayList<>();
                    links.addAll(collectLinks(originChain));
                    links.addAll(collectLinks(targetChain));

                    // sort them ascending by begin and descending by end (default UIMA order)
                    links.sort(new AnnotationComparator());

                    // thread them
                    AnnotationFS prev = null;
                    for (AnnotationFS link : links) {
                        if (prev != null) {
                            // Set next link
                            setNextLink(prev, link);
                            // // Clear arc label - it makes no sense in this mode
                            // setLabel(prev, aFeature, null);
                        }
                        prev = link;
                    }

                    // make sure the last link terminates the chain
                    setNextLink(links.get(links.size() - 1), null);

                    // the chain head needs to point to the first link
                    setFirstLink(originChain, links.get(0));

                    // we don't need the second chain head anymore
                    aJCas.removeFsFromIndexes(targetChain);
                }
            }
            else {
                // if the two links are in the same chain, we just ignore the action
                if (isLinkedListBehavior()) {
                    throw new IllegalStateException(
                            "Cannot connect two spans that are already part of the same chain");
                }
            }
        }

        // We do not actually create a new FS for the arc. Features are set on the originFS.
        return WebAnnoCasUtil.getAddr(aOriginFs);
    }

    @Override
    public void delete(SourceDocument aDocument, String aUsername, JCas aJCas, VID aVid)
    {
        if (aVid.getSubId() == VID.NONE) {
            deleteSpan(aJCas, aVid.getId());
        }
        else {
            deleteArc(aJCas, aVid.getId());
        }
    }

    private void deleteArc(JCas aJCas, int aAddress)
    {
        AnnotationFS linkToDelete = WebAnnoCasUtil.selectByAddr(aJCas, AnnotationFS.class,
                aAddress);

        // Create the tail chain
        // We know that there must be a next link, otherwise no arc would have been rendered!
        newChain(aJCas, getNextLink(linkToDelete));

        // Disconnect the tail from the head
        setNextLink(linkToDelete, null);
    }

    private void deleteSpan(JCas aJCas, int aAddress)
    {
        Type chainType = getAnnotationType(aJCas.getCas());

        AnnotationFS linkToDelete = WebAnnoCasUtil.selectByAddr(aJCas, AnnotationFS.class,
                aAddress);

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
        AnnotationFS prevLinkFs = null;
        chainLoop: for (FeatureStructure chainFs : selectFS(aJCas.getCas(), chainType)) {
            AnnotationFS linkFs = getFirstLink(chainFs);
            prevLinkFs = null; // Reset when entering new chain!

            // Now we seek the link within the current chain
            while (linkFs != null) {
                if (WebAnnoCasUtil.isSame(linkFs, linkToDelete)) {
                    oldChainFs = chainFs;
                    break chainLoop;
                }
                prevLinkFs = linkFs;
                linkFs = getNextLink(linkFs);
            }
        }

        // Did we find the chain?!
        if (oldChainFs == null) {
            throw new IllegalArgumentException("Chain link with address [" + aAddress
                    + "] not found in any chain!");
        }

        AnnotationFS followingLinkToDelete = getNextLink(linkToDelete);

        if (prevLinkFs == null) {
            // case 1: first element removed
            setFirstLink(oldChainFs, followingLinkToDelete);
            aJCas.removeFsFromIndexes(linkToDelete);

            // removed last element form chain?
            if (followingLinkToDelete == null) {
                aJCas.removeFsFromIndexes(oldChainFs);
            }
        }
        else if (followingLinkToDelete == null) {
            // case 3: removing the last link (but not leaving the chain empty)
            setNextLink(prevLinkFs, null);
            aJCas.removeFsFromIndexes(linkToDelete);
        }
        else if (prevLinkFs != null && followingLinkToDelete != null) {
            // case 2: removing a middle link

            // Set up new chain for rest
            newChain(aJCas, followingLinkToDelete);

            // Cut off from old chain
            setNextLink(prevLinkFs, null);
            
            // Delete middle link
            aJCas.removeFsFromIndexes(linkToDelete);
        }
        else {
            throw new IllegalStateException(
                    "Unexpected situation while removing link. Please contact developers.");
        }
    }

    @Override
    public String getAnnotationTypeName()
    {
        return annotationTypeName;
    }

    /**
     * Find the chain head for the given link.
     *
     * @param aJCas the CAS.
     * @param aLink the link to search the chain for.
     * @return the chain.
     */
    private FeatureStructure getChainForLink(JCas aJCas, AnnotationFS aLink)
    {
        Type chainType = getAnnotationType(aJCas.getCas());

        for (FeatureStructure chainFs : selectFS(aJCas.getCas(), chainType)) {
            AnnotationFS linkFs = getFirstLink(chainFs);

            // Now we seek the link within the current chain
            while (linkFs != null) {
                if (WebAnnoCasUtil.isSame(linkFs, aLink)) {
                    return chainFs;
                }
                linkFs = getNextLink(linkFs);
            }
        }

        // This should never happen unless the data in the CAS has been created wrongly
        throw new IllegalArgumentException("Link not part of any chain");
    }

    private List<AnnotationFS> collectLinks(FeatureStructure aChain)
    {
        List<AnnotationFS> links = new ArrayList<>();

        // Now we seek the link within the current chain
        AnnotationFS linkFs = (AnnotationFS) aChain.getFeatureValue(aChain.getType()
                .getFeatureByBaseName(getChainFirstFeatureName()));
        while (linkFs != null) {
            links.add(linkFs);

            linkFs = getNextLink(linkFs);
        }

        return links;
    }

    /**
     * Sort ascending by begin and descending by end.
     */
    private static class AnnotationComparator
        implements Comparator<AnnotationFS>
    {
        @Override
        public int compare(AnnotationFS arg0, AnnotationFS arg1)
        {
            int beginDiff = arg0.getBegin() - arg1.getBegin();
            if (beginDiff == 0) {
                return arg1.getEnd() - arg0.getEnd();
            }
            else {
                return beginDiff;
            }
        }
    }

    /**
     * Create a new chain head feature structure. Already adds the chain to the CAS.
     */
    private FeatureStructure newChain(JCas aJCas, AnnotationFS aFirstLink)
    {
        Type chainType = getAnnotationType(aJCas.getCas());
        FeatureStructure newChain = aJCas.getCas().createFS(chainType);
        newChain.setFeatureValue(chainType.getFeatureByBaseName(getChainFirstFeatureName()),
                aFirstLink);
        aJCas.addFsToIndexes(newChain);
        return newChain;
    }

    /**
     * Create a new link annotation. Already adds the chain to the CAS.
     */
    private AnnotationFS newLink(JCas aJCas, int aBegin, int aEnd)
    {
        String baseName = StringUtils.substringBeforeLast(getAnnotationTypeName(), CHAIN) + LINK;
        Type linkType = CasUtil.getType(aJCas.getCas(), baseName);
        AnnotationFS newLink = aJCas.getCas().createAnnotation(linkType, aBegin, aEnd);
        aJCas.getCas().addFsToIndexes(newLink);
        return newLink;
    }

    /**
     * Set the first link of a chain in the chain head feature structure.
     */
    private void setFirstLink(FeatureStructure aChain, AnnotationFS aLink)
    {
        aChain.setFeatureValue(aChain.getType().getFeatureByBaseName(getChainFirstFeatureName()),
                aLink);
    }

    /**
     * Get the first link of a chain from the chain head feature structure.
     */
    private AnnotationFS getFirstLink(FeatureStructure aChain)
    {
        return (AnnotationFS) aChain.getFeatureValue(aChain.getType().getFeatureByBaseName(
                getChainFirstFeatureName()));
    }

    /**
     * Get the chain link before the given link within the given chain. The given link must be part
     * of the given chain.
     *
     * @param aChain
     *            a chain head feature structure.
     * @param aLink
     *            a link.
     * @return the link before the given link or null if the given link is the first link of the
     *         chain.
     */
    private AnnotationFS getPrevLink(FeatureStructure aChain, AnnotationFS aLink)
    {
        AnnotationFS prevLink = null;
        AnnotationFS curLink = getFirstLink(aChain);
        while (curLink != null) {
            if (WebAnnoCasUtil.isSame(curLink, aLink)) {
                break;
            }
            prevLink = curLink;
            curLink = getNextLink(curLink);
        }
        return prevLink;
    }

    /**
     * Set the link following the current link.
     */
    private void setNextLink(AnnotationFS aLink, AnnotationFS aNext)
    {
        aLink.setFeatureValue(
                aLink.getType().getFeatureByBaseName(getLinkNextFeatureName()), aNext);
    }

    /**
     * Get the link following the current link.
     */
    private AnnotationFS getNextLink(AnnotationFS aLink)
    {
        return (AnnotationFS) aLink.getFeatureValue(aLink.getType().getFeatureByBaseName(
                getLinkNextFeatureName()));
    }

    public boolean isLinkedListBehavior()
    {
        return getLayer().isLinkedListBehavior();
    }

    public String getLinkNextFeatureName()
    {
        return FEAT_NEXT;
    }
    
    public String getChainFirstFeatureName()
    {
        return FEAT_FIRST;
    }
    
    @Override
    public void initialize(AnnotationSchemaService aSchemaService)
    {
        AnnotationFeature relationFeature = new AnnotationFeature();
        relationFeature.setType(CAS.TYPE_NAME_STRING);
        relationFeature.setName(COREFERENCE_RELATION_FEATURE);
        relationFeature.setLayer(getLayer());
        relationFeature.setEnabled(true);
        relationFeature.setUiName("Reference Relation");
        relationFeature.setProject(getLayer().getProject());

        aSchemaService.createFeature(relationFeature);

        AnnotationFeature typeFeature = new AnnotationFeature();
        typeFeature.setType(CAS.TYPE_NAME_STRING);
        typeFeature.setName(COREFERENCE_TYPE_FEATURE);
        typeFeature.setLayer(getLayer());
        typeFeature.setEnabled(true);
        typeFeature.setUiName("Reference Type");
        typeFeature.setProject(getLayer().getProject());

        aSchemaService.createFeature(typeFeature);
    }
}
