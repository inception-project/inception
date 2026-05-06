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
package de.tudarmstadt.ukp.inception.annotation.layer.span;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.LayerFactory;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.DeleteSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistryImpl;

@ExtendWith(MockitoExtension.class)
class SegmentationUnitAdapterTest
{
    private @Mock ConstraintsService constraintsService;

    private FeatureSupportRegistry featureSupportRegistry;
    private LayerSupportRegistryImpl layerSupportRegistry;
    private Project project;

    private AnnotationLayer sentenceLayer;
    private AnnotationLayer tokenLayer;

    private JCas cas;

    @BeforeEach
    public void setup() throws Exception
    {
        if (cas == null) {
            cas = JCasFactory.createJCas();
        }
        else {
            cas.reset();
        }

        project = new Project();
        project.setId(1l);

        tokenLayer = LayerFactory.tokenLayer(project) //
                .withId(1l) //
                .withEnabled(true) //
                .build();

        sentenceLayer = LayerFactory.sentenceLayer(project) //
                .withId(2l) //
                .withEnabled(true) //
                .build();

        var layerBehaviorRegistry = new LayerBehaviorRegistryImpl(asList());
        layerBehaviorRegistry.init();

        featureSupportRegistry = mock(FeatureSupportRegistry.class);
        layerSupportRegistry = new LayerSupportRegistryImpl(asList(new SpanLayerSupportImpl(
                featureSupportRegistry, null, layerBehaviorRegistry, constraintsService)));
        layerSupportRegistry.init();
    }

    @Nested
    class TokenTests
    {
        private SegmentationUnitAdapter sut;

        @BeforeEach
        public void setup() throws Exception
        {
            var tokenSupport = layerSupportRegistry.getLayerSupport(tokenLayer);
            var tokenAdapter = (SpanAdapter) tokenSupport.createAdapter(tokenLayer, ArrayList::new);
            sut = new SegmentationUnitAdapter(tokenAdapter);
        }

        @Test
        void testDeleteInitialTokenInSentence() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 3);
            var s2 = new Sentence(cas, 4, 7);
            var t1 = new Token(cas, 0, 1);
            var t2 = new Token(cas, 2, 3);
            var t3 = new Token(cas, 4, 5);
            var t4 = new Token(cas, 6, 7);
            asList(s1, s2, t1, t2, t3, t4).forEach(cas::addFsToIndexes);

