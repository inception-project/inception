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
import static java.util.Arrays.asList;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.List;

import org.springframework.http.HttpHeaders;

import de.tudarmstadt.ukp.inception.recommendation.imls.hf.model.HfEntityGroup;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.model.HfTokenClassificationTaskRequest;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class HfInferenceClientImpl
    extends HfClientImplBase
    implements HfInferenceClient
{
    private String inferenceUrl = "https://api-inference.huggingface.co/models";

    public HfInferenceClientImpl()
    {
        super();
    }

    public HfInferenceClientImpl(HttpClient aClient)
    {
        super(aClient);
    }

    @Override
    public List<HfEntityGroup> invokeService(String aModelId, String aToken, String aText)
        throws IOException
    {
        HfTokenClassificationTaskRequest hfRequest = new HfTokenClassificationTaskRequest();
        hfRequest.setInputs(aText);

        HttpRequest request = HttpRequest.newBuilder() //
                .uri(URI.create(inferenceUrl + "/" + aModelId)) //
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + aToken) //
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .POST(BodyPublishers.ofString(JSONUtil.toJsonString(hfRequest), UTF_8)) //
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

        return asList(deserializeResponse(response, HfEntityGroup[].class));
    }
}
