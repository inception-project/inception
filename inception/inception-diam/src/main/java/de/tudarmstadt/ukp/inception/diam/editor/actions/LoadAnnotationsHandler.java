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
package de.tudarmstadt.ukp.inception.diam.editor.actions;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toInterpretableJsonString;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.RenderRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.RenderingPipeline;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.VDocumentSerializer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.VDocumentSerializerExtensionPoint;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamEditorAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DiamEditorAutoConfig#loadAnnotationsHandler}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_RENDER_HANDLER)
public class LoadAnnotationsHandler
    extends EditorAjaxRequestHandlerBase
{
    public static final String COMMAND = "loadAnnotations";

    public static final String PARAM_FORMAT = "format";
    public static final String PARAM_TOKEN = "token";

    private final RenderingPipeline renderingPipeline;
    private final VDocumentSerializerExtensionPoint vDocumentSerializerExtensionPoint;

    public LoadAnnotationsHandler(RenderingPipeline aRenderingPipeline,
            VDocumentSerializerExtensionPoint aVDocumentSerializerExtensionPoint)
    {
        renderingPipeline = aRenderingPipeline;
        vDocumentSerializerExtensionPoint = aVDocumentSerializerExtensionPoint;
    }

    @Override
    public String getCommand()
    {
        return COMMAND;
    }

    @Override
    public AjaxResponse handle(AjaxRequestTarget aTarget, Request aRequest)
    {
        try {
            AnnotationPageBase page = (AnnotationPageBase) aTarget.getPage();

            AnnotatorState state = getAnnotatorState();
            RenderRequest request = RenderRequest.builder() //
                    .withState(state) //
                    .withCas(page.getEditorCas()) //
                    .withWindow(state.getWindowBeginOffset(), state.getWindowEndOffset()) //
                    .withVisibleLayers(state.getAnnotationLayers()) //
                    .build();

            VDocument vdoc = renderingPipeline.render(request);

            String format = aRequest.getRequestParameters().getParameterValue(PARAM_FORMAT)
                    .toString();

            VDocumentSerializer<?> ser = vDocumentSerializerExtensionPoint.getExtension(format)
                    .orElseThrow();

            String token = aRequest.getRequestParameters().getParameterValue(PARAM_TOKEN)
                    .toString();
            String json = toInterpretableJsonString(ser.render(vdoc, request));
            aTarget.prependJavaScript(
                    "document['DIAM_TRANSPORT_BUFFER']['" + token + "'] = " + json + ";");
            return new DefaultAjaxResponse();
        }
        catch (Exception e) {
            return handleError("Unable to load data", e);
        }
    }
}
