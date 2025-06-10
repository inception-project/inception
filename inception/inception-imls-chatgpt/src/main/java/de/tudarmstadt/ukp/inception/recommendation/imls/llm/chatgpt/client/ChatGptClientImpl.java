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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class ChatGptClientImpl
    implements ChatGptClient
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String AUTHORIZATION = "Authorization";

    private static final String LIMIT_REQUESTS_DAY = "x-ratelimit-limit-requests-day";
    private static final String LIMIT_TOKENS_MINUTE = "x-ratelimit-limit-tokens-minute";
    private static final String REMAINING_REQUESTS_DAY = "x-ratelimit-remaining-requests-day";
    private static final String REMAINING_TOKENS_MINUTE = "x-ratelimit-remaining-tokens-minute";
    private static final String RESET_REQUESTS_DAY = "x-ratelimit-reset-requests-day";
    private static final String RESET_TOKENS_MINUTE = "x-ratelimit-reset-tokens-minute";

    public static final int HTTP_BAD_REQUEST = 400;

    protected final HttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatGptClientImpl()
    {
        client = HttpClient.newBuilder().build();
    }

    public ChatGptClientImpl(HttpClient aClient)
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
    public String chat(String aUrl, ChatCompletionRequest aRequest) throws IOException
    {
        var startTime = System.currentTimeMillis();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending chat request: {}", JSONUtil.toPrettyJsonString(aRequest));
        }

        var request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(aUrl, "/") + "v1/chat/completions")) //
                .header(CONTENT_TYPE, "application/json") //
                .header(AUTHORIZATION, "Bearer " + aRequest.getApiKey()) //
                .POST(BodyPublishers.ofString(JSONUtil.toJsonString(aRequest), UTF_8)) //
                .build();

        var response = sendRequest(request);

        handleError(response);

        var result = new StringBuilder();
        try (var is = response.body()) {
            var buffer = IOUtils.toString(is, UTF_8);
            if (LOG.isTraceEnabled()) {
                for (var header : response.headers().map().entrySet()) {
                    LOG.trace("Header - {}: {}", header.getKey(), header.getValue());
                }
                LOG.trace("JSON Response: {}", buffer);
            }

            var rateLimitInfo = new RateLimitInfo();
            response.headers().firstValue(LIMIT_REQUESTS_DAY) //
                    .map(NumberUtils::toInt) //
                    .ifPresent(rateLimitInfo::setLimitRequestsDay);
            response.headers().firstValue(LIMIT_TOKENS_MINUTE) //
                    .map(NumberUtils::toInt) //
                    .ifPresent(rateLimitInfo::setLimitTokensMinute);
            response.headers().firstValue(REMAINING_REQUESTS_DAY) //
                    .map(NumberUtils::toInt) //
                    .ifPresent(rateLimitInfo::setRemainingRequestsDay);
            response.headers().firstValue(REMAINING_TOKENS_MINUTE) //
                    .map(NumberUtils::toInt) //
                    .ifPresent(rateLimitInfo::setRemainingTokensMinute);
            response.headers().firstValue(RESET_REQUESTS_DAY) //
                    .map(NumberUtils::toDouble) //
                    .ifPresent(rateLimitInfo::setResetRequestsDay);
            response.headers().firstValue(RESET_TOKENS_MINUTE) //
                    .map(NumberUtils::toDouble) //
                    .ifPresent(rateLimitInfo::setResetTokensMinute);

            var completion = objectMapper.readValue(buffer, ChatCompletionResponse.class);

            if (LOG.isDebugEnabled() && completion.getTimeInfo() != null
                    && completion.getUsage() != null) {
                var promptEvalTokenPerSecond = completion.getUsage().getPromptTokens()
                        / completion.getTimeInfo().getPromptTime();
                var evalTokenPerSecond = completion.getUsage().getCompletionTokens()
                        / completion.getTimeInfo().getCompletionTime();
                LOG.debug("Tokens  - prompt: {} ({} per sec) response: {} ({} per sec)", //
                        completion.getUsage().getPromptTokens(), //
                        promptEvalTokenPerSecond, //
                        completion.getUsage().getCompletionTokens(), //
                        evalTokenPerSecond);
                LOG.debug("Timings - queue: {}sec  prompt: {}sec  response: {}s  total: {}sec", //
                        completion.getTimeInfo().getQueueTime(),
                        completion.getTimeInfo().getPromptTime(),
                        completion.getTimeInfo().getCompletionTime(),
                        completion.getTimeInfo().getTotalTime());
            }

            result.append(completion.getChoices().get(0).getMessage().getContent());
        }

        LOG.trace("[{}] responds ({} ms): [{}]", aRequest.getModel(),
                currentTimeMillis() - startTime, result.toString());

        return result.toString();
    }

    public List<ChatGptModel> listModels(String aUrl, ListModelsRequest aRequest) throws IOException
    {
        var request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(aUrl, "/") + "v1/models")) //
                .header(CONTENT_TYPE, "application/json").GET() //
                .header(AUTHORIZATION, "Bearer " + aRequest.getApiKey()) //
                .timeout(Duration.ofSeconds(10)) //
                .build();

        var response = sendRequest(request);

        handleError(response);

        try (var is = response.body()) {
            return objectMapper.readValue(is, ChatGptModelResponse.class).getData().stream() //
                    .sorted(comparing(ChatGptModel::getId)) //
                    .toList();
        }
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
