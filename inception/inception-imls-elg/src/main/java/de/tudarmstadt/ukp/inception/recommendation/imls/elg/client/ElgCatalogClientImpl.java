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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgCatalogEntity;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgCatalogEntityDetails;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgCatalogSearchResponse;
import de.tudarmstadt.ukp.inception.support.http.HttpClientImplBase;

public class ElgCatalogClientImpl
    extends HttpClientImplBase
    implements ElgCatalogClient
{
    private String searchUrl = "https://live.european-language-grid.eu/catalogue_backend/api/registry/search/";

    public ElgCatalogClientImpl()
    {
        super();
    }

    public ElgCatalogClientImpl(HttpClient aClient)
    {
        super(aClient);
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
    public Optional<ElgCatalogEntity> findServiceById(long aServiceId) throws IOException
    {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("id", Long.toString(aServiceId));
        return queryCatalog(queryParameters).getResults().stream().findFirst();
    }

    @Override
    public ElgCatalogSearchResponse search(String aSearch) throws IOException
    {
        Map<String, String> queryParameters = new LinkedHashMap<>();
        queryParameters.put("resource_type__term", "Tool/Service");
        queryParameters.put("elg_compatible_service__term", "Yes");
        if (isNotBlank(aSearch)) {
            queryParameters.put("search", aSearch);
        }

        return queryCatalog(queryParameters);
    }

    public ElgCatalogSearchResponse queryCatalog(Map<String, String> aQueryParameters)
        throws IOException
    {
        StringBuilder uriBuilder = new StringBuilder(searchUrl);
        if (!aQueryParameters.isEmpty()) {
            uriBuilder.append('?');
            uriBuilder.append(urlEncodeParameters(aQueryParameters));
        }

        HttpRequest request = HttpRequest.newBuilder() //
                .uri(URI.create(uriBuilder.toString())) //
                .build();

        HttpResponse<String> response = sendRequest(request);

        return deserializeResponse(response, ElgCatalogSearchResponse.class);
    }
}
