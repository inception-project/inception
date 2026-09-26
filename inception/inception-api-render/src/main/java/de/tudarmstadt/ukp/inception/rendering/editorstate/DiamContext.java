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
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.support.uima.Range;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;

/**
 * Editor context through which DIAM AJAX request handlers resolve the annotator state, the editor
 * CAS and the action handler of the editor they are serving.
 */
public interface DiamContext
{
    default Project getProject()
    {
        return getAnnotatorState().getProject();
    }

    default AnnotatorState getAnnotatorState()
    {
        return getStateModel().getObject();
    }

    default AnnotatorViewState getViewState()
    {
        return getAnnotatorState();
    }

    default AnnotationSelectionState getSelectionState()
    {
        return getAnnotatorState();
    }

    IModel<AnnotatorState> getStateModel();

    CAS getEditorCas() throws IOException;

    AnnotationActionHandler getActionHandler();

    /**
     * @return whether this editor is a true editor or rather a permanently read-only viewer.
     */
    default boolean isEditor()
    {
        return true;
    }

    /**
     * @return the manager that owns the editor served by this context.
     */
    DocumentEditorManager getDocumentEditorManager();

    /**
     * Reload the document currently set on this context's annotator state into <b>this</b> editor,
     * resetting the state and upgrading the CAS.
     *
     * @param aTarget
     *            the AJAX request target
     */
    default void actionLoadDocument(AjaxRequestTarget aTarget)
    {
        actionLoadDocument(aTarget, 0);
    }

    void actionLoadDocument(AjaxRequestTarget aTarget, int aFocus);

    /**
     * @return the editor this context belongs to - the one identity its manager tracks as active.
     */
    default Optional<DocumentEditor> getHostingEditor()
    {
        return this instanceof DocumentEditor editor ? Optional.of(editor) : Optional.empty();
    }

    /**
     * Make the editor this context belongs to the active one.
     *
     * @param aTarget
     *            the AJAX target, so consumers can be refreshed. May be {@code null} outside a
     *            partial page update.
     */
    default void activate(AjaxRequestTarget aTarget)
    {
        getHostingEditor()
                .ifPresent(editor -> getDocumentEditorManager().setActiveEditor(aTarget, editor));
    }

    /**
     * Activate editor for this context and select the given annotation.
     * 
     * @param aTarget
     *            the AJAX target
     * @param aVid
     *            the VID of the annotation to select, as resolved in this context's editor CAS
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    default void actionActivateAndSelect(AjaxRequestTarget aTarget, VID aVid)
        throws IOException, AnnotationException
    {
        activate(aTarget);
        getActionHandler().actionSelectAndJump(aTarget, aVid);
    }

    /**
     * Activate editor for this context and load selection into the details editor.
     *
     * @param aTarget
     *            the AJAX target
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    default void actionActivateAndLoadSelectionDetails(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        activate(aTarget);
        getActionHandler().actionLoadSelectedAnnotationDetails(aTarget);
    }

    /**
     * Activate editor for this context and delete the selected annotation
     *
     * @param aTarget
     *            the AJAX target
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    default void actionActivateAndDelete(AjaxRequestTarget aTarget)
        throws IOException, AnnotationException
    {
        activate(aTarget);
        getActionHandler().actionDelete(aTarget);
    }

    /**
     * Scroll to the given location <b>within the document this editor already shows</b>.
     * <p>
     * Prefer this over {@link #actionShowSelectedDocument} whenever the caller is not switching
     * documents: passing a document that happens to be the current one works, but it hides whether
     * a switch was intended, and only a switch may change which data owner the editor shows.
     *
     * @param aTarget
     *            the AJAX target
     * @param aBegin
     *            the offset to scroll to
     * @param aEnd
     *            the corresponding end offset
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    default void actionJumpTo(AjaxRequestTarget aTarget, int aBegin, int aEnd)
        throws IOException, AnnotationException
    {
        actionJumpTo(aTarget, aBegin, aEnd, null);
    }

    /**
     * Scroll to the given location within the document this editor already shows, optionally
     * highlighting additional ranges.
     *
     * @param aTarget
     *            the AJAX target
     * @param aBegin
     *            the offset to scroll to
     * @param aEnd
     *            the corresponding end offset
     * @param aAdditionalPingRanges
     *            additional ranges that should ideally be visible, resolved in this context's
     *            editor CAS. May be {@code null} or empty.
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    void actionJumpTo(AjaxRequestTarget aTarget, int aBegin, int aEnd,
            List<VRange> aAdditionalPingRanges)
        throws IOException, AnnotationException;

    /**
     * Open the given document in the editor, scroll to the given location.
     *
     * @param aTarget
     *            the AJAX target
     * @param aDocument
     *            the document to show
     * @param aDataOwner
     *            whose annotations to show. A caller that only wants to move within what the editor
     *            already shows should use {@link #actionJumpTo} instead of naming these.
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
            AnnotationSet aDataOwner, Range aRange)
        throws IOException, AnnotationException
    {
        actionShowSelectedDocument(aTarget, aDocument, aDataOwner, aRange, null);
    }

    /**
     * Open the given document in the editor, scroll to the given location. Optionally highlight
     * additional ranges during the scroll.
     *
     * @param aTarget
     *            the AJAX target
     * @param aDocument
     *            the document to show
     * @param aRange
     *            where to scroll to, or {@link Range#UNDEFINED} to leave the editor wherever
     *            opening the document placed it.
     * @param aAdditionalPingRanges
     *            additional ranges that should ideally be visible, resolved in this context's
     *            editor CAS. May be {@code null} or empty.
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aDataOwner, Range aRange, List<VRange> aAdditionalPingRanges)
        throws IOException, AnnotationException;

    /**
     * Re-render the editor served by this context, e.g. after the annotator state's visible window
     * changed (paging). A page-hosted editor refreshes the editor on its page; a self-contained
     * editor (e.g. a read-only reference document in a sidebar) re-renders itself. This keeps
     * paging with the editor that received the request instead of always refreshing the main
     * editor's page.
     *
     * @param aTarget
     *            the AJAX target
     */
    void actionRefreshDocument(AjaxRequestTarget aTarget);

    /**
     * @return the markup id under which the editor served by this context registers with the host
     *         page's viewport-sync hub, or {@link Optional#empty()} if it has no editor yet or the
     *         editor does not participate in cross-editor scroll synchronization.
     */
    default Optional<String> getViewportSyncClientId()
    {
        return Optional.empty();
    }
}
