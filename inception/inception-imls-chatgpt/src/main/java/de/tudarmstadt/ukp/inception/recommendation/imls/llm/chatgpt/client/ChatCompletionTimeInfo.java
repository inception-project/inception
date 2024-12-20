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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionTimeInfo
{
    private @JsonProperty("queue_time") double queueTime;
    private @JsonProperty("prompt_time") double promptTime;
    private @JsonProperty("completion_time") double completionTime;
    private @JsonProperty("total_time") double totalTime;
    private @JsonProperty("created") long created;

    public double getQueueTime()
    {
        return queueTime;
    }

    public void setQueueTime(double aQueueTime)
    {
        queueTime = aQueueTime;
    }

    public double getPromptTime()
    {
        return promptTime;
    }

    public void setPromptTime(double aPromptTime)
    {
        promptTime = aPromptTime;
    }

    public double getCompletionTime()
    {
        return completionTime;
    }

    public void setCompletionTime(double aCompletionTime)
    {
        completionTime = aCompletionTime;
    }

    public double getTotalTime()
    {
        return totalTime;
    }

    public void setTotalTime(double aTotalTime)
    {
        totalTime = aTotalTime;
    }

    public long getCreated()
    {
        return created;
    }

    public void setCreated(long aCreated)
    {
        created = aCreated;
    }
}
