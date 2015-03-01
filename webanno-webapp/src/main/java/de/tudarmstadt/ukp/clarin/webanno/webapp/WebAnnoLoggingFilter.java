/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class WebAnnoLoggingFilter
    implements Filter
{
    @Override
    public void init(FilterConfig filterConfig)
        throws ServletException
    {
        // Do nothing
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
        throws IOException, ServletException
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            setLoggingUsername(authentication.getName());
        }
        try {
            chain.doFilter(req, resp);
        }
        finally {
            if (authentication != null) {
                clearLoggingUsername();
            }
        }
    }

    @Override
    public void destroy()
    {
        // Do nothing
    }
    
    public static void setLoggingUsername(String aUsername)
    {
        MDC.put("username", aUsername);
        MDC.put("_username", "["+aUsername + "] ");
    }
    
    public static void clearLoggingUsername()
    {
        MDC.remove("_username");
        MDC.remove("username");
    }
}
