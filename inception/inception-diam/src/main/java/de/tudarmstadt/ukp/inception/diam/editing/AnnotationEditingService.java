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
package de.tudarmstadt.ukp.inception.diam.editing;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * Editing operations on an annotation CAS.
 * <p>
 * This service is deliberately free of Wicket: it takes everything it needs as parameters and
 * returns messages rather than reporting them. That keeps it callable from the annotation page, the
 * curation page and the detail panel alike, and unit-testable without a Wicket harness.
 * <p>
 * In particular there is no {@code AjaxRequestTarget} parameter anywhere in this API. Callers are
 * responsible for presenting the returned {@link LogMessage}s and for repainting.
 */
public interface AnnotationEditingService
{
    /**
     * Deletes the annotation with the given {@link VID}, along with everything that depends on it:
     * attached spans, attached relations and slots pointing at it.
     *
     * @param aCas
     *            the CAS to delete from - must be the CAS the VID was resolved against.
     * @param aState
     *            supplies the document, data owner and armed-slot state.
     * @param aVid
     *            the annotation to delete.
     * @param aLayer
     *            the layer of the annotation to delete.
     * @param aAdapter
     *            the adapter for that layer.
     * @return messages describing slots that were cleared or could not be cleared.
     * @throws PartialDeleteException
     *             if the deletion failed after the CAS had already been modified. The CAS is then
     *             inconsistent and must not be persisted.
     * @throws AnnotationException
     *             if the deletion is not permitted. The CAS is unmodified in this case.
     */
    List<LogMessage> deleteAnnotation(CAS aCas, AnnotatorState aState, VID aVid,
            AnnotationLayer aLayer, TypeAdapter aAdapter)
        throws AnnotationException;

    /**
     * Determines what else refers to the given annotation, so the caller can decide whether
     * deleting it needs a confirmation and whether it is permitted at all.
     *
     * @param aProject
     *            the project the annotation belongs to.
     * @param aFS
     *            the annotation to check.
     * @return the attach status.
     */
    AttachStatus checkAttachStatus(Project aProject, AnnotationFS aFS);

    /**
     * Commits the values from the given feature states into the annotation with the given target FS
     * address in the given target CAS using the provided type adapter.
     * <p>
     * Features are committed independently: if one feature cannot be set, the remaining ones are
     * still attempted and the failure is reported as an ERROR message.
     *
     * @param aDocument
     *            the document the target CAS belongs to.
     * @param aDataOwner
     *            the user the annotations belong to.
     * @param aTargetCas
     *            the CAS to write to - must be the CAS the target address was resolved against.
     * @param aTargetFsAddr
     *            address of the annotation to update.
     * @param aAdapter
     *            the adapter for the annotation's layer.
     * @param aFeatureStates
     *            the values to commit.
     * @return a message per feature that could not be set; empty if all features were committed.
     */
    List<LogMessage> commitFeatureStates(SourceDocument aDocument, String aDataOwner,
            CAS aTargetCas, int aTargetFsAddr, TypeAdapter aAdapter,
            List<FeatureState> aFeatureStates);

    /**
     * Reverses the relation with the given address, i.e. replaces it with a relation whose source
     * and target are swapped, carrying the given feature values over to the new relation.
     * <p>
     * The old relation is deleted and a new one is created - the address of the relation therefore
     * changes. Callers holding a {@code Selection} on the old relation must update it to the
     * returned annotation.
     *
     * @param aDocument
     *            the document the CAS belongs to.
     * @param aDataOwner
     *            the user the annotations belong to.
     * @param aCas
     *            the CAS to modify - must be the CAS the address was resolved against.
     * @param aRelationAddr
     *            address of the relation to reverse.
     * @param aAdapter
     *            the adapter for the relation's layer.
     * @param aFeatureStates
     *            feature values to apply to the reversed relation.
     * @param aMessages
     *            collects messages about features that could not be carried over.
     * @return the newly created, reversed relation.
     * @throws AnnotationException
     *             if the layer is not a relation layer. The CAS is unmodified in this case.
     */
    AnnotationFS reverseRelation(SourceDocument aDocument, String aDataOwner, CAS aCas,
            int aRelationAddr, TypeAdapter aAdapter, List<FeatureState> aFeatureStates,
            List<LogMessage> aMessages)
        throws AnnotationException;

    /**
     * Creates a new span annotation to be used as the filler of the currently armed slot.
     *
     * @param aDocument
     *            the document the CAS belongs to.
     * @param aDataOwner
     *            the user the annotations belong to.
     * @param aCas
     *            the CAS to create the annotation in.
     * @param aState
     *            supplies the project, the armed slot and the anchoring mode.
     * @param aBegin
     *            begin offset of the new annotation.
     * @param aEnd
     *            end offset of the new annotation.
     * @return the {@link VID} of the newly created slot filler.
     * @throws AnnotationException
     *             if the armed slot accepts arbitrary annotations rather than a concrete span type
     *             defined in the project - such a slot cannot be filled by marking text.
     */
    VID createSlotFiller(SourceDocument aDocument, String aDataOwner, CAS aCas,
            AnnotatorState aState, int aBegin, int aEnd)
        throws AnnotationException;

    /**
     * Injects the given slot filler into the currently armed slot of the slot host annotation and
     * commits the change to the CAS.
     * <p>
     * This does <b>not</b> clear the armed slot - the caller decides when to do that, because it
     * also has to update its own view of the annotation afterwards.
     *
     * @param aDocument
     *            the document the CAS belongs to.
     * @param aDataOwner
     *            the user the annotations belong to.
     * @param aCas
     *            the CAS to modify - must be the CAS the addresses were resolved against.
     * @param aState
     *            supplies the project and the armed slot.
     * @param aSlotFillerAddr
     *            address of the annotation to place into the slot.
     * @return messages describing features that could not be set.
     * @throws AnnotationException
     *             if no slot is armed.
     */
    List<LogMessage> fillSlot(SourceDocument aDocument, String aDataOwner, CAS aCas,
            AnnotatorState aState, int aSlotFillerAddr)
        throws AnnotationException;

    /**
     * Builds the feature states for the given layer, either from an existing annotation or from
     * remembered values, including the tagsets (re-ordered by the constraint rules where
     * applicable).
     * <p>
     * The result is returned rather than written into the {@link AnnotatorState} so that the caller
     * can replace the feature states in one go. A failure part-way through therefore leaves the
     * previous feature states in place instead of clearing them.
     *
     * @param aCas
     *            the CAS to read from - must be the CAS {@code aFS} belongs to.
     * @param aState
     *            supplies the project, the selection and the constraints.
     * @param aLayer
     *            the layer whose features to load.
     * @param aFS
     *            the annotation to read the values from, or {@code null} when creating a new
     *            annotation.
     * @param aRemembered
     *            values to pre-fill the features with when {@code aFS} is {@code null}; may itself
     *            be {@code null}.
     * @param aMessages
     *            collects messages about problems encountered while loading, e.g. constraints that
     *            could not be evaluated.
     * @return the feature states to show in the feature editors.
     * @throws StaleTypeSystemException
     *             if a feature of the layer does not exist in the CAS type system. No feature
     *             states are returned in this case.
     */
    List<FeatureState> loadFeatureStates(CAS aCas, AnnotatorState aState, AnnotationLayer aLayer,
            FeatureStructure aFS, Map<AnnotationFeature, Serializable> aRemembered,
            List<LogMessage> aMessages)
        throws StaleTypeSystemException;
}
