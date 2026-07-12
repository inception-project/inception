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
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ServerSentEvent;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ServerSentEventReader;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import tools.jackson.databind.ObjectMapper;

public class ChatGptClientImpl
    implements ChatGptClient
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String AUTHORIZATION = "Authorization";

    /** SSE {@code data:} payload that signals the end of the stream. */
    private static final String SSE_DONE_MARKER = "[DONE]";

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
    public ChatCompletionResponse chat(String aUrl, ChatCompletionRequest aRequest)
        throws IOException
    {
        var startTime = System.currentTimeMillis();

        var response = sendChatRequest(aUrl, aRequest);

        handleError(response);

        ChatCompletionResponse completion;
        try (var is = response.body()) {
            var buffer = IOUtils.toString(is, UTF_8);
            if (LOG.isTraceEnabled()) {
                for (var header : response.headers().map().entrySet()) {
                    LOG.trace("Header - {}: {}", header.getKey(), header.getValue());
                }
                LOG.trace("JSON Response: {}", buffer);
            }

            parseRateLimitInfo(response);

            completion = objectMapper.readValue(buffer, ChatCompletionResponse.class);

            logTimings(completion);
        }

        LOG.trace("[{}] responds ({} ms)", aRequest.getModel(), currentTimeMillis() - startTime);

        return completion;
    }

    @Override
    public ChatCompletionResponse chat(String aUrl, ChatCompletionRequest aRequest,
            Consumer<String> aContentCallback)
        throws IOException
    {
        var startTime = System.currentTimeMillis();

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

        parseRateLimitInfo(response);

        try (var events = ServerSentEventReader.parse(response.body())) {
            var completion = assembleSseStream(events, aRequest.getModel(), aContentCallback);

            LOG.trace("[{}] responds ({} ms)", aRequest.getModel(),
                    currentTimeMillis() - startTime);

            return completion;
        }
    }

    /**
     * Assembles the SSE event stream of an OpenAI streaming chat completion into a single
     * {@link ChatCompletionResponse}. The terminal {@code data: [DONE]} sentinel stops assembly;
     * {@code choices[].delta.content} is concatenated (and forwarded to {@code aContentCallback}
     * per delta), fragmented {@code delta.tool_calls} are merged by index, and the final
     * {@code usage} chunk (present when {@code stream_options.include_usage} was set) is captured.
     * Package-private so it can be unit-tested with canned events and no live server.
     */
    ChatCompletionResponse assembleSseStream(Stream<ServerSentEvent> aEvents, String aRequestModel,
            Consumer<String> aContentCallback)
        throws IOException
    {
        var content = new StringBuilder();
        // tool-call fragments keyed by their streaming index, so out-of-order deltas still merge.
        var toolCallsByIndex = new TreeMap<Integer, ChatCompletionToolCall>();
        String finishReason = null;
        String role = null;
        ChatCompletionUsage usage = null;
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

            var chunk = objectMapper.readValue(payload, ChatCompletionResponse.class);

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

    private HttpRequest buildChatHttpRequest(String aUrl, ChatCompletionRequest aRequest)
        throws IOException
    {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending chat request: {}", JSONUtil.toPrettyJsonString(aRequest));
        }

        var request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(aUrl, "/") + "v1/chat/completions")) //
                .header(CONTENT_TYPE, "application/json");

        if (aRequest.getApiKey() != null) {
            request.header(AUTHORIZATION, "Bearer " + aRequest.getApiKey());
        }

        request.POST(BodyPublishers.ofString(JSONUtil.toJsonString(aRequest), UTF_8));

        return request.build();
    }

    private HttpResponse<InputStream> sendChatRequest(String aUrl, ChatCompletionRequest aRequest)
        throws IOException
    {
        return sendRequest(buildChatHttpRequest(aUrl, aRequest));
    }

    private void logTimings(ChatCompletionResponse completion)
    {
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
                    completion.getTimeInfo().getQueueTime(), //
                    completion.getTimeInfo().getPromptTime(), //
                    completion.getTimeInfo().getCompletionTime(), //
                    completion.getTimeInfo().getTotalTime());
        }
    }

    private RateLimitInfo parseRateLimitInfo(HttpResponse<?> response)
    {
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
        return rateLimitInfo;
    }

    /**
     * Merges the tool-call deltas of one streaming chunk into the per-index accumulator. OpenAI
     * fragments {@code function.arguments} across chunks and only sends {@code id} / {@code name}
     * on the first fragment of each call, so the id/name are captured once and the argument strings
     * are concatenated.
     */
    private static void accumulateToolCalls(TreeMap<Integer, ChatCompletionToolCall> aAccumulator,
            List<ChatCompletionToolCall> aDeltaCalls)
    {
        if (aDeltaCalls == null) {
            return;
        }

        for (var deltaCall : aDeltaCalls) {
            var index = deltaCall.getIndex() != null ? deltaCall.getIndex() : aAccumulator.size();
            var target = aAccumulator.computeIfAbsent(index, i -> {
                var tc = new ChatCompletionToolCall();
                tc.setIndex(i);
                tc.setType("function");
                tc.setFunction(new ChatCompletionToolCall.Function());
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

    private static ChatCompletionResponse assembleStreamedResponse(String aModel, String aRole,
            String aContent, List<ChatCompletionToolCall> aToolCalls, String aFinishReason,
            ChatCompletionUsage aUsage)
    {
        var message = new ChatCompletionMessage(aRole != null ? aRole : "assistant", aContent,
                aToolCalls.isEmpty() ? null : aToolCalls, null);

        var choice = new ChatCompletionChoice();
        choice.setIndex(0);
        choice.setMessage(message);
        choice.setFinishReason(aFinishReason);

        var response = new ChatCompletionResponse();
        response.setModel(aModel);
        response.setChoices(new ArrayList<>(List.of(choice)));
        response.setUsage(aUsage);
        return response;
    }

    @Override
    public List<ChatGptModel> listModels(String aUrl, ListModelsRequest aRequest) throws IOException
    {
        var request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(aUrl, "/") + "v1/models")) //
                .header(CONTENT_TYPE, "application/json").GET() //
                .timeout(Duration.ofSeconds(10));

        if (aRequest.getApiKey() != null) {
            request.header(AUTHORIZATION, "Bearer " + aRequest.getApiKey());
        }

        var response = sendRequest(request.build());

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
