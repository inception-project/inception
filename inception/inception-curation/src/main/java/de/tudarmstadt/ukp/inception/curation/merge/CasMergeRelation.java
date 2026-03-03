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

import static de.tudarmstadt.ukp.inception.curation.merge.CasMerge.copyFeatures;
import static de.tudarmstadt.ukp.inception.curation.merge.CasMergeOperationResult.ResultState.CREATED;
import static de.tudarmstadt.ukp.inception.curation.merge.CasMergeOperationResult.ResultState.UPDATED;
import static de.tudarmstadt.ukp.inception.curation.merge.CasMergeSpan.selectCandidateSpansAt;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.FSUtil.getFeature;

import java.util.List;
import java.util.stream.Stream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;

class CasMergeRelation
{
    static CasMergeOperationResult mergeRelationAnnotation(CasMergeContext aContext,
            SourceDocument aDocument, String aDataOwner, AnnotationLayer aAnnotationLayer,
            CAS aTargetCas, AnnotationFS aSourceFs, boolean aAllowStacking)
        throws AnnotationException
    {
        var relationAdapter = (RelationAdapter) aContext.getAdapter(aAnnotationLayer);
        if (aContext.isSilenceEvents()) {
            relationAdapter.silenceEvents();
        }

        if (existsEquivalentRelation(aTargetCas, relationAdapter, aSourceFs)) {
            throw new AlreadyMergedException(
                    "The annotation already exists in the target document.");
        }

        var candidateSources = findRelationEndpointInTargetCas(aContext, aDocument, aTargetCas,
                aSourceFs, relationAdapter.getSourceFeatureName()).limit(2).toList();
        if (candidateSources.size() > 1) {
            throw new MergeConflictException(
                    "There are multiple possible sources endpoints for this relation in "
                            + "the target document. Cannot merge this annotation.");
        }

        var candidateTargets = findRelationEndpointInTargetCas(aContext, aDocument, aTargetCas,
                aSourceFs, relationAdapter.getTargetFeatureName()).limit(2).toList();
        if (candidateTargets.size() > 1) {
            throw new MergeConflictException(
                    "There are multiple possible target endpoints for this relation in the "
                            + "target document. Cannot merge this annotation.");
        }

        // check if target/source exists in the mergeview
        if (candidateSources.isEmpty() || candidateTargets.isEmpty()) {
            throw new UnfulfilledPrerequisitesException("Both the source and target annotation"
                    + " must exist in the target document. Please first merge/create them");
        }

        var originFs = candidateSources.get(0);
        var targetFs = candidateTargets.get(0);

        if (relationAdapter.getAttachFeatureName() != null) {
            var originAttachAnnotation = getFeature(originFs,
                    relationAdapter.getAttachFeatureName(), AnnotationFS.class);
            var targetAttachAnnotation = getFeature(targetFs,
                    relationAdapter.getAttachFeatureName(), AnnotationFS.class);

            if (originAttachAnnotation == null || targetAttachAnnotation == null) {
                throw new UnfulfilledPrerequisitesException(
                        "No annotation to attach to. Cannot merge this relation.");
            }
        }

        var existingAnnos = selectCandidateRelationsAt(aTargetCas, relationAdapter, aSourceFs,
                originFs, targetFs);
        if (existingAnnos.isEmpty() || aAllowStacking) {
            var mergedRelation = relationAdapter.add(aDocument, aDataOwner, originFs, targetFs,
                    aTargetCas);
            try {
                copyFeatures(aContext, aDocument, aDataOwner, relationAdapter, mergedRelation,
                        aSourceFs);
            }
            catch (AnnotationException e) {
                // If there was an error while setting the features, then we skip the entire
                // annotation
                relationAdapter.delete(aDocument, aDataOwner, aTargetCas, VID.of(mergedRelation));
            }
            return new CasMergeOperationResult(CREATED, getAddr(mergedRelation));
        }
        else {
            var mergeTargetFS = existingAnnos.get(0);
            copyFeatures(aContext, aDocument, aDataOwner, relationAdapter, mergeTargetFS,
                    aSourceFs);
            return new CasMergeOperationResult(UPDATED, getAddr(mergeTargetFS));
        }
    }

    private static List<Annotation> selectCandidateRelationsAt(CAS aTargetCas,
            RelationAdapter aAdapter, AnnotationFS aOriginalFs, AnnotationFS aOriginalSourceFs,
            AnnotationFS aOriginalTargetFs)
    {
        var maybeTargetType = aAdapter.getAnnotationType(aTargetCas);
        if (maybeTargetType.isEmpty()) {
            return emptyList();
        }

        return aTargetCas.<Annotation> select(maybeTargetType.get()) //
                .at(aOriginalFs.getBegin(), aOriginalFs.getEnd()) //
                .filter(fs -> aAdapter.isSamePosition(fs, aOriginalFs)) //
                .toList();
    }

    private static Stream<Annotation> findRelationEndpointInTargetCas(CasMergeContext aContext,
            SourceDocument aDocument, CAS aTargetCas, AnnotationFS aRelationFS,
            String aEndpointFeatureName)
    {
        var endpointFSClicked = getFeature(aRelationFS, aEndpointFeatureName, AnnotationFS.class);
        var endpointSpanLayer = aContext.findLayer(aDocument.getProject(),
                endpointFSClicked.getType().getName());
        var endpointSpanAdapter = aContext.getAdapter(endpointSpanLayer);

        return selectCandidateSpansAt(aTargetCas, endpointSpanAdapter, endpointFSClicked);
    }

    private static boolean existsEquivalentRelation(CAS aTargetCas, TypeAdapter aAdapter,
            AnnotationFS aOriginal)
    {
        var targetType = aAdapter.getAnnotationType(aTargetCas);
        if (targetType.isEmpty()) {
            return false;
        }

        return selectCovered(aTargetCas, targetType.get(), aOriginal.getBegin(), aOriginal.getEnd())
                .stream() //
                .anyMatch(fs -> aAdapter.isEquivalentAnnotation(fs, aOriginal));
    }
}
