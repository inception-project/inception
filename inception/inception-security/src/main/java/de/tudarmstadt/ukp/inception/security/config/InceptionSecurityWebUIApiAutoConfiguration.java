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

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.apache.wicket.csp.CSPDirectiveSrcValue.SELF;
import static org.springframework.security.config.http.SessionCreationPolicy.NEVER;

import java.util.ArrayList;

import org.apache.wicket.csp.FixedCSPValue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableConfigurationProperties({ CspPropertiesImpl.class })
@ConditionalOnWebApplication
public class InceptionSecurityWebUIApiAutoConfiguration
{
    public static final String BASE_URL = "/ui";
    public static final String BASE_VIEW_URL = BASE_URL + "/view";
    public static final String BASE_API_URL = BASE_URL + "/api";

    @Order(1000)
    @Bean
    public SecurityFilterChain uiApiFilterChain(HttpSecurity aHttp) throws Exception
    {
        aHttp.securityMatcher(BASE_API_URL + "/**");
        commonConfiguration(aHttp);
        return aHttp.build();
    }

    @Order(1001)
    @Bean
    public SecurityFilterChain uiViewFilterChain(HttpSecurity aHttp, CspProperties aCspProperties)
        throws Exception
    {
        var imgSrcValue = new ArrayList<>(asList(SELF, new FixedCSPValue("data:")));
        aCspProperties.getAllowedImageSources().stream() //
                .map(FixedCSPValue::new) //
                .forEachOrdered(imgSrcValue::add);

        var mediaSrcValue = new ArrayList<>(asList(SELF, new FixedCSPValue("data:")));
        aCspProperties.getAllowedMediaSources().stream() //
                .map(FixedCSPValue::new) //
                .forEachOrdered(mediaSrcValue::add);

        aHttp.securityMatcher(BASE_VIEW_URL + "/**");
        // Views render data that we generally want to display in an IFrame on the editor page
        aHttp.headers(headers -> headers //
                .frameOptions(options -> options //
                        .sameOrigin()) //
                .contentSecurityPolicy(csp -> {
                    csp.policyDirectives(join(";", //
                            "default-src 'none'", //
                            "script-src 'strict-dynamic' 'unsafe-eval'", //
                            "style-src 'self' 'unsafe-inline'", //
                            "img-src " + imgSrcValue.stream().map(t -> t.render(null, null))
                                    .collect(joining(" ")), //
                            "media-src " + mediaSrcValue.stream().map(t -> t.render(null, null))
                                    .collect(joining(" ")), //
                            "connect-src 'self'", //
                            "font-src 'self'", //
                            "manifest-src 'self'", //
                            "child-src 'self'", //
                            "base-uri 'self'", //
                            "frame-src 'self' 'self'"));
                }));
        commonConfiguration(aHttp);
        return aHttp.build();
    }

    private void commonConfiguration(HttpSecurity aHttp) throws Exception
    {
        aHttp.authorizeHttpRequests(authorizeHttpRequests -> {
            authorizeHttpRequests //
                    .requestMatchers("/**").hasAnyRole("USER") //
                    .anyRequest().denyAll();
        });

        aHttp.sessionManagement(sessionManagement -> {
            sessionManagement.sessionCreationPolicy(NEVER);
        });

        aHttp.exceptionHandling(exceptionHandling -> {
            exceptionHandling.defaultAuthenticationEntryPointFor( //
                    new Http403ForbiddenEntryPoint(), //
                    new AntPathRequestMatcher("/**"));
        });
    }
}
