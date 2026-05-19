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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.client;

import java.util.EnumSet;
import java.util.Set;

import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraits;

/**
 * Identifies a concrete LLM endpoint: which provider, where it lives, how to authenticate, which
 * model to use, and which {@link ModelCapability capabilities} the configured model is declared to
 * support. Typically derived from {@code LlmRecommenderTraits} (recommender side) or from assistant
 * configuration.
 */
public record LlmEndpoint( //
        String providerId, //
        String url, //
        String model, //
        AuthenticationTraits auth, //
        Set<ModelCapability> capabilities)
{
    public LlmEndpoint
    {
        capabilities = capabilities != null //
                ? EnumSet.copyOf(capabilities.isEmpty() //
                        ? EnumSet.noneOf(ModelCapability.class) //
                        : capabilities)
                : EnumSet.noneOf(ModelCapability.class);
    }

    public LlmEndpoint(String aProviderId, String aUrl, String aModel, AuthenticationTraits aAuth)
    {
        this(aProviderId, aUrl, aModel, aAuth, EnumSet.noneOf(ModelCapability.class));
    }
}
