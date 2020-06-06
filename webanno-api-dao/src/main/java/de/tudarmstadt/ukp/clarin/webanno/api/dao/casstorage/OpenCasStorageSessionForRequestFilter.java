/*
 * Copyright 2020
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
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpenCasStorageSessionForRequestFilter
    implements Filter
{
    private final Logger log = LoggerFactory.getLogger(getClass());

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
        try (CasStorageSession session = CasStorageSession.open()) {
            chain.doFilter(req, resp);
        }
        catch (IOException |  ServletException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Error creating a CAS storage session", e);
        }
    }

    @Override
    public void destroy()
    {
        // Do nothing
    }
}
