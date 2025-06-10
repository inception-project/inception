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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai;

import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client.AzureAiChatCompletionRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.preset.Preset;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderTraitsEditor_ImplBase;
import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraitsEditor;

public class AzureAiOpenAiRecommenderTraitsEditor
    extends LlmRecommenderTraitsEditor_ImplBase
{
    private static final long serialVersionUID = 1677442652521110324L;

    private @SpringBean RecommendationEngineFactory<AzureAiOpenAiRecommenderTraits> toolFactory;

    public AzureAiOpenAiRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender,
            IModel<List<Preset>> aPresets)
    {
        super(aId, aRecommender, aPresets,
                new ListModel<>(AzureAiChatCompletionRequest.getAllOptions()));
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
}
