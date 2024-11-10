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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt;

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.ChatGptRecommenderTraits.CEREBRAS_API_URL;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.ChatGptRecommenderTraits.GROQ_API_URL;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.ChatGptRecommenderTraits.LOCAL_OLLAMA_API_URL;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.ChatGptRecommenderTraits.OPENAI_API_URL;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatGptClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatGptModel;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ListModelsRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.preset.Preset;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderTraitsEditor_ImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.Option;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraitsEditor;

public class ChatGptRecommenderTraitsEditor
    extends LlmRecommenderTraitsEditor_ImplBase
{
    private static final long serialVersionUID = 1677442652521110324L;

    private @SpringBean RecommendationEngineFactory<ChatGptRecommenderTraits> toolFactory;

    public ChatGptRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender,
            IModel<List<Preset>> aPresets, IModel<List<Option<?>>> aOptions)
    {
        super(aId, aRecommender, aPresets, aOptions);
    }

    @Override
    protected AuthenticationTraitsEditor createAuthenticationTraitsEditor(String aId)
    {
        return new ApiKeyAuthenticationTraitsEditor("authentication",
                Model.of((ApiKeyAuthenticationTraits) getTraits().getObject().getAuthentication()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public RecommendationEngineFactory<LlmRecommenderTraits> getToolFactory()
    {
        return (RecommendationEngineFactory) toolFactory;
    }

    @Override
    protected List<String> listUrls()
    {
        return asList(//
                OPENAI_API_URL, //
                CEREBRAS_API_URL, //
                GROQ_API_URL, //
                LOCAL_OLLAMA_API_URL);
    }

    @Override
    protected List<String> listModels()
    {
        var url = getTraits().map(LlmRecommenderTraits::getUrl).orElse(null).getObject();
        var apiKey = ((ApiKeyAuthenticationTraits) getTraits()
                .map(LlmRecommenderTraits::getAuthentication).orElse(null).getObject()).getApiKey();

        if (!new UrlValidator(new String[] { "http", "https" }).isValid(url) || isBlank(apiKey)) {
            return emptyList();
        }

        var client = new ChatGptClientImpl();
        try {
            return client.listModels(url, ListModelsRequest.builder() //
                    .withApiKey(apiKey) //
                    .build()).stream() //
                    .map(ChatGptModel::getId) //
                    .toList();
        }
        catch (IOException e) {
            return emptyList();
        }
    }
}
