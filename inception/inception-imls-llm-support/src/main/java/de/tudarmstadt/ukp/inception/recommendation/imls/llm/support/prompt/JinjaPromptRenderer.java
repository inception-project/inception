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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt;

import java.io.IOException;
import java.nio.charset.Charset;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.loader.ResourceLocator;
import com.hubspot.jinjava.loader.ResourceNotFoundException;

public class JinjaPromptRenderer
{
    private final Jinjava jinjava;

    public JinjaPromptRenderer()
    {
        var config = new JinjavaConfig();
        jinjava = new Jinjava(config);
        jinjava.setResourceLocator(new ResourceLocator()
        {
            @Override
            public String getString(String aFullName, Charset aEncoding,
                    JinjavaInterpreter aInterpreter)
                throws IOException
            {
                throw new ResourceNotFoundException("Resource not found: " + aFullName);
            }
        });
    }

    public String render(String aTemplate, PromptContext aContext)
    {
        return jinjava.render(aTemplate, aContext.getBindings());
    }
}
