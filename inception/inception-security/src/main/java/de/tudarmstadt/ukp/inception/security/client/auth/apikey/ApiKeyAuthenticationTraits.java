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
package de.tudarmstadt.ukp.inception.security.client.auth.apikey;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationType;

public class ApiKeyAuthenticationTraits
    extends AuthenticationTraits
{
    private static final long serialVersionUID = -8961589333164099450L;

    public static final String TYPE_ID = "ApiKey";

    @JsonProperty("apiKey")
    private String apiKey;

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String aValue)
    {
        apiKey = aValue;
    }

    @Override
    public AuthenticationType getType()
    {
        return AuthenticationType.API_KEY;
    }
}
