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
package de.tudarmstadt.ukp.inception.assistant;

import static org.apache.commons.lang3.StringUtils.isBlank;

import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;

/**
 * Small helpers for building the authentication carried on an {@code LlmEndpoint} from the
 * assistant's configured API keys.
 */
public final class LlmAuth
{
    private LlmAuth()
    {
        // No instances
    }

    /**
     * Builds {@link ApiKeyAuthenticationTraits} for the given API key, or returns {@code null} when
     * the key is blank (i.e. no authentication configured).
     */
    public static AuthenticationTraits apiKeyAuth(String aApiKey)
    {
        if (isBlank(aApiKey)) {
            return null;
        }

        var auth = new ApiKeyAuthenticationTraits();
        auth.setApiKey(aApiKey);
        return auth;
    }
}
