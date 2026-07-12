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
package de.tudarmstadt.ukp.inception.diam.model;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;

/**
 * Editor-scoped context through which DIAM AJAX request handlers resolve the annotator state, the
 * editor CAS and the action handler of the editor they are serving.
 * <p>
 * Handlers used to resolve all of these via {@code getPage()}, which always returned the
 * <i>main</i> editor's page regardless of which editor's {@code DiamAjaxBehavior} received the
 * request. By going through a context held on the behavior instead, handlers can serve alternative
 * editors (e.g. a read-only editor embedded in a sidebar) without leaking actions into the main
 * editor.
 * <p>
 * Editability is <i>not</i> part of this context: it is a property of the
 * {@link AnnotationActionHandler} (which performs the writes), so mutating handlers fail closed via
 * {@code getActionHandler().ensureIsEditable()}.
 * <p>
 * A context is mandatory: every {@code DiamAjaxBehavior} is constructed with one and handlers
 * dereference it unconditionally. {@code AnnotationPageBase} implements this interface, so the main
 * editor simply supplies its page as the context, reproducing the historic {@code getPage()}
 * behavior.
 */
public interface DiamContext
{
    AnnotatorState getAnnotatorState();

    CAS getEditorCas() throws IOException;

    AnnotationActionHandler getActionHandler();

    /**
     * Navigate the editor served by this context to show the given document at the given offsets.
     * <p>
     * A page-hosted editor may switch the document currently displayed on the page; a
     * self-contained editor (e.g. a read-only reference document in a sidebar) scrolls within its
     * own document and does not switch documents. Navigation therefore stays with the editor that
     * received the request instead of always driving the main editor's page.
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
    void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument, int aBegin,
            int aEnd)
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
}
