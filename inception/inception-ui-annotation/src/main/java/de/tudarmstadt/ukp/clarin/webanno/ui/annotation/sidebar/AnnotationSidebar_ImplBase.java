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

import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public abstract class AnnotationSidebar_ImplBase
    extends GenericPanel<AnnotatorState>
{
    private static final long serialVersionUID = 8637373389151630602L;

    private final AnnotationActionHandler actionHandler;
    private final CasProvider casProvider;
    private final AnnotationPageBase2 annotationPage;
    private @SpringBean DocumentService documentService;

    public AnnotationSidebar_ImplBase(final String aId,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider,
            AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aAnnotationPage.getModel());

        actionHandler = aActionHandler;
        casProvider = aCasProvider;
        annotationPage = aAnnotationPage;

        // Allow AJAX updates.
        setOutputMarkupId(true);

        // The sidebar is invisible when no document has been selected. Make sure that we can
        // make it visible via AJAX once the document has been selected.
        setOutputMarkupPlaceholderTag(true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    @Override
    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    public AnnotationActionHandler getActionHandler()
    {
        return actionHandler;
    }

    public CasProvider getCasProvider()
    {
        return casProvider;
    }

    public AnnotationPageBase2 getAnnotationPage()
    {
        return annotationPage;
    }
}
