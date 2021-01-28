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
package de.tudarmstadt.ukp.clarin.webanno.webapp.config;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.support.logging.LoggingFilter;

@Configuration
public class WebAnnoWebInitializer
    implements ServletContextInitializer
{
    @Override
    public void onStartup(ServletContext aServletContext) throws ServletException
    {
        // 2) Make username accessible to logging framework
        FilterRegistration loggingFilter = aServletContext.addFilter("logging",
                LoggingFilter.class);
        loggingFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");

        // 5) Make sure we have one JPA session/transaction per request. Closes session at the
        // end, without this, changed data may not be automatically saved to the DB.
        FilterRegistration openSessionInViewFilter = aServletContext.addFilter("opensessioninview",
                OpenEntityManagerInViewFilter.class);
        openSessionInViewFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false,
                "/*");

        aServletContext.addListener(HttpSessionEventPublisher.class);
    }
}
