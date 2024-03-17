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
package de.tudarmstadt.ukp.inception.security.client.auth;

import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.basic.BasicAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.header.HeaderAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthClientCredentialsAuthenticationTraits;

public enum AuthenticationType
{
    BASIC(BasicAuthenticationTraits.TYPE_ID), //
    HEADER(HeaderAuthenticationTraits.TYPE_ID), //
    API_KEY(ApiKeyAuthenticationTraits.TYPE_ID), //
    OAUTH_CLIENT_CREDENTIALS(OAuthClientCredentialsAuthenticationTraits.TYPE_ID);

    private final String id;

    AuthenticationType(String aId)
    {
        id = aId;
    }

    public String getId()
    {
        return id;
    }

    @Override
    public String toString()
    {
        return id;
    }
}
