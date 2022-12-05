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
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;

public class InceptionSecurityWebUIShared
{
    public static void accessToStaticResources(
            ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry aCfg)
    {
        // Resources need to be publicly accessible so they don't trigger the login
        // page. Otherwise it could happen that the user is redirected to a resource
        // upon login instead of being forwarded to a proper application page.
        aCfg //
                .antMatchers("/favicon.ico").permitAll() //
                .antMatchers("/favicon.png").permitAll() //
                .antMatchers("/assets/**").permitAll() //
                .antMatchers("/images/**").permitAll() //
                .antMatchers("/resources/**").permitAll() //
                .antMatchers("/whoops").permitAll() //
                .antMatchers("/about/**").permitAll() //
                .antMatchers("/wicket/resource/**").permitAll();
    }

    public static void accessToRemoteApiAndSwagger(
            ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry aCfg)
    {
        aCfg //
                .antMatchers("/swagger-ui/**").access("hasAnyRole('ROLE_REMOTE')")
                .antMatchers("/swagger-ui.html").access("hasAnyRole('ROLE_REMOTE')")
                .antMatchers("/v3/**").access("hasAnyRole('ROLE_REMOTE')");
    }

    public static void accessToApplication(
            ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry aCfg)
    {
        aCfg //
                .antMatchers("/admin/**").access("hasAnyRole('ROLE_ADMIN')") //
                .antMatchers("/doc/**").access("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')") //
                .antMatchers("/**").access("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')");

    }
}
