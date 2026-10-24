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
package de.tudarmstadt.ukp.inception.ui.core;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.wicket.settings.ExceptionSettings.AjaxErrorStrategy.REDIRECT_TO_ERROR_PAGE;

import org.apache.wicket.Application;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.protocol.http.servlet.ErrorAttributes;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;

public class ErrorListener
    implements IRequestCycleListener
{
    public static final MetaDataKey<Exception> EXCEPTION = new MetaDataKey<Exception>()
    {
        private static final long serialVersionUID = 1L;
    };

    @Override
    public void onBeginRequest(RequestCycle aCycle)
    {
        // Apply the application's AJAX error strategy also to failures Wicket does not handle
        // itself (an Error escaping the request cycle, or a container-level error such as a 403).
        // These reach the error page through the container's error dispatch, which keeps the error
        // status. With REDIRECT_TO_ERROR_PAGE, Wicket answers the AJAX request with a redirect to
        // the error page, but the Wicket AJAX client ignores redirects in error responses, leaving
        // the user with a page that silently stopped working. Use the status Wicket itself uses
        // for AJAX redirects so the client follows the redirect, as it does for exceptions.
        if (aCycle.getRequest() instanceof ServletWebRequest request && request.isAjax()
                && ErrorAttributes.of(request.getContainerRequest(),
                        request.getFilterPrefix()) != null
                && Application.get().getExceptionSettings()
                        .getAjaxErrorHandlingStrategy() == REDIRECT_TO_ERROR_PAGE
                && aCycle.getResponse() instanceof WebResponse response) {
            response.setStatus(SC_OK);
        }
    }

    @Override
    public IRequestHandler onException(RequestCycle aCycle, Exception aEx)
    {
        aCycle.setMetaData(EXCEPTION, aEx);
        return IRequestCycleListener.super.onException(aCycle, aEx);
    }
}
