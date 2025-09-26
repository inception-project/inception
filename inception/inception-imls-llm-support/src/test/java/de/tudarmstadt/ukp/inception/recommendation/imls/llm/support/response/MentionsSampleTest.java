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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response;

import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderTypeSystemUtils;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.NonTrainableRecommenderEngineImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.rendering.model.Range;

class MentionsSampleTest
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private AnnotationLayer layer;
    private AnnotationFeature feature;
    private Recommender recommender;
    private RecommendationEngine engine;
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

        engine = new NonTrainableRecommenderEngineImplBase(recommender)
        {
            @Override
            public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
                throws RecommendationException
            {
                return null;
            }
        };
    }

    @Test
    void testGenerateExamples()
    {
        String text = "John likes Mary.";
        cas.setDocumentText(text);
        buildAnnotation(cas, Sentence.class).onMatch(text) //
                .buildAndAddToIndexes();
        buildAnnotation(cas, NamedEntity.class).on("John") //
                .withFeature(NamedEntity._FeatName_value, "PER") //
                .buildAndAddToIndexes();
        buildAnnotation(cas, NamedEntity.class).on("Mary") //
                .withFeature(NamedEntity._FeatName_value, "PER") //
                .buildAndAddToIndexes();

        var sut = new SpanJsonAnnotationTaskCodec();
        var examples = sut.generateExamples(engine, cas, 10);

        assertThat(examples).containsKeys(text);
        assertThat(examples.get(text).getMentions()) //
                .extracting(Mention::getCoveredText, Mention::getLabel) //
                .contains( //
                        tuple("John", "PER"), //
                        tuple("Mary", "PER"));

        var template1 = """
                {% for sentence, example in examples.items() %}
                {{ sentence }}
                {% set mentions = example.getLabelledMentions() %}
                {% for mention, label in mentions.items() %}
                {{ mention }} :: {{ label }}
                {% endfor %}
                {% endfor %}""";

        // System.out.println(render(examples, template1));

        var template2 = """
                {% for sentence, example in examples.items() %}
                Extract and classify the named entities from the following sentence as JSON:

                Text:

                '''
                {{ example.getText() }}
                '''

                {{ example.getLabelledMentions() | tojson }}
                {% endfor %}""";

        // System.out.println(render(examples, template2));
    }

    private String render(Map<String, MentionsSample> examples, String template)
    {
        var jinjava = new Jinjava(new JinjavaConfig());
        return jinjava.render(template, Map.of("examples", examples));
    }
}
