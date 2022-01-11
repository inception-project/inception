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

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgCatalogEntityDetails;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgCatalogSearchResponse;

public class ElgCatalogClientImpl implements ElgCatalogClient
{
    private static final int HTTP_BAD_REQUEST = 400;

    private String searchUrl = "https://live.european-language-grid.eu/catalogue_backend/api/registry/search/";

    private final HttpClient client;

    public ElgCatalogClientImpl()
    {
        this(HttpClient.newBuilder().build());
    }

    public ElgCatalogClientImpl(HttpClient aClient)
    {
        client = aClient;
    }

    @Override
    public ElgCatalogEntityDetails details(String aDetailUrl) throws IOException
    {
        HttpRequest request = HttpRequest.newBuilder() //
                .uri(URI.create(aDetailUrl)) //
                .build();

        HttpResponse<String> response = sendRequest(request);

        return deserializeResponse(response, ElgCatalogEntityDetails.class);
    }

    @Override
    public ElgCatalogSearchResponse search(String aSearch) throws IOException
    {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("elg_compatible_service", "true");
        queryParameters.put("function__term", "Named Entity Recognition");
        if (isNotBlank(aSearch)) {
            queryParameters.put("search", aSearch);
        }

        StringBuilder uriBuilder = new StringBuilder(searchUrl);
        uriBuilder.append('?');
        if (!queryParameters.isEmpty()) {
            for (Entry<String, String> param : queryParameters.entrySet()) {
                uriBuilder.append(URLEncoder.encode(param.getKey(), UTF_8));
                uriBuilder.append('=');
                uriBuilder.append(URLEncoder.encode(param.getValue(), UTF_8));
                uriBuilder.append("&");
            }
        }

        HttpRequest request = HttpRequest.newBuilder() //
                .uri(URI.create(uriBuilder.toString())) //
                .build();

        HttpResponse<String> response = sendRequest(request);

        return deserializeResponse(response, ElgCatalogSearchResponse.class);
    }

    private <T> T deserializeResponse(HttpResponse<String> response, Class<T> aResponseType)
        throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(response.body(), aResponseType);
        }
        catch (IOException e) {
            throw new IOException("Error while deserializing response!", e);
        }
    }

    private HttpResponse<String> sendRequest(HttpRequest aRequest) throws IOException
    {
        try {
            HttpResponse<String> response = client.send(aRequest, BodyHandlers.ofString(UTF_8));

            // If the response indicates that the request was not successful,
            // then it does not make sense to go on and try to decode the XMI
            if (response.statusCode() >= HTTP_BAD_REQUEST) {
                String responseBody = getResponseBody(response);
                String msg = format("Request was not successful: [%d] - [%s]",
                        response.statusCode(), responseBody);
                throw new IOException(msg);
            }

            return response;
        }
        catch (IOException | InterruptedException e) {
            throw new IOException("Error while sending request: " + e.getMessage(), e);
        }
    }

    private String getResponseBody(HttpResponse<String> response)
    {
        if (response.body() != null) {
            return response.body();
        }
        else {
            return "";
        }
    }
}
