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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ElgTokenResponse
{
    private @JsonIgnore long submitTime;
    private @JsonProperty("access_token") String accessToken;
    private @JsonProperty("expires_in") long expiresIn;
    private @JsonProperty("refresh_expires_in") long refreshExpiresIn;
    private @JsonProperty("refresh_token") String refreshToken;
    private @JsonProperty("token_type") String tokenType;
    private @JsonProperty("not-before-policy") String notBeforePolicy;
    private @JsonProperty("session_state") String sessionState;
    private @JsonProperty("scope") String scope;

    public void setSubmitTime(long aSubmitTime)
    {
        submitTime = aSubmitTime;
    }

    public long getSubmitTime()
    {
        return submitTime;
    }

    public String getAccessToken()
    {
        return accessToken;
    }

    public void setAccessToken(String aAccessToken)
    {
        accessToken = aAccessToken;
    }

    public long getExpiresIn()
    {
        return expiresIn;
    }

    public void setExpiresIn(long aExpiresIn)
    {
        expiresIn = aExpiresIn;
    }

    public long getRefreshExpiresIn()
    {
        return refreshExpiresIn;
    }

    public void setRefreshExpiresIn(long aRefreshExpiresIn)
    {
        refreshExpiresIn = aRefreshExpiresIn;
    }

    public String getRefreshToken()
    {
        return refreshToken;
    }

    public void setRefreshToken(String aRefreshToken)
    {
        refreshToken = aRefreshToken;
    }

    public String getTokenType()
    {
        return tokenType;
    }

    public void setTokenType(String aTokenType)
    {
        tokenType = aTokenType;
    }

    public String getNotBeforePolicy()
    {
        return notBeforePolicy;
    }

    public void setNotBeforePolicy(String aNotBeforePolicy)
    {
        notBeforePolicy = aNotBeforePolicy;
    }

    public String getSessionState()
    {
        return sessionState;
    }

    public void setSessionState(String aSessionState)
    {
        sessionState = aSessionState;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope(String aScope)
    {
        scope = aScope;
    }
}
