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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation;

import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public abstract class AnnotationEditorBase
    extends Panel
{
    private static final long serialVersionUID = 8637373389151630602L;

    private AnnotationActionHandler actionHandler;
    private JCasProvider jcasProvider;
    
    public AnnotationEditorBase(final String aId, final IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final JCasProvider aJCasProvider)
    {
        super(aId, aModel);
        actionHandler = aActionHandler;
        jcasProvider = aJCasProvider;
        
        // Allow AJAX updates.
        setOutputMarkupId(true);

        // The annotator is invisible when no document has been selected. Make sure that we can
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
    
    public JCasProvider getJCasProvider()
    {
        return jcasProvider;
    }
    
    /**
     * Render the contents of the annotation editor again in this present AJAX request. This
     * typically happens by sending JavaScript commands including the complete data structures as
     * JSON via {@link AjaxRequestTarget#appendJavaScript(CharSequence)}.
     */
    public abstract void render(AjaxRequestTarget aTarget, JCas aJCas);

    /**
     * Request an asynchronous rendering of the annotation editor. This typically happens by
     * injecting a JavaScript command via {@link AjaxRequestTarget#appendJavaScript(CharSequence)}
     * that causes the browser-side code to request the data structures from the server.
     * <p>
     * This entails that the CAS is loaded again when the async rendering request from the browser
     * is triggered. Thus, it is preferred to use {@link #render(AjaxRequestTarget, JCas)} because
     * here we already have the CAS available.
     */
    public abstract void renderLater(AjaxRequestTarget aTarget);

    /**
     * Put some focus/highlight on the annotation with the specified visual ID. This typically
     * happens by sending a suitable JavaScript command via
     * {@link AjaxRequestTarget#appendJavaScript(CharSequence)}.
     * <p>
     * It should not matter if this call is made before or after the call to
     * {@link #render(AjaxRequestTarget, JCas)} or {@link #renderLater(AjaxRequestTarget)}.
     */
    public abstract void setHighlight(AjaxRequestTarget aTarget, VID aAnnotationId);
}
