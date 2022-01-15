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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.preferences.Key;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgTokenResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElgSessionState
    implements Serializable
{
    private static final long serialVersionUID = 5378118174761262214L;

    public static final Key<ElgSessionState> KEY_ELG_SESSION_STATE = new Key<>(
            ElgSessionState.class, "recommender/elg/session");

    private String accessToken;
    private long accessTokenValidUntil;
    private String refreshToken;
    private long refreshTokenValidUntil;

    public String getAccessToken()
    {
        return accessToken;
    }

    public void setAccessToken(String aAccessToken)
    {
        accessToken = aAccessToken;
    }

    public long getAccessTokenValidUntil()
    {
        return accessTokenValidUntil;
    }

    public void setAccessTokenValidUntil(long aAccessTokenValidUntil)
    {
        accessTokenValidUntil = aAccessTokenValidUntil;
    }

    public String getRefreshToken()
    {
        return refreshToken;
    }

    public void setRefreshToken(String aRefreshToken)
    {
        refreshToken = aRefreshToken;
    }

    public long getRefreshTokenValidUntil()
    {
        return refreshTokenValidUntil;
    }

    public void setRefreshTokenValidUntil(long aRefreshTokenValidUntil)
    {
        refreshTokenValidUntil = aRefreshTokenValidUntil;
    }

    public void update(ElgTokenResponse response)
    {
        setAccessToken(response.getAccessToken());
        if (response.getExpiresIn() > 0) {
            setAccessTokenValidUntil(response.getSubmitTime() + (response.getExpiresIn() * 1000));
        }
        else {
            setAccessTokenValidUntil(0);
        }
        setRefreshToken(response.getRefreshToken());
        if (response.getRefreshExpiresIn() > 0) {
            setRefreshTokenValidUntil(
                    response.getSubmitTime() + (response.getRefreshExpiresIn() * 1000));
        }
        else {
            setRefreshTokenValidUntil(0);
        }
    }
    
    public void clear() {
        setAccessToken(null);
        setAccessTokenValidUntil(0);
        setRefreshToken(null);
        setRefreshTokenValidUntil(0);
    }
}
