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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderTypeSystemUtils;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaGenerateResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.prompt.PromptingMode;

@Disabled("Requires locally running ollama")
class OllamaRecommenderTest
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private AnnotationLayer layer;
    private AnnotationFeature feature;
    private Recommender recommender;
    private CAS cas;

    @BeforeEach
    void setup() throws Exception
    {
        layer = AnnotationLayer.builder().forJCasClass(NamedEntity.class).build();
        feature = AnnotationFeature.builder().withLayer(layer).withName(NamedEntity._FeatName_value)
                .build();

        recommender = new Recommender();
        recommender.setLayer(layer);
        recommender.setFeature(feature);

        var tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
        RecommenderTypeSystemUtils.addPredictionFeaturesToTypeSystem(tsd, asList(feature));
        cas = CasFactory.createCas(tsd);
    }

    @Test
    void testPerDocumentUsingReponseAsLabel() throws Exception
    {
        cas.setDocumentText("1 2 3 4 5 6 7 8 9 10");

        var traits = new OllamaRecommenderTraits();
        traits.setModel("mistral");
        traits.setPrompt("What do you see in the following text?\n\n{{ text }}");
        traits.setPromptingMode(PromptingMode.PER_DOCUMENT);
        traits.setExtractionMode(ExtractionMode.RESPONSE_AS_LABEL);

        var sut = new OllamaRecommender(recommender, traits, new OllamaClientImpl());
        sut.predict(new RecommenderContext(), cas);

        var predictions = cas.select(NamedEntity.class)
                .filter(ne -> getFeature(ne, FEATURE_NAME_IS_PREDICTION, Boolean.class)).toList();

        predictions.forEach(prediction -> LOG.info("Prediction: {} {}", prediction.getCoveredText(),
                prediction.getValue()));
        assertThat(predictions).as("predictions").isNotEmpty();
    }

    @Test
    void testPerDocumentUsingMentionsFromJsonList_Numbers() throws Exception
    {
        cas.setDocumentText("1 2 3 4 5 6 7 8 9 10");

        var traits = new OllamaRecommenderTraits();
        traits.setModel("mistral");
        traits.setPrompt(
                "Identify all even numbers in the following list and return them as JSON.\n\n{{ text }}");
        traits.setFormat(OllamaGenerateResponseFormat.JSON);
        traits.setPromptingMode(PromptingMode.PER_DOCUMENT);
        traits.setExtractionMode(ExtractionMode.MENTIONS_FROM_JSON);

        var sut = new OllamaRecommender(recommender, traits, new OllamaClientImpl());
        sut.predict(new RecommenderContext(), cas);

        var predictions = cas.select(NamedEntity.class)
                .filter(ne -> getFeature(ne, FEATURE_NAME_IS_PREDICTION, Boolean.class)).toList();

        predictions.forEach(prediction -> LOG.info("Prediction: {} {}", prediction.getCoveredText(),
                prediction.getValue()));
        assertThat(predictions).as("predictions").isNotEmpty();
    }

    @Test
    void testPerDocumentUsingMentionsFromJsonList_Entities() throws Exception
    {
        cas.setDocumentText(
                "John is going to work at the diner tomorrow. There, he meets a guy working at Starbucks.");

        var traits = new OllamaRecommenderTraits();
        traits.setModel("mistral");
        traits.setPrompt("Identify all named entities in the following text.\n\n{{ text }}");
        traits.setFormat(OllamaGenerateResponseFormat.JSON);
        traits.setPromptingMode(PromptingMode.PER_DOCUMENT);
        traits.setExtractionMode(ExtractionMode.MENTIONS_FROM_JSON);

        var sut = new OllamaRecommender(recommender, traits, new OllamaClientImpl());
        sut.predict(new RecommenderContext(), cas);

        var predictions = cas.select(NamedEntity.class)
                .filter(ne -> getFeature(ne, FEATURE_NAME_IS_PREDICTION, Boolean.class)).toList();

        predictions.forEach(prediction -> LOG.info("Prediction: {} {}", prediction.getCoveredText(),
                prediction.getValue()));
        assertThat(predictions).as("predictions").isNotEmpty();
    }

    @Test
    void testPerDocumentUsingMentionsFromJsonList_Politicians() throws Exception
    {
        cas.setDocumentText(
                "John is will meet President Livingston tomorrow. They will lunch together with the minister of foreign affairs. Later they meet the the Lord of Darkness, Don Horny.");

        var traits = new OllamaRecommenderTraits();
        traits.setModel("mistral");
        traits.setPrompt(
                "Identify all politicians in the following text and return them as JSON.\n\n{{ text }}");
        traits.setFormat(OllamaGenerateResponseFormat.JSON);
        traits.setPromptingMode(PromptingMode.PER_DOCUMENT);
        traits.setExtractionMode(ExtractionMode.MENTIONS_FROM_JSON);

        var sut = new OllamaRecommender(recommender, traits, new OllamaClientImpl());
        sut.predict(new RecommenderContext(), cas);

        var predictions = cas.select(NamedEntity.class)
                .filter(ne -> getFeature(ne, FEATURE_NAME_IS_PREDICTION, Boolean.class)).toList();

        predictions.forEach(prediction -> LOG.info("Prediction: {} {}", prediction.getCoveredText(),
                prediction.getValue()));
        assertThat(predictions).as("predictions").isNotEmpty();
    }
}
