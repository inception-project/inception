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
package de.tudarmstadt.ukp.inception.annotation.layer.behavior;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SENTENCES;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanAnchoringModeBehavior.adjust;
import static de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanAnchoringModeBehavior.adjustToSentences;
import static de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanAnchoringModeBehavior.adjustToSingleToken;
import static de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanAnchoringModeBehavior.adjustToTokens;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.MoveSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;

@ExtendWith(MockitoExtension.class)
class SpanAnchoringModeBehaviorTest
{
    private @Mock TypeAdapter adapter;
    private @Mock AnnotationLayer layer;
    private @Mock SpanLayerSupport spanLayerSupport;
    private @Mock ChainLayerSupport chainLayerSupport;

    private SpanAnchoringModeBehavior sut;
    private JCas jcas;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new SpanAnchoringModeBehavior();
        jcas = JCasFactory.createJCas();
        jcas.setDocumentText("The quick brown fox jumps over the lazy dog.");

        // Create tokens: "The" (0-3), "quick" (4-9), "brown" (10-15), "fox" (16-19),
        // "jumps" (20-25), "over" (26-30), "the" (31-34), "lazy" (35-39), "dog" (40-43)
        addToken(0, 3); // The
        addToken(4, 9); // quick
        addToken(10, 15); // brown
        addToken(16, 19); // fox
        addToken(20, 25); // jumps
        addToken(26, 30); // over
        addToken(31, 34); // the
        addToken(35, 39); // lazy
        addToken(40, 43); // dog

