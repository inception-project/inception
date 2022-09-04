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

import static de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthGrantType.AUTHORIZATION_CODE;
import static de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthGrantType.CLIENT_CREDENTIALS;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import de.tudarmstadt.ukp.inception.support.http.HttpClientImplBase;

public class OAuthAuthenticationClientImpl
    extends HttpClientImplBase
    implements OAuthAuthenticationClient
{
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String GRANT_TYPE = "grant_type";

    private static final String CODE = "code";
    private static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

    private String clientId;
    private String clientSecret;
    private String tokenEndpointUrl;
    private String redirectUri;

    private OAuthAuthenticationClientImpl(Builder builder)
    {
        super(builder.client);
        this.clientId = builder.clientId;
        this.clientSecret = builder.clientSecret;
        this.tokenEndpointUrl = builder.tokenEndpointUrl;
        this.redirectUri = builder.redirectUri;
    }

    @Override
    public boolean requiresSignIn(OAuthSession aSession)
    {
        Date validUntil = aSession.getRefreshTokenValidUntil();

        if (validUntil == null && aSession.getRefreshToken() != null) {
            // If the services does not provide information on how long the refresh token is
            // valid, we assume that it is valid as long as we have a token.
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
        parameters.put(GRANT_TYPE, AUTHORIZATION_CODE.toHeaderValue());
        parameters.put(CLIENT_ID, clientId);
        if (clientSecret != null) {
            parameters.put(CLIENT_SECRET, clientSecret);
        }
        parameters.put(REDIRECT_URI, redirectUri);
        parameters.put(CODE, aCode);

        HttpRequest request = HttpRequest.newBuilder() //
                .POST(BodyPublishers.ofString(urlEncodeParameters(parameters)))
                .uri(URI.create(tokenEndpointUrl)) //
                .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED) //
                .build();

        long submitTime = System.currentTimeMillis();
        var response = deserializeResponse(sendRequest(request), OAuthAccessTokenResponse.class);
        response.setSubmitTime(submitTime);
        return response;
    }

    public OAuthAccessTokenResponse getToken() throws IOException
    {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put(GRANT_TYPE, CLIENT_CREDENTIALS.toHeaderValue());
        parameters.put(CLIENT_ID, clientId);
        if (clientSecret != null) {
            parameters.put(CLIENT_SECRET, clientSecret);
        }

        HttpRequest request = HttpRequest.newBuilder() //
                .POST(BodyPublishers.ofString(urlEncodeParameters(parameters)))
                .uri(URI.create(tokenEndpointUrl)) //
                .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED) //
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
        parameters.put(GRANT_TYPE, REFRESH_TOKEN);
        parameters.put(CLIENT_ID, clientId);
        parameters.put(REFRESH_TOKEN, aRefreshToken);

        HttpRequest request = HttpRequest.newBuilder() //
                .POST(BodyPublishers.ofString(urlEncodeParameters(parameters)))
                .uri(URI.create(tokenEndpointUrl)) //
                .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED) //
                .build();

        long submitTime = System.currentTimeMillis();
        var response = deserializeResponse(sendRequest(request), OAuthAccessTokenResponse.class);
        response.setSubmitTime(submitTime);
        return response;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private HttpClient client;
        private String clientId;
        private String clientSecret;
        private String tokenEndpointUrl;
        private String redirectUri;

        private Builder()
        {
        }

        public Builder withHttpClient(HttpClient aClient)
        {
            this.client = aClient;
            return this;
        }

        public Builder withClientId(String aClientId)
        {
            this.clientId = aClientId;
            return this;
        }

        public Builder withClientSecret(String aClientSecret)
        {
            this.clientSecret = aClientSecret;
            return this;
        }

        public Builder withTokenEndpointUrl(String aTokenEndpointUrl)
        {
            this.tokenEndpointUrl = aTokenEndpointUrl;
            return this;
        }

        public Builder withRedirectUri(String aRedirectUri)
        {
            this.redirectUri = aRedirectUri;
            return this;
        }

        public OAuthAuthenticationClientImpl build()
        {
            return new OAuthAuthenticationClientImpl(this);
        }
    }

}
