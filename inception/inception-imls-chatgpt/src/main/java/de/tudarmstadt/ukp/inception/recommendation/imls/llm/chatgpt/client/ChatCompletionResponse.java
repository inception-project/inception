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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionResponse
{
    private @JsonProperty("model") String model;
    private @JsonProperty("created") long createdAt;
    private @JsonProperty("choices") List<ChatCompletionChoice> choices;
    private @JsonProperty("usage") ChatCompletionUsage usage;
    private @JsonProperty("time_info") ChatCompletionTimeInfo timeInfo;

    public String getModel()
    {
        return model;
    }

    public void setModel(String aModel)
    {
        model = aModel;
    }

    public long getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(long aCreatedAt)
    {
        createdAt = aCreatedAt;
    }

    public List<ChatCompletionChoice> getChoices()
    {
        return choices;
    }

    public void setChoices(List<ChatCompletionChoice> aChoices)
    {
        choices = aChoices;
    }

    public ChatCompletionUsage getUsage()
    {
        return usage;
    }

    public void setUsage(ChatCompletionUsage aUsage)
    {
        usage = aUsage;
    }

    public ChatCompletionTimeInfo getTimeInfo()
    {
        return timeInfo;
    }

    public void setTimeInfo(ChatCompletionTimeInfo aTimeInfo)
    {
        timeInfo = aTimeInfo;
    }
}
