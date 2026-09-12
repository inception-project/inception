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

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;

/**
 * Manages document display and editor lifecycle within a page. Handles routing of document-show
 * requests to the appropriate editor context, tracking the active editor, and potentially creating
 * new editors or switching between existing ones. On a single-editor page, this is straightforward.
 * On a multi-editor page, this handles the logic to decide which editor should display the document
 * and manages editor state transitions.
 */
public interface DocumentEditorManager
{
    /**
     * @return the editor the user is currently working in, or {@link Optional#empty()} if the page
     *         currently has no editor at all.
     */
    Optional<DiamContext> getActiveContext();

    /**
     * @return whether there is an editor with a document in it. This is the direct question the
     *         page's region-visibility gates want to ask, replacing the proxy of "does the page's
     *         {@code AnnotatorState} have a document" - which is only equivalent today because the
     *         page hands its main editor its own state instance.
     *         <p>
     *         Asks the editor's <b>state</b>, not its hierarchy visibility: gates call this during
     *         the render pass that is still deciding those very visibilities, so
     *         {@code isVisibleInHierarchy()} is unstable there (observed alternating within a
     *         single pass). Hierarchy visibility remains the right question for post-render cleanup
     *         such as dropping an active context that is no longer on screen.
     */
    boolean hasEditor();

    /**
     * Set the active context.
     *
     * @param aTarget
     *            the AJAX target, so consumers can be refreshed. May be {@code null} outside a
     *            partial page update.
     * @param aContext
     *            the editor context that became active, or {@code null} to clear.
     */
    void setActiveContext(AjaxRequestTarget aTarget, DiamContext aContext);

    /**
     * Show the given document in an editor. Implementations should typically update their
     * annotation state model to point to the given document and refresh any necessary editors.
     *
     * @param aTarget
     *            the AJAX target
     * @param aDocument
     *            the document to show
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    default void actionShowDocument(AjaxRequestTarget aTarget, SourceDocument aDocument)
        throws IOException, AnnotationException
    {
        actionShowDocument(aTarget, aDocument, 0, 0);
    }

    /**
     * Show the given document in an editor, scrolling to the given location.
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
    default void actionShowDocument(AjaxRequestTarget aTarget, SourceDocument aDocument, int aBegin,
            int aEnd)
        throws IOException, AnnotationException
    {
        actionShowDocument(aTarget, aDocument, aBegin, aEnd, null);
    }

    /**
     * Show the given document in an editor, scrolling to the given location. Optionally highlight
     * additional ranges.
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
     *            additional ranges that should ideally be visible. May be {@code null} or empty.
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    void actionShowDocument(AjaxRequestTarget aTarget, SourceDocument aDocument, int aBegin,
            int aEnd, List<VRange> aAdditionalPingRanges)
        throws IOException, AnnotationException;

    /**
     * Ensure an editor for the given source document is accessible.
     *
     * @param aDocument
     *            the document that is about to be shown
     * @throws AnnotationException
     *             if the document is not accessible here
     */
    void ensureIsAccessible(SourceDocument aDocument) throws AnnotationException;

    /**
     * Find the editor currently showing the given document for the given data owner.
     *
     * @param aDocument
     *            the document to look for
     * @param aDataOwner
     *            the annotations being shown - the same document opened for a different data owner
     *            is a different editor for these purposes
     * @return the editor showing it, or {@link Optional#empty()} if none is
     */
    Optional<DiamContext> findEditorFor(SourceDocument aDocument, AnnotationSet aDataOwner);

    /**
     * Decide which editor should show the given document, creating or reusing one as the page's
     * placement policy requires.
     *
     * @param aDocument
     *            the document to be shown
     * @return the editor to show it in
     * @throws AnnotationException
     *             if no editor can be provided for it
     */
    DiamContext resolveEditorFor(SourceDocument aDocument) throws AnnotationException;
}
