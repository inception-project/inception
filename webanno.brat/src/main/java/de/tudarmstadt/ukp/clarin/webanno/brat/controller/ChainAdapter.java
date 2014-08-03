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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
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
//    private final Log log = LogFactory.getLog(getClass());
    
    public static final String CHAIN = "Chain";
    public static final String LINK = "Link";

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
    
    private boolean linkedListBehavior;

    private AnnotationLayer layer;
    
    public ChainAdapter(AnnotationLayer aLayer, long aLayerId, String aTypeName,
            String aLabelFeatureName, String aFirstFeatureName, String aNextFeatureName)
    {
        layer = aLayer;
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
     * @param aColoringStrategy
     *            the coloring strategy to render this layer (ignored)
     */
    @Override
    public void render(JCas aJcas, List<AnnotationFeature> aFeatures,
            GetDocumentResponse aResponse, BratAnnotatorModel aBratAnnotatorModel,
            ColoringStrategy aColoringStrategy)
    {
        // Get begin and end offsets of window content
        int windowBegin = BratAjaxCasUtil.selectByAddr(aJcas,
                Sentence.class, aBratAnnotatorModel.getSentenceAddress()).getBegin();
        int windowEnd = BratAjaxCasUtil.selectByAddr(aJcas, Sentence.class,
                BratAjaxCasUtil.getLastSentenceAddressInDisplayWindow(aJcas,
                        aBratAnnotatorModel.getSentenceAddress(),
                        aBratAnnotatorModel.getWindowSize())).getEnd();

        // Find the features for the arc and span labels - it is possible that we do not find a
        // feature for arc/span labels because they may have been disabled.
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
            String color = ColoringStrategy.PALETTE_NORMAL_FILTERED[colorIndex
                    % ColoringStrategy.PALETTE_NORMAL_FILTERED.length];
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
                            (spanLabelFeature != null) ? asList(spanLabelFeature)
                                    : Collections.EMPTY_LIST);
                    Offsets offsets = new Offsets(linkFs.getBegin() - windowBegin, 
                            linkFs.getEnd() - windowBegin);
    
                    aResponse.addEntity(new Entity(BratAjaxCasUtil.getAddr(linkFs), bratTypeName,
                            offsets, bratLabelText, color));
                }
                
                // Render arc (we do this on prevLinkFs because then we easily know that the current
                // and last link are within the window ;)
                if (prevLinkFs != null) {
                    String bratLabelText = null;
                    
                    if (linkedListBehavior && arcLabelFeature != null) {
                        // Render arc label
                        bratLabelText = TypeUtil.getBratLabelText(this, prevLinkFs,
                                asList(arcLabelFeature));
                    }
                    else {
                        // Render only chain type
                        bratLabelText = TypeUtil.getBratLabelText(this, prevLinkFs,
                                Collections.EMPTY_LIST);
                    }
                    
                    List<Argument> argumentList = asList(
                            new Argument("Arg1", BratAjaxCasUtil.getAddr(prevLinkFs)), 
                            new Argument("Arg2", BratAjaxCasUtil.getAddr(linkFs)));

                    aResponse.addRelation(new Relation(BratAjaxCasUtil.getAddr(prevLinkFs),
                            bratTypeName, argumentList, bratLabelText, color));
                }

