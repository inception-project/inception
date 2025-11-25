/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.annotation.layer.chain;

import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.isSame;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.TypeAdapter_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.event.ChainLinkCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.event.ChainLinkDeletedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.event.ChainSpanCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.event.ChainSpanDeletedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.CreateRelationAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerBehavior;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationComparator;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

/**
 * Manage interactions with annotations on a chain layer.
 */
public class ChainAdapterImpl
    extends TypeAdapter_ImplBase
    implements ChainAdapter
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<SpanLayerBehavior> behaviors;

    public ChainAdapterImpl(LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer,
            Supplier<Collection<AnnotationFeature>> aFeatures, List<SpanLayerBehavior> aBehaviors,
            ConstraintsService aConstraintsService)
    {
        super(aLayerSupportRegistry, aFeatureSupportRegistry, aConstraintsService, aEventPublisher,
                aLayer, aFeatures);

        if (aBehaviors == null) {
            behaviors = emptyList();
        }
        else {
            var temp = new ArrayList<SpanLayerBehavior>(aBehaviors);
            AnnotationAwareOrderComparator.sort(temp);
            behaviors = temp;
        }
    }

    @Override
    public AnnotationFS handle(CreateSpanAnnotationRequest aRequest) throws AnnotationException
    {
        var request = aRequest;

        for (var behavior : behaviors) {
            request = behavior.onCreate(this, request);
        }

        var newSpan = createChainElementAnnotation(request);

        publishEvent(() -> new ChainSpanCreatedEvent(this, aRequest.getDocument(),
                aRequest.getDocumentOwner(), getLayer(), newSpan));

        return newSpan;
    }

    private AnnotationFS createChainElementAnnotation(CreateSpanAnnotationRequest aRequest)
    {
        // Add the link annotation on the span
        var newLink = newLink(aRequest.getCas(), aRequest.getBegin(), aRequest.getEnd());

        // The added link is a new chain on its own - add the chain head FS
        newChain(aRequest.getCas(), newLink);

        return newLink;
    }

    public AnnotationFS addArc(SourceDocument aDocument, String aUsername, CAS aCas,
            AnnotationFS aOriginFs, AnnotationFS aTargetFs)
    {
        return handle(new CreateRelationAnnotationRequest(aDocument, aUsername, aCas, aOriginFs,
                aTargetFs));
    }

    @Override
    public AnnotationFS handle(CreateRelationAnnotationRequest aRequest)
    {
        var cas = aRequest.getCas();
        var originFs = aRequest.getOriginFs();
        var targetFs = aRequest.getTargetFs();

        // Determine if the links are adjacent. If so, just update the arc label
        var originNext = getNextLink(originFs);
        var targetNext = getNextLink(targetFs);

        // adjacent - origin links to target
        if (isSame(originNext, targetFs)) {
            // Nothing to do
        }
        // adjacent - target links to origin
        else if (isSame(targetNext, originFs)) {
            if (isLinkedListBehavior()) {
                throw new IllegalStateException("Cannot change direction of a link within a chain");
            }
            else {
                // in set mode there are no arc labels anyway
            }
        }
        // if origin and target are not adjacent
        else {
            var originChain = getChainForLink(cas, originFs);
            var targetChain = getChainForLink(cas, targetFs);

            var targetPrev = getPrevLink(targetChain, targetFs);

            if (!isSame(originChain, targetChain)) {
                if (isLinkedListBehavior()) {
                    // if the two links are in different chains then split the chains up at the
                    // origin point and target point and create a new link between origin and target
                    // the tail of the origin chain becomes a new chain

                    // if originFs has a next, then split of the origin chain up
                    // the rest becomes its own chain
                    if (originNext != null) {
                        newChain(cas, originNext);
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
                        cas.removeFsFromIndexes(targetChain);
                    }

                    // connect the rest of the target chain to the origin chain
                    setNextLink(originFs, targetFs);
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
                    cas.removeFsFromIndexes(targetChain);
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

        var nextLink = getNextLink(originFs);
        publishEvent(() -> new ChainLinkCreatedEvent(this, aRequest.getDocument(),
                aRequest.getUsername(), getLayer(), originFs, nextLink));

        // We do not actually create a new FS for the arc. Features are set on the originFS.
        return originFs;
    }

    @Override
    public void delete(SourceDocument aDocument, String aUsername, CAS aCas, VID aVid)
    {
        if (aVid.getSubId() == VID.NONE) {
            deleteSpan(aDocument, aUsername, aCas, aVid.getId());
        }
        else {
            deleteLink(aDocument, aUsername, aCas, aVid.getId());
        }
    }

    private void deleteLink(SourceDocument aDocument, String aUsername, CAS aCas, int aAddress)
    {
        AnnotationFS linkToDelete = ICasUtil.selectByAddr(aCas, AnnotationFS.class, aAddress);

        // Create the tail chain
        // We know that there must be a next link, otherwise no arc would have been rendered!
        AnnotationFS nextLink = getNextLink(linkToDelete);
        newChain(aCas, nextLink);

        // Disconnect the tail from the head
        setNextLink(linkToDelete, null);

        publishEvent(() -> new ChainLinkDeletedEvent(this, aDocument, aUsername, getLayer(),
                linkToDelete, nextLink));
    }

    private void deleteSpan(SourceDocument aDocument, String aUsername, CAS aCas, int aAddress)
    {
        Type chainType = CasUtil.getType(aCas, getChainTypeName());

        AnnotationFS linkToDelete = ICasUtil.selectByAddr(aCas, AnnotationFS.class, aAddress);

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
        chainLoop: for (FeatureStructure chainFs : aCas.select(chainType)) {
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
            throw new IllegalArgumentException(
                    "Chain link with address [" + aAddress + "] not found in any chain!");
        }

        AnnotationFS followingLinkToDelete = getNextLink(linkToDelete);

        if (prevLinkFs == null) {
            // case 1: first element removed
            setFirstLink(oldChainFs, followingLinkToDelete);
            aCas.removeFsFromIndexes(linkToDelete);

            // removed last element form chain?
            if (followingLinkToDelete == null) {
                aCas.removeFsFromIndexes(oldChainFs);
            }
        }
        else if (followingLinkToDelete == null) {
            // case 3: removing the last link (but not leaving the chain empty)
            setNextLink(prevLinkFs, null);
            aCas.removeFsFromIndexes(linkToDelete);
        }
        else if (prevLinkFs != null && followingLinkToDelete != null) {
            // case 2: removing a middle link

            // Set up new chain for rest
            newChain(aCas, followingLinkToDelete);

            // Cut off from old chain
            setNextLink(prevLinkFs, null);

            // Delete middle link
            aCas.removeFsFromIndexes(linkToDelete);
        }
        else {
            throw new IllegalStateException(
                    "Unexpected situation while removing link. Please contact developers.");
        }

        publishEvent(() -> new ChainSpanDeletedEvent(this, aDocument, aUsername, getLayer(),
                linkToDelete));
    }

    @Override
    public String getAnnotationTypeName()
    {
        return getLayer().getName() + LINK;
    }

    @Override
    public String getChainTypeName()
    {
        return getLayer().getName() + CHAIN;
    }

    /**
     * Find the chain head for the given link.
     *
     * @param aCas
     *            the CAS.
     * @param aLink
     *            the link to search the chain for.
     * @return the chain.
     */
    @SuppressWarnings("resource")
    private FeatureStructure getChainForLink(CAS aCas, AnnotationFS aLink)
    {
        Type chainType = CasUtil.getType(aCas, getChainTypeName());

        for (FeatureStructure chainFs : aCas.select(chainType)) {
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
        AnnotationFS linkFs = (AnnotationFS) aChain
                .getFeatureValue(aChain.getType().getFeatureByBaseName(getChainFirstFeatureName()));
        while (linkFs != null) {
            links.add(linkFs);

            linkFs = getNextLink(linkFs);
        }

        return links;
    }

    /**
     * Create a new chain head feature structure. Already adds the chain to the CAS.
     */
    private FeatureStructure newChain(CAS aCas, AnnotationFS aFirstLink)
    {
        var chainType = CasUtil.getType(aCas, getChainTypeName());
        var newChain = aCas.createFS(chainType);
        newChain.setFeatureValue(chainType.getFeatureByBaseName(getChainFirstFeatureName()),
                aFirstLink);
        aCas.addFsToIndexes(newChain);
        return newChain;
    }

    /**
     * Create a new link annotation. Already adds the chain to the CAS.
     */
    private AnnotationFS newLink(CAS aCas, int aBegin, int aEnd)
    {
        var linkType = CasUtil.getType(aCas, getAnnotationTypeName());
        var newLink = aCas.createAnnotation(linkType, aBegin, aEnd);
        aCas.addFsToIndexes(newLink);
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
        return (AnnotationFS) aChain
                .getFeatureValue(aChain.getType().getFeatureByBaseName(getChainFirstFeatureName()));
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
        aLink.setFeatureValue(aLink.getType().getFeatureByBaseName(getLinkNextFeatureName()),
                aNext);
    }

    /**
     * Get the link following the current link.
     */
    private AnnotationFS getNextLink(AnnotationFS aLink)
    {
        return (AnnotationFS) aLink
                .getFeatureValue(aLink.getType().getFeatureByBaseName(getLinkNextFeatureName()));
    }

    @Override
    public boolean isLinkedListBehavior()
    {
        return getLayer().isLinkedListBehavior();
    }

    @Override
    public String getLinkNextFeatureName()
    {
        return FEAT_NEXT;
    }

    @Override
    public String getChainFirstFeatureName()
    {
        return FEAT_FIRST;
    }

    @Override
    public void initializeLayerConfiguration(AnnotationSchemaService aSchemaService)
    {
        var relationFeature = new AnnotationFeature();
        relationFeature.setType(CAS.TYPE_NAME_STRING);
        relationFeature.setName(ARC_LABEL_FEATURE);
        relationFeature.setLayer(getLayer());
        relationFeature.setEnabled(true);
        relationFeature.setUiName("Reference Relation");
        relationFeature.setProject(getLayer().getProject());

        aSchemaService.createFeature(relationFeature);

        var typeFeature = new AnnotationFeature();
        typeFeature.setType(CAS.TYPE_NAME_STRING);
        typeFeature.setName(SPAN_LABEL_FEATURE);
        typeFeature.setLayer(getLayer());
        typeFeature.setEnabled(true);
        typeFeature.setUiName("Reference Type");
        typeFeature.setProject(getLayer().getProject());

        aSchemaService.createFeature(typeFeature);
    }

    @Override
    public List<Pair<LogMessage, AnnotationFS>> validate(CAS aCas)
    {
        var messages = new ArrayList<Pair<LogMessage, AnnotationFS>>();
        for (var behavior : behaviors) {
            var startTime = currentTimeMillis();
            messages.addAll(behavior.onValidate(this, aCas));
            LOG.trace("Validation for [{}] on [{}] took {}ms", behavior.getClass().getSimpleName(),
                    getLayer().getUiName(), currentTimeMillis() - startTime);
        }
        return messages;
    }

    @Override
    public Selection select(VID aVid, AnnotationFS aAnno)
    {
        var selection = new Selection();

        if (aVid.getSubId() == 1) {
            selection.selectArc(aVid, aAnno, getNextLink(aAnno));
        }
        else {
            selection.selectSpan(aAnno);
        }

        return selection;
    }

    @Override
    public Selection selectSpan(AnnotationFS aAnno)
    {
        var selection = new Selection();
        selection.selectSpan(aAnno);
        return selection;
    }

    @Override
    public Selection selectLink(AnnotationFS aAnno)
    {
        var selection = new Selection();

        var nextLink = getNextLink(aAnno);
        if (nextLink != null) {
            selection.selectArc(new VID(aAnno, 1, VID.NONE, VID.NONE), aAnno, nextLink);
            return selection;
        }

        var chain = getChainForLink(aAnno.getCAS(), aAnno);
        var prevLink = getPrevLink(chain, aAnno);
        if (prevLink != null) {
            selection.selectArc(new VID(aAnno, 1, VID.NONE, VID.NONE), prevLink, aAnno);
            return selection;
        }

        return selection;
    }

    @Override
    public boolean isSamePosition(FeatureStructure aFS1, FeatureStructure aFS2)
    {
        if (aFS1 == null || aFS2 == null) {
            return false;
        }

        if (!aFS1.getType().getName().equals(getAnnotationTypeName())) {
            throw new IllegalArgumentException("Expected [" + getAnnotationTypeName()
                    + "] but got [" + aFS1.getType().getName() + "]");
        }

        if (!aFS2.getType().getName().equals(getAnnotationTypeName())) {
            throw new IllegalArgumentException("Expected [" + getAnnotationTypeName()
                    + "] but got [" + aFS2.getType().getName() + "]");
        }

        if (aFS1 instanceof AnnotationFS ann1 && aFS2 instanceof AnnotationFS ann2) {
            if (aFS1 == aFS2) {
                return true;
            }

            return colocated(ann1, ann2);
        }

        throw new IllegalArgumentException("Feature structures need to be annotations");
    }
}
