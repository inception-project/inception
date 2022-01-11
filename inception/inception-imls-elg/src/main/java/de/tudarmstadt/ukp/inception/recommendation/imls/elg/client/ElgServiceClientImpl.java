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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.apache.uima.cas.CAS;
import org.springframework.http.HttpHeaders;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgResponseContainer;

public class ElgServiceClientImpl
    implements ElgServiceClient
{
    private static final int HTTP_BAD_REQUEST = 400;

    private final HttpClient client;

    public ElgServiceClientImpl()
    {
        this(HttpClient.newBuilder().build());
    }

    public ElgServiceClientImpl(HttpClient aClient)
    {
        client = aClient;
    }

    @Override
    public ElgResponse invokeService(String aServiceSync, String aToken, CAS aCas)
        throws IOException
    {
        HttpRequest request = HttpRequest.newBuilder() //
                .uri(URI.create(aServiceSync)) //
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + aToken) //
                .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                .POST(BodyPublishers.ofString(aCas.getDocumentText(), UTF_8)) //
                .build();

        HttpResponse<String> response = sendRequest(request);

        // If the response indicates that the request was not successful,
        // then it does not make sense to go on and try to decode the XMI
        if (response.statusCode() >= HTTP_BAD_REQUEST) {
            String responseBody = getResponseBody(response);
            String msg = format("Request was not successful: [%d] - [%s]", response.statusCode(),
                    responseBody);
            throw new IOException(msg);
        }

        return deserializeServiceResponse(response).getResponse();
    }

    private ElgResponseContainer deserializeServiceResponse(HttpResponse<String> response)
        throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(response.body(), ElgResponseContainer.class);
        }
        catch (IOException e) {
            throw new IOException("Error while deserializing prediction response!", e);
        }
    }

    private HttpResponse<String> sendRequest(HttpRequest aRequest) throws IOException
    {
        try {
            return client.send(aRequest, BodyHandlers.ofString(UTF_8));
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
