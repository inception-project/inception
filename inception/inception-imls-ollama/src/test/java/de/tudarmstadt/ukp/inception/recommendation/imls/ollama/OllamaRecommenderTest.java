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
import static de.tudarmstadt.ukp.inception.recommendation.imls.ollama.OllamaRecommenderTraits.DEFAULT_OLLAMA_URL;
import static de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaGenerateResponseFormat.JSON;
import static de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.prompt.PromptingMode.PER_DOCUMENT;
import static de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.prompt.PromptingMode.PER_SENTENCE;
import static de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.response.ExtractionMode.MENTIONS_FROM_JSON;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.lang.invoke.MethodHandles;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderTypeSystemUtils;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.support.llm.response.ExtractionMode;
import de.tudarmstadt.ukp.inception.support.test.http.HttpTestUtils;

class OllamaRecommenderTest
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private AnnotationLayer layer;
    private AnnotationFeature feature;
    private Recommender recommender;
    private CAS cas;

    @BeforeAll
    static void checkIfOllamaIsRunning()
    {
        assumeThat(HttpTestUtils.checkURL(DEFAULT_OLLAMA_URL)).isTrue();
    }

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
        traits.setPromptingMode(PER_DOCUMENT);
        traits.setExtractionMode(ExtractionMode.RESPONSE_AS_LABEL);

        var sut = new OllamaRecommender(recommender, traits, new OllamaClientImpl());
        sut.predict(new PredictionContext(new RecommenderContext()), cas);

        var predictions = cas.select(NamedEntity.class)
                .filter(ne -> getFeature(ne, FEATURE_NAME_IS_PREDICTION, Boolean.class)).toList();

        predictions.forEach($ -> LOG.info("Prediction: {} {}", $.getCoveredText(), $.getValue()));
        assertThat(predictions).as("predictions").isNotEmpty();
    }

    @Test
    void testPerDocumentUsingMentionsFromJsonList_Numbers() throws Exception
    {
        cas.setDocumentText("1 2 3 4 5 6 7 8 9 10");

        var traits = new OllamaRecommenderTraits();
        traits.setModel("mistral");
        traits.setPrompt("""
                Identify all even numbers in the following list and return them as JSON.

                {{ text }}""");
        traits.setFormat(JSON);
        traits.setPromptingMode(PER_DOCUMENT);
        traits.setExtractionMode(MENTIONS_FROM_JSON);

        var sut = new OllamaRecommender(recommender, traits, new OllamaClientImpl());
        sut.predict(new PredictionContext(new RecommenderContext()), cas);

        var predictions = cas.select(NamedEntity.class)
                .filter(ne -> getFeature(ne, FEATURE_NAME_IS_PREDICTION, Boolean.class)).toList();

        predictions.forEach($ -> LOG.info("Prediction: {} {}", $.getCoveredText(), $.getValue()));
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
        traits.setFormat(JSON);
        traits.setPromptingMode(PER_DOCUMENT);
        traits.setExtractionMode(MENTIONS_FROM_JSON);

        var sut = new OllamaRecommender(recommender, traits, new OllamaClientImpl());
        sut.predict(new PredictionContext(new RecommenderContext()), cas);

        var predictions = cas.select(NamedEntity.class)
                .filter(ne -> getFeature(ne, FEATURE_NAME_IS_PREDICTION, Boolean.class)).toList();

        predictions.forEach($ -> LOG.info("Prediction: {} {}", $.getCoveredText(), $.getValue()));
        assertThat(predictions).as("predictions").isNotEmpty();
    }

    @Test
    void testPerDocumentUsingMentionsFromJsonList_Politicians() throws Exception
    {
        cas.setDocumentText("""
                John is will meet President Livingston tomorrow.
                They will lunch together with the minister of foreign affairs.
                Later they meet the the Lord of Darkness, Don Horny.""");

        var traits = new OllamaRecommenderTraits();
        traits.setModel("mistral");
        traits.setPrompt("""
                Identify all politicians in the following text and return them as JSON.

                {{ text }}""");
        traits.setFormat(JSON);
        traits.setPromptingMode(PER_DOCUMENT);
        traits.setExtractionMode(MENTIONS_FROM_JSON);

        var sut = new OllamaRecommender(recommender, traits, new OllamaClientImpl());
        sut.predict(new PredictionContext(new RecommenderContext()), cas);

        var predictions = cas.select(NamedEntity.class)
                .filter(ne -> getFeature(ne, FEATURE_NAME_IS_PREDICTION, Boolean.class)).toList();

        predictions.forEach($ -> LOG.info("Prediction: {} {}", $.getCoveredText(), $.getValue()));
        assertThat(predictions).as("predictions").isNotEmpty();
    }

    @Test
    void testPerSentenceUsingMentionsFromJsonList_Politicians_fewShjot() throws Exception
    {
        TokenBuilder.create(Token.class, Sentence.class).buildTokens(cas.getJCas(), """
                John is will meet President Livingston tomorrow .
                They will lunch together with the minister of foreign affairs .
                Later they meet the the Lord of Darkness, Don Horny .""");
        buildAnnotation(cas, NamedEntity.class).on("John") //
                .withFeature(NamedEntity._FeatName_value, "PER") //
                .buildAndAddToIndexes();
        buildAnnotation(cas, NamedEntity.class).on("President Livingston") //
                .withFeature(NamedEntity._FeatName_value, "PER") //
                .buildAndAddToIndexes();

        var traits = new OllamaRecommenderTraits();
        traits.setModel("mistral");
        traits.setPrompt("""
                Identify all politicians in the following text and return them as JSON.

                {% for example in examples %}
                Text:
                '''
                {{ example.getText() }}
                '''

                Response:
                {{ example.getLabelledMentions() | tojson }}
                {% endfor %}

                Text:
                '''
                {{ text }}
                '''

                Response:""");
        traits.setFormat(JSON);
        traits.setPromptingMode(PER_SENTENCE);
        traits.setExtractionMode(MENTIONS_FROM_JSON);

        var sut = new OllamaRecommender(recommender, traits, new OllamaClientImpl());
        sut.predict(new PredictionContext(new RecommenderContext()), cas);

        var predictions = cas.select(NamedEntity.class)
                .filter(ne -> getFeature(ne, FEATURE_NAME_IS_PREDICTION, Boolean.class)).toList();

        predictions.forEach($ -> LOG.info("Prediction: {} {}", $.getCoveredText(), $.getValue()));
        assertThat(predictions).as("predictions").isNotEmpty();
    }
}
