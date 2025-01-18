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

import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaGenerateRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaGenerateResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderImplBase;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class OllamaRecommender
    extends LlmRecommenderImplBase<OllamaRecommenderTraits>
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final OllamaClient client;

    public OllamaRecommender(Recommender aRecommender, OllamaRecommenderTraits aTraits,
            OllamaClient aClient, AnnotationSchemaService aSchemaService)
    {
        super(aRecommender, aTraits, aSchemaService);

        client = aClient;
    }

    @Override
    protected String exchange(String aPrompt) throws IOException
    {
        var format = getResponseFormat();

        LOG.trace("Querying ollama [{}]: [{}]", traits.getModel(), aPrompt);
        var request = OllamaGenerateRequest.builder() //
                .withModel(traits.getModel()) //
                .withPrompt(aPrompt) //
                .withFormat(format) //
                .withStream(false) //
                .build();
        var startTime = currentTimeMillis();
        var response = client.generate(traits.getUrl(), request).trim();
        LOG.trace("Ollama [{}] responds ({} ms): [{}]", traits.getModel(),
                currentTimeMillis() - startTime, response);
        return response;
    }

    private OllamaGenerateResponseFormat getResponseFormat()
    {
        OllamaGenerateResponseFormat format = null;
        if (traits.getFormat() != null) {
            format = switch (traits.getFormat()) {
            case JSON -> OllamaGenerateResponseFormat.JSON;
            default -> null;
            };
        }
        return format;
    }
}
