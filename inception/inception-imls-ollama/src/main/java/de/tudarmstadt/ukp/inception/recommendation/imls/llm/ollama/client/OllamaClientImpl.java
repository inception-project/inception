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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class OllamaClientImpl
    implements OllamaClient
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    public static final int HTTP_BAD_REQUEST = 400;

    private final OllamaMetrics metrics;

    protected final HttpClient client;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaClientImpl()
    {
        client = HttpClient.newBuilder().build();
        metrics = new OllamaMetricsImpl();
    }

    public OllamaClientImpl(HttpClient aClient, OllamaMetrics aMetrics)
    {
        client = aClient;
        metrics = aMetrics;
    }

    protected HttpResponse<InputStream> sendRequest(HttpRequest aRequest) throws IOException
    {
        try {
            return client.send(aRequest, HttpResponse.BodyHandlers.ofInputStream());
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
    public String generate(String aUrl, OllamaGenerateRequest aRequest) throws IOException
    {
        return generate(aUrl, aRequest, null);
    }

    @Override
    public String generate(String aUrl, OllamaGenerateRequest aRequest,
            Consumer<OllamaGenerateResponse> aCallback)
        throws IOException
    {
        var request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(aUrl, "/") + "api/generate")) //
                .header(CONTENT_TYPE, "application/json")
                .POST(BodyPublishers.ofString(JSONUtil.toJsonString(aRequest), UTF_8)) //
                .build();

        var rawResponse = sendRequest(request);

        handleError(rawResponse);

        var result = new StringBuilder();
        try (var is = rawResponse.body()) {
            var iter = objectMapper.readerFor(OllamaGenerateResponse.class).readValues(is);
            while (iter.hasNext()) {
                var response = (OllamaGenerateResponse) iter.nextValue();

                if (response.isDone()) {
                    collectMetrics(response);
                }

                result.append(response.getResponse());

                if (aCallback != null) {
                    aCallback.accept(response);
                }
            }
        }

        return result.toString().trim();
    }

    @Override
    public OllamaChatResponse generate(String aUrl, OllamaChatRequest aRequest,
            Consumer<OllamaChatResponse> aCallback)
        throws IOException
    {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending chat request: {}", JSONUtil.toPrettyJsonString(aRequest));
        }

        var request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(aUrl, "/") + "api/chat")) //
                .header(CONTENT_TYPE, "application/json")
                .POST(BodyPublishers.ofString(JSONUtil.toJsonString(aRequest), UTF_8)) //
                .build();

        var rawResponse = sendRequest(request);

        handleError(rawResponse);

        var result = new StringBuilder();
        OllamaChatResponse finalResponse = null;
        try (var is = rawResponse.body()) {
            var iter = objectMapper.readerFor(OllamaChatResponse.class).readValues(is);
            while (iter.hasNext()) {
                var response = (OllamaChatResponse) iter.nextValue();

                if (response.isDone()) {
                    collectMetrics(response);
                    finalResponse = response;
                }

                result.append(response.getMessage().content());

                if (aCallback != null) {
                    aCallback.accept(response);
                }
            }
        }

        var role = finalResponse.getMessage().role();
        finalResponse.setMessage(new OllamaChatMessage(role, result.toString().trim()));

        return finalResponse;
    }

    @Override
    public List<Pair<String, float[]>> embed(String aUrl, OllamaEmbedRequest aRequest)
        throws IOException
    {
        var request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(aUrl, "/") + "api/embed")) //
                .header(CONTENT_TYPE, "application/json")
                .POST(BodyPublishers.ofString(JSONUtil.toJsonString(aRequest), UTF_8)) //
                .build();

        var rawResponse = sendRequest(request);

        if (aRequest.input().size() == 1 && rawResponse.statusCode() >= HTTP_BAD_REQUEST) {
            LOG.error("Error embedding string [{}]", aRequest.input().get(0));
        }

        handleError(rawResponse);

        try (var is = rawResponse.body()) {
            var response = objectMapper.readValue(is, OllamaEmbedResponse.class);

            collectMetrics(response);

            var result = new ArrayList<Pair<String, float[]>>();
            for (int i = 0; i < response.embeddings().length; i++) {
                result.add(Pair.of(aRequest.input().get(i), response.embeddings()[i]));
            }

            return result;
        }
    }

    private void collectMetrics(OllamaTokenMetrics response)
    {
        if (LOG.isDebugEnabled()) {
            var loadDuration = response.getLoadDuration() / 1_000_000_000;
            var promptEvalDuration = response.getPromptEvalDuration() / 1_000_000_000d;
            var promptEvalTokenPerSecond = response.getPromptEvalCount() / promptEvalDuration;
            var evalDuration = response.getEvalDuration() / 1_000_000_000d;
            var evalTokenPerSecond = evalDuration > 0 ? response.getEvalCount() / evalDuration : 0;
            var totalDuration = response.getTotalDuration() / 1_000_000_000;
            LOG.debug("Tokens  - prompt: {} ({} per sec) response: {} ({} per sec)", //
                    response.getPromptEvalCount(), //
                    promptEvalTokenPerSecond, //
                    response.getEvalCount(), //
                    evalTokenPerSecond);
            LOG.debug("Timings - load: {}sec  prompt: {}sec  response: {}sec  total: {}sec", //
                    loadDuration, promptEvalDuration, evalDuration, totalDuration);
        }

        if (metrics != null) {
            metrics.handleResponse(response);
        }
    }

    private void collectMetrics(OllamaEmbedResponse response)
    {
        if (LOG.isDebugEnabled()) {
            var loadDurationMs = response.loadDuration() / 1_000_000;
            var evalDurationMs = (response.totalDuration() - response.loadDuration()) / 1_000_000d;
            var promptEvalTokenPerSecond = evalDurationMs > 0
                    ? (double) response.promptEvalCount() / evalDurationMs * 1000.0
                    : 0;
            var totalDurationMs = response.totalDuration() / 1_000_000d;
            LOG.debug("Tokens  - prompt: {} ({} per sec)", //
                    response.promptEvalCount(), //
                    format("%.2f", promptEvalTokenPerSecond));
            LOG.debug("Timings - load: {}ms  response: {}ms  total: {}ms", //
                    loadDurationMs, evalDurationMs, totalDurationMs);
        }
    }

    @Override
    public List<OllamaModel> listModels(String aUrl) throws IOException
    {
        var request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(aUrl, "/") + "api/tags")) //
                .header(CONTENT_TYPE, "application/json").GET() //
                .timeout(TIMEOUT) //
                .build();

        var response = sendRequest(request);

        handleError(response);

        try (var is = response.body()) {
            return objectMapper.readValue(is, OllamaTagsResponse.class).getModels();
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
