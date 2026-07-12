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
package de.tudarmstadt.ukp.inception.editor.action;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;

public interface AnnotationActionHandler
{
    /**
     * @deprecated Replaced by {@code CreateRelationAnnotationHandler} and
     *             {@code CreateSpanAnnotationHandler}.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    void actionCreateOrUpdate(AjaxRequestTarget aTarget, CAS aCas)
        throws IOException, AnnotationException;

    /**
     * Load the annotation pointed to in {@link AnnotatorState#getSelection()} in the detail panel.
     * 
     * @param aTarget
     *            the AJAX target
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    void actionSelect(AjaxRequestTarget aTarget) throws IOException, AnnotationException;

    /**
     * @deprecated This method is not able to handle sub-annotations such as chain links. Better use
     *             {@link #actionSelect(AjaxRequestTarget, VID)}
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    void actionSelect(AjaxRequestTarget aTarget, AnnotationFS aAnnoFs)
        throws IOException, AnnotationException;

    void actionSelect(AjaxRequestTarget aTarget, VID aVid) throws IOException, AnnotationException;

    void actionSelectAndJump(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException;

    void actionJump(AjaxRequestTarget aTarget, VID aVid) throws IOException, AnnotationException;

    /**
     * @deprecated This method is not able to handle sub-annotations such as chain links. Better use
     *             {@link #actionSelectAndJump(AjaxRequestTarget, VID)}
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    void actionSelectAndJump(AjaxRequestTarget aTarget, AnnotationFS aFS)
        throws IOException, AnnotationException;

    /**
     * @deprecated This method is not able to handle sub-annotations such as chain links. Better use
     *             {@link #actionJump(AjaxRequestTarget, VID)}
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    void actionJump(AjaxRequestTarget aTarget, AnnotationFS aFS)
        throws IOException, AnnotationException;

    void actionJump(AjaxRequestTarget aTarget, int aBegin, int aEnd)
        throws IOException, AnnotationException;

    /**
     * Navigate to the given document at the given offsets.
     * <p>
     * The default stays within the editor's current document, scrolling to the offsets via
     * {@link #actionJump(AjaxRequestTarget, int, int)} - appropriate for a self-contained editor
     * (e.g. a read-only reference-document viewer). A handler that hosts a document-switchable
     * editor (the main editor's detail panel) overrides this to switch the displayed document to
     * {@code aDocument} first. This is the seam through which a cross-document scroll-to (search
     * hit / cross-document link) opens the target document instead of jumping to raw offsets in the
     * currently displayed one.
     *
     * @param aTarget
     *            the AJAX target
     * @param aDocument
     *            the document to show
     * @param aBegin
     *            the offset to scroll to
     * @param aEnd
     *            the corresponding end offset
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    default void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            int aBegin, int aEnd)
        throws IOException, AnnotationException
    {
        actionJump(aTarget, aBegin, aEnd);
    }

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
     * @deprecated Use either of the other two {@link #actionFillSlot} variants instead.
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    default void actionFillSlot(AjaxRequestTarget aTarget, CAS aCas, int aSlotFillerBegin,
            int aSlotFillerEnd, VID aExistingSlotFillerId)
        throws IOException, AnnotationException
    {
        if (aExistingSlotFillerId.isNotSet()) {
            actionFillSlot(aTarget, aCas, aSlotFillerBegin, aSlotFillerEnd);
        }
        else {
            actionFillSlot(aTarget, aCas, aExistingSlotFillerId);
        }
    }

    /**
     * Fill the currently armed slot with the given annotation.
     * 
     * @param aTarget
     *            the AJAX request target.
     * @param aCas
     *            the CAS in which the slot is going to be filled.
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
    void actionFillSlot(AjaxRequestTarget aTarget, CAS aCas, int aSlotFillerBegin,
            int aSlotFillerEnd)
        throws IOException, AnnotationException;

    /**
     * Fill the currently armed slot with the given annotation.
     * 
     * @param aTarget
     *            the AJAX request target.
     * @param aCas
     *            the CAS in which the slot is going to be filled.
     * @param aExistingSlotFillerId
     *            ID of the existing span annotation to be filled into the armed slot
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    void actionFillSlot(AjaxRequestTarget aTarget, CAS aCas, VID aExistingSlotFillerId)
        throws IOException, AnnotationException;

    CAS getEditorCas() throws IOException;

    void writeEditorCas() throws IOException, AnnotationException;

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
