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

import java.io.IOException;
import java.util.EnumSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.filter.OncePerRequestFilter;

import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LoggingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class InceptionServletContextInitializer
    implements ServletContextInitializer
{
    private @Autowired RepositoryProperties repoProperties;

    @Override
    public void onStartup(ServletContext aServletContext) throws ServletException
    {
        configureCoep(aServletContext);
        configureLogging(aServletContext);
        configurePerRequestJpaSession(aServletContext);
        configureSpringSessionLookup(aServletContext);
    }

    private void configureSpringSessionLookup(ServletContext aServletContext)
    {
        // Provide Spring with access to the HTTP sessions
        aServletContext.addListener(HttpSessionEventPublisher.class);
    }

    private void configurePerRequestJpaSession(ServletContext aServletContext)
    {
        // Make sure we have one JPA session/transaction per request. Closes session at the
        // end, without this, changed data may not be automatically saved to the DB.
        FilterRegistration openSessionInViewFilter = aServletContext.addFilter("opensessioninview",
                OpenEntityManagerInViewFilter.class);
        openSessionInViewFilter.addMappingForUrlPatterns(EnumSet.of(REQUEST), false, "/*");
    }

    private void configureLogging(ServletContext aServletContext)
    {
        // Make username / repository accessible to logging framework
        FilterRegistration loggingFilter = aServletContext.addFilter("logging",
                new LoggingFilter(repoProperties.getPath().getAbsolutePath().toString()));
        loggingFilter.addMappingForUrlPatterns(EnumSet.of(REQUEST, FORWARD, ASYNC), false, "/*");
    }

    private void configureCoep(ServletContext aServletContext)
    {
        FilterRegistration coepFilter = aServletContext.addFilter("coep", new OncePerRequestFilter()
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
        });
        coepFilter.addMappingForUrlPatterns(EnumSet.of(REQUEST, FORWARD, ASYNC), false, "/*");
    }
}
