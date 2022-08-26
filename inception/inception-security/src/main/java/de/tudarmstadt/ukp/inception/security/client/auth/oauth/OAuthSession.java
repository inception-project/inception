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

import static java.time.Duration.ZERO;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public interface OAuthSession
{
    Instant getLastUpdate();

    void setLastUpdate(Instant aDate);

    Duration getAccessTokenExpiresIn();

    void setAccessTokenExpiresIn(Duration aTime);

    Duration getRefreshTokenExpiresIn();

    void setRefreshTokenExpiresIn(Duration aTime);

    String getAccessToken();

    void setAccessToken(String aAccessToken);

    Date getAccessTokenValidUntil();

    void setAccessTokenValidUntil(Date aAccessTokenValidUntil);

    String getRefreshToken();

    void setRefreshToken(String aRefreshToken);

    Date getRefreshTokenValidUntil();

    void setRefreshTokenValidUntil(Date aRefreshTokenValidUntil);

    default void update(OAuthAccessTokenResponse response)
    {
        setLastUpdate(Instant.ofEpochMilli(response.getSubmitTime()));

        setAccessToken(response.getAccessToken());
        setAccessTokenExpiresIn(Duration.ofSeconds(response.getExpiresIn()));

        if (response.getExpiresIn() > 0) {
            setAccessTokenValidUntil(
                    new Date(response.getSubmitTime() + (response.getExpiresIn() * 1000)));
        }
        else {
            setAccessTokenValidUntil(null);
        }

        setRefreshToken(response.getRefreshToken());
        setRefreshTokenExpiresIn(Duration.ofSeconds(response.getRefreshExpiresIn()));

        if (response.getRefreshExpiresIn() > 0) {
            setRefreshTokenValidUntil(
                    new Date(response.getSubmitTime() + (response.getRefreshExpiresIn() * 1000)));
        }
        else {
            setRefreshTokenValidUntil(null);
        }
    }

    default void clear()
    {
        setAccessTokenExpiresIn(ZERO);
        setRefreshTokenExpiresIn(ZERO);
        setLastUpdate(null);
        setAccessToken(null);
        setAccessTokenValidUntil(null);
        setRefreshToken(null);
        setRefreshTokenValidUntil(null);
    }
}