            var req = new DeleteSpanAnnotationRequest(null, null, cas.getCas(), t3);
            sut.handle(req);

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1", "2", "3 4");
        }

        @Test
        void testDeleteFinalTokenInSentence() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 3);
            var s2 = new Sentence(cas, 4, 7);
            var t1 = new Token(cas, 0, 1);
            var t2 = new Token(cas, 2, 3);
            var t3 = new Token(cas, 4, 5);
            var t4 = new Token(cas, 6, 7);
            asList(s1, s2, t1, t2, t3, t4).forEach(cas::addFsToIndexes);

            var req = new DeleteSpanAnnotationRequest(null, null, cas.getCas(), t2);
            sut.handle(req);

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1 2", "3", "4");
        }

        @Test
        void testDeleteInitialTokenInDocument() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 3);
            var s2 = new Sentence(cas, 4, 7);
            var t1 = new Token(cas, 0, 1);
            var t2 = new Token(cas, 2, 3);
            var t3 = new Token(cas, 4, 5);
            var t4 = new Token(cas, 6, 7);
            asList(s1, s2, t1, t2, t3, t4).forEach(cas::addFsToIndexes);

            var req = new DeleteSpanAnnotationRequest(null, null, cas.getCas(), t1);
            sut.handle(req);

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1 2", "3", "4");
        }

        @Test
        void testDeleteFinalTokenInDocument() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 3);
            var s2 = new Sentence(cas, 4, 7);
            var t1 = new Token(cas, 0, 1);
            var t2 = new Token(cas, 2, 3);
            var t3 = new Token(cas, 4, 5);
            var t4 = new Token(cas, 6, 7);
            asList(s1, s2, t1, t2, t3, t4).forEach(cas::addFsToIndexes);

            var req = new DeleteSpanAnnotationRequest(null, null, cas.getCas(), t4);
            sut.handle(req);

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1", "2", "3 4");
        }

        @Test
        void testDeleteLastTokenInSentence() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 1);
            var s2 = new Sentence(cas, 2, 3);
            var s3 = new Sentence(cas, 4, 7);
            var t1 = new Token(cas, 0, 1);
            var t2 = new Token(cas, 2, 3);
            var t3 = new Token(cas, 4, 5);
            var t4 = new Token(cas, 6, 7);
            asList(s1, s2, s3, t1, t2, t3, t4).forEach(cas::addFsToIndexes);

            var req = new DeleteSpanAnnotationRequest(null, null, cas.getCas(), t2);

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.handle(req)) //
                    .withMessageContaining("last unit cannot be deleted");

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1", "2", "3", "4");
        }

        @Test
        void testSplitTokenInMiddleBeforeWhitespace() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 7);
            var t1 = new Token(cas, 0, 3);
            var t2 = new Token(cas, 4, 7);
            asList(s1, t1, t2).forEach(cas::addFsToIndexes);

            sut.handle(CreateSpanAnnotationRequest.builder() //
                    .withCas(cas.getCas())//
                    .withRange(1, 1)//
                    .build());

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1", "2", "3 4");
        }

        @Test
        void testSplitTokenInMiddleAfterWhitespace() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 7);
            var t1 = new Token(cas, 0, 3);
            var t2 = new Token(cas, 4, 7);
            asList(s1, t1, t2).forEach(cas::addFsToIndexes);

            sut.handle(CreateSpanAnnotationRequest.builder() //
                    .withCas(cas.getCas())//
                    .withRange(2, 2)//
                    .build());

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1", "2", "3 4");
        }

        @Test
        void testSplitTokenAtStartBoundary() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 7);
            var t1 = new Token(cas, 0, 3);
            var t2 = new Token(cas, 4, 7);
            asList(s1, t1, t2).forEach(cas::addFsToIndexes);

            var req = CreateSpanAnnotationRequest.builder() //
                    .withCas(cas.getCas()) //
                    .withRange(0, 0) //
                    .build();

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.handle(req)) //
                    .withMessageContaining("may not create a zero-width unit");

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1 2", "3 4");
        }

        @Test
        void testSplitTokenAtEndBoundary() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 7);
            var t1 = new Token(cas, 0, 3);
            var t2 = new Token(cas, 4, 7);
            asList(s1, t1, t2).forEach(cas::addFsToIndexes);

            var req = CreateSpanAnnotationRequest.builder() //
                    .withCas(cas.getCas()) //
                    .withRange(3, 3) //
                    .build();

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.handle(req)) //
                    .withMessageContaining("may not create a zero-width unit");

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1 2", "3 4");
        }

        @Test
        void testSplitDirectlyAdjacentToken() throws Exception
        {
            cas.setDocumentText("1 23 4");

            var s1 = new Sentence(cas, 0, 6);
            var t1 = new Token(cas, 0, 3);
            var t2 = new Token(cas, 3, 6);
            asList(s1, t1, t2).forEach(cas::addFsToIndexes);

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1 2", "3 4");

            var req = CreateSpanAnnotationRequest.builder() //
                    .withCas(cas.getCas()) //
                    .withRange(3, 3) //
                    .build();

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.handle(req)) //
                    .withMessageContaining("may not create a zero-width unit");

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1 2", "3 4");
        }

        @Test
        void testCreateNewToken() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 7);
            var t1 = new Token(cas, 0, 3);
            var t2 = new Token(cas, 4, 7);
            asList(s1, t1, t2).forEach(cas::addFsToIndexes);

            var req = CreateSpanAnnotationRequest.builder() //
                    .withCas(cas.getCas()) //
                    .withRange(1, 2) //
                    .build();

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.handle(req)) //
                    .withMessageContaining("Can only split unit, not create an entirely new one.");

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1 2", "3 4");
        }
    }

    @Nested
    class SentenceTests
    {
        private SegmentationUnitAdapter sut;

        @BeforeEach
        public void setup() throws Exception
        {
            var sentenceSupport = layerSupportRegistry.getLayerSupport(sentenceLayer);
            var sentenceAdapter = (SpanAdapter) sentenceSupport.createAdapter(sentenceLayer,
                    ArrayList::new);
            sut = new SegmentationUnitAdapter(sentenceAdapter);
        }

        @Test
        void testDeleteInitialSentenceInDocument() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 1);
            var s2 = new Sentence(cas, 2, 5);
            var s3 = new Sentence(cas, 6, 7);
            var t1 = new Token(cas, 0, 1);
            var t2 = new Token(cas, 2, 3);
            var t3 = new Token(cas, 4, 5);
            var t4 = new Token(cas, 6, 7);
            asList(s1, s2, s3, t1, t2, t3, t4).forEach(cas::addFsToIndexes);

            assertThat(cas.select(Sentence.class).asList()) //
                    .extracting(Sentence::getCoveredText) //
                    .containsExactly("1", "2 3", "4");

            var req = new DeleteSpanAnnotationRequest(null, null, cas.getCas(), s1);
            sut.handle(req);

            assertThat(cas.select(Sentence.class).asList()) //
                    .extracting(Sentence::getCoveredText) //
                    .containsExactly("1 2 3", "4");

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1", "2", "3", "4");
        }

        @Test
        void testDeleteSentence() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 1);
            var s2 = new Sentence(cas, 2, 5);
            var s3 = new Sentence(cas, 6, 7);
            var t1 = new Token(cas, 0, 1);
            var t2 = new Token(cas, 2, 3);
            var t3 = new Token(cas, 4, 5);
            var t4 = new Token(cas, 6, 7);
            asList(s1, s2, s3, t1, t2, t3, t4).forEach(cas::addFsToIndexes);

            assertThat(cas.select(Sentence.class).asList()) //
                    .extracting(Sentence::getCoveredText) //
                    .containsExactly("1", "2 3", "4");

            var req = new DeleteSpanAnnotationRequest(null, null, cas.getCas(), s2);
            sut.handle(req);

            assertThat(cas.select(Sentence.class).asList()) //
                    .extracting(Sentence::getCoveredText) //
                    .containsExactly("1 2 3", "4");

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1", "2", "3", "4");
        }

        @Test
        void testDeleteFinalSentenceInDocument() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 1);
            var s2 = new Sentence(cas, 2, 5);
            var s3 = new Sentence(cas, 6, 7);
            var t1 = new Token(cas, 0, 1);
            var t2 = new Token(cas, 2, 3);
            var t3 = new Token(cas, 4, 5);
            var t4 = new Token(cas, 6, 7);
            asList(s1, s2, s3, t1, t2, t3, t4).forEach(cas::addFsToIndexes);

            assertThat(cas.select(Sentence.class).asList()) //
                    .extracting(Sentence::getCoveredText) //
                    .containsExactly("1", "2 3", "4");

            var req = new DeleteSpanAnnotationRequest(null, null, cas.getCas(), s3);
            sut.handle(req);

            assertThat(cas.select(Sentence.class).asList()) //
                    .extracting(Sentence::getCoveredText) //
                    .containsExactly("1", "2 3 4");

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1", "2", "3", "4");
        }

        @Test
        void testDeleteLastSentenceInDocument() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 7);
            var t1 = new Token(cas, 0, 1);
            var t2 = new Token(cas, 2, 3);
            var t3 = new Token(cas, 4, 5);
            var t4 = new Token(cas, 6, 7);
            asList(s1, t1, t2, t3, t4).forEach(cas::addFsToIndexes);

            var req = new DeleteSpanAnnotationRequest(null, null, cas.getCas(), s1);

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.handle(req)) //
                    .withMessageContaining("last unit cannot be deleted");

            assertThat(cas.select(Sentence.class).asList()) //
                    .extracting(Sentence::getCoveredText) //
                    .containsExactly("1 2 3 4");

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1", "2", "3", "4");
        }

        @Test
        void testSplitSentenceAtTokenBoundaryBeforeWhitespace() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 7);
            var t1 = new Token(cas, 0, 1);
            var t2 = new Token(cas, 2, 3);
            var t3 = new Token(cas, 4, 5);
            var t4 = new Token(cas, 6, 7);
            asList(s1, t1, t2, t3, t4).forEach(cas::addFsToIndexes);

            sut.handle(CreateSpanAnnotationRequest.builder() //
                    .withCas(cas.getCas()) //
                    .withRange(3, 3) //
                    .build());

            assertThat(cas.select(Sentence.class).asList()) //
                    .extracting(Sentence::getCoveredText) //
                    .containsExactly("1 2", "3 4");

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1", "2", "3", "4");
        }

        @Test
        void testSplitSentenceAtTokenBoundaryAfterWhitespace() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 7);
            var t1 = new Token(cas, 0, 1);
            var t2 = new Token(cas, 2, 3);
            var t3 = new Token(cas, 4, 5);
            var t4 = new Token(cas, 6, 7);
            asList(s1, t1, t2, t3, t4).forEach(cas::addFsToIndexes);

            var req = CreateSpanAnnotationRequest.builder() //
                    .withCas(cas.getCas()) //
                    .withRange(4, 4) //
                    .build();
            sut.handle(req);

            assertThat(cas.select(Sentence.class).asList()) //
                    .extracting(Sentence::getCoveredText) //
                    .containsExactly("1 2", "3 4");

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1", "2", "3", "4");
        }

        @Test
        void testSplitSentenceInsideToken() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 7);
            var t1 = new Token(cas, 0, 1);
            var t2 = new Token(cas, 2, 5);
            var t3 = new Token(cas, 6, 7);
            asList(s1, t1, t2, t3).forEach(cas::addFsToIndexes);

            var req = CreateSpanAnnotationRequest.builder() //
                    .withCas(cas.getCas()) //
                    .withRange(4, 4) //
                    .build();

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.handle(req)) //
                    .withMessageContaining("Sentences can only be split at token boundaries.");

            assertThat(cas.select(Sentence.class).asList()) //
                    .extracting(Sentence::getCoveredText) //
                    .containsExactly("1 2 3 4");

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1", "2 3", "4");
        }

        @Test
        void testCreateNewSentence() throws Exception
        {
            cas.setDocumentText("1 2 3 4");

            var s1 = new Sentence(cas, 0, 7);
            var t1 = new Token(cas, 0, 3);
            var t2 = new Token(cas, 4, 7);
            asList(s1, t1, t2).forEach(cas::addFsToIndexes);

            var req = CreateSpanAnnotationRequest.builder() //
                    .withCas(cas.getCas()) //
                    .withRange(1, 2) //
                    .build();

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.handle(req)) //
                    .withMessageContaining("Can only split unit, not create an entirely new one.");

            assertThat(cas.select(Token.class).asList()) //
                    .extracting(Token::getCoveredText) //
                    .containsExactly("1 2", "3 4");
        }
    }
}
