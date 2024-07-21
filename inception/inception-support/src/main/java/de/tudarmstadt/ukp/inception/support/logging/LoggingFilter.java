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
package de.tudarmstadt.ukp.inception.support.logging;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class LoggingFilter
    implements Filter
{
    public static final String PARAM_REPOSITORY_PATH = "repoPath";

    private String repoPath;

    public LoggingFilter(String aRepoPath)
    {
        repoPath = aRepoPath;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
        throws IOException, ServletException
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            setLoggingUsername(authentication.getName());
        }

        if (repoPath != null) {
            setRepositoryPath(repoPath);
        }

        try {
            chain.doFilter(req, resp);
        }
        finally {
            if (authentication != null) {
                clearLoggingUsername();
            }

            if (repoPath != null) {
                clearRepositoryPath();
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
        MDC.put(Logging.KEY_USERNAME, aUsername);
        MDC.put("_username", "[" + aUsername + "] ");
    }

    public static void clearLoggingUsername()
    {
        MDC.remove("_username");
        MDC.remove(Logging.KEY_USERNAME);
    }

    public static void setRepositoryPath(String aRepositoryPath)
    {
        MDC.put(Logging.KEY_REPOSITORY_PATH, aRepositoryPath);
    }

    public static void clearRepositoryPath()
    {
        MDC.remove(Logging.KEY_REPOSITORY_PATH);
    }
}
