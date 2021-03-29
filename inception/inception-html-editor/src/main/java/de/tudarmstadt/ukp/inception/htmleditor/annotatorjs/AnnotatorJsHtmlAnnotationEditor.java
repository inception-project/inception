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
package de.tudarmstadt.ukp.inception.htmleditor.annotatorjs;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID.NONE_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toInterpretableJsonString;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

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
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
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
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.model.Annotation;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.resources.AnnotatorJsCssResourceReference;
import de.tudarmstadt.ukp.inception.htmleditor.annotatorjs.resources.AnnotatorJsJavascriptResourceReference;

public class AnnotatorJsHtmlAnnotationEditor
    extends HtmlAnnotationEditorImplBase
{
    private static final long serialVersionUID = -3358207848681467993L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean ColoringService coloringService;

    private StoreAdapter storeAdapter;

    public AnnotatorJsHtmlAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
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

        aResponse.render(CssHeaderItem.forReference(AnnotatorJsCssResourceReference.get()));
        aResponse.render(
                JavaScriptHeaderItem.forReference(AnnotatorJsJavascriptResourceReference.get()));

        if (getModelObject().getDocument() != null) {
            aResponse.render(OnDomReadyHeaderItem.forScript(initAnnotatorJs(vis, storeAdapter)));
        }
    }

    private String initAnnotatorJs(WebComponent aContainer, StoreAdapter aAdapter)
    {
        String callbackUrl = aAdapter.getCallbackUrl().toString();
        StringBuilder script = new StringBuilder();
        script.append(
                "var ann = $('#" + aContainer.getMarkupId() + "').annotator({readOnly: false});");
        script.append("ann.annotator('addPlugin', 'Store', {");
        script.append("    prefix: null,");
        script.append("    emulateJSON: true,");
        script.append("    emulateHTTP: true,");
        script.append("    urls: {");
        script.append("        read:    '" + callbackUrl + "',");
        script.append("        create:  '" + callbackUrl + "',");
        script.append("        update:  '" + callbackUrl + "',");
        script.append("        destroy: '" + callbackUrl + "',");
        script.append("        search:  '" + callbackUrl + "',");
        script.append("        select:  '" + callbackUrl + "'");
        script.append("    }");
        script.append("});");
        return WicketUtil.wrapInTryCatch(script.toString());
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        // REC: I didn't find a good way of clearing the annotations, so we do it the hard way:
        // - re-render the entire document
        // - re-add all the annotations
        aTarget.add(vis);
        aTarget.appendJavaScript(initAnnotatorJs(vis, storeAdapter));
    }

    private class StoreAdapter
        extends AbstractDefaultAjaxBehavior
    {
        private static final long serialVersionUID = -7919362960963563800L;

        @Override
        protected void respond(AjaxRequestTarget aTarget)
        {
            final IRequestParameters reqParams = getRequest().getRequestParameters();

            // We use "emulateHTTP" to get the method as a parameter - this makes it easier to
            // access the method without having to go to the native container request.
            String method = reqParams.getParameterValue("_method").toString();

            // We use "emulateJSON" to get the JSON payload as a parameter - again makes it
            // easier to access the payload without having to go to the native container request.
            String payload = reqParams.getParameterValue("json").toString();

            LOG.debug("[" + method + "]: " + payload);

            try {
                // Loading existing annotations
                if ("GET".equals(method)) {
                    read(aTarget);
                }

                // Update existing annotation
                if ("PUT".equals(method) && isNotEmpty(payload)) {
                    update(aTarget, payload);
                }

                // New annotation created
                if ("POST".equals(method) && isNotEmpty(payload)) {
                    create(aTarget, payload);
                }

                // Existing annotation deleted
                if ("DELETE".equals(method) && isNotEmpty(payload)) {
                    delete(aTarget, payload);
                }

                // Existing annotation deleted
                if ("HEAD".equals(method) && isNotEmpty(payload)) {
                    select(aTarget, payload);
                }
            }
            catch (Exception e) {
                handleError("Error", e);
            }
        }

        private void select(AjaxRequestTarget aTarget, String payload)
            throws JsonParseException, JsonMappingException, IOException
        {
            Annotation anno = JSONUtil.getObjectMapper().readValue(payload, Annotation.class);

            if (anno.getRanges().isEmpty()) {
                // Spurious creation event that is to be ignored.
                return;
            }

            VID paramId = VID.parse(anno.getId());

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

        private void create(AjaxRequestTarget aTarget, String payload)
            throws JsonParseException, JsonMappingException, IOException
        {
            Annotation anno = JSONUtil.getObjectMapper().readValue(payload, Annotation.class);

            if (anno.getRanges().isEmpty()) {
                // Spurious creation event that is to be ignored.
                return;
            }

            try {
                int begin = anno.getRanges().get(0).getStartOffset();
                int end = anno.getRanges().get(0).getEndOffset();

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

        private void delete(AjaxRequestTarget aTarget, String aPayload)
        {
            // We delete annotations via the detail sidebar, so this method is no needed.
        }

        private void update(AjaxRequestTarget aTarget, String aPayload)
        {
            // We update annotations via the detail sidebar, so this method is no needed.
        }

        private void read(AjaxRequestTarget aTarget)
            throws JsonParseException, JsonMappingException, IOException
        {
            CAS cas = getCasProvider().get();

            VDocument vdoc = render(cas, 0, cas.getDocumentText().length());

            AnnotatorJsRenderer renderer = new AnnotatorJsRenderer(coloringService,
                    annotationService);
            List<Annotation> annotations = renderer.render(getModelObject(), vdoc, cas, null);

            String json = toInterpretableJsonString(annotations);

            // Since we cannot pass the JSON directly to AnnotatorJS, we attach it to the HTML
            // element into which AnnotatorJS governs. In our modified annotator-full.js, we pick it
            // up from there and then pass it on to AnnotatorJS to do the rendering.
            aTarget.prependJavaScript("Wicket.$('" + vis.getMarkupId() + "').temp = " + json + ";");
        }
    }
}