        // Create sentences
        addSentence(0, 44); // The quick brown fox jumps over the lazy dog.
    }

    private void addToken(int aBegin, int aEnd)
    {
        Token token = new Token(jcas, aBegin, aEnd);
        token.addToIndexes();
    }

    private void addSentence(int aBegin, int aEnd)
    {
        Sentence sentence = new Sentence(jcas, aBegin, aEnd);
        sentence.addToIndexes();
    }

    @Nested
    class AcceptsTests
    {
        @Test
        void testAcceptsSpanLayerSupport()
        {
            assertThat(sut.accepts(spanLayerSupport)).isTrue();
        }

        @Test
        void testAcceptsChainLayerSupport()
        {
            assertThat(sut.accepts(chainLayerSupport)).isTrue();
        }
    }

    @Nested
    class CreateTests
    {
        @Test
        void testSkipsTokenLayer() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn(Token.class.getName());

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(0) //
                    .withEnd(3) //
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result).isSameAs(request);
        }

        @Test
        void testSkipsSentenceLayer() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn(Sentence.class.getName());

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(0) //
                    .withEnd(44) //
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result).isSameAs(request);
        }

        @Test
        void testRejectsZeroWidthWhenNotAllowed() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getAnchoringMode()).thenReturn(SINGLE_TOKEN);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(5) //
                    .withEnd(5) //
                    .build();

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.onCreate(adapter, request)) //
                    .withMessageContaining("zero-width");
        }

        @Test
        void testAllowsZeroWidthWhenAllowed() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getAnchoringMode()).thenReturn(CHARACTERS);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(5) //
                    .withEnd(5) //
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result).isSameAs(request);
        }

        @Test
        void testAdjustsToTokens() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getAnchoringMode()).thenReturn(TOKENS);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(5) // middle of "quick"
                    .withEnd(12) // middle of "brown"
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result.getBegin()).isEqualTo(4); // start of "quick"
            assertThat(result.getEnd()).isEqualTo(15); // end of "brown"
        }

        @Test
        void testAdjustsToSingleToken() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getAnchoringMode()).thenReturn(SINGLE_TOKEN);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(4) // start of "quick"
                    .withEnd(8) // middle of "quick"
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result.getBegin()).isEqualTo(4); // start of "quick"
            assertThat(result.getEnd()).isEqualTo(9); // end of "quick"
        }

        @Test
        void testAdjustsToSentences() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getAnchoringMode()).thenReturn(SENTENCES);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(10) //
                    .withEnd(20) //
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result.getBegin()).isEqualTo(0); // start of sentence
            assertThat(result.getEnd()).isEqualTo(44); // end of sentence
        }

        @Test
        void testDoesNotAdjustCharacterMode() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getAnchoringMode()).thenReturn(CHARACTERS);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(5) //
                    .withEnd(12) //
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result.getBegin()).isEqualTo(5);
            assertThat(result.getEnd()).isEqualTo(12);
        }

        @Test
        void testUsesRequestAnchoringModeWhenAllowed() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getAnchoringMode()).thenReturn(TOKENS);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(4) //
                    .withEnd(8) //
                    .withAnchoringMode(SINGLE_TOKEN) //
                    .build();

            var result = sut.onCreate(adapter, request);

            // Should use SINGLE_TOKEN mode from request (first token only)
            assertThat(result.getBegin()).isEqualTo(4); // start of "quick"
            assertThat(result.getEnd()).isEqualTo(9); // end of "quick"
        }
    }

    @Nested
    class MoveTests
    {
        @BeforeEach
        void setup()
        {
            when(adapter.getLayer()).thenReturn(layer);
        }

        @Test
        void testAdjustsToTokensOnMove() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getAnchoringMode()).thenReturn(TOKENS);

            var request = MoveSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(5) // middle of "quick"
                    .withEnd(12) // middle of "brown"
                    .build();

            var result = sut.onMove(adapter, request);

            assertThat(result.getBegin()).isEqualTo(4); // start of "quick"
            assertThat(result.getEnd()).isEqualTo(15); // end of "brown"
        }

        @Test
        void testRejectsZeroWidthOnMoveWhenNotAllowed() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getAnchoringMode()).thenReturn(SINGLE_TOKEN);

            var request = MoveSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(5) //
                    .withEnd(5) //
                    .build();

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.onMove(adapter, request)) //
                    .withMessageContaining("zero-width");
        }
    }

    @Nested
    class AdjustMethodTests
    {
        @Test
        void testAdjustCharactersMode() throws Exception
        {
            var result = adjust(jcas.getCas(), CHARACTERS, new int[] { 5, 12 });

            assertThat(result).isEqualTo(new int[] { 5, 12 });
        }

        @Test
        void testAdjustTokensMode() throws Exception
        {
            var result = adjust(jcas.getCas(), TOKENS, new int[] { 5, 12 });

            assertThat(result).isEqualTo(new int[] { 4, 15 }); // "quick brown"
        }

        @Test
        void testAdjustSingleTokenMode() throws Exception
        {
            var result = adjust(jcas.getCas(), SINGLE_TOKEN, new int[] { 4, 8 });

            assertThat(result).isEqualTo(new int[] { 4, 9 }); // "quick"
        }

        @Test
        void testAdjustSentencesMode() throws Exception
        {
            var result = adjust(jcas.getCas(), SENTENCES, new int[] { 10, 20 });

            assertThat(result).isEqualTo(new int[] { 0, 44 }); // entire sentence
        }
    }

    @Nested
    class AdjustToTokensTests
    {
        @Test
        void testAdjustsToSingleToken() throws Exception
        {
            var result = adjustToTokens(jcas.getCas(), new int[] { 5, 8 });

            assertThat(result).isEqualTo(new int[] { 4, 9 }); // "quick"
        }

        @Test
        void testAdjustsToMultipleTokens() throws Exception
        {
            var result = adjustToTokens(jcas.getCas(), new int[] { 5, 18 });

            assertThat(result).isEqualTo(new int[] { 4, 19 }); // "quick brown fox"
        }

        @Test
        void testAdjustsToExactTokenBoundaries() throws Exception
        {
            var result = adjustToTokens(jcas.getCas(), new int[] { 4, 9 });

            assertThat(result).isEqualTo(new int[] { 4, 9 }); // "quick"
        }

        @Test
        void testThrowsWhenNoTokensFound() throws Exception
        {
            jcas.reset();
            jcas.setDocumentText("test");

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> adjustToTokens(jcas.getCas(), new int[] { 0, 4 })) //
                    .withMessageContaining("No tokens found");
        }
    }

    @Nested
    class AdjustToSingleTokenTests
    {
        @Test
        void testNoNeedToAdjustToExactToken() throws Exception
        {
            var result = adjustToSingleToken(jcas.getCas(), new int[] { 10, 15 });

            assertThat(result).isEqualTo(new int[] { 10, 15 }); // "brown"
        }

        @Test
        void testAdjustsSubtokenToExactToken() throws Exception
        {
            var result = adjustToSingleToken(jcas.getCas(), new int[] { 11, 14 });

            assertThat(result).isEqualTo(new int[] { 10, 15 }); // "brown"
        }

        @Test
        void testThrowsWhenSelectionTooLarge() throws Exception
        {
            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> adjustToSingleToken(jcas.getCas(), new int[] { 5, 18 })) //
                    .withMessageContaining("Annotation must not cover multiple tokens");
        }

        @Test
        void testThrowsWhenNoTokensFound() throws Exception
        {
            jcas.reset();
            jcas.setDocumentText("test");

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> adjustToSingleToken(jcas.getCas(), new int[] { 0, 4 })) //
                    .withMessageContaining("No tokens found");
        }
    }

    @Nested
    class AdjustToSentencesTests
    {
        @Test
        void testAdjustsToSingleSentence() throws Exception
        {
            var result = adjustToSentences(jcas.getCas(), new int[] { 10, 20 });

            assertThat(result).isEqualTo(new int[] { 0, 44 });
        }

        @Test
        void testAdjustsToMultipleSentences() throws Exception
        {
            jcas.reset();
            jcas.setDocumentText("First sentence. Second sentence. Third sentence.");
            addSentence(0, 15); // "First sentence."
            addSentence(16, 32); // "Second sentence."
            addSentence(33, 48); // "Third sentence."

            var result = adjustToSentences(jcas.getCas(), new int[] { 10, 25 });

            assertThat(result).isEqualTo(new int[] { 0, 32 }); // first two sentences
        }

        @Test
        void testAdjustsToExactSentenceBoundaries() throws Exception
        {
            var result = adjustToSentences(jcas.getCas(), new int[] { 0, 44 });

            assertThat(result).isEqualTo(new int[] { 0, 44 });
        }

        @Test
        void testThrowsWhenNoSentencesFound() throws Exception
        {
            jcas.reset();
            jcas.setDocumentText("test");

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> adjustToSentences(jcas.getCas(), new int[] { 0, 4 })) //
                    .withMessageContaining("No sentences found");
        }
    }
}
