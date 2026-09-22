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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.ws;

import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.TOP;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor.DocumentEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.url.EditorUrlParameterStrategy;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.url.UrlFragmentTarget;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorViewState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;
import de.tudarmstadt.ukp.inception.rendering.editorstate.EditorBoundEvent;
import de.tudarmstadt.ukp.inception.support.uima.Range;

public abstract class DocumentEditorWorkspace_ImplBase
    extends Panel
    implements DocumentEditorManager
{
    private static final long serialVersionUID = -7009877919455815827L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected DocumentEditorWorkspace_ImplBase(String aId)
    {
        super(aId);
    }

    /**
     * Open the first editor in the workspace and activate it. Call if you need to make sure an
     * editor exists.
     *
     * @param aTarget
     *            the AJAX target.
     */
    public abstract void openDocumentEditor(AjaxRequestTarget aTarget);

    /**
     * Drop the active context if it is no longer displayed.
     *
     * @param aTarget
     *            the AJAX target (optional).
     */
    public abstract void dropActiveEditorIfNotDisplayed(AjaxRequestTarget aTarget);

    /**
     * @return how this workspace describes itself in the URL fragment.
     */
    protected abstract EditorUrlParameterStrategy getUrlParameterStrategy();

    /**
     * @return the editors this workspace's URL fragment describes, in display order. Never
     *         {@code null}, possibly empty before the first document has been opened.
     */
    protected abstract List<DocumentEditorPanel> getEditorsForUrlFragment();

    /**
     * @return the parameters the URL fragment should carry for what is currently open here.
     */
    public Map<String, Object> getUrlFragmentParameters()
    {
        var editors = getEditorsForUrlFragment().stream() //
                .filter(editor -> editor.getModelObject() != null
                        && editor.getModelObject().getDocument() != null) //
                .toList();

        if (editors.isEmpty()) {
            return Map.of();
        }

        return getUrlParameterStrategy().getUrlFragmentParameters(
                editors.stream().map(DocumentEditorPanel::getModelObject).toList());
    }

    /**
     * @param aState
     *            an editor state, typically taken from an {@link EditorBoundEvent}.
     * @return whether the given state belongs to an editor this workspace's URL fragment describes.
     */
    public boolean anyEditorOwnsState(AnnotatorViewState aState)
    {
        if (aState == null) {
            return false;
        }

        return getEditorsForUrlFragment().stream()
                .anyMatch(editor -> editor.getModelObject() == aState);
    }

    /**
     * @return the editors the fragment describes, in display order.
     */
    public List<DocumentEditorPanel> getEditorsDescribedByUrlFragment()
    {
        return getEditorsForUrlFragment();
    }

    /**
     * Parse the page parameters.
     * 
     * @param aDocument
     *            the raw {@code d} parameter.
     * @param aFocus
     *            the raw {@code f} parameter.
     * @param aDataOwner
     *            the raw {@code u} parameter.
     * @return what the fragment asks for, one entry per editor, in display order.
     */
    public List<UrlFragmentTarget> parseUrlFragmentTargets(StringValue aDocument,
            StringValue aFocus, StringValue aDataOwner)
    {
        return getUrlParameterStrategy().parseUrlFragmentTargets(aDocument, aFocus, aDataOwner);
    }

    /**
     * @return the focus the fragment asks for in that editor, or empty if it asks for none.
     */
    public Optional<Integer> parseUrlFragmentFocusIfPresent(StringValue aFocus, int aIndex)
    {
        return getUrlParameterStrategy().parseUrlFragmentFocusIfPresent(aFocus, aIndex);
    }

    /**
     * Show exactly the given documents in the workspace.
     *
     * @param aRequests
     *            what should be open, in display order. Normalised here - callers need not dedup or
     *            respect {@link #getMaxEditors()}.
     */
    public void actionShowDocumentsInEditors(AjaxRequestTarget aTarget,
            List<EditorRequest> aRequests)
        throws IOException, AnnotationException
    {
        var requests = normalizeEditorRequests(aRequests);

        if (requests.isEmpty()) {
            return;
        }

        var editors = getEditorsForUrlFragment();

        for (var i = 0; i < requests.size(); i++) {
            var request = requests.get(i);
            var editor = i < editors.size() ? editors.get(i) : null;

            if (editor == null) {
                // No editor at this position yet - open one showing the request.
                openEditorAt(aTarget, i, request);
                continue;
            }

            reconcileEditor(aTarget, editor, request);
        }

        closeSurplusEditors(aTarget, requests.size());
    }

    private List<EditorRequest> normalizeEditorRequests(List<EditorRequest> aRequests)
    {
        if (aRequests == null || aRequests.isEmpty()) {
            return emptyList();
        }

        var deduped = new ArrayList<EditorRequest>();
        for (var request : aRequests) {
            var alreadyRequested = deduped.stream() //
                    .anyMatch(r -> r.matches(request.document(), request.dataOwner()));
            if (!alreadyRequested) {
                deduped.add(request);
            }
        }

        var cap = getMaxEditors();
        return deduped.size() <= cap ? deduped : deduped.subList(0, cap);
    }

    private void reconcileEditor(AjaxRequestTarget aTarget, DocumentEditorPanel aEditor,
            EditorRequest aRequest)
        throws IOException, AnnotationException
    {
        var state = aEditor.getAnnotatorState();

        if (!aRequest.matches(state.getDocument(), state.getDataOwner())) {
            // A different document and/or data owner belongs here now.
            aEditor.actionShowSelectedDocument(aTarget, aRequest.document(), aRequest.dataOwner(),
                    Range.UNDEFINED, null);

            // Opening places the editor at the resumption point. Only move off it if the fragment
            // actually asks for a unit.
            aRequest.focus().ifPresent(focus -> moveToUnit(aTarget, aEditor, focus));
            return;
        }

        // The editor already shows the right document/data owner, maybe adjust focus
        if (aRequest.focus().isEmpty()) {
            return;
        }

        if (!aEditor.isShowingLoadedDocument()) {
            // If it is not showing yet, it should already have been configured for the right focus
            // earlier in the request, so skip here to avoid doing it again
            return;
        }

        aRequest.focus() //
                .filter(focus -> focus != state.getFocusUnitIndex()) //
                .ifPresent(focus -> moveToUnit(aTarget, aEditor, focus));
    }

    protected void moveToUnit(AjaxRequestTarget aTarget, DocumentEditorPanel aEditor, int aFocus)
    {
        try {
            // Move within the panel's own state - it is the one that has a paging strategy.
            aEditor.getModelObject().moveToUnit(aEditor.getEditorCas(), aFocus, TOP);
            aEditor.actionRefreshDocument(aTarget);
        }
        catch (Exception e) {
            handleEditorReconciliationError(aTarget, aEditor, e);
        }
    }

    protected void handleEditorReconciliationError(AjaxRequestTarget aTarget,
            DocumentEditorPanel aEditor, Exception aException)
    {
        var state = aEditor.getAnnotatorState();
        LOG.error("Error reading CAS of document {} for user {}", state.getDocument(),
                state.getDataOwner(), aException);
        error("Error reading CAS " + aException.getMessage());
        if (aTarget != null) {
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    /**
     * Open an editor at the given position showing the request.
     */
    protected abstract void openEditorAt(AjaxRequestTarget aTarget, int aIndex,
            EditorRequest aRequest)
        throws IOException, AnnotationException;

    /**
     * Close the editors past the last requested position.
     */
    protected abstract void closeSurplusEditors(AjaxRequestTarget aTarget, int aRequestedCount);
}
