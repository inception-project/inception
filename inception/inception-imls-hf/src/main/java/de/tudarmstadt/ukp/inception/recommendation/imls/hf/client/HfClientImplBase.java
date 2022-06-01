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
package de.tudarmstadt.ukp.inception.recommendation.imls.hf.client;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HfClientImplBase
{
    public static final int HTTP_BAD_REQUEST = 400;

    protected final HttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HfClientImplBase()
    {
        this.client = HttpClient.newBuilder().build();
    }

    public HfClientImplBase(HttpClient aClient)
    {
        this.client = aClient;
    }

    protected HttpResponse<String> sendRequest(HttpRequest aRequest) throws IOException
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

    protected String getResponseBody(HttpResponse<String> response)
    {
        if (response.body() != null) {
            return response.body();
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
}
