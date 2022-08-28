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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg.client;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;

import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgUserInfoResponse;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthAccessTokenResponse;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthGrantType;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthSession;
import de.tudarmstadt.ukp.inception.support.http.HttpClientImplBase;

public class ElgAuthenticationClientImpl
    extends HttpClientImplBase
    implements ElgAuthenticationClient
{
    private static final String ELG_CLIENT_ID = "elg-oob";
    private static final String CODE_URL = "https://live.european-language-grid.eu/auth/realms/ELG/protocol/openid-connect/auth?client_id=elg-oob&redirect_uri=urn:ietf:wg:oauth:2.0:oob&response_type=code&scope=offline_access";
    private static final String TOKEN_URL = "https://live.european-language-grid.eu/auth/realms/ELG/protocol/openid-connect/token/";
    private static final String USER_INFO_URL = "https://live.european-language-grid.eu/auth/realms/ELG/protocol/openid-connect/userinfo/";

    public ElgAuthenticationClientImpl()
    {
        super();
    }

    public ElgAuthenticationClientImpl(HttpClient aClient)
    {
        super(aClient);
    }

    @Override
    public String getCodeUrl()
    {
        return CODE_URL;
    }

    @Override
    public boolean requiresSignIn(OAuthSession aSession)
    {
        Date validUntil = aSession.getRefreshTokenValidUntil();

        if (validUntil == null && aSession.getRefreshToken() != null) {
            // ELG currently seems to not provide information on how long the refresh token is
            // valid, so we assume that it is valid as long as we have a token.
            return false;
        }

        if (validUntil == null) {
            return true;
        }

        // Refresh if the token will expire in less than 60 seconds
        return Instant.now().isAfter(validUntil.toInstant().minus(60, SECONDS));
    }

    @Override
    public boolean requiresRefresh(OAuthSession aSession)
    {
        Date validUntil = aSession.getAccessTokenValidUntil();
        if (validUntil == null) {
            return true;
        }

        return Instant.now().isAfter(validUntil.toInstant());
    }

    @Override
    public boolean refreshSessionIfNecessary(OAuthSession aSession) throws IOException
    {
        if (!requiresRefresh(aSession)) {
            return false;
        }

        aSession.update(refreshToken(aSession.getRefreshToken()));
        return true;
    }

    @Override
    public OAuthAccessTokenResponse getToken(String aCode) throws IOException
    {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("grant_type", OAuthGrantType.AUTHORIZATION_CODE.toHeaderValue());
        parameters.put("client_id", ELG_CLIENT_ID);
        parameters.put("redirect_uri", "urn:ietf:wg:oauth:2.0:oob");
        parameters.put("code", aCode);

        HttpRequest request = HttpRequest.newBuilder() //
                .POST(BodyPublishers.ofString(urlEncodeParameters(parameters)))
                .uri(URI.create(TOKEN_URL)) //
                .header("Content-Type", "application/x-www-form-urlencoded") //
                .build();

        long submitTime = System.currentTimeMillis();
        var response = deserializeResponse(sendRequest(request), OAuthAccessTokenResponse.class);
        response.setSubmitTime(submitTime);
        return response;
    }

    @Override
    public OAuthAccessTokenResponse refreshToken(String aRefreshToken) throws IOException
    {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("grant_type", "refresh_token");
        parameters.put("client_id", ELG_CLIENT_ID);
        parameters.put("refresh_token", aRefreshToken);

        HttpRequest request = HttpRequest.newBuilder() //
                .POST(BodyPublishers.ofString(urlEncodeParameters(parameters)))
                .uri(URI.create(TOKEN_URL)) //
                .header("Content-Type", "application/x-www-form-urlencoded") //
                .build();

        long submitTime = System.currentTimeMillis();
        var response = deserializeResponse(sendRequest(request), OAuthAccessTokenResponse.class);
        response.setSubmitTime(submitTime);
        return response;
    }

    @Override
    public ElgUserInfoResponse getUserInfo(String aAccessToken) throws IOException
    {
        HttpRequest request = HttpRequest.newBuilder() //
                .uri(URI.create(USER_INFO_URL)) //
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + aAccessToken).build();

        HttpResponse<String> response = sendRequest(request);

        return deserializeResponse(response, ElgUserInfoResponse.class);
    }
}
