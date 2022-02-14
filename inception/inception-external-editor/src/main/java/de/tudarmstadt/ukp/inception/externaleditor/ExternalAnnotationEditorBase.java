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
package de.tudarmstadt.ukp.inception.externaleditor;

import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil.wrapInTryCatch;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.DocumentViewExtensionPoint;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.DiamJavaScriptReference;
import de.tudarmstadt.ukp.inception.externaleditor.model.AnnotationEditorProperties;
import de.tudarmstadt.ukp.inception.externaleditor.resources.ExternalEditorJavascriptResourceReference;

public abstract class ExternalAnnotationEditorBase
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -196999336741495239L;

    protected static final String MID_VIS = "vis";

    private @SpringBean DocumentViewExtensionPoint documentViewExtensionPoint;
    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;
    private @SpringBean ServletContext context;

    private DiamAjaxBehavior diamBehavior;
    private Component vis;

    public ExternalAnnotationEditorBase(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        setOutputMarkupPlaceholderTag(true);

        add(diamBehavior = new DiamAjaxBehavior());

        vis = makeView();
        vis.setOutputMarkupPlaceholderTag(true);
        add(vis);
    }
    
    protected Component getViewComponent() {
        return vis;
    }

    public DiamAjaxBehavior getDiamBehavior()
    {
        return diamBehavior;
    }

    protected abstract Component makeView();

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(JavaScriptHeaderItem.forReference(DiamJavaScriptReference.get()));

        aResponse.render(
                JavaScriptHeaderItem.forReference(ExternalEditorJavascriptResourceReference.get()));

        if (getModelObject().getDocument() != null && getProperties() != null) {
            aResponse.render(OnDomReadyHeaderItem.forScript(initScript()));
        }
    }

    @Override
    protected void onRemove()
    {
        getRequestCycle().find(IPartialPageRequestHandler.class)
                .ifPresent(target -> target.prependJavaScript(destroyScript()));

        super.onRemove();
    }

    protected abstract AnnotationEditorProperties getProperties();

    private String getPropertiesAsJson()
    {
        AnnotationEditorProperties props = getProperties();
        try {
            return JSONUtil.toInterpretableJsonString(props);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private CharSequence destroyScript()
    {
        return wrapInTryCatch(
                "ExternalEditor.destroy(document.getElementById('" + vis.getMarkupId() + "'));");
    }

    private String initScript()
    {
        return wrapInTryCatch("ExternalEditor.getOrInitialize(document.getElementById('"
                + vis.getMarkupId() + "'), Diam.factory(), " + getPropertiesAsJson() + ");");
    }

    private String renderScript()
    {
        return wrapInTryCatch("ExternalEditor.getOrInitialize(document.getElementById('"
                + vis.getMarkupId() + "'), Diam.factory(), " + getPropertiesAsJson()
                + ").then(e => e.loadAnnotations());");
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(renderScript());
    }
}
