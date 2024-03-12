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

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.recommendation.imls.azureaiopenai.client.GenerateResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.ExtractionMode;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.prompt.PromptingMode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Preset
    implements Serializable
{
    private static final long serialVersionUID = -4643627579405577703L;

    private String name;

    private String model;

    private String prompt;

    private boolean raw;

    private GenerateResponseFormat format;

    private PromptingMode promptingMode = PromptingMode.PER_ANNOTATION;

    private ExtractionMode extractionMode = ExtractionMode.RESPONSE_AS_LABEL;

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
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

    public boolean isRaw()
    {
        return raw;
    }

    public void setRaw(boolean aRaw)
    {
        raw = aRaw;
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
}
