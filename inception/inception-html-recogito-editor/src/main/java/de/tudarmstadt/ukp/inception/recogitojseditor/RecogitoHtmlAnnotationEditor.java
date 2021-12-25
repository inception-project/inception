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

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toInterpretableJsonString;
import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil.wrapInTryCatch;

import java.io.IOException;
import java.time.Duration;

import javax.servlet.http.HttpServletRequest;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.StringResourceStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandlerExtensionPoint;
import de.tudarmstadt.ukp.inception.htmleditor.HtmlAnnotationEditorImplBase;
import de.tudarmstadt.ukp.inception.recogitojseditor.model.WebAnnotations;
import de.tudarmstadt.ukp.inception.recogitojseditor.resources.RecogitoJsCssResourceReference;
import de.tudarmstadt.ukp.inception.recogitojseditor.resources.RecogitoJsJavascriptResourceReference;

public class RecogitoHtmlAnnotationEditor
    extends HtmlAnnotationEditorImplBase
{
    private static final long serialVersionUID = -3358207848681467993L;
    private static final Logger LOG = LoggerFactory.getLogger(RecogitoHtmlAnnotationEditor.class);

    private StoreAdapter storeAdapter;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean ColoringService coloringService;
    private @SpringBean EditorAjaxRequestHandlerExtensionPoint handlers;

    public RecogitoHtmlAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);

        storeAdapter = new StoreAdapter();
        add(storeAdapter);
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
        String callbackUrl = storeAdapter.getCallbackUrl().toString();
        return wrapInTryCatch(
                "RecogitoEditor.getInstance('" + vis.getMarkupId() + "', '" + callbackUrl + "');");
    }

    private String renderScript()
    {
        String callbackUrl = storeAdapter.getCallbackUrl().toString();
        return wrapInTryCatch(
                "RecogitoEditor.getInstance('" + vis.getMarkupId() + "', '" + callbackUrl + "').loadAnnotations();");
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(renderScript());
    }

    private class StoreAdapter
        extends AbstractDefaultAjaxBehavior
    {
        private static final long serialVersionUID = -7919362960963563800L;

        @Override
        protected void respond(AjaxRequestTarget aTarget)
        {
            if (!(getRequest().getContainerRequest() instanceof HttpServletRequest)) {
                return;
            }

            HttpServletRequest request = (HttpServletRequest) getRequest().getContainerRequest();

            LOG.debug("[" + request.getMethod() + "]");

            try {
                if ("GET".equals(request.getMethod())) {
                    read(aTarget);
                    return;
                }

                handlers.getHandler(getRequest()) //
                        .map(handler -> handler.handle(aTarget, getRequest())) //
                        .orElse(null);
            }
            catch (Exception e) {
                handleError("Error", e);
            }
        }

        private void read(AjaxRequestTarget aTarget)
            throws JsonParseException, JsonMappingException, IOException
        {
            CAS cas = getCasProvider().get();

            VDocument vdoc = render(cas, 0, cas.getDocumentText().length());
            
            RecogitoJsRenderer renderer = new RecogitoJsRenderer();
            WebAnnotations annotations = renderer.render(getModelObject(), vdoc, cas);

            String json = toInterpretableJsonString(annotations);

            StringResourceStream resource = new StringResourceStream(json, "application/ld+json");

            ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(resource);
            handler.setFileName("data.json");
            handler.setCacheDuration(Duration.ofSeconds(1));
            handler.setContentDisposition(ContentDisposition.INLINE);

            LOG.trace("Sending back RecogitoJS JSON data");

            getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
        }
    }
}
