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

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatChunk;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatOptions;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.FinishReason;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmChatClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmEndpoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ModelCapability;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ModelInfo;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ToolCall;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ToolDescriptor;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.UsageInfo;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * {@link LlmChatClient} adapter for Ollama, exposing chat, streaming, embeddings, and model
 * discovery through the provider-neutral abstraction. Delegates to the existing
 * {@link OllamaClient}; pure pass-through with no recommender-specific defaults applied here.
 * <p>
 * Tool calling is bridged here: {@link ChatOptions#tools()} are translated from
 * {@code ToolDescriptor} into {@link OllamaTool} when building the chat request.
 */
public class OllamaLlmChatClient
    implements LlmChatClient
{
    public static final String ID = "ollama";

    private final OllamaClient client;

    public OllamaLlmChatClient(OllamaClient aClient)
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
                ModelCapability.EMBEDDINGS, //
                ModelCapability.TOOLS);
    }

    @Override
    public ChatResult chat(LlmEndpoint aEndpoint, List<ChatMessage> aMessages, ChatOptions aOptions)
        throws IOException
    {
        var request = buildChatRequest(aEndpoint, aMessages, aOptions, false);
        var response = client.chat(aEndpoint.url(), request);
        return toChatResult(response);
    }

    @Override
    public ChatResult chatStream(LlmEndpoint aEndpoint, List<ChatMessage> aMessages,
            ChatOptions aOptions, Consumer<ChatChunk> aOnChunk)
        throws IOException
    {
        var request = buildChatRequest(aEndpoint, aMessages, aOptions, true);
        Consumer<OllamaChatResponse> callback = chunk -> {
            var msg = chunk.getMessage();
            if (msg == null) {
                return;
            }
            if (msg.content() == null && msg.thinking() == null) {
                return;
            }
            aOnChunk.accept(new ChatChunk(msg.content(), msg.thinking()));
        };
        var response = client.chat(aEndpoint.url(), request, callback);
        return toChatResult(response);
    }

    @Override
    public List<float[]> embed(LlmEndpoint aEndpoint, List<String> aInputs,
            Map<String, Object> aOptions)
        throws IOException
    {
        var builder = OllamaEmbedRequest.builder() //
                .withApiKey(apiKey(aEndpoint)) //
                .withModel(aEndpoint.model()) //
                .withInput(aInputs);

        if (aOptions != null && !aOptions.isEmpty()) {
            builder.withOptions(aOptions);
        }

        return client.embed(aEndpoint.url(), builder.build()).stream() //
                .map(p -> p.getRight()) //
                .toList();
    }

    @Override
    public List<ModelInfo> listModels(LlmEndpoint aEndpoint) throws IOException
    {
        return client.listModels(aEndpoint.url(), apiKey(aEndpoint)).stream() //
                .map(t -> new ModelInfo(t.name())) //
                .toList();
    }

    private OllamaChatRequest buildChatRequest(LlmEndpoint aEndpoint, List<ChatMessage> aMessages,
            ChatOptions aOptions, boolean aStream)
    {
        var messages = aMessages.stream() //
                .map(OllamaLlmChatClient::toOllamaMessage) //
                .toList();

        var builder = OllamaChatRequest.builder() //
                .withApiKey(apiKey(aEndpoint)) //
                .withModel(aEndpoint.model()) //
                .withMessages(messages) //
                .withFormat(toFormat(aOptions)) //
                .withThink(false) //
                .withStream(aStream);

        if (aOptions.options() != null) {
            builder.withExtraOptions(aOptions.options());
        }

        if (aOptions.tools() != null && !aOptions.tools().isEmpty()) {
            builder.withTools(aOptions.tools().stream() //
                    .map(OllamaLlmChatClient::toOllamaTool) //
                    .toList());
        }

        return builder.build();
    }

    private static OllamaChatMessage toOllamaMessage(ChatMessage aMessage)
    {
        // tool_call_id is not currently part of the OllamaChatMessage DTO; Ollama matches tool
        // results to calls positionally. Drop the id on the way out.
        var toolCalls = aMessage.toolCalls().stream() //
                .map(OllamaLlmChatClient::toOllamaToolCall) //
                .toList();
        return new OllamaChatMessage(aMessage.role().getName(), aMessage.content(),
                aMessage.thinking(), toolCalls);
    }

    private static OllamaToolCall toOllamaToolCall(ToolCall aCall)
    {
        var functionCall = new OllamaFunctionCall();
        functionCall.setName(aCall.name());
        if (aCall.arguments() != null && !aCall.arguments().isNull()) {
            @SuppressWarnings("unchecked")
            var arguments = JSONUtil.getObjectMapper().convertValue(aCall.arguments(), Map.class);
            functionCall.setArguments(arguments);
        }
        var toolCall = new OllamaToolCall();
        toolCall.setFunction(functionCall);
        return toolCall;
    }

    private static OllamaTool toOllamaTool(ToolDescriptor aDescriptor)
    {
        var function = OllamaFunction.builder() //
                .withName(aDescriptor.name()) //
                .withDescription(aDescriptor.description()) //
                .withParameters(toOllamaParameters(aDescriptor.parametersSchema())) //
                .build();
        return OllamaTool.builder() //
                .withType("function") //
                .withFunction(function) //
                .build();
    }

    private static OllamaFunctionParameters toOllamaParameters(JsonNode aSchema)
    {
        var builder = OllamaFunctionParameters.builder();
        if (aSchema == null || !aSchema.isObject()) {
            return builder.build();
        }

        var typeNode = aSchema.get("type");
        if (typeNode != null && typeNode.isTextual()) {
            builder.withType(typeNode.asText());
        }

        var requiredNode = aSchema.get("required");
        if (requiredNode != null && requiredNode.isArray()) {
            for (var item : requiredNode) {
                if (item.isTextual()) {
                    builder.addRequired(item.asText());
                }
            }
        }

        var propsNode = aSchema.get("properties");
        if (propsNode != null && propsNode.isObject()) {
            for (var entry : propsNode.properties()) {
                if (entry.getValue() instanceof ObjectNode propDef) {
                    builder.addProperty(entry.getKey(), propDef);
                }
            }
        }

        return builder.build();
    }

    private static ChatResult toChatResult(OllamaChatResponse aResponse)
    {
        var ollamaMessage = aResponse.getMessage();
        var content = ollamaMessage != null && ollamaMessage.content() != null //
                ? ollamaMessage.content() //
                : "";
        var role = ollamaMessage != null && ollamaMessage.role() != null //
                ? roleFromOllama(ollamaMessage.role()) //
                : ChatMessage.Role.ASSISTANT;

        var toolCalls = ollamaMessage != null && ollamaMessage.toolCalls() != null //
                ? ollamaMessage.toolCalls().stream() //
                        .map(OllamaLlmChatClient::toToolCall) //
                        .toList() //
                : Collections.<ToolCall> emptyList();

        var finishReason = aResponse.isDone() //
                ? (toolCalls.isEmpty() ? FinishReason.STOP : FinishReason.TOOL_CALLS) //
                : null;

        var usage = new UsageInfo( //
                aResponse.getPromptEvalCount() != 0 ? aResponse.getPromptEvalCount() : null, //
                aResponse.getEvalCount() != 0 ? aResponse.getEvalCount() : null, //
                aResponse.getPromptEvalCount() + aResponse.getEvalCount() != 0 //
                        ? aResponse.getPromptEvalCount() + aResponse.getEvalCount() //
                        : null);

        var thinking = ollamaMessage != null ? ollamaMessage.thinking() : null;
        return new ChatResult(new ChatMessage(role, content, thinking, null), toolCalls,
                finishReason, usage);
    }

    private static ToolCall toToolCall(OllamaToolCall aCall)
    {
        var fn = aCall.getFunction();
        if (fn == null) {
            return new ToolCall(null, null, null);
        }
        // Round-trip through the object mapper so values become proper TextNode / IntNode / ...
        // — putPOJO would wrap them in POJONode, which makes JsonNode#isTextual / isNumber lie.
        var args = (ObjectNode) JSONUtil.getObjectMapper().valueToTree(fn.getArguments());
        return new ToolCall(null, fn.getName(), args);
    }

    private static ChatMessage.Role roleFromOllama(String aRole)
    {
        return switch (aRole) {
        case "system" -> ChatMessage.Role.SYSTEM;
        case "user" -> ChatMessage.Role.USER;
        case "tool" -> ChatMessage.Role.TOOL;
        default -> ChatMessage.Role.ASSISTANT;
        };
    }

    private static JsonNode toFormat(ChatOptions aOptions)
    {
        if (aOptions.jsonSchema() != null) {
            return aOptions.jsonSchema();
        }
        if (aOptions.responseFormat() == ResponseFormat.JSON) {
            return JsonNodeFactory.instance.textNode("json");
        }
        return null;
    }

    private static String apiKey(LlmEndpoint aEndpoint)
    {
        var auth = aEndpoint.auth();
        if (auth instanceof ApiKeyAuthenticationTraits apiKeyAuth) {
            return apiKeyAuth.getApiKey();
        }
        if (auth == null) {
            return null;
        }
        throw new IllegalArgumentException(
                "Ollama client only supports " + ApiKeyAuthenticationTraits.class.getSimpleName()
                        + " or no auth; got [" + auth.getClass().getName() + "]");
    }
}
