/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import java.io.IOException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;

public abstract class AnnotationSidebar_ImplBase
    extends Panel
{
    private static final long serialVersionUID = 8637373389151630602L;

    private AnnotationActionHandler actionHandler;
    private CasProvider casProvider;
    private AnnotationPage annotationPage;
    private @SpringBean DocumentService documentService;

    public AnnotationSidebar_ImplBase(final String aId, final IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel);
        actionHandler = aActionHandler;
        casProvider = aCasProvider;

        annotationPage = aAnnotationPage;

        // Allow AJAX updates.
        setOutputMarkupId(true);

        // The sidebar is invisible when no document has been selected. Make sure that we can
        // make it visible via AJAX once the document has been selected.
        setOutputMarkupPlaceholderTag(true);
    }

    public void setModel(IModel<AnnotatorState> aModel)
    {
        setDefaultModel(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    public void setModelObject(AnnotatorState aModel)
    {
        setDefaultModelObject(aModel);
    }

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

    public AnnotationPage getAnnotationPage()
    {
        return annotationPage;
    }
    
    /**
     * Show the next document if it exists, starting in a certain begin offset
     */
    protected void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            int aBegin, int aEnd)
        throws IOException
    {
        getAnnotationPage().actionShowSelectedDocument(aTarget, aDocument, aBegin, aEnd);
    }
}
