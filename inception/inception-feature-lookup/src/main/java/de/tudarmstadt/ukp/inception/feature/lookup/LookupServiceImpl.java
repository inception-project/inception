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
package de.tudarmstadt.ukp.inception.feature.lookup;

import static de.tudarmstadt.ukp.inception.feature.lookup.config.LookupServicePropertiesImpl.HARD_QUERY_CONTEXT_LENGTH;
import static de.tudarmstadt.ukp.inception.feature.lookup.config.LookupServicePropertiesImpl.HARD_QUERY_LENGTH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;

import de.tudarmstadt.ukp.inception.feature.lookup.config.LookupServiceProperties;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationType;
import de.tudarmstadt.ukp.inception.security.client.auth.header.HeaderAuthenticationTraits;
import de.tudarmstadt.ukp.inception.support.http.HttpClientImplBase;

public class LookupServiceImpl
    extends HttpClientImplBase
    implements LookupService
{
    static final String PARAM_QUERY = "q";
    static final String PARAM_QUERY_CONTEXT = "qc";
    static final String PARAM_LIMIT = "l";
    static final String PARAM_ID = "id";

    private final LookupServiceProperties properties;

    public LookupServiceImpl(LookupServiceProperties aProperties)
    {
        super(HttpClient.newBuilder().connectTimeout(aProperties.getConnectTimeout()).build());

        properties = aProperties;
    }

    @Override
    public Optional<LookupEntry> lookup(LookupFeatureTraits aTraits, String aId) throws IOException
    {
        var queryParameters = Map.of( //
                PARAM_ID, aId);

        var requestBuilder = HttpRequest.newBuilder() //
                .uri(URI.create(
                        aTraits.getRemoteUrl() + "?" + urlEncodeParameters(queryParameters))) //
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE) //
                .timeout(properties.getReadTimeout()).GET();

        applyAuthenticationHeaderIfRequired(aTraits, requestBuilder);

        HttpRequest request = requestBuilder.build();

        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString(UTF_8));
            if (response.statusCode() == HTTP_NOT_FOUND) {
                return Optional.empty();
            }

            handleBadRequest(response);

            return Optional.ofNullable(deserializeResponse(response, LookupEntry.class));
        }
        catch (IOException e) {
            throw e;
        }
        catch (InterruptedException e) {
            throw new IOException("Error while sending request: " + e.getMessage(), e);
        }
    }

    private void applyAuthenticationHeaderIfRequired(LookupFeatureTraits aTraits,
            Builder requestBuilder)
    {
        if (aTraits.getAuthenticationType() == AuthenticationType.HEADER) {
            HeaderAuthenticationTraits authTraits = (HeaderAuthenticationTraits) aTraits
                    .getAuthentication();
            requestBuilder.header(authTraits.getHeader(), authTraits.getValue());
        }
    }

    @Override
    public List<LookupEntry> query(LookupFeatureTraits aTraits, String aQuery, String aQueryContext)
        throws IOException
    {
        var limit = Math.min(properties.getHardMaxResults(), aTraits.getLimit());
        var query = abbreviate(aQuery, "", HARD_QUERY_LENGTH);

        var queryParameters = new LinkedHashMap<String, String>();
        queryParameters.put(PARAM_QUERY, query);
        queryParameters.put(PARAM_LIMIT, Integer.toString(limit));
        if (!isBlank(aQueryContext) && aTraits.isIncludeQueryContext()) {
            var queryContext = abbreviate(aQueryContext, "", HARD_QUERY_CONTEXT_LENGTH);
            queryParameters.put(PARAM_QUERY_CONTEXT, queryContext);
        }

        var requestBuilder = HttpRequest.newBuilder() //
                .uri(URI.create(
                        aTraits.getRemoteUrl() + "?" + urlEncodeParameters(queryParameters))) //
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE) //
                .timeout(properties.getReadTimeout()).GET();

        applyAuthenticationHeaderIfRequired(aTraits, requestBuilder);

        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = sendRequest(request);

        List<LookupEntry> result = deserializeResponse(response, LookupQueryResponse.class);
        if (result.size() > limit) {
            result = result.subList(0, limit);
        }
        return result;
    }

    private static class LookupQueryResponse
        extends ArrayList<LookupEntry>
    {
        private static final long serialVersionUID = -6064962216841132866L;

        // Materialize generic types for deserialization
    }
}
