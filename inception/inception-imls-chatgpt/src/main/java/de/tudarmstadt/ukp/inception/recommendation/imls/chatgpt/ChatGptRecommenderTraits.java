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
package de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.client.GenerateResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.prompt.PromptingMode;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.response.ExtractionMode;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraits;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatGptRecommenderTraits
    implements Serializable
{
    private static final long serialVersionUID = 6433061638746045602L;

    public static final String DEFAULT_CHATGPT_URL = "https://api.openai.com/v1";

    private String url = DEFAULT_CHATGPT_URL;

    private AuthenticationTraits authentication;

    private String prompt;

    private String model = "gpt-3.5-turbo";

    private GenerateResponseFormat format;

    private PromptingMode promptingMode = PromptingMode.PER_ANNOTATION;

    private ExtractionMode extractionMode = ExtractionMode.RESPONSE_AS_LABEL;

    private @JsonInclude(NON_EMPTY) Map<String, Object> options = new LinkedHashMap<String, Object>();

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

    public String getModel()
    {
        return model;
    }

    public void setModel(String aModel)
    {
        model = aModel;
    }

    public String getPrompt()
    {
        return prompt;
    }

    public void setPrompt(String aPrompt)
    {
        prompt = aPrompt;
    }

    public PromptingMode getPromptingMode()
    {
        return promptingMode;
    }

    public void setPromptingMode(PromptingMode aPromptingMode)
    {
        promptingMode = aPromptingMode;
    }

    public GenerateResponseFormat getFormat()
    {
        return format;
    }

    public void setFormat(GenerateResponseFormat aFormat)
    {
        format = aFormat;
    }

    public ExtractionMode getExtractionMode()
    {
        return extractionMode;
    }

    public void setExtractionMode(ExtractionMode aExtractionMode)
    {
        extractionMode = aExtractionMode;
    }

    public Map<String, Object> getOptions()
    {
        return Collections.unmodifiableMap(options);
    }

    public void setOptions(Map<String, Object> aOptions)
    {
        options.clear();
        options.putAll(aOptions);
    }
}
