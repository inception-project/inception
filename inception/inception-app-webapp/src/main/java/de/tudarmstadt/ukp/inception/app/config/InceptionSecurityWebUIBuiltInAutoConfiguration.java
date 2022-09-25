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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import de.tudarmstadt.ukp.clarin.webanno.security.InceptionDaoAuthenticationProvider;

@EnableWebSecurity
public class InceptionSecurityWebUIBuiltInAutoConfiguration
{
    @Profile("auto-mode-builtin")
    @Bean
    public SecurityFilterChain webUiFilterChain(HttpSecurity aHttp,
            SessionRegistry aSessionRegistry)
        throws Exception
    {
        // @formatter:off
        aHttp
            .rememberMe()
            .and()
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/login.html*").permitAll()
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
                .antMatchers("/" + NS_PROJECT + "/*/join-project/**").permitAll()
                .antMatchers("/swagger-ui/**").access("hasAnyRole('ROLE_REMOTE')")
                .antMatchers("/swagger-ui.html").access("hasAnyRole('ROLE_REMOTE')")
                .antMatchers("/v3/**").access("hasAnyRole('ROLE_REMOTE')")
                .antMatchers("/admin/**").access("hasAnyRole('ROLE_ADMIN')")
                .antMatchers("/doc/**").access("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
                .antMatchers("/**").access("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
                .anyRequest().denyAll()
            .and()
            .exceptionHandling()
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login.html"), 
                        new AntPathRequestMatcher("/**"))
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

    @Bean(name = "authenticationProvider")
    @Profile("auto-mode-builtin")
    public DaoAuthenticationProvider internalAuthenticationProvider(
            @Lazy UserDetailsManager aUserDetails, PasswordEncoder aPasswordEncoder)
    {
        DaoAuthenticationProvider authProvider = new InceptionDaoAuthenticationProvider();
        authProvider.setUserDetailsService(aUserDetails);
        authProvider.setPasswordEncoder(aPasswordEncoder);
        return authProvider;
    }
}
