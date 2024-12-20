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

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@ConditionalOnWebApplication
public class InceptionSecurityActuatorAutoConfiguration
{
    public static final String BASE_URL = "/actuator";

    @Order(2)
    @Bean
    public SecurityFilterChain actuatorFilterChain(HttpSecurity aHttp) throws Exception
    {
        aHttp.securityMatcher(BASE_URL + "/**");
        aHttp.authorizeHttpRequests(rules -> rules //
                .requestMatchers(BASE_URL + "/health").permitAll() //
                .anyRequest().denyAll());
        aHttp.sessionManagement(rules -> rules //
                .sessionCreationPolicy(STATELESS));
        return aHttp.build();
    }
}
