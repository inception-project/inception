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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;

import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;
import jakarta.servlet.http.HttpServletRequest;

public interface EditorAjaxRequestHandler
    extends Extension<Request>
{
    int PRIO_CONTEXT_MENU = -10;
    int PRIO_RENDER_HANDLER = 0;
    int PRIO_NAVIGATE_HANDLER = 50;
    int PRIO_SLOT_FILLER_HANDLER = 100;
    int PRIO_UNARM_SLOT_HANDLER = 180;
    int PRIO_EXTENSION_HANDLER = 190;
    int PRIO_ANNOTATION_HANDLER = 200;

    String PARAM_ACTION = "action";
    String PARAM_ARC_ID = "arcId";
    String PARAM_ID = "id";
    String PARAM_DOCUMENT_ID = "docId";
    String PARAM_SCROLL_TO = "scrollTo";
    String PARAM_OFFSETS = "offsets";
    String PARAM_TARGET_SPAN_ID = "targetSpanId";
    String PARAM_ORIGIN_SPAN_ID = "originSpanId";
    String PARAM_LAYER_ID = "layerId";

    String ACTION_CONTEXT_MENU = "contextMenu";

    default String getRequestMethod(Request aRequest)
    {
        if (!(aRequest.getContainerRequest() instanceof HttpServletRequest)) {
            throw new IllegalArgumentException("Request is not a HttpServletRequest");
        }

        var request = (HttpServletRequest) aRequest.getContainerRequest();

        return request.getMethod();
    }

    String getCommand();

    @Override
    default boolean accepts(Request aRequest)
    {
        return getCommand().equals(
                aRequest.getRequestParameters().getParameterValue(PARAM_ACTION).toOptionalString());
    }

    AjaxResponse handle(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget, Request aRequest);
}
