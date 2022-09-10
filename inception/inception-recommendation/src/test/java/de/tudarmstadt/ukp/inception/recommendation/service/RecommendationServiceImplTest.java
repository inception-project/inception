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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SENTENCES;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.support.uima.FeatureStructureBuilder.buildFS;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.SKIPPED;
import static de.tudarmstadt.ukp.inception.recommendation.service.RecommendationServiceImpl.extractSuggestions;
import static de.tudarmstadt.ukp.inception.recommendation.service.RecommendationServiceImpl.getOffsets;
import static de.tudarmstadt.ukp.inception.recommendation.service.RecommendationServiceImpl.hideSuggestionsRejectedOrSkipped;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_BOOLEAN;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOUBLE;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.apache.uima.UIMAFramework;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;

class RecommendationServiceImplTest
{
    private TokenBuilder<Token, Sentence> tokenBuilder;
    private SourceDocument doc1;
    private SourceDocument doc2;
    private AnnotationLayer layer1;
    private AnnotationLayer layer2;
    private AnnotationFeature feature1;
    private AnnotationFeature feature2;

    @BeforeEach
    void setup()
    {
        tokenBuilder = new TokenBuilder<>(Token.class, Sentence.class);
        doc1 = new SourceDocument("doc1", null, null);
        doc1.setId(1l);
        doc2 = new SourceDocument("doc2", null, null);
        doc2.setId(2l);
        layer1 = AnnotationLayer.builder().withId(1l).withName("layer1").build();
        layer2 = AnnotationLayer.builder().withId(2l).withName("layer2").build();
        feature1 = AnnotationFeature.builder().withName("feat1").build();
        feature2 = AnnotationFeature.builder().withName("feat2").build();
    }

    @Test
    void thatRejectedSuggestionIsHidden()
    {
        var records = asList(LearningRecord.builder() //
                .withSourceDocument(doc1) //
                .withLayer(layer1) //
                .withAnnotationFeature(feature1) //
                .withOffsetBegin(0) //
                .withOffsetEnd(10) //
                .withAnnotation("x") //
                .withUserAction(REJECTED) //
                .build());

        var doc1Suggestion = makeSuggestion(0, 10, "x", doc1, layer1, feature1);
        assertThat(doc1Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc1Suggestion, records);
        assertThat(doc1Suggestion.isVisible()) //
                .as("Suggestion in same document/layer/feature should be hidden") //
                .isFalse();
        assertThat(doc1Suggestion.getReasonForHiding().trim()).isEqualTo("rejected");

        var doc2Suggestion = makeSuggestion(0, 10, "x", doc2, layer1, feature1);
        assertThat(doc2Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc2Suggestion, records);
        assertThat(doc2Suggestion.isVisible()) //
                .as("Suggestion in other document should not be hidden") //
                .isTrue();

        var doc3Suggestion = makeSuggestion(0, 10, "x", doc1, layer2, feature1);
        assertThat(doc3Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc3Suggestion, records);
        assertThat(doc3Suggestion.isVisible()) //
                .as("Suggestion in other layer should not be hidden") //
                .isTrue();

        var doc4Suggestion = makeSuggestion(0, 10, "x", doc1, layer1, feature2);
        assertThat(doc4Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc3Suggestion, records);
        assertThat(doc4Suggestion.isVisible()) //
                .as("Suggestion in other feature should not be hidden") //
                .isTrue();
    }

    @Test
    void thatSkippedSuggestionIsHidden()
    {
        var records = asList(LearningRecord.builder() //
                .withSourceDocument(doc1) //
                .withLayer(layer1) //
                .withAnnotationFeature(feature1) //
                .withOffsetBegin(0) //
                .withOffsetEnd(10) //
                .withAnnotation("x") //
                .withUserAction(SKIPPED) //
                .build());

        var doc1Suggestion = makeSuggestion(0, 10, "x", doc1, layer1, feature1);
        assertThat(doc1Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc1Suggestion, records);
        assertThat(doc1Suggestion.isVisible()) //
                .as("Suggestion in same document/layer/feature should be hidden") //
                .isFalse();
        assertThat(doc1Suggestion.getReasonForHiding().trim()).isEqualTo("skipped");

        var doc2Suggestion = makeSuggestion(0, 10, "x", doc2, layer1, feature1);
        assertThat(doc2Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc2Suggestion, records);
        assertThat(doc2Suggestion.isVisible()) //
                .as("Suggestion in other document should not be hidden") //
                .isTrue();

        var doc3Suggestion = makeSuggestion(0, 10, "x", doc1, layer2, feature1);
        assertThat(doc3Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc3Suggestion, records);
        assertThat(doc3Suggestion.isVisible()) //
                .as("Suggestion in other layer should not be hidden") //
                .isTrue();

        var doc4Suggestion = makeSuggestion(0, 10, "x", doc1, layer1, feature2);
        assertThat(doc4Suggestion.isVisible()).isTrue();
        hideSuggestionsRejectedOrSkipped(doc3Suggestion, records);
        assertThat(doc4Suggestion.isVisible()) //
                .as("Suggestion in other feature should not be hidden") //
                .isTrue();
    }

