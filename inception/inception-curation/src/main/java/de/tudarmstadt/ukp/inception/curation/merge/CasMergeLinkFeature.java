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
package de.tudarmstadt.ukp.inception.curation.merge;

import static de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode.ARRAY;
import static de.tudarmstadt.ukp.inception.curation.merge.CasMergeOperationResult.ResultState.UPDATED;
import static de.tudarmstadt.ukp.inception.curation.merge.CasMergeSpan.selectCandidateSpansAt;
import static de.tudarmstadt.ukp.inception.schema.api.feature.MaterializedLink.toMaterializedLink;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static java.util.Comparator.comparing;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.LinkWithRoleModel;
import de.tudarmstadt.ukp.inception.schema.api.feature.MaterializedLink;

class CasMergeLinkFeature
{
    private static final Logger LOG = LoggerFactory.getLogger(CasMerge.class);

    static CasMergeOperationResult mergeSlotFeature(CasMergeContext aContext,
            SourceDocument aDocument, String aDataOwner, AnnotationLayer aAnnotationLayer,
            CAS aTargetCas, AnnotationFS aSourceFs, String aSourceFeature, int aSourceSlotIndex)
        throws AnnotationException
    {
        var adapter = aContext.getAdapter(aAnnotationLayer);
        if (aContext.isSilenceEvents()) {
            adapter.silenceEvents();
        }

        var slotFeature = adapter.getFeature(aSourceFeature).orElseThrow(
                () -> new AnnotationException("Feature [" + aSourceFeature + "] not found"));
        if (slotFeature.getMultiValueMode() != ARRAY) {
            throw new AnnotationException("Feature [" + aSourceFeature + "] is not a slot feature");
        }

        List<LinkWithRoleModel> sourceLinks = adapter.getFeatureValue(slotFeature, aSourceFs);
        var sourceLink = sourceLinks.get(aSourceSlotIndex);

        var targetLinkHost = findLinkHostInTargetCas(aTargetCas, aSourceFs, adapter, slotFeature,
                sourceLink);
        var targetLinkTarget = findLinkTargetInTargetCas(aTargetCas, aSourceFs, adapter,
                slotFeature, sourceLink, aSourceSlotIndex, aContext);

        List<LinkWithRoleModel> targetLinks = adapter.getFeatureValue(slotFeature, targetLinkHost);

        var newLink = new LinkWithRoleModel(sourceLink);
        newLink.targetAddr = getAddr(targetLinkTarget);

        // Override an existing link if no roles are used. If roles are used, then the user may want
        // to link the same target multiple times with different roles - hence we simply add.
        switch (slotFeature.getLinkMode()) {
        case WITH_ROLE:
            var traits = aContext.readLinkTraits(slotFeature);
            if (traits.isEnableRoleLabels()) {
                if (targetLinks.stream().noneMatch(l -> l.targetAddr == newLink.targetAddr
                        && Objects.equals(l.role, newLink.role))) {
                    targetLinks.add(newLink);
                    LOG.trace("     `-> added {}",
                            toMaterializedLink(targetLinkHost, slotFeature, newLink));
                }
                else {
                    throw new AlreadyMergedException(
                            "The slot has already been filled with this annotation in the target document.");
                }
            }
            else {
                var existing = existingLinkWithTarget(newLink, targetLinks);
                if (existing != null && existing.equals(newLink)) {
                    throw new AlreadyMergedException(
                            "The slot has already been filled with this annotation in the target document.");
                }
                if (existing != null) {
                    targetLinks.remove(existing);
                    LOG.trace("     `-> removed {}",
                            toMaterializedLink(targetLinkHost, slotFeature, existing));
                }
                targetLinks.add(newLink);
                LOG.trace("     `-> added {}",
                        toMaterializedLink(targetLinkHost, slotFeature, newLink));
            }
            break;
        default:
            throw new AnnotationException("Feature [" + aSourceFeature + "] is not a slot feature");
        }

        adapter.setFeatureValue(aDocument, aDataOwner, aTargetCas, getAddr(targetLinkHost),
                slotFeature, targetLinks);

        return new CasMergeOperationResult(UPDATED, getAddr(targetLinkHost));
    }

