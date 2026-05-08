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

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.OllamaRecommenderTraits.DEFAULT_OLLAMA_URL;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaOptions;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaTag;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.preset.Preset;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderTraitsEditor_ImplBase;

public class OllamaRecommenderTraitsEditor
    extends LlmRecommenderTraitsEditor_ImplBase
{
    private static final long serialVersionUID = 1677442652521110324L;

    private @SpringBean RecommendationEngineFactory<OllamaRecommenderTraits> toolFactory;
    private @SpringBean OllamaClient ollamaClient;

    public OllamaRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender,
            IModel<List<Preset>> aPresets)
    {
        super(aId, aRecommender, aPresets, new ListModel<>(OllamaOptions.getAllOptions()));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public RecommendationEngineFactory<LlmRecommenderTraits> getToolFactory()
    {
        return (RecommendationEngineFactory) toolFactory;
    }

    @Override
    protected List<String> listModels()
    {
        var url = getTraits().map(LlmRecommenderTraits::getUrl).orElse(null).getObject();
        if (!new UrlValidator(new String[] { "http", "https" }).isValid(url)) {
            return emptyList();
        }

        try {
            return ollamaClient.listModels(url).stream().map(OllamaTag::name).toList();
        }
        catch (IOException e) {
            return emptyList();
        }
    }

    @Override
    protected List<String> listUrls()
    {
        return asList(DEFAULT_OLLAMA_URL);
    }
}
