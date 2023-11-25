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

import static jakarta.servlet.DispatcherType.ASYNC;
import static jakarta.servlet.DispatcherType.FORWARD;
import static jakarta.servlet.DispatcherType.REQUEST;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.EventListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.filter.OncePerRequestFilter;

import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.support.logging.LoggingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class InceptionServletContextInitializer
{
    private @Autowired RepositoryProperties repoProperties;

    @Bean
    public ServletListenerRegistrationBean<EventListener> springSessionLookup()
    {
        var registration = new ServletListenerRegistrationBean<EventListener>();
        registration.setListener(new HttpSessionEventPublisher());
        registration.setOrder(0);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<OpenEntityManagerInViewFilter> perRequestJpaSession()
    {
        // Make sure we have one JPA session/transaction per request. Closes session at the
        // end, without this, changed data may not be automatically saved to the DB.
        var registration = new FilterRegistrationBean<>(new OpenEntityManagerInViewFilter());
        registration.setName("opensessioninview");
        registration.setDispatcherTypes(REQUEST);
        registration.setUrlPatterns(asList("/*"));
        registration.setOrder(0);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<LoggingFilter> loggingFilter()
    {
        // Make username / repository accessible to logging framework
        var registration = new FilterRegistrationBean<>(
                new LoggingFilter(repoProperties.getPath().getAbsolutePath().toString()));
        registration.setName("logging");
        registration.setDispatcherTypes(REQUEST, FORWARD, ASYNC);
        registration.setUrlPatterns(asList("/*"));
        registration.setOrder(0);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> coepFilter()
    {
        OncePerRequestFilter filter = new OncePerRequestFilter()
        {
            @Override
            protected void doFilterInternal(HttpServletRequest aRequest,
                    HttpServletResponse aResponse, FilterChain aFilterChain)
                throws ServletException, IOException
            {
                // We need this in particular for non-Wicket resources served by Spring MVC
                HttpServletResponse response = (HttpServletResponse) aResponse;
                response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
                response.setHeader("Cross-Origin-Resource-Policy", "same-site");
                response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
                aFilterChain.doFilter(aRequest, aResponse);
            }
        };
        var registration = new FilterRegistrationBean<>(filter);
        registration.setName("coep");
        registration.setDispatcherTypes(REQUEST, FORWARD, ASYNC);
        registration.setUrlPatterns(asList("/*"));
        registration.setOrder(0);
        return registration;
    }
}