    private static LinkWithRoleModel existingLinkWithTarget(LinkWithRoleModel aLink,
            List<LinkWithRoleModel> aLinks)
    {
        for (var lr : aLinks) {
            if (lr.targetAddr == aLink.targetAddr) {
                return lr;
            }
        }
        return null;
    }

    private static AnnotationFS findLinkTargetInTargetCas(CAS aTargetCas, AnnotationFS aSourceFS,
            TypeAdapter aAdapter, AnnotationFeature aSlotFeature, LinkWithRoleModel aSourceLink,
            int aSourceSlotIndex, CasMergeContext aContext)
        throws UnfulfilledPrerequisitesException
    {
        var sourceCas = aSourceFS.getCAS();
        var sourceLinkTarget = selectAnnotationByAddr(sourceCas, aSourceLink.targetAddr);
        var sourceLinkTargetAdapter = aContext.getAdapter(aContext
                .findLayer(aAdapter.getLayer().getProject(), sourceLinkTarget.getType().getName()));

        var candidateTarget = selectBestCandidateTarget(aTargetCas, aAdapter, aSourceFS, aContext,
                aSourceLink, aSlotFeature, aSourceSlotIndex);

        if (candidateTarget.isEmpty()) {
            throw new UnfulfilledPrerequisitesException("There is no ["
                    + sourceLinkTargetAdapter.getLayer().getUiName() + "] annotation at ["
                    + sourceLinkTarget.getBegin() + "," + sourceLinkTarget.getEnd()
                    + "] which could serve as the link target. Please add one first.");
        }

        return candidateTarget.get();
    }

    private static Optional<Annotation> selectBestCandidateTarget(CAS aTargetCas,
            TypeAdapter aAdapter, AnnotationFS aSourceFS, CasMergeContext aContext,
            LinkWithRoleModel aSourceLink, AnnotationFeature aSlotFeature, int aSourceSlotIndex)
        throws UnfulfilledPrerequisitesException
    {
        var sourceCas = aSourceFS.getCAS();
        var sourceLinkTarget = selectAnnotationByAddr(sourceCas, aSourceLink.targetAddr);
        var sourceLinkTargetAdapter = aContext.getAdapter(aContext
                .findLayer(aAdapter.getLayer().getProject(), sourceLinkTarget.getType().getName()));

        // Get potential candidates
        var candidateTargetLinkTargets = selectCandidateSpansAt(aTargetCas, sourceLinkTargetAdapter,
                sourceLinkTarget).toList();

        // If there are none, well... return nothing
        if (candidateTargetLinkTargets.isEmpty()) {
            return empty();
        }

        // If there is exactly one, return that.
        if (candidateTargetLinkTargets.size() == 1) {
            return Optional.of(candidateTargetLinkTargets.get(0));
        }

        // If there is more than one candidate, we need to find the best fit
        // First we look at the other features of the annotation. If any of these are different, we
        // discard the candidate.
        var filteredTargetCandidates = candidateTargetLinkTargets.stream() //
                .filter(candidate -> sourceLinkTargetAdapter.countNonEqualFeatures(candidate,
                        sourceLinkTarget, (fs, f) -> f.getLinkMode() == LinkMode.NONE) == 0) //
                .toList();

        // If there are none, well... return nothing
        if (filteredTargetCandidates.isEmpty()) {
            return empty();
        }

        // If there is exactly one, return that.
        if (filteredTargetCandidates.size() == 1) {
            return Optional.of(filteredTargetCandidates.get(0));
        }

        // Still more than one, then we need to look at the slots...
        var matSourceLinks = getMaterializedLinks(aAdapter, aSourceFS);

        var sortedTargetCandidates = filteredTargetCandidates.stream() //
                .sorted(comparing(candidateTarget -> countLinkDifference(aAdapter, aSourceFS,
                        aSlotFeature, aSourceLink, matSourceLinks, candidateTarget)))
                .toList();

        return sortedTargetCandidates.stream().findFirst();
    }

