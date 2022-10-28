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
package de.tudarmstadt.ukp.inception.security.config;

import static org.springframework.security.config.http.SessionCreationPolicy.NEVER;

import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

public class InceptionSecurityWebUIApiAutoConfiguration
{
    public static final String BASE_URL = "/ui";
    public static final String BASE_VIEW_URL = BASE_URL + "/view";
    public static final String BASE_API_URL = BASE_URL + "/api";

    @Order(1000)
    @Bean
    public SecurityFilterChain uiApiFilterChain(HttpSecurity aHttp) throws Exception
    {
        aHttp.antMatcher(BASE_API_URL + "/**");
        commonConfiguration(aHttp);
        return aHttp.build();
    }

    @Order(1001)
    @Bean
    public SecurityFilterChain uiViewFilterChain(HttpSecurity aHttp) throws Exception
    {
        aHttp.antMatcher(BASE_VIEW_URL + "/**");
        // Views render data that we generally want to display in an IFrame on the editor page
        aHttp.headers().frameOptions().sameOrigin();
        commonConfiguration(aHttp);
        return aHttp.build();
    }

    private void commonConfiguration(HttpSecurity aHttp) throws Exception
    {
        aHttp.authorizeRequests() //
                .antMatchers("/**").access("hasAnyRole('ROLE_USER')") //
                .anyRequest().denyAll();
        aHttp.sessionManagement().sessionCreationPolicy(NEVER);
        aHttp.exceptionHandling() //
                .defaultAuthenticationEntryPointFor( //
                        new Http403ForbiddenEntryPoint(), //
                        new AntPathRequestMatcher("/**"));

    }
}
