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

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

public class ExternalRecommenderV2Api
{
    private static final Logger LOG = LoggerFactory.getLogger(ExternalRecommenderV2Api.class);

    private final HttpClient client;
    private final String remoteUrl;

    public ExternalRecommenderV2Api(URI aRemoteUrl, Duration aTimeout)
    {
        remoteUrl = StringUtils.removeEnd(aRemoteUrl.toString(), "/");

        client = HttpClient.newBuilder() //
                .version(HttpClient.Version.HTTP_1_1) //
                .connectTimeout(aTimeout).build();
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

    public ClassifierInfo getClassifierInfo(String aClassifierId)
        throws ExternalRecommenderApiException
    {
        URI url = URI.create(remoteUrl + "/classifier/" + aClassifierId);
        HttpRequest request = HttpRequest.newBuilder() //
                .uri(url) //
                .GET() //
                .build();

        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            return fromJsonString(ClassifierInfo.class, response.body());
        }
        catch (Exception e) {
            String message = String.format("Error while getting info for classifier [%s]", url);
            LOG.error(message, e);
            throw new ExternalRecommenderApiException(message, e);
        }
    }

    public Document predict(String aClassifierId, String aModelId, Document aDocument)
        throws ExternalRecommenderApiException
    {
        String urlString = String.format("/classifier/%s/%s/predict", aClassifierId,
                URLEncoder.encode(aModelId, StandardCharsets.UTF_8));
        URI url = URI.create(remoteUrl + urlString);

        try {
            HttpRequest request = HttpRequest.newBuilder() //
                    .uri(url) //
                    .header("Content-Type", "application/json") //
                    .POST(BodyPublishers.ofString(JSONUtil.toJsonString(aDocument))) //
                    .build();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            LOG.info("Predicting finished with status code [{}]", response.statusCode());

            if (response.statusCode() == 200) {
                return JSONUtil.fromJsonString(Document.class, response.body());
            }
            else {
                String msg = "Error while predicting: " + response.body();
                LOG.error(msg);
                throw new ExternalRecommenderApiException(msg);
            }
        }
        catch (IOException | InterruptedException e) {
            LOG.error("Error while predicting", e);
            throw new ExternalRecommenderApiException("Error while predicting", e);
        }
    }

}
