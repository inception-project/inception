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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ModelCapability;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptingMode;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ExtractionMode;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraits;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmRecommenderTraits
    implements Serializable
{
    private static final long serialVersionUID = 6433061638746045602L;

    private String url;

    private String model;

    private String prompt;

    private PromptingMode promptingMode = PromptingMode.PER_PARAGRAPH;

    private ExtractionMode extractionMode = ExtractionMode.MENTIONS_FROM_JSON;

    private @JsonInclude(NON_EMPTY) Map<String, Object> options = new LinkedHashMap<String, Object>();

    private @JsonInclude(NON_EMPTY) Set<ModelCapability> capabilities = EnumSet
            .of(ModelCapability.JSON_SCHEMA);

    private boolean interactive;

    private boolean justificationEnabled;

    private AuthenticationTraits authentication;

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

    public boolean isInteractive()
    {
        return interactive;
    }

    public void setInteractive(boolean aInteractive)
    {
        interactive = aInteractive;
    }

    public AuthenticationTraits getAuthentication()
    {
        return authentication;
    }

    public void setAuthentication(AuthenticationTraits aAuthentication)
    {
        authentication = aAuthentication;
    }

    public Set<ModelCapability> getCapabilities()
    {
        return Collections.unmodifiableSet(capabilities);
    }

    public void setCapabilities(Set<ModelCapability> aCapabilities)
    {
        capabilities = aCapabilities != null && !aCapabilities.isEmpty() //
                ? EnumSet.copyOf(aCapabilities) //
                : EnumSet.noneOf(ModelCapability.class);
    }

    /**
     * Derived view of {@link ModelCapability#JSON_SCHEMA} membership in {@link #getCapabilities()}.
     * Suppressed from JSON output so persisted traits use the {@code capabilities} field as the
     * source of truth; legacy JSON rows carrying this boolean still deserialize via the setter.
     */
    @JsonIgnore
    public boolean isStructuredOutputSupported()
    {
        return capabilities.contains(ModelCapability.JSON_SCHEMA);
    }

    public void setStructuredOutputSupported(boolean aStructuredOutputSupported)
    {
        if (aStructuredOutputSupported) {
            capabilities.add(ModelCapability.JSON_SCHEMA);
        }
        else {
            capabilities.remove(ModelCapability.JSON_SCHEMA);
        }
    }

    public boolean isJustificationEnabled()
    {
        return justificationEnabled;
    }

    public void setJustificationEnabled(boolean aJustificationEnabled)
    {
        justificationEnabled = aJustificationEnabled;
    }
}
