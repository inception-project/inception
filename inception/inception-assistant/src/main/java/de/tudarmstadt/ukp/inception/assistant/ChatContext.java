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

import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.ASSISTANT;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.model.MPerformanceMetrics;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaOptions;

public class ChatContext
{
    private final AssistantProperties properties;
    private final OllamaClient ollamaClient;
    private final String sessionOwner;
    private final Project project;

    public ChatContext(AssistantProperties aProperties, OllamaClient aOllamaClient,
            String aSessionOwner, Project aProject)
    {
        properties = aProperties;
        ollamaClient = aOllamaClient;
        sessionOwner = aSessionOwner;
        project = aProject;
    }
    
    public Project getProject()
    {
        return project;
    }
    
    public String getSessionOwner()
    {
        return sessionOwner;
    }

    public MTextMessage generate(List<MTextMessage> aMessasges,
            BiConsumer<UUID, OllamaChatResponse> aCallback)
        throws IOException
    {
        var responseId = UUID.randomUUID();
        var request = OllamaChatRequest.builder() //
                .withModel(properties.getChat().getModel()) //
                .withStream(true) //
                .withMessages(aMessasges.stream() //
                        .map(msg -> new OllamaChatMessage(msg.role(), msg.message())) //
                        .toList()) //
                .withOption(OllamaOptions.NUM_CTX, properties.getChat().getContextLength()) //
                .withOption(OllamaOptions.TOP_P, properties.getChat().getTopP()) //
                .withOption(OllamaOptions.TOP_K, properties.getChat().getTopK()) //
                .withOption(OllamaOptions.REPEAT_PENALTY, properties.getChat().getRepeatPenalty()) //
                .withOption(OllamaOptions.TEMPERATURE, properties.getChat().getTemperature()) //
                .build();

        var startTime = System.currentTimeMillis();
        var response = ollamaClient.generate(properties.getUrl(), request, msg -> aCallback.accept(responseId, msg));
        var endTime = System.currentTimeMillis();

        return MTextMessage.builder() //
                .withId(responseId) //
                .withActor(properties.getNickname()) //
                .withRole(ASSISTANT) //
                .withMessage(response.getMessage().content()) //
                .withPerformance(new MPerformanceMetrics(endTime - startTime))
                .build();
    }
}
