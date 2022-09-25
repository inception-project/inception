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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import de.tudarmstadt.ukp.clarin.webanno.security.InceptionDaoAuthenticationProvider;

public class InceptionSecurityRemoteApiAutoConfiguration
{
    @Order(1)
    @Bean
    public SecurityFilterChain remoteApiFilterChain(PasswordEncoder aPasswordEncoder,
            UserDetailsManager aUserDetailsService, HttpSecurity aHttp)
        throws Exception
    {
        DaoAuthenticationProvider authProvider = new InceptionDaoAuthenticationProvider();
        authProvider.setUserDetailsService(aUserDetailsService);
        authProvider.setPasswordEncoder(aPasswordEncoder);

        // @formatter:off
        aHttp
            .antMatcher("/api/**")
            .csrf().disable()
            // We hard-wire the internal user DB as the authentication provider here because
            // because the API shouldn't work with external pre-authentication
            .authenticationProvider(authProvider)
            .authorizeRequests()
                .anyRequest().access("hasAnyRole('ROLE_REMOTE')")
            .and()
            .httpBasic()
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        // @formatter:on
        return aHttp.build();
    }
}
