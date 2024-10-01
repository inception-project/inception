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
package de.tudarmstadt.ukp.inception.support.wicket;

import org.apache.wicket.Application;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ResourceReference;

import jakarta.servlet.ServletContext;

public class ServletContextUtils
{
    public static String referenceToUrl(ServletContext aServletContext,
            ResourceReference aResourceReference)
    {
        var handler = new ResourceReferenceRequestHandler(aResourceReference, new PageParameters());

        var contextPath = aServletContext.getContextPath();
        if (!contextPath.startsWith("/")) {
            contextPath = '/' + contextPath;
        }

        var resourcePath = Application.get().getRootRequestMapper().mapHandler(handler).toString();
        if (!contextPath.endsWith("/")) {
            resourcePath = '/' + resourcePath;
        }

        return contextPath + resourcePath;
    }
}
