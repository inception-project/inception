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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.ResolvableType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityProperties;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;

public class OAuth2LoginPanel
    extends Panel
{
    private static final long serialVersionUID = -3709147438732584586L;

    private @SpringBean SecurityProperties securityProperties;

    public OAuth2LoginPanel(String aId, boolean aSkipAutoLogin)
    {
        super(aId);

        List<LoginLink> loginLinks = getLoginLinks();

        if (!aSkipAutoLogin && isNotBlank(securityProperties.getAutoLogin())) {
            var maybeAutoLoginTarget = loginLinks.stream() //
                    .filter(link -> securityProperties.getAutoLogin()
                            .equals(link.getRegistrationId()))
                    .findFirst();

            if (maybeAutoLoginTarget.isPresent()) {
                throw new RedirectToUrlException(maybeAutoLoginTarget.get().getLoginUrl());
            }
        }

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

        add(LambdaBehavior.visibleWhen(() -> !loginLinks.isEmpty()));
    }

    /*
     * Code adapted from {@link
     * org.springframework.security.config.annotation.web.configurers.oauth2.client.
     * OAuth2LoginConfigurer.getLoginLinks()}
     */
    @SuppressWarnings("unchecked")
    private List<LoginLink> getLoginLinks()
    {
        try {
            // We cannot use @SpringBean here because that returns a proxy that
            // ResolvableType.forInstance below won't be able to resolve.
            var clientRegistrationRepository = ApplicationContextProvider.getApplicationContext()
                    .getBean(ClientRegistrationRepository.class);

            Iterable<ClientRegistration> clientRegistrations = null;
            ResolvableType type = ResolvableType.forInstance(clientRegistrationRepository)
                    .as(Iterable.class);

            if (type != ResolvableType.NONE
                    && ClientRegistration.class.isAssignableFrom(type.resolveGenerics()[0])) {
                clientRegistrations = (Iterable<ClientRegistration>) clientRegistrationRepository;
            }

            if (clientRegistrations == null) {
                return Collections.emptyList();
            }

            // String authorizationRequestBaseUri =
            // (this.authorizationEndpointConfig.authorizationRequestBaseUri != null)
            // ? this.authorizationEndpointConfig.authorizationRequestBaseUri
            // : OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;
            String authorizationRequestBaseUri = DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;
            List<LoginLink> loginLinkList = new ArrayList<>();
            clientRegistrations.forEach((registration) -> {
                if (AUTHORIZATION_CODE.equals(registration.getAuthorizationGrantType())) {
                    String authorizationRequestUri = authorizationRequestBaseUri + "/"
                            + registration.getRegistrationId();
                    loginLinkList.add(new LoginLink(registration.getRegistrationId(),
                            registration.getClientName(), authorizationRequestUri));
                }
            });

            return loginLinkList;
        }
        catch (NoSuchBeanDefinitionException e) {
            // No OAuth2 clients configured
            return Collections.emptyList();
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