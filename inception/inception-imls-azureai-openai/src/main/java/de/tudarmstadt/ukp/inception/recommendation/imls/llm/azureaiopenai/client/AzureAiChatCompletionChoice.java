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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureAiChatCompletionChoice
{
    private @JsonProperty("index") int index;
    private @JsonProperty("message") AzureAiChatCompletionMessage message;
    private @JsonProperty("finish_reason") String finishReason;

    public int getIndex()
    {
        return index;
    }

    public void setIndex(int aIndex)
    {
        index = aIndex;
    }

    public AzureAiChatCompletionMessage getMessage()
    {
        return message;
    }

    public void setMessage(AzureAiChatCompletionMessage aMessage)
    {
        message = aMessage;
    }

    public String getFinishReason()
    {
        return finishReason;
    }

    public void setFinishReason(String aFinishReason)
    {
        finishReason = aFinishReason;
    }
}
