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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatGptRecommenderTraits
    extends LlmRecommenderTraits
    implements Serializable
{
    private static final long serialVersionUID = 6433061638746045602L;

    public static final String OPENAI_API_URL = "https://api.openai.com";
    public static final String LOCAL_OLLAMA_API_URL = "http://localhost:11434";
    public static final String GROQ_API_URL = "https://api.groq.com/openai";
    public static final String CEREBRAS_API_URL = "https://api.cerebras.ai";

    public ChatGptRecommenderTraits()
    {
        setUrl(OPENAI_API_URL);
        setModel("gpt-4o-mini");
        setAuthentication(new ApiKeyAuthenticationTraits());
    }
}
