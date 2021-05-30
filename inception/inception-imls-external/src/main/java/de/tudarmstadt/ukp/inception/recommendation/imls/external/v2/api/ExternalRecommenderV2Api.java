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
package de.tudarmstadt.ukp.inception.recommendation.imls.external.v2.api;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.getObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

public class ExternalRecommenderV2Api
{
    private static final Logger LOG = LoggerFactory.getLogger(ExternalRecommenderV2Api.class);

    private final HttpClient client;
    private final String remoteUrl;
    private final Duration connectionTimeout;

    public ExternalRecommenderV2Api(URI aRemoteUrl, Duration aConnectionTimeout)
    {
        remoteUrl = StringUtils.removeEnd(aRemoteUrl.toString(), "/");
        connectionTimeout = aConnectionTimeout;

        client = HttpClient.newBuilder() //
                .version(HttpClient.Version.HTTP_1_1) //
                .connectTimeout(aConnectionTimeout).build();
    }

    public ExternalRecommenderV2Api(URI aRemoteUrl)
    {
        this(aRemoteUrl, Duration.ofSeconds(10));
    }

    public boolean isReachable()
    {
        URI url = URI.create(remoteUrl + "/ping");
        HttpRequest request = HttpRequest.newBuilder() //
                .uri(url) //
                .GET() //
                .build();

        try {
            HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
            return response.statusCode() == 200;
        }
        catch (Exception e) {
            LOG.warn("Error while pinging external recommender server [{}]", url, e);
            return false;
        }
    }

    public List<ClassifierInfo> getAvailableClassifiers()
    {
        URI url = URI.create(remoteUrl + "/classifier");
        HttpRequest request = HttpRequest.newBuilder() //
                .uri(url) //
                .GET() //
                .build();

        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            return getObjectMapper().readValue(response.body(), new TypeReference<>()
            {
            });

        }
        catch (Exception e) {
            LOG.warn("Error while getting available classifiers [{}]", url, e);
            return Collections.emptyList();
        }
    }

    public Optional<ClassifierInfo> getClassifierInfo(String aClassifierId)
    {
        URI url = URI.create(remoteUrl + "/classifier/" + aClassifierId);
        HttpRequest request = HttpRequest.newBuilder() //
                .uri(url) //
                .GET() //
                .build();

        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            return Optional.of(fromJsonString(ClassifierInfo.class, response.body()));
        }
        catch (Exception e) {
            LOG.warn("Error while getting info for classifier [{}]", url, e);
            return Optional.empty();
        }
    }
}
