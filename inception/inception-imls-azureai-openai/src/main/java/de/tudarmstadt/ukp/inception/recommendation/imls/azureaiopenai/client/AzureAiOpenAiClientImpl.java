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
package de.tudarmstadt.ukp.inception.recommendation.imls.azureaiopenai.client;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class AzureAiOpenAiClientImpl
    implements AzureAiOpenAiClient
{
    public static final int HTTP_BAD_REQUEST = 400;

    protected final HttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AzureAiOpenAiClientImpl()
    {
        this.client = HttpClient.newBuilder().build();
    }

    public AzureAiOpenAiClientImpl(HttpClient aClient)
    {
        this.client = aClient;
    }

    protected HttpResponse<InputStream> sendRequest(HttpRequest aRequest) throws IOException
    {
        try {
            var response = client.send(aRequest, HttpResponse.BodyHandlers.ofInputStream());

            handleError(response);

            return response;
        }
        catch (IOException | InterruptedException e) {
            throw new IOException("Error while sending request: " + e.getMessage(), e);
        }
    }

    protected String getResponseBody(HttpResponse<InputStream> response) throws IOException
    {
        if (response.body() != null) {
            return IOUtils.toString(response.body(), StandardCharsets.UTF_8);
        }
        else {
            return "";
        }
    }

    protected <T> T deserializeResponse(HttpResponse<String> response, Class<T> aType)
        throws IOException
    {
        try {
            return objectMapper.readValue(response.body(), aType);
        }
        catch (IOException e) {
            throw new IOException("Error while deserializing server response!", e);
        }
    }

    protected String urlEncodeParameters(Map<String, String> aParameters)
    {
        if (aParameters.isEmpty()) {
            return "";
        }
        StringBuilder uriBuilder = new StringBuilder();
        for (Entry<String, String> param : aParameters.entrySet()) {
            if (uriBuilder.length() > 0) {
                uriBuilder.append("&");
            }
            uriBuilder.append(URLEncoder.encode(param.getKey(), UTF_8));
            uriBuilder.append('=');
            uriBuilder.append(URLEncoder.encode(param.getValue(), UTF_8));
        }

        return uriBuilder.toString();
    }

    @Override
    public String generate(String aUrl, ChatCompletionRequest aRequest) throws IOException
    {
        var request = HttpRequest.newBuilder() //
                .uri(URI.create(
                        appendIfMissing(aUrl, "/") + "chat/completions?api-version=2023-05-15")) //
                .header(HttpHeaders.CONTENT_TYPE, "application/json") //
                .header("api-key", aRequest.getApiKey()) //
                .POST(BodyPublishers.ofString(JSONUtil.toJsonString(aRequest), UTF_8)) //
                .build();

        var response = sendRequest(request);

        handleError(response);

        var result = new StringBuilder();
        try (var is = response.body()) {
            var completion = objectMapper.readValue(is, ChatCompletionResponse.class);
            result.append(completion.getChoices().get(0).getMessage().getContent());
        }

        return result.toString();
    }

    private void handleError(HttpResponse<InputStream> response) throws IOException
    {
        if (response.statusCode() >= HTTP_BAD_REQUEST) {
            var responseBody = getResponseBody(response);
            var msg = format("Request was not successful: [%d] - [%s]", response.statusCode(),
                    responseBody);
            throw new IOException(msg);
        }
    }
}
