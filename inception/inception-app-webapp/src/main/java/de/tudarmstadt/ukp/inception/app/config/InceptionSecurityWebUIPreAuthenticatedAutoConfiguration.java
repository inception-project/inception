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

import static de.tudarmstadt.ukp.inception.app.config.InceptionSecurityWebUIShared.accessToApplication;
import static de.tudarmstadt.ukp.inception.app.config.InceptionSecurityWebUIShared.accessToRemoteApiAndSwagger;
import static de.tudarmstadt.ukp.inception.app.config.InceptionSecurityWebUIShared.accessToStaticResources;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_AUTH_MODE_EXTERNAL_PREAUTH;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.preauth.ShibbolethRequestHeaderAuthenticationFilter;

@ConditionalOnWebApplication
@EnableWebSecurity
public class InceptionSecurityWebUIPreAuthenticatedAutoConfiguration
{
    private @Value("${auth.preauth.header.principal:remote_user}") String preAuthPrincipalHeader;

    @Profile(PROFILE_AUTH_MODE_EXTERNAL_PREAUTH)
    @Bean
    public SecurityFilterChain externalPreAuthenticationFilterChain(HttpSecurity aHttp,
            ShibbolethRequestHeaderAuthenticationFilter aFilter, SessionRegistry aSessionRegistry)
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

        aHttp.rememberMe(Customizer.withDefaults()) //
                .addFilterBefore(aFilter, RequestHeaderAuthenticationFilter.class);

        aHttp.authorizeHttpRequests(authz -> {
            accessToStaticResources(authz);
            accessToRemoteApiAndSwagger(authz);
            accessToApplication(authz);
            authz.anyRequest().denyAll();
        });

        aHttp.exceptionHandling(
                exception -> exception.authenticationEntryPoint(new Http403ForbiddenEntryPoint()))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        aHttp.sessionManagement(session -> session
                // Configuring an unlimited session per-user maximum as a side-effect registers
                // the ConcurrentSessionFilter which checks for valid sessions in the session
                // registry. This allows us to indirectly invalidate a server session by marking
                // its Spring-security registration as invalid and have Spring Security in turn
                // mark the server session as invalid on the next request. This is used e.g. to
                // force-sign-out users that are being deleted.
                .maximumSessions(-1) //
                .sessionRegistry(aSessionRegistry));

        return aHttp.build();
    }

    @Bean
    @Profile(PROFILE_AUTH_MODE_EXTERNAL_PREAUTH)
    public ShibbolethRequestHeaderAuthenticationFilter preAuthFilter(
            AuthenticationConfiguration aAuthenticationConfiguration, UserDao aUserRepository)
        throws Exception
    {
        var filter = new ShibbolethRequestHeaderAuthenticationFilter();
        filter.setPrincipalRequestHeader(preAuthPrincipalHeader);
        filter.setAuthenticationManager(aAuthenticationConfiguration.getAuthenticationManager());
        filter.setUserRepository(aUserRepository);
        filter.setExceptionIfHeaderMissing(true);
        return filter;
    }
}