    private static Annotation findLinkHostInTargetCas(CAS aTargetCas, AnnotationFS aSourceFs,
            TypeAdapter adapter, AnnotationFeature slotFeature, LinkWithRoleModel sourceLink)
        throws UnfulfilledPrerequisitesException
    {
        var candidateHosts = selectBestCandidateSlotHost(aTargetCas, adapter, aSourceFs,
                slotFeature, sourceLink);

        if (candidateHosts.isEmpty()) {
            throw new UnfulfilledPrerequisitesException(
                    "There is no suitable [" + adapter.getLayer().getUiName() + "] annotation at ["
                            + aSourceFs.getBegin() + "," + aSourceFs.getEnd()
                            + "] into which the link could be merged. Please add one first.");
        }

        return candidateHosts.get();
    }

    static Optional<Annotation> selectBestCandidateSlotHost(CAS aTargetCas, TypeAdapter aAdapter,
            AnnotationFS aSourceFS, AnnotationFeature aSlotFeature, LinkWithRoleModel aSourceLink)
    {
        var targetType = aAdapter.getAnnotationType(aTargetCas);
        if (targetType.isEmpty()) {
            return empty();
        }

        // Get potential candidates
        var allCandidateHosts = aTargetCas.<Annotation> select(targetType.get()) //
                .at(aSourceFS.getBegin(), aSourceFS.getEnd()) //
                .collect(toCollection(ArrayList::new));

        // If there are none, well... return nothing
        if (allCandidateHosts.isEmpty()) {
            return empty();
        }

        // If there is exactly one, return that.
        if (allCandidateHosts.size() == 1) {
            return Optional.of(allCandidateHosts.get(0));
        }

        // If there is more than one candidate, we need to find the best fit
        // First we look at the other features of the annotation. If any of these are different, we
        // discard the candidate.
        var filteredCandidateJpsts = allCandidateHosts.stream() //
                .filter(candidate -> aAdapter.countNonEqualFeatures(candidate, aSourceFS,
                        (fs, f) -> f.getLinkMode() == LinkMode.NONE) == 0) //
                .toList();

        // If there are none, well... return nothing
        if (filteredCandidateJpsts.isEmpty()) {
            return empty();
        }

        // If there is exactly one, return that.
        if (filteredCandidateJpsts.size() == 1) {
            return Optional.of(filteredCandidateJpsts.get(0));
        }

        // Still more than one, then we need to look at the slots...
        var matSourceLinks = getMaterializedLinks(aAdapter, aSourceFS);

        var filterestCandidateHosts2 = filteredCandidateJpsts.stream() //
                .sorted(comparing(candidateTarget -> countLinkDifference(aAdapter, aSourceFS,
                        aSlotFeature, aSourceLink, matSourceLinks, candidateTarget)))
                .toList();

        return filterestCandidateHosts2.stream().findFirst();
    }

    private static int countLinkDifference(TypeAdapter aAdapter, AnnotationFS aSourceFS,
            AnnotationFeature aSlotFeature, LinkWithRoleModel aSourceLink,
            List<MaterializedLink> matSourceLinks, Annotation candidateTarget)
    {
        var matTargetLinks = getMaterializedLinks(aAdapter, candidateTarget);

        matTargetLinks.add(toMaterializedLink(aSourceFS, aSlotFeature, aSourceLink));

        // Let's check which source links have not been merged yet
        var unmatchedSourceLinks = new ArrayList<MaterializedLink>();
        for (var matSourceLink : matSourceLinks) {
            var removed = matTargetLinks.remove(matSourceLink);
            if (!removed) {
                unmatchedSourceLinks.add(matSourceLink);
            }
        }

        return unmatchedSourceLinks.size() + matTargetLinks.size();
    }

    private static ArrayList<MaterializedLink> getMaterializedLinks(TypeAdapter aAdapter,
            AnnotationFS candidateTarget)
    {
        var matTargetLinks = new ArrayList<MaterializedLink>();
        var linkFeatures = aAdapter.listFeatures().stream() //
                .filter(f -> f.getLinkMode() != LinkMode.NONE) //
                .toList();
        for (var linkFeature : linkFeatures) {
            List<LinkWithRoleModel> targetLinks = aAdapter.getFeatureValue(linkFeature,
                    candidateTarget);
            targetLinks.stream() //
                    .map(link -> toMaterializedLink(candidateTarget, linkFeature, link)) //
                    .forEach(matTargetLinks::add);
        }

        return matTargetLinks;
    }
}
