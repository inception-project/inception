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
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.cas.AnnotationBase;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.CreateDocumentAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerTraits;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;

class CasMergeDocument
{
    static CasMergeOperationResult mergeDocumentAnnotation(CasMergeContext aContext,
            SourceDocument aDocument, String aTargetDataOwner, AnnotationLayer aAnnotationLayer,
            CAS aTargetCas, AnnotationBase aSourceFs)
        throws AnnotationException
    {
        var adapter = (DocumentMetadataLayerAdapter) aContext.getAdapter(aAnnotationLayer);
        if (aContext.isSilenceEvents()) {
            adapter.silenceEvents();
        }

        var allowStacking = !adapter.getTraits(DocumentMetadataLayerTraits.class)
                .map(DocumentMetadataLayerTraits::isSingleton).orElse(false);

        if (existsEquivalent(aTargetCas, adapter, aSourceFs)) {
            throw new AlreadyMergedException(
                    "The annotation already exists in the target document.");
        }

        // A silenced adapter publishes nothing anyway - batching would only make it build the
        // events and throw them away
        if (adapter.isSilenced()) {
            return mergeIntoTarget(aContext, aDocument, aTargetDataOwner, adapter, allowStacking,
                    aTargetCas, aSourceFs);
        }

        // Publish the events of the merge only if it succeeds - the events of a failed merge must
        // not end up e.g. in the undo history. Mind that a failed merge only removes an annotation
        // it created; when it updates an existing annotation, values copied before the failure
        // remain set.
        try (var eventBatch = adapter.batchEvents()) {
            var result = mergeIntoTarget(aContext, aDocument, aTargetDataOwner, adapter,
                    allowStacking, aTargetCas, aSourceFs);
            eventBatch.commit();
            return result;
        }
    }

    private static CasMergeOperationResult mergeIntoTarget(CasMergeContext aContext,
            SourceDocument aDocument, String aTargetDataOwner,
            DocumentMetadataLayerAdapter aAdapter, boolean aAllowStacking, CAS aTargetCas,
            AnnotationBase aSourceFs)
        throws AnnotationException
    {
        // a) if stacking allowed add this new annotation to the merge view
        var targetType = aAdapter.getAnnotationType(aTargetCas).get();
        var existingAnnos = aTargetCas.select(targetType);
        if (existingAnnos.isEmpty() || aAllowStacking) {
            // Create the annotation via the adapter - this also takes care of attaching to an
            // annotation if necessary
            var mergedAnn = aAdapter.handle(CreateDocumentAnnotationRequest.builder() //
                    .withDocument(aDocument, aTargetDataOwner, aTargetCas) //
                    .build());

            var mergedSpanAddr = -1;
            try {
                copyFeatures(aContext, aDocument, aTargetDataOwner, aAdapter, mergedAnn, aSourceFs);
                mergedSpanAddr = getAddr(mergedAnn);
            }
            catch (AnnotationException e) {
                // If there was an error while setting the features, then we skip the entire
                // annotation
                aAdapter.delete(aDocument, aTargetDataOwner, aTargetCas, VID.of(mergedAnn));
                throw e;
            }
            return new CasMergeOperationResult(CREATED, mergedSpanAddr);
        }
        // b) if stacking is not allowed, modify the existing annotation with this one - unless we
        // are asked to preserve annotations already present in the target (e.g. curator decisions),
        // in which case we leave the existing annotation untouched.
        else if (aContext.isPreserveExisting()) {
            throw new AnnotationPreservedException(
                    "The target already contains a document annotation which is preserved.");
        }
        else {
            var annoToUpdate = existingAnnos.get(0);
            copyFeatures(aContext, aDocument, aTargetDataOwner, aAdapter, annoToUpdate, aSourceFs);
            var mergedSpanAddr = getAddr(annoToUpdate);
            return new CasMergeOperationResult(UPDATED, mergedSpanAddr);
        }
    }

    private static boolean existsEquivalent(CAS aTargetCas, TypeAdapter aAdapter,
            AnnotationBase aOriginal)
    {
        var targetType = aAdapter.getAnnotationType(aTargetCas);
        if (targetType.isEmpty()) {
            return false;
        }

        return aTargetCas.<AnnotationBase> select(targetType.get()) //
                .anyMatch(fs -> aAdapter.isEquivalentAnnotation(fs, aOriginal));
    }
}
