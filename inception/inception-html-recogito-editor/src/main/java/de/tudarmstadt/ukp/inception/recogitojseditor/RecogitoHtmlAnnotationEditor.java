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
package de.tudarmstadt.ukp.inception.recogitojseditor;

import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil.wrapInTryCatch;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.htmleditor.HtmlAnnotationEditorImplBase;
import de.tudarmstadt.ukp.inception.recogitojseditor.resources.RecogitoJsCssResourceReference;
import de.tudarmstadt.ukp.inception.recogitojseditor.resources.RecogitoJsJavascriptResourceReference;

public class RecogitoHtmlAnnotationEditor
    extends HtmlAnnotationEditorImplBase
{
    private static final long serialVersionUID = -3358207848681467993L;

    private DiamAjaxBehavior diamBehavior;
    
    public RecogitoHtmlAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);

        add(diamBehavior = new DiamAjaxBehavior());
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(CssHeaderItem.forReference(RecogitoJsCssResourceReference.get()));
        aResponse.render(
                JavaScriptHeaderItem.forReference(RecogitoJsJavascriptResourceReference.get()));

        if (getModelObject().getDocument() != null) {
            aResponse.render(OnDomReadyHeaderItem.forScript(initScript()));
        }
    }

    @Override
    protected void onRemove()
    {
        super.onRemove();

        getRequestCycle().find(IPartialPageRequestHandler.class)
                .ifPresent(target -> target.prependJavaScript(destroyScript()));
    }

    private CharSequence destroyScript()
    {
        return wrapInTryCatch("RecogitoEditor.destroy('" + vis.getMarkupId() + "');");
    }

    private String initScript()
    {
        String callbackUrl = diamBehavior.getCallbackUrl().toString();
        return wrapInTryCatch(
                "RecogitoEditor.getInstance('" + vis.getMarkupId() + "', '" + callbackUrl + "');");
    }

    private String renderScript()
    {
        String callbackUrl = diamBehavior.getCallbackUrl().toString();
        return wrapInTryCatch(
                "RecogitoEditor.getInstance('" + vis.getMarkupId() + "', '" + callbackUrl + "').loadAnnotations();");
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(renderScript());
    }
}