//                if (BratAjaxCasUtil.isSame(linkFs, nextLinkFs)) {
//                    log.error("Loop in CAS detected, aborting rendering of chains");
//                    break;
//                }

                prevLinkFs = linkFs;
                linkFs = nextLinkFs;
            }
        }        
    }
            
    public int addSpan(String aLabelValue, JCas aJCas, int aBegin, int aEnd,
            AnnotationFeature aFeature)
        throws MultipleSentenceCoveredException
    {
        List<Token> tokens = BratAjaxCasUtil.selectOverlapping(aJCas, Token.class, aBegin, aEnd);
        
        if (!BratAjaxCasUtil.isSameSentence(aJCas, aBegin, aEnd)) {
            throw new MultipleSentenceCoveredException(
                    "Annotation coveres multiple sentences, "
                            + "limit your annotation to single sentence!");
        }
        
        // update the begin and ends (no sub token selection)
        int begin = tokens.get(0).getBegin();
        int end = tokens.get(tokens.size() - 1).getEnd();
        
        // Add the link annotation on the span
        AnnotationFS newLink = newLink(aJCas, begin, end, aFeature, aLabelValue);
        
        // The added link is a new chain on its own - add the chain head FS
        newChain(aJCas, newLink);
        
        return BratAjaxCasUtil.getAddr(newLink);
    }

    public int addArc(JCas aJCas, AnnotationFS aOriginFs,
            AnnotationFS aTargetFs, String aValue, AnnotationFeature aFeature)
    {
        // Determine if the links are adjacent. If so, just update the arc label
        AnnotationFS originNext = getNextLink(aOriginFs);
        AnnotationFS targetNext = getNextLink(aTargetFs);

        // adjacent - origin links to target
        if (BratAjaxCasUtil.isSame(originNext, aTargetFs)) {
            BratAjaxCasUtil.setFeature(aOriginFs, aFeature, aValue);
        }
        // adjacent - target links to origin
        else if (BratAjaxCasUtil.isSame(targetNext, aOriginFs)) {
            if (linkedListBehavior) {
                throw new IllegalStateException("Cannot change direction of a link within a chain");
//              BratAjaxCasUtil.setFeature(aTargetFs, aFeature, aValue);
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

            if (!BratAjaxCasUtil.isSame(originChain, targetChain)) {
                if (linkedListBehavior) {
                    // if the two links are in different chains then split the chains up at the
                    // origin point and target point and create a new link betweek origin and target
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
                    BratAjaxCasUtil.setFeature(aOriginFs, aFeature, aValue);
                }
                else {
                  // collect all the links
                  List<AnnotationFS> links = new ArrayList<AnnotationFS>();
                  links.addAll(collectLinks(originChain));
                  links.addAll(collectLinks(targetChain));
                  
                  // sort them ascending by begin and descending by end (default UIMA order)
                  Collections.sort(links, new AnnotationComparator());
  
                  // thread them
                  AnnotationFS prev = null;
                  for (AnnotationFS link : links) {
                      if (prev != null) {
                          // Set next link
                          setNextLink(prev, link);
//                          // Clear arc label - it makes no sense in this mode
//                          setLabel(prev, aFeature, null);
                      }
                      prev = link;
                  }
  
                  // make sure the last link terminates the chain
                  setNextLink(links.get(links.size()-1), null);
  
                  // the chain head needs to point to the first link
                  setFirstLink(originChain, links.get(0));
                  
                  // we don't need the second chain head anymore
                  aJCas.removeFsFromIndexes(targetChain);
                }
            }
            else {
                // if the two links are in the same chain, we just ignore the action
                if (linkedListBehavior) {
                    throw new IllegalStateException(
                            "Cannot connect two spans that are already part of the same chain");
                }
            }
        }
        
        // We do not actually create a new FS for the arc. Features are set on the originFS.
        return BratAjaxCasUtil.getAddr(aOriginFs);
    }

    @Override
    public void delete(JCas aJCas, int aAddress)
    {
        // BEGIN HACK - ISSUE 933
        if (isArc) {
            deleteArc(aJCas, aAddress);
        }
        else {
            deleteSpan(aJCas, aAddress);
        }
        // END HACK - ISSUE 933
    }
    
    public void deleteArc(JCas aJCas, int aAddress)
    {
        AnnotationFS linkToDelete = BratAjaxCasUtil.selectByAddr(aJCas, AnnotationFS.class,
                aAddress);        
        
        // Create the tail chain
        // We know that there must be a next link, otherwise no arc would have been rendered!
        newChain(aJCas, getNextLink(linkToDelete));
        
        // Disconnect the tail from the head
        setNextLink(linkToDelete, null);
    }
    
    public void deleteSpan(JCas aJCas, int aAddress)
    {
        Type chainType = getAnnotationType(aJCas.getCas());

        AnnotationFS linkToDelete = BratAjaxCasUtil.selectByAddr(aJCas, AnnotationFS.class,
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
                if (BratAjaxCasUtil.isSame(linkFs, linkToDelete)) {
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
        }
        else {
            throw new IllegalStateException(
                    "Unexpected situation while removing link. Please contact developers.");
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
                if (BratAjaxCasUtil.isSame(linkFs, aLink)) {
                    return chainFs;
                }
                linkFs = getNextLink(linkFs);
            }
        }
        
        // This should never happen unless the data in the CAS has been created erratically
        throw new IllegalArgumentException("Link not part of any chain");
    }
    
    private List<AnnotationFS> collectLinks(FeatureStructure aChain) 
    {
        List<AnnotationFS> links = new ArrayList<AnnotationFS>();

        // Now we seek the link within the current chain
        AnnotationFS linkFs = (AnnotationFS) aChain.getFeatureValue(aChain.getType()
                .getFeatureByBaseName(chainFirstFeatureName));
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
        newChain.setFeatureValue(chainType.getFeatureByBaseName(chainFirstFeatureName), aFirstLink);
        aJCas.addFsToIndexes(newChain);
        return newChain;
    }

    /**
     * Create a new link annotation. Already adds the chain to the CAS.
     */
    private AnnotationFS newLink(JCas aJCas, int aBegin, int aEnd, AnnotationFeature aFeature,
            String aLabelValue)
    {
        String baseName = StringUtils.substringBeforeLast(getAnnotationTypeName(), CHAIN) + LINK;
        Type linkType = CasUtil.getType(aJCas.getCas(), baseName);
        AnnotationFS newLink = aJCas.getCas().createAnnotation(linkType, aBegin, aEnd);
        BratAjaxCasUtil.setFeature(newLink, aFeature, aLabelValue);
        aJCas.getCas().addFsToIndexes(newLink);
        return newLink;
    }
    
    /**
     * Set the first link of a chain in the chain head feature structure.
     */
    private void setFirstLink(FeatureStructure aChain, AnnotationFS aLink)
    {
        aChain.setFeatureValue(aChain.getType().getFeatureByBaseName(chainFirstFeatureName), aLink);
    }
    
    /**
     * Get the first link of a chain from the chain head feature structure.
     */
    private AnnotationFS getFirstLink(FeatureStructure aChain)
    {
        return (AnnotationFS) aChain.getFeatureValue(aChain.getType().getFeatureByBaseName(
                chainFirstFeatureName));
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
            if (BratAjaxCasUtil.isSame(curLink, aLink)) {
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
                aLink.getType().getFeatureByBaseName(linkNextFeatureName), aNext);
    }
    
    /**
     * Get the link following the current link.
     */
    private AnnotationFS getNextLink(AnnotationFS aLink)
    {
        return (AnnotationFS) aLink.getFeatureValue(aLink.getType().getFeatureByBaseName(
                linkNextFeatureName));
    }

   
    // BEGIN HACK - ISSUE 933
    private boolean isArc = false;
    
    public void setArc(boolean aIsArc)
    {
        isArc = aIsArc;
    }
    // END HACK - ISSUE 933
    
    /**
     * Controls whether the chain behaves like a linked list or like a set. When operating as a
     * set, chains are automatically threaded and no arrows and labels are displayed on arcs.
     * When operating as a linked list, chains are not threaded and arrows and labels are displayed
     * on arcs.
     */
    public void setLinkedListBehavior(boolean aBehaveLikeSet)
    {
        linkedListBehavior = aBehaveLikeSet;
    }
    
    public boolean isLinkedListBehavior()
    {
        return linkedListBehavior;
    }
    
    @Override
    public AnnotationLayer getLayer()
    {
        return layer;
    }
}
