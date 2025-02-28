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
import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.ASSISTANT;
import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.model.MCallResponse;
import de.tudarmstadt.ukp.inception.assistant.model.MPerformanceMetrics;
import de.tudarmstadt.ukp.inception.assistant.model.MReference;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage.Builder;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaOptions;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class ChatContext
{
    private final AssistantProperties properties;
    private final OllamaClient ollamaClient;
    private final String sessionOwner;
    private final Project project;
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

    public Project getProject()
    {
        return project;
    }

    public String getSessionOwner()
    {
        return sessionOwner;
    }

    public MTextMessage chat(List<MTextMessage> aMessasges) throws IOException
    {
        return chat(aMessasges, null);
    }

    public MTextMessage chat(List<MTextMessage> aMessasges,
            BiConsumer<UUID, MTextMessage> aCallback)
        throws IOException
    {
        var responseId = UUID.randomUUID();
        var chatProperties = properties.getChat();
        var request = OllamaChatRequest.builder() //
                .withModel(chatProperties.getModel()) //
                .withStream(true) //
                .withMessages(aMessasges.stream() //
                        .map(msg -> new OllamaChatMessage(msg.role(), msg.message())) //
                        .toList()) //
                .withOption(OllamaOptions.NUM_CTX, chatProperties.getContextLength()) //
                .withOption(OllamaOptions.TOP_P, chatProperties.getTopP()) //
                .withOption(OllamaOptions.TOP_K, chatProperties.getTopK()) //
                .withOption(OllamaOptions.REPEAT_PENALTY, chatProperties.getRepeatPenalty()) //
                .withOption(OllamaOptions.TEMPERATURE, chatProperties.getTemperature()) //
                .build();

        var references = new LinkedHashMap<String, MReference>();
        aMessasges.stream() //
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
        var response = ollamaClient.chat(properties.getUrl(), request,
                aCallback != null ? msg -> streamMessage(aCallback, responseId, msg) : null);
        var tokens = response.getEvalCount();
        var endTime = currentTimeMillis();

        // Send a final and complete message also including final metrics
        return newMessage(responseId) //
                .withMessage(response.getMessage().content()) //
                .withPerformance(MPerformanceMetrics.builder() //
                        .withDuration(endTime - startTime) //
                        .withTokens(tokens) //
                        .build()) //
                // Include all refs in the final message again just to be sure
                .withReferences(references.values()) //
                .build();
    }

    public <T> MCallResponse<T> call(Class<T> aResult, List<MTextMessage> aMessasges)
        throws IOException
    {
        var schema = generator.generateSchema(aResult);

        var responseId = UUID.randomUUID();
        var chatProperties = properties.getChat();
        var request = OllamaChatRequest.builder() //
                .withModel(chatProperties.getModel()) //
                .withStream(true) //
                .withMessages(aMessasges.stream() //
                        .map(msg -> new OllamaChatMessage(msg.role(), msg.message())) //
                        .toList()) //
                .withFormat(schema) //
                .withOption(OllamaOptions.NUM_CTX, chatProperties.getContextLength()) //
                .withOption(OllamaOptions.TOP_P, chatProperties.getTopP()) //
                .withOption(OllamaOptions.TOP_K, chatProperties.getTopK()) //
                .withOption(OllamaOptions.REPEAT_PENALTY, chatProperties.getRepeatPenalty()) //
                .withOption(OllamaOptions.TEMPERATURE, chatProperties.getTemperature()) //
                .build();

        var references = new LinkedHashMap<String, MReference>();
        aMessasges.stream() //
                .flatMap(msg -> msg.references().stream()) //
                .forEach(r -> references.put(r.id(), r));

        // Generate the actual response
        var startTime = currentTimeMillis();
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
                // Include all refs in the final message again just to be sure
                .withReferences(references.values()) //
                .build();
    }

    private void streamMessage(BiConsumer<UUID, MTextMessage> aCallback, UUID responseId,
            OllamaChatResponse msg)
    {
        var responseMessage = newMessage(responseId) //
                .withMessage(msg.getMessage().content()) //
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
