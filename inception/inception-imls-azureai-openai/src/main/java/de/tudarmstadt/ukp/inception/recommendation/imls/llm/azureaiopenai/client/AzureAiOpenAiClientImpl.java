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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

import org.apache.commons.io.IOUtils;

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
        client = HttpClient.newBuilder().build();
    }

    public AzureAiOpenAiClientImpl(HttpClient aClient)
    {
        client = aClient;
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
            return IOUtils.toString(response.body(), UTF_8);
        }

        return "";
    }

    @Override
    public String generate(String aUrl, AzureAiChatCompletionRequest aRequest) throws IOException
    {
        var request = HttpRequest.newBuilder() //
                .uri(URI.create(
                        appendIfMissing(aUrl, "/") + "chat/completions?api-version=2023-05-15")) //
                .header(CONTENT_TYPE, "application/json") //
                .header("api-key", aRequest.getApiKey()) //
                .POST(BodyPublishers.ofString(JSONUtil.toJsonString(aRequest), UTF_8)) //
                .build();

        var response = sendRequest(request);

        handleError(response);

        var result = new StringBuilder();
        try (var is = response.body()) {
            var completion = objectMapper.readValue(is, AzureAiChatCompletionResponse.class);
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
