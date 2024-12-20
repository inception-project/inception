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

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ListModelsRequest
{
    private final @JsonIgnore String apiKey;

    private ListModelsRequest(Builder builder)
    {
        apiKey = builder.apiKey;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String apiKey;

        private Builder()
        {
        }

        public Builder withApiKey(String aApiKey)
        {
            this.apiKey = aApiKey;
            return this;
        }

        public ListModelsRequest build()
        {
            return new ListModelsRequest(this);
        }
    }
}
