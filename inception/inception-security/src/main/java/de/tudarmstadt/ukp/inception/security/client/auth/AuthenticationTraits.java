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

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.basic.BasicAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.header.HeaderAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthClientCredentialsAuthenticationTraits;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ //
        @Type(value = BasicAuthenticationTraits.class, //
                name = BasicAuthenticationTraits.TYPE_ID),
        @Type(value = ApiKeyAuthenticationTraits.class, //
                name = ApiKeyAuthenticationTraits.TYPE_ID),
        @Type(value = HeaderAuthenticationTraits.class, //
                name = HeaderAuthenticationTraits.TYPE_ID),
        @Type(value = OAuthClientCredentialsAuthenticationTraits.class, //
                name = OAuthClientCredentialsAuthenticationTraits.TYPE_ID) })
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AuthenticationTraits
    implements Serializable
{
    private static final long serialVersionUID = 5358860222920519699L;

    public abstract AuthenticationType getType();
}
