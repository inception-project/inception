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
package de.tudarmstadt.ukp.inception.assistant;

import static com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON;
import static com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12;
import static de.tudarmstadt.ukp.inception.assistant.config.AssistantChatProperties.CAP_TOOLS;
import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.ASSISTANT;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.model.MCallResponse;
import de.tudarmstadt.ukp.inception.assistant.model.MChatMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MChatResponse;
import de.tudarmstadt.ukp.inception.assistant.model.MPerformanceMetrics;
import de.tudarmstadt.ukp.inception.assistant.model.MReference;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage.Builder;
import de.tudarmstadt.ukp.inception.assistant.model.MToolCall;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolLibrary;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaOptions;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaTool;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class ChatContext
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AssistantProperties properties;
    private final OllamaClient ollamaClient;
    private final String sessionOwner;
    private final Project project;
    private final List<ToolLibrary> tools = new ArrayList<>();
    private SchemaGenerator generator;

    public ChatContext(AssistantProperties aProperties, OllamaClient aOllamaClient,
            String aSessionOwner, Project aProject)
    {
        properties = aProperties;
        ollamaClient = aOllamaClient;
        sessionOwner = aSessionOwner;
        project = aProject;
        generator = new SchemaGenerator(new SchemaGeneratorConfigBuilder(DRAFT_2020_12, PLAIN_JSON) //
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT) //
                .with(new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED)) //
                .build());
    }

    public void addToolLibrary(ToolLibrary aTool)
    {
        tools.add(aTool);
    }

    public List<ToolLibrary> getTools()
    {
        return tools;
    }

    public Project getProject()
    {
        return project;
    }

    public String getSessionOwner()
    {
        return sessionOwner;
    }

    public MChatResponse chat(List<MTextMessage> aMessages) throws IOException
    {
        return chat(aMessages, null);
    }

    public MChatResponse chat(List<? extends MChatMessage> aMessages,
            BiConsumer<UUID, MTextMessage> aCallback)
        throws IOException
    {
        return chat(aMessages, aCallback, null);
    }

    public MChatResponse chat(List<? extends MChatMessage> aMessages,
            BiConsumer<UUID, MTextMessage> aCallback, UUID aResponseId)
        throws IOException
    {
        var responseId = aResponseId != null ? aResponseId : UUID.randomUUID();
        var chatProperties = properties.getChat();
        var requestBuilder = OllamaChatRequest.builder() //
                .withModel(chatProperties.getModel()) //
                .withStream(true) //
                .withMessages(aMessages.stream() //
                        .map(msg -> new OllamaChatMessage(msg.role(), msg.textRepresentation(),
                                msg.thinking(), msg.toolName())) //
                        .toList()) //
                .withOption(OllamaOptions.NUM_CTX, chatProperties.getContextLength()) //
                .withOption(OllamaOptions.TOP_P, chatProperties.getTopP()) //
                .withOption(OllamaOptions.TOP_K, chatProperties.getTopK()) //
                .withOption(OllamaOptions.REPEAT_PENALTY, chatProperties.getRepeatPenalty()) //
                .withOption(OllamaOptions.TEMPERATURE, chatProperties.getTemperature());

        if (chatProperties.getCapabilities().contains(CAP_TOOLS) && !tools.isEmpty()) {
            try {
                var ollamaTools = tools.stream() //
                        .flatMap(o -> OllamaTool.forService(o).stream()) //
                        .toList();
                requestBuilder.withTools(ollamaTools);

            }
            catch (Exception e) {
                LOG.error("Unable to map tools", e);
            }
        }

        var references = new LinkedHashMap<String, MReference>();
        aMessages.stream() //
                .flatMap(msg -> msg.references().stream()) //
                .forEach(r -> references.put(r.id(), r));

        // Send initial message with the assistant nickname and the references
        if (aCallback != null) {
            var firstMessage = newMessage(responseId) //
                    .withReferences(references.values()) //
                    .notDone() //
                    .build();
            aCallback.accept(responseId, firstMessage);
        }

        // Generate the actual response
        var startTime = currentTimeMillis();
        var firstTokenTime = new AtomicLong(0l);
        var request = requestBuilder.build();
        var response = ollamaClient.chat(properties.getUrl(), request, msg -> {
            firstTokenTime.compareAndSet(0l, currentTimeMillis());
            if (aCallback != null) {
                streamMessage(aCallback, responseId, msg);
            }
        });
        var tokens = response.getEvalCount();
        var endTime = currentTimeMillis();

        var toolCalls = new ArrayList<MToolCall>();
        if (isNotEmpty(response.getMessage().toolCalls()) && isNotEmpty(request.getTools())) {
            // var mapper = new ObjectMapper();

            for (var call : response.getMessage().toolCalls()) {
                var maybeTool = request.getTool(call);
                if (maybeTool.isEmpty()) {
                    continue;
                }

                // var args = new LinkedHashMap<String, Object>();
                // for (var param : tool.get().getFunction().getImplementation().getParameters()) {
                // var paramName = ToolUtils.getParameterName(param);
                // var paramValue = call.getFunction().getArguments().get(paramName);
                // if (paramValue instanceof JsonNode jsonValue) {
                // paramValue = mapper.treeToValue(jsonValue, param.getType());
                // }
                //
                // args.put(paramName, paramValue);
                // }

                var tool = maybeTool.get();
                toolCalls.add(MToolCall.builder() //
                        .withActor(tool.getFunction().getActor()) //
                        .withInstance(tool.getFunction().getService()) //
                        .withMethod(tool.getFunction().getImplementation()) //
                        .withArguments(call.getFunction().getArguments()) //
                        .withStop(tool.isStop()) //
                        .build());
            }
        }

        // Send a final and complete message also including final metrics
        return MChatResponse.builder() //
                .withMessage(newMessage(responseId) //
                        .withMessage(response.getMessage().content()) //
                        .withThinking(response.getMessage().thinking()) //
                        .withPerformance(MPerformanceMetrics.builder() //
                                .withDelay(firstTokenTime.get() - startTime) //
                                .withDuration(endTime - firstTokenTime.get()) //
                                .withTokens(tokens) //
                                .build()) //
                        // Include all references in the final message again just to be sure
                        .withReferences(references.values()) //
                        .build()) //
                .withToolCalls(toolCalls) //
                .build();
    }

    public <T> MCallResponse<T> call(Class<T> aResult, List<MTextMessage> aMessages)
        throws IOException
    {
        var schema = generator.generateSchema(aResult);

        var responseId = UUID.randomUUID();
        var chatProperties = properties.getChat();
        var requestBuilder = OllamaChatRequest.builder() //
                .withModel(chatProperties.getModel()) //
                .withStream(false) //
                .withMessages(aMessages.stream() //
                        .map(msg -> new OllamaChatMessage(msg.role(), msg.message(), msg.thinking(),
                                msg.toolName())) //
                        .toList()) //
                .withFormat(schema) //
                .withOption(OllamaOptions.NUM_CTX, chatProperties.getContextLength()) //
                .withOption(OllamaOptions.TOP_P, chatProperties.getTopP()) //
                .withOption(OllamaOptions.TOP_K, chatProperties.getTopK()) //
                .withOption(OllamaOptions.REPEAT_PENALTY, chatProperties.getRepeatPenalty()) //
                .withOption(OllamaOptions.TEMPERATURE, chatProperties.getTemperature());

        var references = new LinkedHashMap<String, MReference>();
        aMessages.stream() //
                .flatMap(msg -> msg.references().stream()) //
                .forEach(r -> references.put(r.id(), r));

        // Generate the actual response
        var startTime = currentTimeMillis();
        var request = requestBuilder.build();
        var response = ollamaClient.chat(properties.getUrl(), request, null);
        var tokens = response.getEvalCount();
        var endTime = currentTimeMillis();

        var payload = JSONUtil.fromJsonString(aResult, response.getMessage().content());

        // Send a final and complete message also including final metrics
        return MCallResponse.builder(aResult) //
                .withId(responseId) //
                .withActor(properties.getNickname()) //
                .withRole(ASSISTANT) //
                .withPayload(payload) //
                .withPerformance(MPerformanceMetrics.builder() //
                        .withDuration(endTime - startTime) //
                        .withTokens(tokens) //
                        .build()) //
                // Include all references in the final message again just to be sure
                .withReferences(references.values()) //
                .build();
    }

    private void streamMessage(BiConsumer<UUID, MTextMessage> aCallback, UUID responseId,
            OllamaChatResponse msg)
    {
        if (isEmpty(msg.getMessage().content()) && isEmpty(msg.getMessage().thinking())) {
            return;
        }

        var responseMessage = newMessage(responseId) //
                .withMessage(msg.getMessage().content()) //
                .withThinking(msg.getMessage().thinking()) //
                .notDone();

        aCallback.accept(responseId, responseMessage.build());
    }

    private Builder newMessage(UUID responseId)
    {
        return MTextMessage.builder() //
                .withId(responseId) //
                .withActor(properties.getNickname()) //
                .withRole(ASSISTANT);
    }
}
