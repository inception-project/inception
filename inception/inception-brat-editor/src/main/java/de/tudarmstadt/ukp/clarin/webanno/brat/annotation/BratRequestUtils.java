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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratProtocolNames.PARAM_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratProtocolNames.PARAM_ARC_ID;
import static de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratProtocolNames.PARAM_ID;

import java.io.IOException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.IRequestParameters;

import com.fasterxml.jackson.databind.JsonNode;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.ArcAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.SpanAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

public class BratRequestUtils
{
    public static String getActionFromRequest(IRequestParameters request)
    {
        return request.getParameterValue(PARAM_ACTION).toString();
    }

    /**
     * Parse annotation ID if present in request
     */
    public static VID getVidFromRequest(IRequestParameters request)
    {
        String action = getActionFromRequest(request);
        final VID paramId;
        if (!request.getParameterValue(PARAM_ID).isEmpty()
                && !request.getParameterValue(PARAM_ARC_ID).isEmpty()) {
            throw new IllegalStateException(
                    "[id] and [arcId] cannot be both set at the same time!");
        }
        else if (!request.getParameterValue(PARAM_ID).isEmpty()) {
            paramId = VID.parseOptional(request.getParameterValue(PARAM_ID).toString());
        }
        else {
            VID arcId = VID.parseOptional(request.getParameterValue(PARAM_ARC_ID).toString());
            // HACK: If an arc was clicked that represents a link feature, then
            // open the associated span annotation instead.
            if (arcId.isSlotSet() && ArcAnnotationResponse.is(action)) {
                action = SpanAnnotationResponse.COMMAND;
                paramId = new VID(arcId.getId());
            }
            else {
                paramId = arcId;
            }
        }
        return paramId;
    }

    public static void attachResponse(AjaxRequestTarget aTarget, Component vis, Object result)
        throws IOException
    {
        String json;
        if (result instanceof String) {
            json = (String) result;
        }
        else {
            json = toJson(result);
        }

        // Since we cannot pass the JSON directly to Brat, we attach it to the HTML
        // element into which BRAT renders the SVG. In our modified ajax.js, we pick it
        // up from there and then pass it on to BRAT to do the rendering.
        aTarget.prependJavaScript("Wicket.$('" + vis.getMarkupId() + "').temp = " + json + ";");

    }

    private static String toJson(Object result) throws IOException
    {
        String json = "[]";
        if (result instanceof JsonNode) {
            json = JSONUtil.toInterpretableJsonString((JsonNode) result);
        }
        else {
            json = JSONUtil.toInterpretableJsonString(result);
        }
        return json;
    }

}
