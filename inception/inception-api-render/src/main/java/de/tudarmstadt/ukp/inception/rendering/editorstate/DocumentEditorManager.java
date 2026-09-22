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
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.support.uima.Range;

/**
 * Manages document display and editor life-cycle within a page. Handles routing of document-show
 * requests to the appropriate editor, tracking the active editor, and potentially creating new
 * editors or switching between existing ones. On a single-editor page, this is straightforward. On
 * a multi-editor page, this handles the logic to decide which editor should display the document
 * and manages editor state transitions.
 */
public interface DocumentEditorManager
    extends Serializable
{
    /**
     * @return the editor the user is currently working in, or {@link Optional#empty()} if the page
     *         currently has no editor at all.
     */
    Optional<DocumentEditor> getActiveEditor();

    /**
     * @return whether there is any editor with a document in it.
     */
    boolean hasOpenDocument();

    /**
     * @param aEditor
     *            the editor in question.
     * @return whether that editor may be closed.
     */
    boolean canCloseEditor(DocumentEditor aEditor);

    /**
     * @param aEditor
     *            the editor in question.
     * @return whether that editor should show that it is the active one (e.g. not necessary if
     *         there is only a single editor).
     */
    default boolean isMarkedActiveEditor(DocumentEditor aEditor)
    {
        return false;
    }

    /**
     * Close an editor, dropping the document it shows. Callers must check
     * {@link #canCloseEditor(DocumentEditor)} first.
     *
     * @param aTarget
     *            the AJAX target.
     * @param aEditor
     *            the editor to close.
     */
    default void closeEditor(AjaxRequestTarget aTarget, DocumentEditor aEditor)
    {
        throw new UnsupportedOperationException("This workspace cannot close editors");
    }

    /**
     * Set the active editor.
     *
     * @param aTarget
     *            the AJAX target, so consumers can be refreshed. May be {@code null} outside a
     *            partial page update.
     * @param aEditor
     *            the editor that became active, or {@code null} to clear.
     */
    void setActiveEditor(AjaxRequestTarget aTarget, DocumentEditor aEditor);

    /**
     * Show the given document in an editor. Implementations should typically update their
     * annotation state model to point to the given document and refresh any necessary editors.
     *
     * @param aTarget
     *            the AJAX target
     * @param aDocument
     *            the document to show
     * @param aDataOwner
     *            whose annotations to show.
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    default void actionShowDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aDataOwner)
        throws IOException, AnnotationException
    {
        actionShowDocument(aTarget, aDocument, aDataOwner, Range.UNDEFINED);
    }

    /**
     * Show the given document, preferably in the given editor.
     *
     * @param aTarget
     *            the AJAX target
     * @param aPreferredEditor
     *            the editor the document should land in, or {@code null} for no preference. An
     *            editor the manager does not host is ignored like {@code null}.
     * @param aDocument
     *            the document to show
     * @param aDataOwner
     *            whose annotations to show.
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    default void actionShowDocument(AjaxRequestTarget aTarget, DocumentEditor aPreferredEditor,
            SourceDocument aDocument, AnnotationSet aDataOwner)
        throws IOException, AnnotationException
    {
        // Nothing to disambiguate when there is only one editor.
        actionShowDocument(aTarget, aDocument, aDataOwner);
    }

    /**
     * Show the given document in an editor, scrolling to the given location.
     *
     * @param aTarget
     *            the AJAX target
     * @param aDocument
     *            the document to show
     * @param aDataOwner
     *            whose annotations to show
     * @param aRange
     *            where to scroll to, or {@link Range#UNDEFINED} to leave the editor wherever
     *            opening the document placed it.
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    default void actionShowDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aDataOwner, Range aRange)
        throws IOException, AnnotationException
    {
        actionShowDocument(aTarget, aDocument, aDataOwner, aRange, null);
    }

    /**
     * Show the given document in an editor, scrolling to the given location. Optionally highlight
     * additional ranges.
     *
     * @param aTarget
     *            the AJAX target
     * @param aDocument
     *            the document to show
     * @param aDataOwner
     *            whose annotations to show
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
    void actionShowDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aDataOwner, Range aRange, List<VRange> aAdditionalPingRanges)
        throws IOException, AnnotationException;

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
    Optional<DocumentEditor> findEditorFor(SourceDocument aDocument, AnnotationSet aDataOwner);

    /**
     * @return maximal number of editors that may be opened at the same time in this workspace.
     */
    int getMaxEditors();
}
