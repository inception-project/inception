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
package de.tudarmstadt.ukp.inception.rendering.editorstate;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.inception.support.uima.Range;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;

public interface AnnotationActionHandler
{
    /**
     * Load the {@link AnnotatorState#getSelection() selected annotation} into the detail panel.
     * 
     * @param aTarget
     *            the AJAX target
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    void actionLoadSelectedAnnotationDetails(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException;

    void actionSelect(AjaxRequestTarget aTarget, VID aVid) throws IOException, AnnotationException;

    void actionSelectAndJump(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException;

    void actionJump(AjaxRequestTarget aTarget, int aBegin, int aEnd)
        throws IOException, AnnotationException;

    /**
     * Scroll to the given location, additionally pinging the given ranges.
     * <p>
     * The default discards the ping ranges - they are a scrolling nicety, so an editor that cannot
     * honor them still scrolls correctly. Implementors that can honor them override this.
     *
     * @param aTarget
     *            the AJAX target
     * @param aBegin
     *            the offset to scroll to
     * @param aEnd
     *            the corresponding end offset
     * @param aAdditionalPingRanges
     *            additional ranges that should ideally be visible. May be {@code null} or empty.
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    default void actionJump(AjaxRequestTarget aTarget, int aBegin, int aEnd,
            List<VRange> aAdditionalPingRanges)
        throws IOException, AnnotationException
    {
        actionJump(aTarget, aBegin, aEnd);
    }

    /**
     * Scroll to the given location within the document already being shown.
     *
     * @param aTarget
     *            the AJAX target
     * @param aBegin
     *            the offset to scroll to
     * @param aEnd
     *            the corresponding end offset
     * @param aAdditionalPingRanges
     *            additional ranges that should ideally be visible. May be {@code null} or empty.
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    default void actionJumpTo(AjaxRequestTarget aTarget, int aBegin, int aEnd,
            List<VRange> aAdditionalPingRanges)
        throws IOException, AnnotationException
    {
        actionJump(aTarget, aBegin, aEnd, aAdditionalPingRanges);
    }

    default void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aDataOwner, Range aRange)
        throws IOException, AnnotationException
    {
        actionShowSelectedDocument(aTarget, aDocument, aDataOwner, aRange, null);
    }

    /**
     * Navigate to the given document at the given offsets, additionally pinging the given ranges.
     *
     * @param aTarget
     *            the AJAX target
     * @param aDocument
     *            the document to show
     * @param aRange
     *            where to scroll to, or {@link Range#UNDEFINED} to leave the editor wherever
     *            opening the document placed it.
     * @param aAdditionalPingRanges
     *            additional ranges that should ideally be visible. May be {@code null} or empty.
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aDataOwner, Range aRange, List<VRange> aAdditionalPingRanges)
        throws IOException, AnnotationException;

    /**
     * Delete currently selected annotation.
     * 
     * @param aTarget
     *            the AJAX target
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    void actionDelete(AjaxRequestTarget aTarget) throws IOException, AnnotationException;

    /**
     * Clear the currently selected annotation from the editor panel.
     * 
     * @param aTarget
     *            the AJAX target
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    void actionClear(AjaxRequestTarget aTarget) throws AnnotationException;

    /**
     * Reverse the currently selected relation.
     * 
     * @param aTarget
     *            the AJAX target
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    void actionReverse(AjaxRequestTarget aTarget) throws IOException, AnnotationException;

    /**
     * Fill the currently armed slot with the given annotation.
     * 
     * @param aTarget
     *            the AJAX request target.
     * @param aSlotFillerBegin
     *            the begin of the span selected by the user to create a new annotation or the begin
     *            of the span of the selected existing annotation.
     * @param aSlotFillerEnd
     *            the corresponding end.
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    void actionFillSlot(AjaxRequestTarget aTarget, int aSlotFillerBegin, int aSlotFillerEnd)
        throws IOException, AnnotationException;

    /**
     * Fill the currently armed slot with the given annotation.
     * 
     * @param aTarget
     *            the AJAX request target.
     * @param aExistingSlotFillerId
     *            ID of the existing span annotation to be filled into the armed slot
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    void actionFillSlot(AjaxRequestTarget aTarget, VID aExistingSlotFillerId)
        throws IOException, AnnotationException;

    CAS getEditorCas() throws IOException;

    void writeEditorCas() throws IOException, AnnotationException;

    void writeEditorCas(CAS aCas) throws IOException, AnnotationException;

    /**
     * @return whether the editor served by this handler accepts mutations.
     */
    default boolean isEditable()
    {
        try {
            ensureIsEditable();
            return true;
        }
        catch (AnnotationException e) {
            return false;
        }
    }

    /**
     * Fail closed if the editor served by this handler does not accept mutations. Mutating callers
     * invoke this before touching the CAS.
     * <p>
     * Editability is an authorization decision with no safe default - a permissive default would
     * fail open (silently allowing mutations on read-only/finished documents), a restrictive one
     * would silently disable legitimately editable handlers. Every implementor must therefore make
     * this decision explicitly.
     *
     * @throws AnnotationException
     *             if the editor is not editable.
     */
    void ensureIsEditable() throws AnnotationException;
}
