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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama;

import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaGenerateResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.option.LlmRecommenderTraits;

public class OllamaRecommenderTraits
    extends LlmRecommenderTraits
{
    public static final String DEFAULT_OLLAMA_URL = "http://localhost:11434/";

    private static final long serialVersionUID = -8760059914187478368L;

    private String url = DEFAULT_OLLAMA_URL;

    private String model;

    private boolean raw;

    private OllamaGenerateResponseFormat format;

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

    public boolean isRaw()
    {
        return raw;
    }

    public void setRaw(boolean aRaw)
    {
        raw = aRaw;
    }

    public OllamaGenerateResponseFormat getFormat()
    {
        return format;
    }

    public void setFormat(OllamaGenerateResponseFormat aFormat)
    {
        format = aFormat;
    }
}
