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
package de.tudarmstadt.ukp.inception.annotation.layer.span.recommender;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SENTENCES;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.inception.annotation.layer.span.recommender.SpanSuggestionSupport.getOffsets;
import static de.tudarmstadt.ukp.inception.annotation.layer.span.recommender.SpanSuggestionSupport.getOffsetsAnchoredOnTokens;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.config.SpanRecommenderProperties;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderTypeSystemUtils;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.ExtractionContext;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils;

@ExtendWith(MockitoExtension.class)
class SpanSuggestionExtractionTest
{
    private @Mock RecommendationService recommendationService;
    private @Mock LearningRecordService learningRecordService;
    private @Mock ApplicationEventPublisher applicationEventPublisher;
    private @Mock AnnotationSchemaService schemaService;
    private @Mock FeatureSupportRegistry featureSupportRegistry;
    private @Mock SpanRecommenderProperties recommenderProperties;

    private TokenBuilder<Token, Sentence> tokenBuilder;
    private Project project;
    private SourceDocument document;
    private CAS originalCas;

    private SpanSuggestionSupport sut;

    @BeforeEach
    void setup() throws Exception
    {
        tokenBuilder = new TokenBuilder<>(Token.class, Sentence.class);
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

        sut = new SpanSuggestionSupport(recommendationService, learningRecordService,
                applicationEventPublisher, schemaService, featureSupportRegistry,
                recommenderProperties);
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
                .onMatch("\\bis\\b") //
                .withFeature(feature.getName(), "verb") //
                .withFeature(FEATURE_NAME_IS_PREDICTION, true) //
                .buildAndAddToIndexes();

        var ctx = new ExtractionContext(0, recommender, document, originalCas, predictionCas);
        var suggestions = sut.extractSuggestions(ctx);

        assertThat(suggestions) //
                .filteredOn(a -> a instanceof SpanSuggestion) //
                .map(a -> (SpanSuggestion) a) //
                .extracting( //
                        SpanSuggestion::getRecommenderName, //
                        SpanSuggestion::getLabel) //
                .containsExactly( //
                        tuple(recommender.getName(), "verb"));
    }

    @Test
    void thatZeroWithAnnotationsAreCorrectlyAnchoredOnTokens() throws Exception
    {
        var jCas = createJCas();
        TokenBuilder.create(Token.class, Sentence.class).buildTokens(jCas, "  This is  a test.  ");
        var textLength = jCas.getDocumentText().length();
        var tokens = jCas.select(Token.class).asList();
        var firstTokenBegin = tokens.get(0).getBegin();
        var lastTokenEnd = tokens.get(tokens.size() - 1).getEnd();

        assertThat(getOffsetsAnchoredOnTokens(jCas.getCas(), new Annotation(jCas, 0, 0))).get()
                .as("Zero-width annotation before first token snaps to first token start")
                .isEqualTo(new Offset(firstTokenBegin, firstTokenBegin));

        assertThat(getOffsetsAnchoredOnTokens(jCas.getCas(),
                new Annotation(jCas, textLength, textLength))).get()
                        .as("Zero-width annotation after last token snaps to last token end")
                        .isEqualTo(new Offset(lastTokenEnd, lastTokenEnd));

        assertThat(getOffsetsAnchoredOnTokens(jCas.getCas(), new Annotation(jCas, 4, 4))).get()
                .as("Zero-width annotation within token remains") //
                .isEqualTo(new Offset(4, 4));

        assertThat(getOffsetsAnchoredOnTokens(jCas.getCas(), new Annotation(jCas, 10, 10))).get()
                .as("Zero-width annotation between tokens snaps to end of previous") //
                .isEqualTo(new Offset(9, 9));
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
}
