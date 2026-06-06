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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.login;

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.login.OAuth2LoginPanel.toAutoLoginRedirectUrl;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Reproduces the auto-login redirect loop that occurs when INCEPTION is served under a non-root
 * {@code server.servlet.context-path} (e.g. {@code /inception}) behind a reverse proxy.
 * <p>
 * The login button works because it is rendered as a verbatim
 * {@link org.apache.wicket.markup.html.link.ExternalLink} and therefore needs a context-absolute
 * URL. Auto-login instead hands that same URL to
 * {@link org.apache.wicket.request.flow.RedirectToUrlException}, whose
 * {@code RedirectRequestHandler} treats any URL starting with {@code '/'} as context-relative and
 * re-applies the servlet context path via {@code UrlRenderer#renderContextRelativeUrl}. So a
 * context-absolute URL gets the context path a second time
 * ({@code /inception/inception/oauth2/...}) and login bounces between {@code /login.html} and
 * {@code /oauth2/authorization/...} forever.
 * <p>
 * The redirect URL handed to {@code RedirectToUrlException} must therefore be context-relative.
 */
class OAuth2LoginPanelTest
{
    private static final String CONTEXT_PATH = "/inception";
    private static final String REGISTRATION_ID = "oidc";

    // The context-absolute URL the login button renders verbatim (built by getLoginLinks()).
    private static final String LOGIN_URL = CONTEXT_PATH + "/oauth2/authorization/"
            + REGISTRATION_ID;

    @Test
    void thatAutoLoginRedirectDoesNotDoubleTheServletContextPath()
    {
        var redirectUrl = toAutoLoginRedirectUrl(CONTEXT_PATH, LOGIN_URL);

        assertThat(renderRedirectLocation(CONTEXT_PATH, redirectUrl)) //
                .as("Wicket re-applies the context path on redirect, so the auto-login target must "
                        + "be context-relative; otherwise the context path is doubled and login "
                        + "enters an infinite redirect loop")
                .isEqualTo("/inception/oauth2/authorization/oidc");
    }

    @Test
    void thatRootContextIsUnaffected()
    {
        var loginUrlAtRoot = "/oauth2/authorization/" + REGISTRATION_ID;

        var redirectUrl = toAutoLoginRedirectUrl("", loginUrlAtRoot);

        assertThat(renderRedirectLocation("", redirectUrl)) //
                .isEqualTo("/oauth2/authorization/oidc");
    }

    /**
     * Faithful model of Apache Wicket 10.9.1 {@code RedirectRequestHandler#respond} +
     * {@code UrlRenderer#renderContextRelativeUrl} for a request issued at the context root: a URL
     * starting with {@code '/'} has that slash stripped and the servlet context path prepended (a
     * URL that does not start with {@code '/'} is emitted unchanged).
     */
    private static String renderRedirectLocation(String aContextPath, String aRedirectUrl)
    {
        if (aRedirectUrl.isEmpty() || aRedirectUrl.charAt(0) != '/') {
            return aRedirectUrl;
        }

        return removeEnd(aContextPath, "/") + "/" + aRedirectUrl.substring(1);
    }
}
