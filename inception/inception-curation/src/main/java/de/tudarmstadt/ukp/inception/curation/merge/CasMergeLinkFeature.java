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
import static java.util.stream.Collectors.toCollection;

import java.lang.invoke.MethodHandles;
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

public class CasMergeLinkFeature
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static CasMergeOperationResult mergeSlotFeature(CasMergeContext aContext,
            SourceDocument aDocument, String aUsername, AnnotationLayer aAnnotationLayer,
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

        var candidateHosts = selectBestCandidateSlotHost(aTargetCas, adapter, aSourceFs,
                slotFeature, sourceLink);
        if (candidateHosts.isEmpty()) {
            throw new UnfulfilledPrerequisitesException(
                    "There is no suitable [" + adapter.getLayer().getUiName() + "] annotation at ["
                            + aSourceFs.getBegin() + "," + aSourceFs.getEnd()
                            + "] into which the link could be merged. Please add one first.");
        }
        var targetFS = candidateHosts.get();
        List<LinkWithRoleModel> targetLinks = adapter.getFeatureValue(slotFeature, targetFS);

        var targetLinkTarget = findLinkTargetInTargetCas(aContext, adapter, aSourceFs.getCAS(),
                sourceLink, slotFeature, aSourceSlotIndex, aTargetCas);
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
                targetLinks.remove(existing);
                targetLinks.add(newLink);
            }
            break;
        default:
            throw new AnnotationException("Feature [" + aSourceFeature + "] is not a slot feature");
        }

        adapter.setFeatureValue(aDocument, aUsername, aTargetCas, getAddr(targetFS), slotFeature,
                targetLinks);

        return new CasMergeOperationResult(UPDATED, getAddr(targetFS));
    }

    private static AnnotationFS findLinkTargetInTargetCas(CasMergeContext aContext,
            TypeAdapter aAdapter, CAS aSourceCas, LinkWithRoleModel aSourceLink,
            AnnotationFeature aSlotFeature, int aSourceSlotIndex, CAS aTargetCas)
        throws UnfulfilledPrerequisitesException
    {
        var sourceLinkTarget = selectAnnotationByAddr(aSourceCas, aSourceLink.targetAddr);
        var sourceLinkTargetAdapter = aContext.getAdapter(aContext
                .findLayer(aAdapter.getLayer().getProject(), sourceLinkTarget.getType().getName()));

        var candidateTargetLinkTargets = selectCandidateSpansAt(aTargetCas, sourceLinkTargetAdapter,
                sourceLinkTarget).limit(2).toList();

        if (candidateTargetLinkTargets.size() > 1) {
            throw new UnfulfilledPrerequisitesException(
                    "There are multiple possible targets. Cannot merge this link.");
        }

        // So it must be empty
        if (candidateTargetLinkTargets.isEmpty()) {
            throw new UnfulfilledPrerequisitesException(
                    "There is no suitable target for this link. Merge or create the target first.");
        }

        return candidateTargetLinkTargets.get(0);
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

    private static Optional<Annotation> selectBestCandidateSlotHost(CAS aTargetCas,
            TypeAdapter aAdapter, AnnotationFS aSourceFS, AnnotationFeature aSlotFeature,
            LinkWithRoleModel aSourceLink)
    {
        var targetType = aAdapter.getAnnotationType(aTargetCas);
        if (targetType.isEmpty()) {
            return Optional.empty();
        }

        // Get potential candidates
        var allCandidateTargets = aTargetCas.<Annotation> select(targetType.get()) //
                .at(aSourceFS.getBegin(), aSourceFS.getEnd()) //
                .collect(toCollection(ArrayList::new));

        // If there are none, well... return nothing
        if (allCandidateTargets.isEmpty()) {
            return Optional.empty();
        }

        // If there is exactly one, return that.
        if (allCandidateTargets.size() == 1) {
            return Optional.of(allCandidateTargets.get(0));
        }

        // If there is more than one candidate, we need to find the best fit
        // First we look at the other features of the annotation. If any of these are different, we
        // discard the candiate.
        var filteredCandidateTargets = allCandidateTargets.stream() //
                .filter(candidate -> aAdapter.countNonEqualFeatures(candidate, aSourceFS,
                        (fs, f) -> f.getLinkMode() == LinkMode.NONE) == 0) //
                .toList();

        // If there are none, well... return nothing
        if (filteredCandidateTargets.isEmpty()) {
            return Optional.empty();
        }

        // If there is exactly one, return that.
        if (filteredCandidateTargets.size() == 1) {
            return Optional.of(allCandidateTargets.get(0));
        }

        // Still more than one, then we need to look at the slots...
        List<LinkWithRoleModel> sourceLinks = aAdapter.getFeatureValue(aSlotFeature, aSourceFS);
        var matSourceLinks = sourceLinks.stream() //
                .map(link -> toMaterializedLink(aSourceFS, aSlotFeature, link)) //
                .toList();

        var filterestCandidateTargets2 = filteredCandidateTargets.stream() //
                .sorted(comparing(candidateTarget -> countLinkDifference(aAdapter, aSourceFS,
                        aSlotFeature, aSourceLink, matSourceLinks, candidateTarget)))
                .toList();

        return filterestCandidateTargets2.stream().findFirst();
    }

    private static int countLinkDifference(TypeAdapter aAdapter, AnnotationFS aSourceFS,
            AnnotationFeature aSlotFeature, LinkWithRoleModel aSourceLink,
            List<MaterializedLink> matSourceLinks, Annotation candidateTarget)
    {
        List<LinkWithRoleModel> targetLinks = aAdapter.getFeatureValue(aSlotFeature,
                candidateTarget);
        var matTargetLinks = targetLinks.stream() //
                .map(link -> toMaterializedLink(candidateTarget, aSlotFeature, link)) //
                .collect(toCollection(ArrayList::new));

        // Let's assume we would already have merged the link - this may in the best case either
        // reduce the difference to 0 or increase it to 1 if we added it once to often
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
}
