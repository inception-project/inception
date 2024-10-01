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
package de.tudarmstadt.ukp.inception.app.config;

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.inception.app.config.InceptionSecurityWebUIShared.accessToApplication;
import static de.tudarmstadt.ukp.inception.app.config.InceptionSecurityWebUIShared.accessToRemoteApiAndSwagger;
import static de.tudarmstadt.ukp.inception.app.config.InceptionSecurityWebUIShared.accessToStaticResources;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import de.tudarmstadt.ukp.inception.security.oauth.OAuth2Adapter;
import de.tudarmstadt.ukp.inception.security.saml.Saml2Adapter;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService;

@ConditionalOnWebApplication
@EnableWebSecurity
public class InceptionSecurityWebUIBuiltInAutoConfiguration
{
    @Profile(DeploymentModeService.PROFILE_AUTH_MODE_DATABASE)
    @Bean
    public SecurityFilterChain webUiFilterChain(HttpSecurity aHttp,
            SessionRegistry aSessionRegistry, OAuth2Adapter aOAuth2Handling,
            Saml2Adapter aSaml2Handling,
            Optional<ClientRegistrationRepository> aClientRegistrationRepository,
            Optional<RelyingPartyRegistrationRepository> aRelyingPartyRegistrationRepository)
        throws Exception
    {
        aHttp.csrf(csrf -> {
            // Instead of disabling the Spring CSRF filter, we just disable the CSRF validation for
            // the Wicket UI (Wicket has its own CSRF mechanism). This way, Spring will still
            // populate the CSRF token attribute in the request which we will need later when we
            // need to provide the token to the JavaScript code in the browser to make callbacks to
            // Spring MVC controllers.
            csrf.requireCsrfProtectionMatcher(
                    new NegatedRequestMatcher(AnyRequestMatcher.INSTANCE));
        });
        aHttp.headers(headers -> {
            headers.frameOptions(frameOptions -> {
                frameOptions.sameOrigin();
            });
        });

        var authorizations = aHttp.authorizeHttpRequests();
        authorizations.requestMatchers("/login.html*").permitAll();
        accessToStaticResources(authorizations);
        accessToRemoteApiAndSwagger(authorizations);
        authorizations.requestMatchers(NS_PROJECT + "/*/join-project/**").permitAll();
        accessToApplication(authorizations);
        authorizations.anyRequest().denyAll();

        // Must use "defaultAuthenticationEntryPointFor" instead of "formLogin" because
        // if we use formLogin, Spring will handle the form submit and we want the Wicket
        // login page to handle the form submit instead!
        // .formLogin(form -> form.loginPage("/login.html").permitAll())
        aHttp.exceptionHandling().defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login.html"),
                new AntPathRequestMatcher("/**"));

        if (aClientRegistrationRepository.isPresent()) {
            aHttp.oauth2Login() //
                    .loginPage("/login.html") //
                    .userInfoEndpoint() //
                    .oidcUserService(aOAuth2Handling::loadOidcUser) //
                    .userService(aOAuth2Handling::loadUserOAuth2User);
        }

        if (aRelyingPartyRegistrationRepository.isPresent()) {
            RelyingPartyRegistrationResolver relyingPartyRegistrationResolver = //
                    new DefaultRelyingPartyRegistrationResolver(
                            aRelyingPartyRegistrationRepository.get());
            Saml2MetadataFilter filter = new Saml2MetadataFilter(relyingPartyRegistrationResolver,
                    new OpenSamlMetadataResolver());
            aHttp.addFilterBefore(filter, Saml2WebSsoAuthenticationFilter.class);

            aHttp.saml2Login(c -> {
                c.loginPage("/login.html");
                c.withObjectPostProcessor(new ObjectPostProcessor<Object>()
                {
                    @Override
                    public <O> O postProcess(O aObject)
                    {
                        if (aObject instanceof OpenSaml4AuthenticationProvider) {
                            var provider = (OpenSaml4AuthenticationProvider) aObject;
                            var converter = OpenSaml4AuthenticationProvider
                                    .createDefaultResponseAuthenticationConverter();
                            provider.setResponseAuthenticationConverter(token -> {
                                var authentication = converter.convert(token);
                                authentication = aSaml2Handling.process(token, authentication);
                                return authentication;
                            });
                        }

                        return aObject;
                    }
                });
            });
        }

        aHttp.sessionManagement()
                // Configuring an unlimited session per-user maximum as a side-effect registers
                // the ConcurrentSessionFilter which checks for valid sessions in the session
                // registry. This allows us to indirectly invalidate a server session by marking
                // its Spring-security registration as invalid and have Spring Security in turn
                // mark the server session as invalid on the next request. This is used e.g. to
                // force-sign-out users that are being deleted.
                .maximumSessions(-1) //
                .sessionRegistry(aSessionRegistry);

        return aHttp.build();
    }
}
