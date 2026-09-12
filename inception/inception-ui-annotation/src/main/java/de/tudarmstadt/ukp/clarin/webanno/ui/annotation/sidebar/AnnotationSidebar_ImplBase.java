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

import java.util.Optional;

import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;

public abstract class AnnotationSidebar_ImplBase
    extends GenericPanel<AnnotatorState>
{
    private static final long serialVersionUID = 8637373389151630602L;

    private final AnnotationPageBase2 annotationPage;
    private @SpringBean DocumentService documentService;

    public AnnotationSidebar_ImplBase(final String aId, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aAnnotationPage.getModel());

        annotationPage = aAnnotationPage;

        setOutputMarkupPlaceholderTag(true);
    }

    public AnnotationPageBase2 getAnnotationPage()
    {
        return annotationPage;
    }

    /**
     * @return the editor the user is currently working in, or {@link Optional#empty()} if the page
     *         has no editor.
     */
    public Optional<DiamContext> getActiveContext()
    {
        return annotationPage.getActiveContext();
    }

    /**
     * @return the document editor manager for the page.
     */
    public DocumentEditorManager getDocumentEditorManager()
    {
        return annotationPage;
    }
}
