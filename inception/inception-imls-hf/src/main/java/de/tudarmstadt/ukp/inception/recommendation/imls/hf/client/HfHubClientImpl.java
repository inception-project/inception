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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import de.tudarmstadt.ukp.inception.recommendation.imls.hf.model.HfModelCard;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.model.HfModelDetails;

public class HfHubClientImpl
    extends HfClientImplBase
    implements HfHubClient
{
    private String searchUrl = "https://huggingface.co/api/models";

    public HfHubClientImpl()
    {
        super();
    }

    public HfHubClientImpl(HttpClient aClient)
    {
        super(aClient);
    }

    @Override
    public HfModelDetails details(String aModelId) throws IOException
    {
        HttpRequest request = HttpRequest.newBuilder() //
                .uri(URI.create(searchUrl + "/" + aModelId)) //
                .build();

        HttpResponse<String> response = sendRequest(request);

        return deserializeResponse(response, HfModelDetails.class);
    }

    @Override
    public HfModelCard[] listModels(String aSearch) throws IOException
    {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("cardData", "true");
        queryParameters.put("pipeline_tag", "token-classification");
        queryParameters.put("other", "endpoints_compatible");
        if (isNotBlank(aSearch)) {
            queryParameters.put("search", aSearch);
        }

        return queryCatalog(queryParameters);
    }

    public HfModelCard[] queryCatalog(Map<String, String> aQueryParameters) throws IOException
    {
        StringBuilder uriBuilder = new StringBuilder(searchUrl);
        if (!aQueryParameters.isEmpty()) {
            uriBuilder.append('?');
            uriBuilder.append(urlEncodeParameters(aQueryParameters));
        }

        HttpRequest request = HttpRequest.newBuilder() //
                .uri(URI.create(uriBuilder.toString())) //
                .timeout(Duration.ofSeconds(10)) //
                .build();

        HttpResponse<String> response = sendRequest(request);

        return deserializeResponse(response, HfModelCard[].class);
    }
}
