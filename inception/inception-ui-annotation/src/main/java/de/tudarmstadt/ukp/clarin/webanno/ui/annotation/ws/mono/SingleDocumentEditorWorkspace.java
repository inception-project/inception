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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.ws.mono;

import static org.apache.wicket.event.Broadcast.BREADTH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.danekja.java.util.function.serializable.SerializableFunction;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor.DocumentEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.url.EditorUrlParameterStrategy;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.ws.DocumentEditorWorkspace_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.ws.EditorRequest;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditor;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;
import de.tudarmstadt.ukp.inception.rendering.selection.ActiveEditorChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.uima.Range;

/**
 * Hosts exactly one {@link DocumentEditorPanel} and manages its life-cycle.
 *
 * @see DocumentEditorManager
 */
public class SingleDocumentEditorWorkspace
    extends DocumentEditorWorkspace_ImplBase
{
    private static final long serialVersionUID = 4325019867696628201L;

    private static final String MID_DOCUMENT_EDITOR_PANEL = "documentEditorPanel";

    private final SerializableFunction<String, DocumentEditorPanel> editorPanelFactory;
    private final EditorUrlParameterStrategy urlParameterStrategy;

    private DocumentEditorPanel documentEditorPanel;
    private DocumentEditor activeEditor;

    /**
     * @param aUrlParameterStrategy
     *            how the single editor hosted here describes itself in the URL. Handed in rather
     *            than chosen here because whether the data owner is pinned is page policy - the
     *            scheme itself is the same one either way.
     */
    public SingleDocumentEditorWorkspace(String aId,
            SerializableFunction<String, DocumentEditorPanel> aEditorPanelFactory,
            EditorUrlParameterStrategy aUrlParameterStrategy)
    {
        super(aId);

        editorPanelFactory = aEditorPanelFactory;
        urlParameterStrategy = aUrlParameterStrategy;

        setOutputMarkupPlaceholderTag(true);
        add(LambdaBehavior.visibleWhen(this::hasOpenDocument));
        add(new EmptyPanel(MID_DOCUMENT_EDITOR_PANEL));
    }

    @Override
    public void openDocumentEditor(AjaxRequestTarget aTarget)
    {
        if (documentEditorPanel == null) {
            documentEditorPanel = editorPanelFactory.apply(MID_DOCUMENT_EDITOR_PANEL);
            replace(documentEditorPanel);
        }

        setActiveEditor(aTarget, documentEditorPanel);
    }

    @Override
    public Optional<DocumentEditor> getActiveEditor()
    {
        return Optional.ofNullable(activeEditor);
    }

    @Override
    public void setActiveEditor(AjaxRequestTarget aTarget, DocumentEditor aEditor)
    {
        if (aEditor == activeEditor) {
            return;
        }

        activeEditor = aEditor;

        var page = findPage();
        if (page != null) {
            page.send(page, BREADTH, new ActiveEditorChangedEvent(activeEditor, aTarget));
        }
    }

    @Override
    public boolean hasOpenDocument()
    {
        return documentEditorPanel != null //
                && documentEditorPanel.getModelObject() != null //
                && documentEditorPanel.getModelObject().getDocument() != null;
    }

    @Override
    public void dropActiveEditorIfNotDisplayed(AjaxRequestTarget aTarget)
    {
        if (isActiveEditorDisplayed()) {
            return;
        }

        var fallback = documentEditorPanel != null && isDisplayed(documentEditorPanel)
                ? documentEditorPanel
                : null;

        setActiveEditor(aTarget, fallback);
    }

    private boolean isActiveEditorDisplayed()
    {
        return getActiveEditor().map(this::isDisplayed).orElse(false);
    }

    private boolean isDisplayed(DocumentEditor aEditor)
    {
        if (!(aEditor instanceof Component component)) {
            return true;
        }

        return component.findParent(Page.class) != null && component.isVisibleInHierarchy();
    }

    @Override
    protected void onBeforeRender()
    {
        super.onBeforeRender();
        dropActiveEditorIfNotDisplayed(null);
    }

    @Override
    public Optional<DocumentEditor> findEditorFor(SourceDocument aDocument,
            AnnotationSet aDataOwner)
    {
        if (documentEditorPanel == null) {
            return Optional.empty();
        }

        var state = documentEditorPanel.getAnnotatorState();
        if (!Objects.equals(state.getDocument(), aDocument)
                || !Objects.equals(state.getDataOwner(), aDataOwner)) {
            return Optional.empty();
        }

        return Optional.of(documentEditorPanel);
    }

    @Override
    public void actionShowDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aDataOwner, Range aRange, List<VRange> aAdditionalPingRanges)
        throws IOException, AnnotationException
    {
        Validate.notNull(aDocument, "Document must be specified");
        Validate.notNull(aDataOwner, "Data owner must be specified");

        openDocumentEditor(aTarget);

        documentEditorPanel.actionShowSelectedDocument(aTarget, aDocument, aDataOwner, aRange,
                aAdditionalPingRanges);
    }

    @Override
    protected EditorUrlParameterStrategy getUrlParameterStrategy()
    {
        return urlParameterStrategy;
    }

    @Override
    protected List<DocumentEditorPanel> getEditorsForUrlFragment()
    {
        // There is at most one editor here, and the scalar scheme describes exactly it.
        var editors = new ArrayList<DocumentEditorPanel>();
        if (documentEditorPanel != null) {
            editors.add(documentEditorPanel);
        }

        return editors;
    }

    @Override
    public boolean canCloseEditor(DocumentEditor aEditor)
    {
        return false;
    }

    @Override
    public int getMaxEditors()
    {
        return 1;
    }

    @Override
    protected void openEditorAt(AjaxRequestTarget aTarget, int aIndex, EditorRequest aRequest)
        throws IOException, AnnotationException
    {
        actionShowDocument(aTarget, aRequest.document(), aRequest.dataOwner());

        aRequest.focus().ifPresent(focus -> getEditorsForUrlFragment().stream() //
                .findFirst() //
                .ifPresent(editor -> moveToUnit(aTarget, editor, focus)));
    }

    @Override
    protected void closeSurplusEditors(AjaxRequestTarget aTarget, int aRequestedCount)
    {
        // Nothing to do - a single-editor workspace is never asked for fewer than it has, the
        // request list having been truncated to getMaxEditors() == 1 above.
    }
}
