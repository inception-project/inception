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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.fit.factory.CasFactory.createText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.uima.cas.CAS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils;

@ExtendWith(MockitoExtension.class)
class PredictionsTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected Project project;
    protected User user;
    protected AnnotationLayer layer;
    protected CAS cas;
    protected Predictions sut;

    @BeforeEach
    void setup() throws Exception
    {
        project = Project.builder().withId(1l).withName("Project").build();
        user = User.builder().withUsername("user").build();
        layer = AnnotationLayer.builder().withId(1l).withName("Entity").build();

        cas = createText(contentOf(getClass().getResource("/text/lorem.txt"), UTF_8), "en");
        SegmentationUtils.splitSentences(cas);
        SegmentationUtils.tokenize(cas);

        sut = new Predictions(user, user.getUsername(), project);
    }

    @Test
    void testGetGroupedPredictions() throws Exception
    {
        var sentences = cas.select(Sentence.class).asList();
        var winBegin = sentences.get(Math.round(sentences.size() * 0.25f)).getBegin();
        var winEnd = sentences.get(Math.round(sentences.size() * 0.75f)).getEnd();

        sut.inheritSuggestions(generatePredictions(100, 1, 100));
        assertThat(sut.size()).isEqualTo(10000);

        var groups = sut.getGroupedPredictions(SpanSuggestion.class, 10L, layer, winBegin, winEnd);
        assertThat(groups).isNotEmpty();
    }

    @Test
    void timeGetGroupedPredictions() throws Exception
    {
        var sentences = cas.select(Sentence.class).asList();
        var winBegin = sentences.get(Math.round(sentences.size() * 0.25f)).getBegin();
        var winEnd = sentences.get(Math.round(sentences.size() * 0.75f)).getEnd();

        sut = new Predictions(user, user.getUsername(), project);
        var generatedPredictions = generatePredictions(10_000, 1, 1_000);
        sut.inheritSuggestions(generatedPredictions);
        var documents = generatedPredictions.stream() //
                .map(AnnotationSuggestion::getDocumentId) //
                .distinct() //
                .collect(toList());

        var rng = new Random(1234l);
        for (int i = 0; i < 100; i++) {
            var document = documents.get(rng.nextInt(documents.size()));
            var start = System.currentTimeMillis();
            sut.getGroupedPredictions(SpanSuggestion.class, document, layer, winBegin, winEnd);
            LOG.info("getGroupedPredictions {} - {}ms", sut.size(),
                    System.currentTimeMillis() - start);
        }
    }

    @Test
    void thatIdsAreAssigned() throws Exception
    {
        var doc = SourceDocument.builder() //
                .withId(1L) //
                .withName("doc") //
                .build();
        sut = new Predictions(user, user.getUsername(), project);
        sut.putSuggestions(1, 0, 0, asList( //
                SpanSuggestion.builder() //
                        .withId(AnnotationSuggestion.NEW_ID) //
                        .withDocument(doc) //
                        .build()));

        assertThat(sut.getSuggestionsByDocument(doc)) //
                .extracting(AnnotationSuggestion::getId) //
                .containsExactly(0);

        var inheritedPredictions = sut.getSuggestionsByDocument(doc);
        sut = new Predictions(sut);
        sut.putSuggestions(1, 0, 0, asList( //
                SpanSuggestion.builder() //
                        .withId(AnnotationSuggestion.NEW_ID) //
                        .withDocument(doc) //
                        .build()));
        sut.inheritSuggestions(inheritedPredictions);

        assertThat(sut.getSuggestionsByDocument(doc)) //
                .extracting(AnnotationSuggestion::getId) //
                .containsExactlyInAnyOrder(0, 1);
    }

    private List<AnnotationSuggestion> generatePredictions(int aDocs, int aRecommenders,
            int aSuggestions)
        throws Exception
    {
        var labels = asList("PER", "ORG", "LOC", "MISC");
        var rng = new Random(1234l);

        var tokens = cas.select(Token.class).asList();

        var result = new ArrayList<AnnotationSuggestion>();
        for (var docId = 0l; docId < aDocs; docId++) {
            var doc = SourceDocument.builder().withId(docId).withName("doc" + docId).build();
            for (var recId = 0l; recId < aRecommenders; recId++) {
                var feature = AnnotationFeature.builder().withId(recId).withName("feat" + recId)
                        .build();
                var rec = Recommender.builder().withId(recId).withName("rec" + recId)
                        .withLayer(layer).withFeature(feature).build();
                for (int annId = 0; annId < aSuggestions; annId++) {
                    var label = labels.get(rng.nextInt(labels.size()));
                    var token = tokens.get(rng.nextInt(tokens.size()));
                    var ann = SpanSuggestion.builder() //
                            .withId(annId) //
                            .withDocument(doc) //
                            .withRecommender(rec) //
                            .withScore(rng.nextDouble()) //
                            .withScoreExplanation(null) //
                            .withLabel(label) //
                            .withUiLabel(label) //
                            .withPosition(new Offset(token.getBegin(), token.getEnd())) //
                            .withCoveredText(token.getCoveredText()) //
                            .build();
                    result.add(ann);
                }
            }
        }

        return result;
    }
}
