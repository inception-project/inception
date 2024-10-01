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
package de.tudarmstadt.ukp.clarin.webanno.security;

import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class WicketSecurityUtils
{
    public static String getCsrfTokenFromSession()
    {
        var httpRequest = (HttpServletRequest) RequestCycle.get().getRequest()
                .getContainerRequest();

        var token = (CsrfToken) httpRequest.getAttribute("_csrf");
        if (token != null) {
            return token.getToken();
        }

        var httpResponse = (HttpServletResponse) RequestCycle.get().getResponse()
                .getContainerResponse();

        var csrfTokenRepository = new HttpSessionCsrfTokenRepository();
        var csrfToken = csrfTokenRepository.loadDeferredToken(httpRequest, httpResponse);

        if (csrfToken != null) {
            return csrfToken.get().getToken();
        }
        else {
            return "";
        }
    }

    /**
     * Checks if auto-logout is enabled. For Winstone, we get a max session length of 0, so here it
     * is disabled.
     */
    public static int getAutoLogoutTime()
    {
        int duration = 0;
        var request = RequestCycle.get().getRequest();
        if (request instanceof ServletWebRequest servletRequest) {
            var session = servletRequest.getContainerRequest().getSession();
            if (session != null) {
                duration = session.getMaxInactiveInterval();
            }
        }
        return duration;
    }
}
