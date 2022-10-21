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

import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService.PROFILE_AUTH_MODE_EXTERNAL_PREAUTH;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.preauth.ShibbolethRequestHeaderAuthenticationFilter;

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
        // @formatter:off
        aHttp
            .rememberMe()
            .and()
            .csrf().disable()
            .addFilterBefore(aFilter, RequestHeaderAuthenticationFilter.class)
            .authorizeRequests()
                // Resources need to be publicly accessible so they don't trigger the login
                // page. Otherwise it could happen that the user is redirected to a resource
                // upon login instead of being forwarded to a proper application page.
                .antMatchers("/favicon.ico").permitAll()
                .antMatchers("/favicon.png").permitAll()
                .antMatchers("/assets/**").permitAll()
                .antMatchers("/images/**").permitAll()
                .antMatchers("/resources/**").permitAll()
                .antMatchers("/whoops").permitAll()
                .antMatchers("/about/**").permitAll()
                .antMatchers("/wicket/resource/**").permitAll()
                .antMatchers("/swagger-ui/**").access("hasAnyRole('ROLE_REMOTE')")
                .antMatchers("/swagger-ui.html").access("hasAnyRole('ROLE_REMOTE')")
                .antMatchers("/v3/**").access("hasAnyRole('ROLE_REMOTE')")
                .antMatchers("/admin/**").access("hasAnyRole('ROLE_ADMIN')")
                .antMatchers("/doc/**").access("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
                .antMatchers("/**").access("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
                .anyRequest().denyAll()
            .and()
            .exceptionHandling()
                .authenticationEntryPoint(new Http403ForbiddenEntryPoint())
            .and()
                .headers().frameOptions().sameOrigin()
            .and()
            .sessionManagement() 
                // Configuring an unlimited session per-user maximum as a side-effect registers
                // the ConcurrentSessionFilter which checks for valid sessions in the session
                // registry. This allows us to indirectly invalidate a server session by marking
                // its Spring-security registration as invalid and have Spring Security in turn
                // mark the server session as invalid on the next request. This is used e.g. to
                // force-sign-out users that are being deleted.
                .maximumSessions(-1)
                .sessionRegistry(aSessionRegistry);
        // @formatter:on
        return aHttp.build();
    }

    @Bean
    @Profile(PROFILE_AUTH_MODE_EXTERNAL_PREAUTH)
    public ShibbolethRequestHeaderAuthenticationFilter preAuthFilter(
            AuthenticationConfiguration aAuthenticationConfiguration, UserDao aUserRepository)
        throws Exception
    {
        ShibbolethRequestHeaderAuthenticationFilter filter = new ShibbolethRequestHeaderAuthenticationFilter();
        filter.setPrincipalRequestHeader(preAuthPrincipalHeader);
        filter.setAuthenticationManager(aAuthenticationConfiguration.getAuthenticationManager());
        filter.setUserRepository(aUserRepository);
        filter.setExceptionIfHeaderMissing(true);
        return filter;
    }
}
