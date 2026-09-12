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
     * Make the editor served by this context the active one.
     *
     * @param aTarget
     *            the AJAX target, so consumers can be refreshed. May be {@code null} outside a
     *            partial page update.
     */
    default void activate(AjaxRequestTarget aTarget)
    {
        getDocumentEditorManager().setActiveContext(aTarget, this);
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
     * Open the given document in the editor, scroll to the given location.
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
        actionShowSelectedDocument(aTarget, aDocument, aBegin, aEnd, null);
    }

    /**
     * Open the given document in the editor, scroll to the given location. Optionally highlight
     * additional ranges during the scroll.
     *
     * @param aTarget
     *            the AJAX target
     * @param aDocument
     *            the document to show
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
    void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument, int aBegin,
            int aEnd, List<VRange> aAdditionalPingRanges)
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
