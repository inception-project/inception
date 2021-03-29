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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID.NONE_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toInterpretableJsonString;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.io.IOException;
import java.time.Duration;

import javax.servlet.http.HttpServletRequest;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebComponent;
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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.inception.htmleditor.HtmlAnnotationEditorImplBase;
import de.tudarmstadt.ukp.inception.recogitojseditor.model.WebAnnotation;
import de.tudarmstadt.ukp.inception.recogitojseditor.model.WebAnnotationTextPositionSelector;
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
            aResponse.render(OnDomReadyHeaderItem.forScript(initRecogitoJs(vis, storeAdapter)));
        }
    }

    private String initRecogitoJs(WebComponent aContainer, StoreAdapter aAdapter)
    {
        String markupId = aContainer.getMarkupId();
        String callbackUrl = aAdapter.getCallbackUrl().toString();
        StringBuilder script = new StringBuilder();
        script.append("(function() {\n");
        script.append("  var r = Recogito.init({\n");
        script.append("    content: document.getElementById('" + markupId + "'),\n");
        script.append("    mode: 'pre'\n");
        script.append("  });\n");
        script.append("\n");
        script.append("  r.on('createAnnotation', function(annotation) { \n");
        script.append("    Wicket.Ajax.ajax({\n");
        script.append("      'm' : 'POST',\n");
        script.append("      'c' : '" + markupId + "',\n");
        script.append("      'u' : '" + callbackUrl + "',\n");
        script.append("      'ep' : {\n");
        script.append("        'action': 'createAnnotation',\n");
        script.append("        'annotation': JSON.stringify(annotation)\n");
        script.append("      }});\n");
        script.append("  \n");
        script.append("  });\n");
        script.append("  r.on('selectAnnotation', function(annotation) { \n");
        script.append("    Wicket.Ajax.ajax({\n");
        script.append("      'm' : 'POST',\n");
        script.append("      'c' : '" + markupId + "',\n");
        script.append("      'u' : '" + callbackUrl + "',\n");
        script.append("      'ep' : {\n");
        script.append("        'action': 'selectAnnotation',\n");
        script.append("        'id': annotation.id\n");
        script.append("      }});\n");
        script.append("  \n");
        script.append("  });\n");
        script.append("  r.loadAnnotations('" + callbackUrl + "');\n");
        script.append("})();");
        return WicketUtil.wrapInTryCatch(script.toString());
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        // REC: I didn't find a good way of clearing the annotations, so we do it the hard way:
        // - re-render the entire document
        // - re-add all the annotations
        aTarget.add(vis);
        aTarget.appendJavaScript(initRecogitoJs(vis, storeAdapter));
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
                // Loading existing annotations
                switch (request.getMethod()) {
                case "GET":
                    read(aTarget);
                    break;
                case "POST": {
                    String action = getRequest().getRequestParameters().getParameterValue("action")
                            .toString();
                    switch (action) {
                    case "selectAnnotation":
                        select(aTarget);
                        break;
                    case "createAnnotation":
                        create(aTarget);
                        break;
                    }
                }
                }
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

            RecogitoJsRenderer renderer = new RecogitoJsRenderer(coloringService, annotationService);
            WebAnnotations annotations = renderer.render(getModelObject(), vdoc, cas, null);
            
            String json = toInterpretableJsonString(annotations);

            StringResourceStream resource = new StringResourceStream(json, "application/ld+json");

            ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(resource);
            handler.setFileName("data.json");
            handler.setCacheDuration(Duration.ofSeconds(1));
            handler.setContentDisposition(ContentDisposition.INLINE);

            getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
        }

        private void select(AjaxRequestTarget aTarget)
            throws JsonParseException, JsonMappingException, IOException
        {
            VID paramId = VID
                    .parse(getRequest().getRequestParameters().getParameterValue("id").toString());

            try {
                CAS cas = getCasProvider().get();

                if (paramId.isSynthetic()) {
                    extensionRegistry.fireAction(getActionHandler(), getModelObject(), aTarget, cas,
                            paramId, "spanOpenDialog");
                    return;
                }

                AnnotationFS fs = selectByAddr(cas, AnnotationFS.class, paramId.getId());

                AnnotatorState state = getModelObject();
                if (state.isSlotArmed()) {
                    // When filling a slot, the current selection is *NOT* changed. The
                    // Span annotation which owns the slot that is being filled remains
                    // selected!
                    getActionHandler().actionFillSlot(aTarget, cas, fs.getBegin(), fs.getEnd(),
                            paramId);
                }
                else {
                    state.getSelection().selectSpan(paramId, cas, fs.getBegin(), fs.getEnd());
                    getActionHandler().actionSelect(aTarget);
                }
            }
            catch (AnnotationException | IOException e) {
                handleError("Unable to select span annotation", e);
            }
        }

        private void create(AjaxRequestTarget aTarget)
            throws JsonParseException, JsonMappingException, IOException
        {
            WebAnnotation anno = JSONUtil.fromJsonString(WebAnnotation.class,
                    getRequest().getRequestParameters().getParameterValue("annotation").toString());

            try {
                WebAnnotationTextPositionSelector selector = anno.getTarget().stream() //
                        .filter(t -> isNotEmpty(t.getSelector()))
                        .flatMap(t -> t.getSelector().stream())
                        .filter(s -> s instanceof WebAnnotationTextPositionSelector)
                        .map(s -> (WebAnnotationTextPositionSelector) s).findFirst().get();
                int begin = selector.getStart();
                int end = selector.getEnd();

                if (!(begin > -1 && end > -1)) {
                    throw new AnnotationException(
                            "Unable to create span annotation: No match was found");
                }

                CAS cas = getCasProvider().get();
                AnnotatorState state = getModelObject();
                if (state.isSlotArmed()) {
                    // When filling a slot, the current selection is *NOT* changed. The
                    // Span annotation which owns the slot that is being filled remains
                    // selected!
                    getActionHandler().actionFillSlot(aTarget, cas, begin, end, NONE_ID);
                }
                else {
                    state.getSelection().selectSpan(cas, begin, end);
                    getActionHandler().actionCreateOrUpdate(aTarget, cas);
                }
            }
            catch (IOException | AnnotationException e) {
                handleError("Unable to create span annotation", e);
            }
        }
    }
}
