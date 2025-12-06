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
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

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
        aHttp.rememberMe(Customizer.withDefaults())

                .csrf(AbstractHttpConfigurer::disable)

                .addFilterBefore(aFilter, RequestHeaderAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> {
                    accessToStaticResources(auth);
                    accessToRemoteApiAndSwagger(auth);
                    accessToApplication(auth);
                    auth.anyRequest().denyAll();
                })

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new Http403ForbiddenEntryPoint()))

                .headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin))

                .sessionManagement(
                        session -> session.maximumSessions(-1).sessionRegistry(aSessionRegistry));

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
