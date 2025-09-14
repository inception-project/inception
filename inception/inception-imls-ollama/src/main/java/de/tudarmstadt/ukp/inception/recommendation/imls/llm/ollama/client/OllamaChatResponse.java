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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaChatResponse
    implements OllamaTokenMetrics
{
    private @JsonProperty("model") String model;
    private @JsonProperty("created_at") String createdAt;
    private @JsonProperty("message") OllamaChatMessage message;
    private @JsonProperty("done") boolean done;
    private @JsonProperty("context") List<Integer> context;

    private @JsonProperty("total_duration") long totalDuration;
    private @JsonProperty("load_duration") long loadDuration;
    private @JsonProperty("prompt_eval_duration") long promptEvalDuration;
    private @JsonProperty("eval_duration") long evalDuration;
    private @JsonProperty("prompt_eval_count") int promptEvalCount;
    private @JsonProperty("eval_count") int evalCount;

    public String getModel()
    {
        return model;
    }

    public void setModel(String aModel)
    {
        model = aModel;
    }

    public String getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(String aCreatedAt)
    {
        createdAt = aCreatedAt;
    }

    public OllamaChatMessage getMessage()
    {
        return message;
    }

    public void setMessage(OllamaChatMessage aMessage)
    {
        message = aMessage;
    }

    public boolean isDone()
    {
        return done;
    }

    public void setDone(boolean aDone)
    {
        done = aDone;
    }

    public List<Integer> getContext()
    {
        return context;
    }

    public void setContext(List<Integer> aContext)
    {
        context = aContext;
    }

    @Override
    public long getTotalDuration()
    {
        return totalDuration;
    }

    public void setTotalDuration(long aTotalDuration)
    {
        totalDuration = aTotalDuration;
    }

    @Override
    public long getLoadDuration()
    {
        return loadDuration;
    }

    public void setLoadDuration(long aLoadDuration)
    {
        loadDuration = aLoadDuration;
    }

    @Override
    public long getPromptEvalDuration()
    {
        return promptEvalDuration;
    }

    public void setPromptEvalDuration(long aPromptEvalDuration)
    {
        promptEvalDuration = aPromptEvalDuration;
    }

    @Override
    public long getEvalDuration()
    {
        return evalDuration;
    }

    public void setEvalDuration(long aEvalDuration)
    {
        evalDuration = aEvalDuration;
    }

    @Override
    public int getPromptEvalCount()
    {
        return promptEvalCount;
    }

    public void setPromptEvalCount(int aPromptEvalCount)
    {
        promptEvalCount = aPromptEvalCount;
    }

    @Override
    public int getEvalCount()
    {
        return evalCount;
    }

    public void setEvalCount(int aEvalCount)
    {
        evalCount = aEvalCount;
    }
}
