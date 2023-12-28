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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Arrays;

import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.rendering.model.Range;

class RecommendationServiceImplTest
{
    private TokenBuilder<Token, Sentence> tokenBuilder;
    private SourceDocument doc1;
    private SourceDocument doc2;
    private AnnotationLayer layer1;

    @BeforeEach
    void setup()
    {
        tokenBuilder = new TokenBuilder<>(Token.class, Sentence.class);
        doc1 = new SourceDocument("doc1", null, null);
        doc1.setId(1l);
        doc2 = new SourceDocument("doc2", null, null);
        doc2.setId(2l);
        layer1 = AnnotationLayer.builder().withId(1l).withName("layer1").build();
    }

    @Test
    void testReconciliation() throws Exception
    {
        var sessionOwner = User.builder().withUsername("user").build();
        var doc = SourceDocument.builder().withName("doc1").build();
        var layer = AnnotationLayer.builder().withId(1l).build();
        var feature = AnnotationFeature.builder().withName("feature").withLayer(layer).build();
        var rec = Recommender.builder().withId(1l).withName("rec").withLayer(layer)
                .withFeature(feature).build();
        var project = Project.builder().withId(1l).build();

        var existingSuggestions = Arrays.<AnnotationSuggestion> asList( //
                SpanSuggestion.builder() //
                        .withId(0) //
                        .withPosition(0, 10) //
                        .withDocument(doc) //
                        .withLabel("aged") //
                        .withRecommender(rec) //
                        .build(),
                SpanSuggestion.builder() //
                        .withId(1) //
                        .withPosition(0, 10) //
                        .withDocument(doc) //
                        .withLabel("removed") //
                        .withRecommender(rec) //
                        .build());
        var activePredictions = new Predictions(sessionOwner, sessionOwner.getUsername(), project);
        activePredictions.inheritSuggestions(existingSuggestions);

        var newSuggestions = Arrays.<AnnotationSuggestion> asList( //
                SpanSuggestion.builder() //
                        .withId(2) //
                        .withPosition(0, 10) //
                        .withDocument(doc) //
                        .withLabel("aged") //
                        .withRecommender(rec) //
                        .build(),
                SpanSuggestion.builder() //
                        .withId(3) //
                        .withPosition(new Offset(0, 10)) //
                        .withDocument(doc) //
                        .withLabel("added") //
                        .withRecommender(rec) //
                        .build());

        var result = RecommendationServiceImpl.reconcile(activePredictions, doc, rec,
                new Range(0, 10), newSuggestions);

        assertThat(result.suggestions()) //
                .extracting(AnnotationSuggestion::getId, AnnotationSuggestion::getLabel,
                        AnnotationSuggestion::getAge) //
                .containsExactlyInAnyOrder(tuple(0, "aged", 1), tuple(3, "added", 0));
    }
}
