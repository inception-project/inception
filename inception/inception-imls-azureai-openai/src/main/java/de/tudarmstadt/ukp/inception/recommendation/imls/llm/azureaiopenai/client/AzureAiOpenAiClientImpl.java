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
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ServerSentEvent;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ServerSentEventReader;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import tools.jackson.databind.ObjectMapper;

public class AzureAiOpenAiClientImpl
    implements AzureAiOpenAiClient
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final int HTTP_BAD_REQUEST = 400;

    /** SSE {@code data:} payload that signals the end of the stream. */
    private static final String SSE_DONE_MARKER = "[DONE]";

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
    public AzureAiChatCompletionResponse generate(String aUrl,
            AzureAiChatCompletionRequest aRequest)
        throws IOException
    {
        var response = sendRequest(buildChatHttpRequest(aUrl, aRequest));

        handleError(response);

        try (var is = response.body()) {
            return objectMapper.readValue(is, AzureAiChatCompletionResponse.class);
        }
    }

    @Override
    public AzureAiChatCompletionResponse generate(String aUrl,
            AzureAiChatCompletionRequest aRequest, Consumer<String> aContentCallback)
        throws IOException
    {
        HttpResponse<Stream<String>> response;
        try {
            var request = buildChatHttpRequest(aUrl, aRequest);
            response = client.send(request, HttpResponse.BodyHandlers.ofLines());
        }
        catch (IOException | InterruptedException e) {
            throw new IOException("Error while sending request: " + e.getMessage(), e);
        }

        if (response.statusCode() >= HTTP_BAD_REQUEST) {
            var body = response.body().collect(Collectors.joining("\n"));
            throw new IOException(
                    format("Request was not successful: [%d] - [%s]", response.statusCode(), body));
        }

        try (var events = ServerSentEventReader.parse(response.body())) {
            return assembleSseStream(events, aRequest.getModel(), aContentCallback);
        }
    }

    /**
     * Assembles the SSE event stream of an Azure OpenAI streaming chat completion into a single
     * {@link AzureAiChatCompletionResponse}. The terminal {@code data: [DONE]} sentinel stops
     * assembly; {@code choices[].delta.content} is concatenated (and forwarded to
     * {@code aContentCallback} per delta), fragmented {@code delta.tool_calls} are merged by index,
     * and the final {@code usage} chunk (present when {@code stream_options.include_usage} was set)
     * is captured. Package-private so it can be unit-tested with canned events and no live server.
     */
    AzureAiChatCompletionResponse assembleSseStream(Stream<ServerSentEvent> aEvents,
            String aRequestModel, Consumer<String> aContentCallback)
        throws IOException
    {
        var content = new StringBuilder();
        // tool-call fragments keyed by their streaming index, so out-of-order deltas still merge.
        var toolCallsByIndex = new TreeMap<Integer, AzureAiChatCompletionToolCall>();
        String finishReason = null;
        String role = null;
        AzureAiChatCompletionUsage usage = null;
        String model = aRequestModel;

        var iter = aEvents.iterator();
        while (iter.hasNext()) {
            var payload = iter.next().data();
            if (payload == null) {
                continue;
            }
            if (SSE_DONE_MARKER.equals(payload)) {
                break;
            }

            LOG.trace("SSE chunk: {}", payload);

            var chunk = objectMapper.readValue(payload, AzureAiChatCompletionResponse.class);

            if (chunk.getModel() != null) {
                model = chunk.getModel();
            }

            if (chunk.getUsage() != null) {
                usage = chunk.getUsage();
            }

            if (chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
                continue;
            }

            var choice = chunk.getChoices().get(0);
            if (choice.getFinishReason() != null) {
                finishReason = choice.getFinishReason();
            }

            var delta = choice.getDelta();
            if (delta == null) {
                continue;
            }

            if (delta.getRole() != null) {
                role = delta.getRole();
            }

            if (delta.getContent() != null && !delta.getContent().isEmpty()) {
                content.append(delta.getContent());
                if (aContentCallback != null) {
                    aContentCallback.accept(delta.getContent());
                }
            }

            accumulateToolCalls(toolCallsByIndex, delta.getToolCalls());
        }

        return assembleStreamedResponse(model, role, content.toString(),
                new ArrayList<>(toolCallsByIndex.values()), finishReason, usage);
    }

    private HttpRequest buildChatHttpRequest(String aUrl, AzureAiChatCompletionRequest aRequest)
        throws IOException
    {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending chat request: {}", JSONUtil.toPrettyJsonString(aRequest));
        }

        // The deployment is embedded in the base URL and the API version is fixed; the model is not
        // sent on the wire (it is @JsonIgnore in the request).
        return HttpRequest.newBuilder() //
                .uri(URI.create(
                        appendIfMissing(aUrl, "/") + "chat/completions?api-version=2023-05-15")) //
                .header(CONTENT_TYPE, "application/json") //
                .header("api-key", aRequest.getApiKey()) //
                .POST(BodyPublishers.ofString(JSONUtil.toJsonString(aRequest), UTF_8)) //
                .build();
    }

    /**
     * Merges the tool-call deltas of one streaming chunk into the per-index accumulator. Azure
     * OpenAI fragments {@code function.arguments} across chunks and only sends {@code id} /
     * {@code name} on the first fragment of each call, so the id/name are captured once and the
     * argument strings are concatenated.
     */
    private static void accumulateToolCalls(
            TreeMap<Integer, AzureAiChatCompletionToolCall> aAccumulator,
            List<AzureAiChatCompletionToolCall> aDeltaCalls)
    {
        if (aDeltaCalls == null) {
            return;
        }

        for (var deltaCall : aDeltaCalls) {
            var index = resolveToolCallIndex(aAccumulator, deltaCall.getIndex(), deltaCall.getId());
            var target = aAccumulator.computeIfAbsent(index, i -> {
                var tc = new AzureAiChatCompletionToolCall();
                tc.setIndex(i);
                tc.setType("function");
                tc.setFunction(new AzureAiChatCompletionToolCall.Function());
                return tc;
            });

            if (deltaCall.getId() != null) {
                target.setId(deltaCall.getId());
            }
            if (deltaCall.getType() != null) {
                target.setType(deltaCall.getType());
            }

            var deltaFn = deltaCall.getFunction();
            if (deltaFn != null) {
                if (deltaFn.getName() != null) {
                    target.getFunction().setName(deltaFn.getName());
                }
                if (deltaFn.getArguments() != null) {
                    var existing = target.getFunction().getArguments();
                    target.getFunction().setArguments(
                            (existing != null ? existing : "") + deltaFn.getArguments());
                }
            }
        }
    }

    /**
     * Resolves the accumulator key for a tool-call delta. Well-behaved OpenAI-compatible servers
     * send an explicit {@code index} on every fragment; that is always authoritative. When a server
     * omits it, a fragment carrying an {@code id} marks the start of a new call (next free index),
     * whereas an id-less continuation fragment (only {@code arguments}) belongs to the call started
     * most recently, i.e. the highest index seen so far. Using {@code aAccumulator.size()} as the
     * fallback mis-merged such continuations into a fresh index, splitting one call into several.
     */
    private static int resolveToolCallIndex(
            TreeMap<Integer, AzureAiChatCompletionToolCall> aAccumulator, Integer aIndex,
            String aId)
    {
        if (aIndex != null) {
            return aIndex;
        }

        if (aId == null && !aAccumulator.isEmpty()) {
            return aAccumulator.lastKey();
        }

        return aAccumulator.isEmpty() ? 0 : aAccumulator.lastKey() + 1;
    }

    private static AzureAiChatCompletionResponse assembleStreamedResponse(String aModel,
            String aRole, String aContent, List<AzureAiChatCompletionToolCall> aToolCalls,
            String aFinishReason, AzureAiChatCompletionUsage aUsage)
    {
        var message = new AzureAiChatCompletionMessage(aRole != null ? aRole : "assistant",
                aContent, aToolCalls.isEmpty() ? null : aToolCalls, null);

        var choice = new AzureAiChatCompletionChoice();
        choice.setIndex(0);
        choice.setMessage(message);
        choice.setFinishReason(aFinishReason);

        var response = new AzureAiChatCompletionResponse();
        response.setModel(aModel);
        response.setChoices(new ArrayList<>(List.of(choice)));
        response.setUsage(aUsage);
        return response;
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
