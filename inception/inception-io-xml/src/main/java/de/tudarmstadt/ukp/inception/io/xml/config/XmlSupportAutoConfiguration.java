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
package de.tudarmstadt.ukp.inception.io.xml.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.io.xml.XmlFormatSupport;
import de.tudarmstadt.ukp.inception.io.xml.css.StylesheetRegistry;
import de.tudarmstadt.ukp.inception.io.xml.css.StylesheetRegistryImpl;

@Configuration
public class XmlSupportAutoConfiguration
{
    @Bean
    @ConditionalOnProperty(prefix = "format.generic-xml", name = "enabled", //
            havingValue = "true", matchIfMissing = false)
    public XmlFormatSupport xmlFormatSupport()
    {
        return new XmlFormatSupport();
    }

    @Bean
    public StylesheetRegistry stylesheetRegistry()
    {
        return new StylesheetRegistryImpl();
    }
}
