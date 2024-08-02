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

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import de.tudarmstadt.ukp.clarin.webanno.security.config.LoginProperties;
import de.tudarmstadt.ukp.inception.security.oauth.OAuth2Adapter;
import jakarta.servlet.ServletContext;

public class OAuth2LoginPanel
    extends Panel
{
    private static final long serialVersionUID = -3709147438732584586L;

    private @SpringBean LoginProperties loginProperties;
    private @SpringBean OAuth2Adapter oAuth2Adapter;
    private @SpringBean ServletContext servletContext;

    public OAuth2LoginPanel(String aId)
    {
        super(aId);

        var loginLinks = getLoginLinks();

        add(new ListView<LoginLink>("clients", loginLinks)
        {
            private static final long serialVersionUID = 3596608487017547416L;

            @Override
            protected void populateItem(ListItem<LoginLink> aItem)
            {
                aItem.queue(
                        new Label("clientName", aItem.getModel().map(LoginLink::getClientName)));
                aItem.queue(
                        new ExternalLink("loginUrl", aItem.getModel().map(LoginLink::getLoginUrl)));
            }
        });

        setVisibilityAllowed(!loginLinks.isEmpty());
    }

    /**
     * Perform auto-login via OAuth2 if an auto-login is configured.
     */
    public void autoLogin()
    {
        if (isBlank(loginProperties.getAutoLogin())) {
            return;
        }

        var loginLinks = getLoginLinks();
        var maybeAutoLoginTarget = loginLinks.stream() //
                .filter(link -> loginProperties.getAutoLogin().equals(link.getRegistrationId()))
                .findFirst();

        if (maybeAutoLoginTarget.isPresent()) {
            throw new RedirectToUrlException(maybeAutoLoginTarget.get().getLoginUrl());
        }
    }

    /*
     * Code adapted from {@link
     * org.springframework.security.config.annotation.web.configurers.oauth2.client.
     * OAuth2LoginConfigurer.getLoginLinks()}
     */
    private List<LoginLink> getLoginLinks()
    {
        try {
            var registrations = oAuth2Adapter.getOAuthClientRegistrations().stream() //
                    .filter(r -> AUTHORIZATION_CODE.equals(r.getAuthorizationGrantType())) //
                    .toList();

            // String authorizationRequestBaseUri =
            // (this.authorizationEndpointConfig.authorizationRequestBaseUri != null)
            // ? this.authorizationEndpointConfig.authorizationRequestBaseUri
            // : OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;

            var authorizationRequestBaseUri = removeEnd(servletContext.getContextPath(), "/")
                    + DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;

            var loginLinkList = new ArrayList<LoginLink>();
            registrations.forEach((registration) -> {
                var authorizationRequestUri = authorizationRequestBaseUri + "/"
                        + registration.getRegistrationId();
                loginLinkList.add(new LoginLink(registration.getRegistrationId(),
                        registration.getClientName(), authorizationRequestUri));
            });

            return loginLinkList;
        }
        catch (NoSuchBeanDefinitionException e) {
            // No OAuth2 clients configured
            return emptyList();
        }
    }

    private static class LoginLink
        implements Serializable
    {
        private static final long serialVersionUID = 5192419196982922286L;

        private final String registrationId;
        private final String clientName;
        private final String loginUrl;

        public LoginLink(String aRegistrationId, String aClientName, String aLoginUrl)
        {
            registrationId = aRegistrationId;
            clientName = aClientName;
            loginUrl = aLoginUrl;
        }

        public String getRegistrationId()
        {
            return registrationId;
        }

        public String getClientName()
        {
            return clientName;
        }

        public String getLoginUrl()
        {
            return loginUrl;
        }
    }
}
