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
package de.tudarmstadt.ukp.inception.recommendation.service;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderTypeSystemUtils;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils;

class SpanSuggestionExtractionTest
{
    private Project project;
    private SourceDocument document;
    private CAS originalCas;

    @BeforeEach
    void setup() throws Exception
    {
        project = Project.builder() //
                .withId(1l) //
                .withName("Test") //
                .build();
        document = SourceDocument.builder() //
                .withId(1l) //
                .withProject(project) //
                .withName("Doc") //
                .build();

        originalCas = CasFactory.createCas();
        originalCas.setDocumentText("This is a test.");

        SegmentationUtils.splitSentences(originalCas);
        SegmentationUtils.tokenize(originalCas);
    }

    @Test
    void testSpanExtraction() throws Exception
    {
        var layer = AnnotationLayer.builder() //
                .withId(1l) //
                .forJCasClass(NamedEntity.class) //
                .withType(SpanLayerSupport.TYPE) //
                .build();
        var feature = AnnotationFeature.builder() //
                .withLayer(layer) //
                .withName(NamedEntity._FeatName_value) //
                .build();
        var recommender = Recommender.builder() //
                .withId(1l) //
                .withName("recommender") //
                .withProject(project) //
                .withLayer(layer) //
                .withFeature(feature) //
                .build();
        AnnotationFeature[] aFeatures = { feature };

        var predictionCas = RecommenderTypeSystemUtils.makePredictionCas(originalCas, aFeatures);

        buildAnnotation(predictionCas, feature.getLayer().getName()) //
                .on("\\bis\\b") //
                .withFeature(feature.getName(), "verb") //
                .withFeature(FEATURE_NAME_IS_PREDICTION, true) //
                .buildAndAddToIndexes();

        var suggestions = SuggestionExtraction.extractSuggestions(1, originalCas, predictionCas,
                document, recommender);

        assertThat(suggestions) //
                .filteredOn(a -> a instanceof SpanSuggestion) //
                .map(a -> (SpanSuggestion) a) //
                .extracting( //
                        SpanSuggestion::getRecommenderName, //
                        SpanSuggestion::getLabel) //
                .containsExactly( //
                        tuple(recommender.getName(), "verb"));
    }
}
