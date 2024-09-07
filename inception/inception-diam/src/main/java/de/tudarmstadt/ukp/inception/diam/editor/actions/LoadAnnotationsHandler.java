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

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toInterpretableJsonString;

import java.io.IOException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.compact.CompactSerializerImpl;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderingPipeline;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.serialization.VDocumentSerializer;
import de.tudarmstadt.ukp.inception.rendering.vmodel.serialization.VDocumentSerializerExtensionPoint;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link DiamAutoConfig#loadAnnotationsHandler}.
 * </p>
 */
@Order(EditorAjaxRequestHandler.PRIO_RENDER_HANDLER)
public class LoadAnnotationsHandler
    extends EditorAjaxRequestHandlerBase
{
    public static final String COMMAND = "loadAnnotations";

    public static final String PARAM_FORMAT = "format";
    public static final String PARAM_BEGIN = "begin";
    public static final String PARAM_END = "end";
    public static final String PARAM_TEXT = "text";
    public static final String PARAM_CLIP_SPANS = "clip";
    public static final String PARAM_CLIP_ARCS = "clipArcs";
    public static final String PARAM_LONG_ARCS = "longArcs";

    private final RenderingPipeline renderingPipeline;
    private final VDocumentSerializerExtensionPoint vDocumentSerializerExtensionPoint;
    private final UserDao userService;

    public LoadAnnotationsHandler(RenderingPipeline aRenderingPipeline,
            VDocumentSerializerExtensionPoint aVDocumentSerializerExtensionPoint,
            UserDao aUserService)
    {
        renderingPipeline = aRenderingPipeline;
        vDocumentSerializerExtensionPoint = aVDocumentSerializerExtensionPoint;
        userService = aUserService;
    }

    @Override
    public String getCommand()
    {
        return COMMAND;
    }

    @Override
    public AjaxResponse handle(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
            Request aRequest)
    {
        try {
            var request = prepareRenderRequest(aRequest);
            var vdoc = renderingPipeline.render(request);
            var json = serializeToWireFormat(aRequest, request, vdoc);
            attachResponse(aTarget, aRequest, json);
            return new DefaultAjaxResponse();
        }
        catch (Exception e) {
            return handleError("Unable to load annotations", e);
        }
    }

    private RenderRequest prepareRenderRequest(Request aRequest) throws IOException
    {
        var page = getPage();
        var state = getAnnotatorState();

        var begin = aRequest.getRequestParameters().getParameterValue(PARAM_BEGIN)
                .toInt(state.getWindowBeginOffset());
        var end = aRequest.getRequestParameters().getParameterValue(PARAM_END)
                .toInt(state.getWindowEndOffset());
        var includeText = aRequest.getRequestParameters().getParameterValue(PARAM_TEXT)
                .toBoolean(true);
        var clipSpans = aRequest.getRequestParameters().getParameterValue(PARAM_CLIP_SPANS)
                .toBoolean(true);
        var clipArcs = aRequest.getRequestParameters().getParameterValue(PARAM_CLIP_ARCS)
                .toBoolean(true);
        var longArcs = aRequest.getRequestParameters().getParameterValue(PARAM_LONG_ARCS)
                .toBoolean(false);

        return RenderRequest.builder() //
                .withState(state) //
                .withSessionOwner(userService.getCurrentUser()) //
                .withCas(page.getEditorCas()) //
                .withWindow(begin, end) //
                .withText(includeText) //
                .withClipSpans(clipSpans) //
                .withClipArcs(clipArcs) //
                .withLongArcs(longArcs) //
                .withVisibleLayers(state.getAnnotationLayers()) //
                .build();
    }

    private String serializeToWireFormat(Request aRequest, RenderRequest request, VDocument vdoc)
        throws IOException
    {
        String format = aRequest.getRequestParameters().getParameterValue(PARAM_FORMAT)
                .toString(CompactSerializerImpl.ID);

        VDocumentSerializer<?> ser = vDocumentSerializerExtensionPoint.getExtension(format)
                .orElseThrow(
                        () -> new IllegalArgumentException("Unknown serializer: [" + format + "]"));

        return toInterpretableJsonString(ser.render(vdoc, request));
    }
}
