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

import static java.util.Collections.emptyMap;

import java.util.Map;

import org.slf4j.MDC;

public class MDCContext
    implements AutoCloseable
{
    private final Map<String, String> context;

    public static MDCContext open()
    {
        return new MDCContext();
    }

    private MDCContext()
    {
        context = MDC.getCopyOfContextMap();
    }

    public MDCContext with(String aKey, String aValue)
    {
        MDC.put(aKey, aValue);
        return this;
    }

    @Override
    public void close()
    {
        if (context != null) {
            MDC.setContextMap(context);
        }
        else {
            MDC.setContextMap(emptyMap());
        }
    }
}
