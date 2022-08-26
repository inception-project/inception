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
package de.tudarmstadt.ukp.inception.security.client.auth.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationType;

public class OAuthClientCredentialsAuthenticationTraits
    extends AuthenticationTraits
{
    private static final long serialVersionUID = -2738799956736738180L;

    public static final String TYPE_ID = "OAuthClientCredentials";

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("clientSecret")
    private String clientSecret;

    @JsonProperty("tokenEndpointUrl")
    private String tokenEndpointUrl;

    public String getClientId()
    {
        return clientId;
    }

    public void setClientId(String aClientId)
    {
        clientId = aClientId;
    }

    public String getClientSecret()
    {
        return clientSecret;
    }

    public void setClientSecret(String aClientSecret)
    {
        clientSecret = aClientSecret;
    }

    @Override
    public AuthenticationType getType()
    {
        return AuthenticationType.OAUTH_CLIENT_CREDENTIALS;
    }

    public void setTokenEndpointUrl(String aTokenEndpointUrl)
    {
        tokenEndpointUrl = aTokenEndpointUrl;
    }

    public String getTokenEndpointUrl()
    {
        return tokenEndpointUrl;
    }
}
