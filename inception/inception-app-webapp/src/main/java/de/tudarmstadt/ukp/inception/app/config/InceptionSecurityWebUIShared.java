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

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

public class InceptionSecurityWebUIShared
{
    public static void accessToStaticResources(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry aCfg)
    {
        // Resources need to be publicly accessible so they don't trigger the login
        // page. Otherwise it could happen that the user is redirected to a resource
        // upon login instead of being forwarded to a proper application page.
        aCfg //
                .requestMatchers("/favicon.ico").permitAll() //
                .requestMatchers("/favicon.png").permitAll() //
                .requestMatchers("/assets/**").permitAll() //
                .requestMatchers("/images/**").permitAll() //
                .requestMatchers("/resources/**").permitAll() //
                .requestMatchers("/whoops").permitAll() //
                .requestMatchers("/nowhere").permitAll() //
                .requestMatchers("/about/**").permitAll() //
                .requestMatchers("/wicket/resource/**").permitAll();
    }

    public static void accessToRemoteApiAndSwagger(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry aCfg)
    {
        aCfg //
                .requestMatchers("/swagger-ui/**").hasAnyRole("REMOTE") //
                .requestMatchers("/swagger-ui.html").hasAnyRole("REMOTE") //
                .requestMatchers("/v3/**") //
                .hasAnyRole("REMOTE");
    }

    public static void accessToApplication(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry aCfg)
    {
        aCfg //
                .requestMatchers("/admin/**").hasAnyRole("ADMIN") //
                .requestMatchers("/doc/**").hasAnyRole("ADMIN", "USER") //
                .requestMatchers("/**").hasAnyRole("ADMIN", "USER");
    }
}
