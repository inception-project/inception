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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama;

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaOptions.SEED;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaOptions.TEMPERATURE;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaOptions.TOP_P;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationTaskCodecExtensionPoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatBasedLlmRecommenderImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class OllamaRecommender
    extends ChatBasedLlmRecommenderImplBase<OllamaRecommenderTraits>
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final OllamaClient client;

    public OllamaRecommender(Recommender aRecommender, OllamaRecommenderTraits aTraits,
            OllamaClient aClient, AnnotationSchemaService aSchemaService,
            AnnotationTaskCodecExtensionPoint aResponseExtractorExtensionPoint)
    {
        super(aRecommender, aTraits, aSchemaService, aResponseExtractorExtensionPoint);

        client = aClient;
    }

    @Override
    protected String exchange(List<ChatMessage> aMessages, ResponseFormat aFormat, JsonNode aSchema)
        throws IOException
    {
        var format = getResponseFormat(aFormat, aSchema);
        var messages = aMessages.stream() //
                .map(m -> new OllamaChatMessage(m.role().getName(), m.content())) //
                .toList();

        var request = OllamaChatRequest.builder() //
                .withModel(traits.getModel()) //
                .withMessages(messages) //
                .withFormat(format) //
                .withThink(false) //
                .withStream(false);

        var options = traits.getOptions();
        // https://platform.openai.com/docs/api-reference/chat/create recommends to set temperature
        // or top_p but not both.
        if (!options.containsKey(TEMPERATURE.getName()) && !options.containsKey(TOP_P.getName())) {
            request.withOption(TEMPERATURE, 0.0d);
        }
        request.withOption(SEED, 0xdeadbeef);
        request.withExtraOptions(options);

        var response = client.chat(traits.getUrl(), request.build(), null).getMessage().content();

        return response;
    }

    private JsonNode getResponseFormat(ResponseFormat aFormat, JsonNode aSchema)
        throws JsonProcessingException
    {
        if (aSchema != null) {
            return aSchema;
        }

        if (aFormat == ResponseFormat.JSON) {
            return JsonNodeFactory.instance.textNode("json");
        }

        return null;
    }
}
