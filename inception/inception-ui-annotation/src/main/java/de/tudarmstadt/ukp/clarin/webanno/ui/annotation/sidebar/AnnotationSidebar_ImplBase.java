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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import java.io.IOException;
import java.util.Optional;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditor;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;
import de.tudarmstadt.ukp.inception.support.uima.Range;

public abstract class AnnotationSidebar_ImplBase
    extends GenericPanel<AnnotatorState>
{
    private static final long serialVersionUID = 8637373389151630602L;

    private final SidebarContext context;
    private @SpringBean DocumentService documentService;

    public AnnotationSidebar_ImplBase(final String aId, SidebarContext aContext)
    {
        super(aId, LoadableDetachableModel.of(() -> aContext.activeEditor() //
                .map(DiamContext::getAnnotatorState) //
                .orElse(null)));

        context = aContext;

        setOutputMarkupPlaceholderTag(true);
    }

    public AnnotationPageBase2 getAnnotationPage()
    {
        return context.page();
    }

    public Project getProject()
    {
        return context.page().getProject();
    }

    public SidebarContext getContext()
    {
        return context;
    }

    /**
     * @return the editor the user is currently working in, or {@link Optional#empty()} if the page
     *         has no editor.
     */
    public Optional<DocumentEditor> getActiveEditor()
    {
        return getDocumentEditorManager().getActiveEditor();
    }

    /**
     * @return the document editor manager for the page.
     */
    public DocumentEditorManager getDocumentEditorManager()
    {
        return context.manager();
    }

    /**
     * Take the user to the given location, preferably in the active editor. An editor already
     * showing the document for the data owner is used instead, so the document is never opened
     * twice. Afterwards, the editor showing the document is the active one.
     *
     * @param aTarget
     *            the AJAX target
     * @param aDocument
     *            the document to show
     * @param aDataOwner
     *            whose annotations to show
     * @param aRange
     *            where to scroll to
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    public void actionJumpToDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aDataOwner, Range aRange)
        throws IOException, AnnotationException
    {
        // A jump must not split the view: preferring the active editor makes it replace that
        // editor's document instead of filling an empty editor or adding one.
        getDocumentEditorManager().actionShowDocument(aTarget, getActiveEditor().orElse(null),
                aDocument, aDataOwner, aRange);
    }
}
