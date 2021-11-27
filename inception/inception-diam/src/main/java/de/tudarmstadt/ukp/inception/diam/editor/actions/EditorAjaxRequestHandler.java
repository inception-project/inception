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

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.Request;

import de.tudarmstadt.ukp.clarin.webanno.support.extensionpoint.Extension;

public interface EditorAjaxRequestHandler
    extends Extension<Request>
{
    public static final String PARAM_COMMAND = "cmd";
    public static final String PARAM_VID = "vid";

    public static final String PARAM_ACTION = "action";
    public static final String PARAM_ARC_ID = "arcId";
    public static final String PARAM_ID = "id";
    public static final String PARAM_OFFSETS = "offsets";
    public static final String PARAM_TARGET_SPAN_ID = "targetSpanId";
    public static final String PARAM_ORIGIN_SPAN_ID = "originSpanId";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_LAZY_DETAIL_DATABASE = "database";
    public static final String PARAM_LAZY_DETAIL_KEY = "key";

    String ACTION_CONTEXT_MENU = "contextMenu";

    default String getRequestMethod(Request aRequest)
    {
        if (!(aRequest.getContainerRequest() instanceof HttpServletRequest)) {
            throw new IllegalArgumentException("Request is not a HttpServletRequest");
        }

        HttpServletRequest request = (HttpServletRequest) aRequest.getContainerRequest();

        return request.getMethod();
    }

    String getCommand();

    @Override
    default boolean accepts(Request aRequest)
    {
        return getCommand().equals(aRequest.getRequestParameters().getParameterValue(PARAM_COMMAND)
                .toOptionalString());
    }

    void handle(AjaxRequestTarget aTarget, Request aRequest);
}
