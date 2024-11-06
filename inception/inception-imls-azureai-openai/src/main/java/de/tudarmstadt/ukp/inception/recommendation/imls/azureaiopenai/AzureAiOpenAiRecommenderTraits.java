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
package de.tudarmstadt.ukp.inception.recommendation.imls.azureaiopenai;

import de.tudarmstadt.ukp.inception.recommendation.imls.azureaiopenai.client.GenerateResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.option.LlmRecommenderTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraits;

public class AzureAiOpenAiRecommenderTraits
    extends LlmRecommenderTraits
{
    private static final long serialVersionUID = 6433061638746045602L;

    public static final String DEFAULT_AZURE_AI_OPENAI_URL = "https://RESOURCE.openai.azure.com/openai/deployments/DEPLOYMENT";

    private String url = DEFAULT_AZURE_AI_OPENAI_URL;

    private AuthenticationTraits authentication;

    private GenerateResponseFormat format;

    public AuthenticationTraits getAuthentication()
    {
        return authentication;
    }

    public void setAuthentication(AuthenticationTraits aAuthentication)
    {
        authentication = aAuthentication;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String aUrl)
    {
        url = aUrl;
    }

    public GenerateResponseFormat getFormat()
    {
        return format;
    }

    public void setFormat(GenerateResponseFormat aFormat)
    {
        format = aFormat;
    }
}
