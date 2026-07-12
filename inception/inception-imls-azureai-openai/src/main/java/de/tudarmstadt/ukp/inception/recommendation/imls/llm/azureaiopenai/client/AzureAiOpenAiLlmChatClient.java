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

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client.AzureAiResponseFormatType.JSON_OBJECT;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client.AzureAiResponseFormatType.JSON_SCHEMA;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatOptionsTranslator.applyIfPresent;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatChunk;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatOptions;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.FinishReason;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmChatClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmEndpoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ModelCapability;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ToolCall;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.UsageInfo;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import tools.jackson.databind.JsonNode;

/**
 * {@link LlmChatClient} adapter for Azure OpenAI Service. The Azure URL embeds the deployment name
 * and API version, so {@link LlmEndpoint#model()} is not sent on the wire (it is captured by the
 * URL). Delegates to the existing {@link AzureAiOpenAiClient}; behaviour is a faithful translation,
 * with no recommender-specific defaults applied here.
 */
public class AzureAiOpenAiLlmChatClient
    implements LlmChatClient
{
    public static final String ID = "azure-openai";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AzureAiOpenAiClient client;

    public AzureAiOpenAiLlmChatClient(AzureAiOpenAiClient aClient)
    {
        client = aClient;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public Set<ModelCapability> supportedCapabilities()
    {
        return EnumSet.of( //
                ModelCapability.CHAT, //
                ModelCapability.JSON_SCHEMA, //
                ModelCapability.STREAMING, //
                ModelCapability.TOOLS);
    }

    @Override
    public ChatResult chat(LlmEndpoint aEndpoint, List<ChatMessage> aMessages, ChatOptions aOptions)
        throws IOException
    {
        var request = buildChatRequest(aEndpoint, aMessages, aOptions, false);
        var response = client.generate(aEndpoint.url(), request);
        return toChatResult(response);
    }

    @Override
    public ChatResult chatStream(LlmEndpoint aEndpoint, List<ChatMessage> aMessages,
            ChatOptions aOptions, Consumer<ChatChunk> aOnChunk)
        throws IOException
    {
        var request = buildChatRequest(aEndpoint, aMessages, aOptions, true);
        // Azure OpenAI standard chat has no separate thinking channel, so only the content delta is
        // surfaced; thinking stays null.
        Consumer<String> contentCallback = delta -> aOnChunk.accept(new ChatChunk(delta, null));
        var response = client.generate(aEndpoint.url(), request, contentCallback);
        return toChatResult(response);
    }

    private AzureAiChatCompletionRequest buildChatRequest(LlmEndpoint aEndpoint,
            List<ChatMessage> aMessages, ChatOptions aOptions, boolean aStream)
    {
        var messages = aMessages.stream() //
                .map(AzureAiOpenAiLlmChatClient::toChatCompletionMessage) //
                .toList();

        var builder = AzureAiChatCompletionRequest.builder() //
                .withApiKey(apiKey(aEndpoint)) //
                .withModel(aEndpoint.model()) //
                .withMessages(messages) //
                .withFormat(toResponseFormat(aOptions)) //
                .withStream(aStream);

        // Translate the provider-neutral fields to Azure OpenAI's parameters; explicit options
        // below override them.
        applyIfPresent(aOptions.temperature(),
                v -> builder.withOption(AzureAiChatCompletionRequest.TEMPERATURE, v));
        applyIfPresent(aOptions.topP(),
                v -> builder.withOption(AzureAiChatCompletionRequest.TOP_P, v));

        if (aOptions.options() != null) {
            builder.withExtraOptions(aOptions.options());
        }

        if (aOptions.tools() != null && !aOptions.tools().isEmpty()) {
            builder.withTools(aOptions.tools());
        }

        return builder.build();
    }

    private static AzureAiChatCompletionMessage toChatCompletionMessage(ChatMessage aMessage)
    {
        List<AzureAiChatCompletionToolCall> toolCalls = null;
        if (aMessage.toolCalls() != null && !aMessage.toolCalls().isEmpty()) {
            toolCalls = new ArrayList<>();
            for (var call : aMessage.toolCalls()) {
                toolCalls.add(toWireToolCall(call));
            }
        }

        // Azure OpenAI requires tool_call_id on tool-result (role=tool) messages so the model can
        // match the result to its preceding call.
        return new AzureAiChatCompletionMessage(aMessage.role().getName(), aMessage.content(),
                toolCalls, aMessage.toolCallId());
    }

    private static AzureAiChatCompletionToolCall toWireToolCall(ToolCall aCall)
    {
        var function = new AzureAiChatCompletionToolCall.Function();
        function.setName(aCall.name());
        // Azure OpenAI carries the arguments as a JSON string, not an object.
        function.setArguments(aCall.arguments() != null && !aCall.arguments().isNull() //
                ? aCall.arguments().toString() //
                : "{}");

        var toolCall = new AzureAiChatCompletionToolCall();
        toolCall.setId(aCall.id());
        toolCall.setType("function");
        toolCall.setFunction(function);
        return toolCall;
    }

    private static ChatResult toChatResult(AzureAiChatCompletionResponse aResponse)
    {
        if (aResponse.getChoices() == null || aResponse.getChoices().isEmpty()) {
            return new ChatResult(new ChatMessage(ChatMessage.Role.ASSISTANT, ""), emptyList(),
                    null, toUsageInfo(aResponse.getUsage()));
        }

        var choice = aResponse.getChoices().get(0);
        var message = choice.getMessage();

        var content = message != null && message.getContent() != null ? message.getContent() : "";

        var toolCalls = toToolCalls(message);

        var finishReason = toFinishReason(choice.getFinishReason());

        var usage = toUsageInfo(aResponse.getUsage());

        return new ChatResult(new ChatMessage(ChatMessage.Role.ASSISTANT, content, null, null),
                toolCalls, finishReason, usage);
    }

    private static List<ToolCall> toToolCalls(AzureAiChatCompletionMessage aMessage)
    {
        if (aMessage == null || aMessage.getToolCalls() == null
                || aMessage.getToolCalls().isEmpty()) {
            return emptyList();
        }

        var result = new ArrayList<ToolCall>();
        for (var wireCall : aMessage.getToolCalls()) {
            var name = wireCall.getFunction() != null ? wireCall.getFunction().getName() : null;
            var arguments = wireCall.getFunction() != null ? wireCall.getFunction().getArguments()
                    : null;
            result.add(new ToolCall(wireCall.getId(), name, parseArguments(arguments)));
        }
        return result;
    }

    /**
     * Parses the Azure OpenAI tool-call {@code arguments} JSON <em>string</em> into a
     * {@link JsonNode}. A malformed or non-object payload must not abort the whole turn, so on any
     * parse failure - or if the payload does not represent a JSON object - an empty object node is
     * returned instead.
     */
    private static JsonNode parseArguments(String aArguments)
    {
        if (aArguments == null || aArguments.isBlank()) {
            return JSONUtil.getObjectMapper().createObjectNode();
        }

        try {
            var node = JSONUtil.getObjectMapper().readTree(aArguments);
            if (node == null || !node.isObject()) {
                LOG.warn("Ignoring non-object tool-call arguments: [{}]", aArguments);
                return JSONUtil.getObjectMapper().createObjectNode();
            }
            return node;
        }
        catch (Exception e) {
            LOG.warn("Ignoring malformed tool-call arguments: [{}]", aArguments, e);
            return JSONUtil.getObjectMapper().createObjectNode();
        }
    }

    private static FinishReason toFinishReason(String aFinishReason)
    {
        if (aFinishReason == null) {
            return null;
        }

        return switch (aFinishReason) {
        case "stop" -> FinishReason.STOP;
        case "length" -> FinishReason.LENGTH;
        case "tool_calls" -> FinishReason.TOOL_CALLS;
        case "content_filter" -> FinishReason.CONTENT_FILTER;
        default -> FinishReason.OTHER;
        };
    }

    private static UsageInfo toUsageInfo(AzureAiChatCompletionUsage aUsage)
    {
        if (aUsage == null) {
            return null;
        }

        return new UsageInfo( //
                aUsage.getPromptTokens() != 0 ? (int) aUsage.getPromptTokens() : null, //
                aUsage.getCompletionTokens() != 0 ? (int) aUsage.getCompletionTokens() : null, //
                aUsage.getTotalTokens() != 0 ? (int) aUsage.getTotalTokens() : null);
    }

    private static String apiKey(LlmEndpoint aEndpoint)
    {
        var auth = aEndpoint.auth();
        if (auth == null) {
            throw new IllegalArgumentException(
                    "Azure OpenAI requires an API key but none is configured");
        }
        if (auth instanceof ApiKeyAuthenticationTraits apiKeyAuth) {
            var key = apiKeyAuth.getApiKey();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "Azure OpenAI requires an API key but the configured key is blank");
            }
            return key;
        }
        throw new IllegalArgumentException(
                "Azure OpenAI client requires " + ApiKeyAuthenticationTraits.class.getSimpleName()
                        + " but got [" + auth.getClass().getName() + "]");
    }

    private static AzureAiGenerateResponseFormat toResponseFormat(ChatOptions aOptions)
    {
        if (aOptions.jsonSchema() != null) {
            return AzureAiGenerateResponseFormat.builder() //
                    .withType(JSON_SCHEMA) //
                    .withSchema("response", aOptions.jsonSchema()) //
                    .build();
        }

        if (aOptions.responseFormat() == ResponseFormat.JSON) {
            return AzureAiGenerateResponseFormat.builder() //
                    .withType(JSON_OBJECT) //
                    .build();
        }

        return null;
    }
}
