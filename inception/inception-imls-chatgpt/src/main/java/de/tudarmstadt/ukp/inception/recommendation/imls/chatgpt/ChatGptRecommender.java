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
package de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt;

import static de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.prompt.PromptContextGenerator.VAR_EXAMPLES;
import static de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.prompt.PromptContextGenerator.getPromptContextGenerator;
import static de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.response.ResponseExtractor.getResponseExtractor;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.NonTrainableRecommenderEngineImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.client.ChatCompletionRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.client.ChatGptClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.client.ResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.prompt.JinjaPromptRenderer;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class ChatGptRecommender
    extends NonTrainableRecommenderEngineImplBase
{
    private static final int MAX_FEW_SHOT_EXAMPLES = 10;

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ChatGptRecommenderTraits traits;

    private final ChatGptClient client;
    private final JinjaPromptRenderer promptRenderer;

    public ChatGptRecommender(Recommender aRecommender, ChatGptRecommenderTraits aTraits,
            ChatGptClient aClient)
    {
        super(aRecommender);

        traits = aTraits;
        client = aClient;
        promptRenderer = new JinjaPromptRenderer();
    }

    @Override
    public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        var responseExtractor = getResponseExtractor(traits.getExtractionMode());
        var examples = responseExtractor.generate(this, aCas, MAX_FEW_SHOT_EXAMPLES);
        var globalBindings = Map.of(VAR_EXAMPLES, examples);

        getPromptContextGenerator(traits.getPromptingMode())
                .generate(this, aCas, aBegin, aEnd, globalBindings).forEach(promptContext -> {
                    try {
                        var prompt = promptRenderer.render(traits.getPrompt(), promptContext);
                        var response = query(prompt);
                        responseExtractor.extract(this, aCas, promptContext, response);
                    }
                    catch (IOException e) {
                        aContext.log(LogMessage.warn(getRecommender().getName(),
                                "Remote failed to respond: %s", getRootCauseMessage(e)));
                        LOG.error("Remote failed to respond: {}", getRootCauseMessage(e));
                    }
                });

        return new Range(aBegin, aEnd);
    }

    private String query(String aPrompt) throws IOException
    {
        LOG.trace("Query: [{}]", aPrompt);
        var request = ChatCompletionRequest.builder() //
                .withApiKey(((ApiKeyAuthenticationTraits) traits.getAuthentication()).getApiKey()) //
                .withPrompt(aPrompt) //
                .withModel(traits.getModel());

        if (traits.getFormat() != null) {
            request.withResponseFormat(
                    ResponseFormat.builder().withType(traits.getFormat()).build());
        }

        var response = client.generate(traits.getUrl(), request.build()).trim();
        LOG.trace("Response: [{}]", response);
        return response;
    }
}
