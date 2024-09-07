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
package de.tudarmstadt.ukp.inception.io.tei.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.io.tei.TeiFormatSupport;
import de.tudarmstadt.ukp.inception.io.tei.TeiXmlDocumentFormatSupport;

@Configuration
public class TeiFormatsAutoConfiguration
{
    @Bean
    @ConditionalOnProperty(prefix = "format.dkpro-core-tei", name = "enabled", //
            havingValue = "true", matchIfMissing = true)
    public TeiFormatSupport teiFormatSupport()
    {
        return new TeiFormatSupport();
    }

    @Bean
    @ConditionalOnProperty(prefix = "format.tei", name = "enabled", //
            havingValue = "true", matchIfMissing = false)
    public TeiXmlDocumentFormatSupport teiXmlDocumentFormatSupport()
    {
        return new TeiXmlDocumentFormatSupport();
    }
}
