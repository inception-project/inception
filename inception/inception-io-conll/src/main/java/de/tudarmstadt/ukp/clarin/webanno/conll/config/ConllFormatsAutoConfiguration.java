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
package de.tudarmstadt.ukp.clarin.webanno.conll.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.conll.Conll2000FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.conll.Conll2002FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.conll.Conll2003FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.conll.Conll2006FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.conll.Conll2009FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.conll.Conll2012FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.conll.ConllCoreNlpFormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.conll.ConllUFormatSupport;

@Configuration
public class ConllFormatsAutoConfiguration
{
    @Bean
    @ConditionalOnProperty(prefix = "format.conll2000", name = "enabled", //
            havingValue = "true", matchIfMissing = true)
    public Conll2000FormatSupport conll2000FormatSupport()
    {
        return new Conll2000FormatSupport();
    }

    @Bean
    @ConditionalOnProperty(prefix = "format.conll2002", name = "enabled", //
            havingValue = "true", matchIfMissing = true)
    public Conll2002FormatSupport conll2002FormatSupport()
    {
        return new Conll2002FormatSupport();
    }

    @Bean
    @ConditionalOnProperty(prefix = "format.conll2003", name = "enabled", //
            havingValue = "true", matchIfMissing = true)
    public Conll2003FormatSupport conll2003FormatSupport()
    {
        return new Conll2003FormatSupport();
    }

    @Bean
    @ConditionalOnProperty(prefix = "format.conll2006", name = "enabled", //
            havingValue = "true", matchIfMissing = true)
    public Conll2006FormatSupport conll2006FormatSupport()
    {
        return new Conll2006FormatSupport();
    }

    @Bean
    @ConditionalOnProperty(prefix = "format.conll2009", name = "enabled", //
            havingValue = "true", matchIfMissing = true)
    public Conll2009FormatSupport conll2009FormatSupport()
    {
        return new Conll2009FormatSupport();
    }

    @Bean
    @ConditionalOnProperty(prefix = "format.conll2012", name = "enabled", //
            havingValue = "true", matchIfMissing = true)
    public Conll2012FormatSupport conll2012FormatSupport()
    {
        return new Conll2012FormatSupport();
    }

    @Bean
    @ConditionalOnProperty(prefix = "format.conllcorenlp", name = "enabled", //
            havingValue = "true", matchIfMissing = true)
    public ConllCoreNlpFormatSupport conllCoreNlpFormatSupport()
    {
        return new ConllCoreNlpFormatSupport();
    }

    @Bean
    @ConditionalOnProperty(prefix = "format.conllu", name = "enabled", //
            havingValue = "true", matchIfMissing = true)
    public ConllUFormatSupport conllUFormatSupport()
    {
        return new ConllUFormatSupport();
    }
}