    @Test
    void testOffsetAlignmentWithAnchoringOnCharacters() throws Exception
    {
        var mode = CHARACTERS;
        var targetCas = CasFactory.createCas();
        tokenBuilder.buildTokens(targetCas.getJCas(), "This is a test .");

        var suggestionCas = JCasFactory.createText(targetCas.getDocumentText());

        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, 0, 1))) //
                .as("Trivial case: one character") //
                .get().isEqualTo(new Offset(0, 1));
        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, 4, 8))) //
                .as("Leading and trailing space should be removed") //
                .get().isEqualTo(new Offset(5, 7));
        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, -10, 100))) //
                .as("Range should be clipped to document boundaries").get()
                .isEqualTo(new Offset(0, targetCas.getDocumentText().length()));
    }

    @Test
    void testOffsetAlignmentWithAnchoringOnSingleToken() throws Exception
    {
        var mode = SINGLE_TOKEN;
        var targetCas = CasFactory.createCas();
        tokenBuilder.buildTokens(targetCas.getJCas(), "This is a test .");

        var suggestionCas = JCasFactory.createText(targetCas.getDocumentText());

        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, 0, 1))) //
                .as("Reduce to empty if no token is fully covered") //
                .isEmpty();
        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, 0, 4))) //
                .as("Trivial case: one token") //
                .get().isEqualTo(new Offset(0, 4));
        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, 0, 7))) //
                .as("Discard suggestion if it covers more than one token") //
                .isEmpty();
        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, -10, 100))) //
                .as("Discard suggestion if it covers more than one token (2)") //
                .isEmpty();
    }

    @Test
    void testOffsetAlignmentWithAnchoringOnTokens() throws Exception
    {
        var mode = TOKENS;
        var targetCas = CasFactory.createCas();
        tokenBuilder.buildTokens(targetCas.getJCas(), "This is a test .");

        var suggestionCas = JCasFactory.createText(targetCas.getDocumentText());

        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, 0, 1))) //
                .as("Reduce to empty if no token is fully covered") //
                .isEmpty();
        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, 0, 4))) //
                .as("Trivial case: one token") //
                .get().isEqualTo(new Offset(0, 4));
        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, 0, 7))) //
                .as("Trivial case: two tokens") //
                .get().isEqualTo(new Offset(0, 7));
        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, 2, 12))) //
                .as("Discard incompletely covered tokens") //
                .get().isEqualTo(new Offset(5, 9));
        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, -10, 100))) //
                .as("Range should be clipped to document boundaries").get()
                .isEqualTo(new Offset(0, targetCas.getDocumentText().length()));
    }

    @Test
    void testOffsetAlignmentWithAnchoringOnSentences() throws Exception
    {
        var mode = SENTENCES;
        var targetCas = CasFactory.createCas();
        tokenBuilder.buildTokens(targetCas.getJCas(), "This is a test .\nAnother sentence here .");

        var suggestionCas = JCasFactory.createText(targetCas.getDocumentText());
        var sentences = targetCas.select(Sentence.class).asList();
        var sent1 = sentences.get(0);
        var sent2 = sentences.get(1);

        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, 0, 1))) //
                .as("Reduce to empty if no sentence is fully covered") //
                .isEmpty();
        assertThat(getOffsets(mode, targetCas,
                new Annotation(suggestionCas, sent1.getBegin(), sent1.getEnd()))) //
                        .as("Trivial case: one sentence") //
                        .get().isEqualTo(new Offset(sent1.getBegin(), sent1.getEnd()));
        assertThat(getOffsets(mode, targetCas,
                new Annotation(suggestionCas, sent1.getBegin(), sent2.getEnd()))) //
                        .as("Trivial case: two sentences") //
                        .get().isEqualTo(new Offset(sent1.getBegin(), sent2.getEnd()));
        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, 0, 30))) //
                .as("Discard incompletely covered sentences") //
                .get().isEqualTo(new Offset(0, 16));
        assertThat(getOffsets(mode, targetCas, new Annotation(suggestionCas, -10, 100))) //
                .as("Range should be clipped to document boundaries").get()
                .isEqualTo(new Offset(0, targetCas.getDocumentText().length()));
    }

    @Test
    void testExtractSuggestionsWithSpanSuggestions() throws Exception
    {
        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();
        var predType = tsd.addType("Prediction", null, TYPE_NAME_ANNOTATION);
        predType.addFeature("value", null, TYPE_NAME_STRING);
        predType.addFeature("value" + FEATURE_NAME_SCORE_SUFFIX, null, TYPE_NAME_DOUBLE);
        predType.addFeature("value" + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX, null,
                TYPE_NAME_STRING);
        predType.addFeature(FEATURE_NAME_IS_PREDICTION, null, TYPE_NAME_BOOLEAN);

        var targetCas = createCas(mergeTypeSystems(asList(tsd, createTypeSystemDescription())));
        tokenBuilder.buildTokens(targetCas.getJCas(), "This is a test .\nAnother sentence here .");

        var layer = AnnotationLayer.builder() //
                .withId(1l) //
                .forUimaType(targetCas.getTypeSystem().getType(predType.getName())) //
                .withType(WebAnnoConst.SPAN_TYPE) //
                .withAnchoringMode(TOKENS) //
                .build();
        var feature = AnnotationFeature.builder() //
                .withId(1l) //
                .withName("value") //
                .withLayer(layer1) //
                .build();
        var recommender = Recommender.builder() //
                .withId(1l) //
                .withEnabled(true) //
                .withLayer(layer) //
                .withFeature(feature) //
                .withMaxRecommendations(3) //
                .build();

        var suggestionCas = createCas(mergeTypeSystems(asList(tsd, createTypeSystemDescription())));
        buildFS(suggestionCas, predType.getName()) //
                .withFeature(Annotation._FeatName_begin, 0) //
                .withFeature(Annotation._FeatName_end, 4) //
                .withFeature("value", "foo") //
                .withFeature("value" + FEATURE_NAME_SCORE_SUFFIX, 1.0d) //
                .withFeature("value" + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX, "one") //
                .withFeature(FEATURE_NAME_IS_PREDICTION, true) //
                .buildAndAddToIndexes();
        buildFS(suggestionCas, predType.getName()) //
                .withFeature(Annotation._FeatName_begin, 5) //
                .withFeature(Annotation._FeatName_end, 12) //
                .withFeature("value", "bar") //
                .withFeature("value" + FEATURE_NAME_SCORE_SUFFIX, 0.5d) //
                .withFeature("value" + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX, "two") //
                .withFeature(FEATURE_NAME_IS_PREDICTION, true) //
                .buildAndAddToIndexes();

        var suggestions = extractSuggestions(targetCas, suggestionCas, doc1, recommender);

        assertThat(suggestions) //
                .extracting( //
                        AnnotationSuggestion::getLabel, //
                        AnnotationSuggestion::getScore, //
                        s -> s.getScoreExplanation().orElse(null), //
                        AnnotationSuggestion::getPosition)
                .containsExactly( //
                        tuple("foo", 1.0d, "one", new Offset(0, 4)), //
                        tuple("bar", 0.5d, "two", new Offset(5, 9)));
    }

    private SpanSuggestion makeSuggestion(int aBegin, int aEnd, String aLabel, SourceDocument aDoc,
            AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        return new SpanSuggestion(0, // aId,
                0, // aRecommenderId,
                "", // aRecommenderName
                aLayer.getId(), // aLayerId,
                aFeature.getName(), // aFeature,
                aDoc.getName(), // aDocumentName
                aBegin, // aBegin
                aEnd, // aEnd
                "", // aCoveredText,
                aLabel, // aLabel
                aLabel, // aUiLabel
                0.0, // aScore
                "" // aScoreExplanation
        );
    }
}
