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

import static de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType.ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.MultipleSentenceCoveredException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.MoveSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;

@ExtendWith(MockitoExtension.class)
class SpanCrossSentenceBehaviorTest
{
    private @Mock TypeAdapter adapter;
    private @Mock AnnotationLayer layer;
    private @Mock SpanLayerSupport spanLayerSupport;
    private @Mock ChainLayerSupport chainLayerSupport;

    private SpanCrossSentenceBehavior sut;
    private JCas jcas;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new SpanCrossSentenceBehavior();
        jcas = JCasFactory.createJCas();
        jcas.setDocumentText("First sentence. Second sentence. Third sentence.");

        // First sentence: 0-15
        addSentence(0, 15);
        addToken(0, 5); // First
        addToken(6, 14); // sentence

        // Second sentence: 16-32
        addSentence(16, 32);
        addToken(16, 22); // Second
        addToken(23, 31); // sentence

        // Third sentence: 33-48
        addSentence(33, 48);
        addToken(33, 38); // Third
        addToken(39, 47); // sentence
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

    private Annotation addAnnotation(int aBegin, int aEnd)
    {
        Annotation anno = new Annotation(jcas, aBegin, aEnd);
        anno.addToIndexes();
        return anno;
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
                    .withEnd(48) //
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
                    .withEnd(48) //
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result).isSameAs(request);
        }

        @Test
        void testAllowsCrossSentenceWhenEnabled() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(true);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(0) //
                    .withEnd(48) // Spans all three sentences
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result).isSameAs(request);
        }

        @Test
        void testAllowsWithinSingleSentence() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(0) //
                    .withEnd(15) // Within first sentence
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result).isSameAs(request);
        }

        @Test
        void testRejectsCrossSentenceWhenDisabled() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(0) //
                    .withEnd(32) // Spans first and second sentences
                    .build();

            assertThatExceptionOfType(MultipleSentenceCoveredException.class) //
                    .isThrownBy(() -> sut.onCreate(adapter, request)) //
                    .withMessageContaining("multiple sentences");
        }

        @Test
        void testRejectsCrossSentenceBoundary() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(10) //
                    .withEnd(20) // Crosses from first to second sentence
                    .build();

            assertThatExceptionOfType(MultipleSentenceCoveredException.class) //
                    .isThrownBy(() -> sut.onCreate(adapter, request)) //
                    .withMessageContaining("multiple sentences");
        }
    }

    @Nested
    class MoveTests
    {
        @Test
        void testAllowsMoveWithinSameSentence() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);

            var request = MoveSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(16) //
                    .withEnd(32) // Within second sentence
                    .build();

            var result = sut.onMove(adapter, request);

            assertThat(result).isSameAs(request);
        }

        @Test
        void testRejectsMoveCrossSentence() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);

            var request = MoveSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(10) //
                    .withEnd(25) // Crosses sentence boundaries
                    .build();

            assertThatExceptionOfType(MultipleSentenceCoveredException.class) //
                    .isThrownBy(() -> sut.onMove(adapter, request)) //
                    .withMessageContaining("multiple sentences");
        }

        @Test
        void testAllowsMoveCrossSentenceWhenEnabled() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn("custom.Type");
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(true);

            var request = MoveSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(10) //
                    .withEnd(25) // Crosses sentence boundaries
                    .build();

            var result = sut.onMove(adapter, request);

            assertThat(result).isSameAs(request);
        }
    }

    @Nested
    class RenderTests
    {
        @Test
        void testSkipsRenderWhenCrossSentenceEnabled() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(true);

            var vdoc = new VDocument();
            vdoc.setWindowBegin(0);
            vdoc.setWindowEnd(48);

            Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();
            var anno = addAnnotation(0, 32);
            annoToSpanIdx.put(anno, new VSpan(layer, anno, new VRange(0, 32), null));

            sut.onRender(adapter, vdoc, annoToSpanIdx);

            // No comments should be added
            assertThat(vdoc.comments()).isEmpty();
        }

        @Test
        void testSkipsRenderWhenNoAnnotations() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);

            var vdoc = new VDocument();
            vdoc.setWindowBegin(0);
            vdoc.setWindowEnd(48);

            Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();

            sut.onRender(adapter, vdoc, annoToSpanIdx);

            // No comments should be added
            assertThat(vdoc.comments()).isEmpty();
        }

        @Test
        void testAddsErrorForCrossSentenceAnnotation() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);

            var vdoc = new VDocument();
            vdoc.setWindowBegin(0);
            vdoc.setWindowEnd(48);

            Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();
            var anno = addAnnotation(0, 32); // Crosses sentences
            annoToSpanIdx.put(anno, new VSpan(layer, anno, new VRange(0, 32), null));

            sut.onRender(adapter, vdoc, annoToSpanIdx);

            assertThat(vdoc.comments()).hasSize(1);
            var commentsArray = vdoc.comments().toArray(new VComment[0]);
            assertThat(commentsArray[0].getCommentType()).isEqualTo(ERROR);
            assertThat(commentsArray[0].getComment()).contains("Crossing sentence boundaries");
        }

        @Test
        void testNoErrorForWithinSentenceAnnotation() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);

            var vdoc = new VDocument();
            vdoc.setWindowBegin(0);
            vdoc.setWindowEnd(48);

            Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();
            var anno = addAnnotation(0, 15); // Within first sentence
            annoToSpanIdx.put(anno, new VSpan(layer, anno, new VRange(0, 15), null));

            sut.onRender(adapter, vdoc, annoToSpanIdx);

            assertThat(vdoc.comments()).isEmpty();
        }

        @Test
        void testHandlesMultipleAnnotations() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);

            var vdoc = new VDocument();
            vdoc.setWindowBegin(0);
            vdoc.setWindowEnd(48);

            Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();
            var anno1 = addAnnotation(0, 15); // Within sentence 1
            var anno2 = addAnnotation(0, 32); // Crosses sentences
            var anno3 = addAnnotation(16, 32); // Within sentence 2
            annoToSpanIdx.put(anno1, new VSpan(layer, anno1, new VRange(0, 15), null));
            annoToSpanIdx.put(anno2, new VSpan(layer, anno2, new VRange(0, 32), null));
            annoToSpanIdx.put(anno3, new VSpan(layer, anno3, new VRange(16, 32), null));

            sut.onRender(adapter, vdoc, annoToSpanIdx);

            // Only anno2 should have an error
            assertThat(vdoc.comments()).hasSize(1);
        }
    }

    @Nested
    class ValidateTests
    {
        @Test
        void testSkipsValidationWhenCrossSentenceEnabled() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(true);

            var result = sut.onValidate(adapter, jcas.getCas());

            assertThat(result).isEmpty();
        }

        @Test
        void testReturnsEmptyWhenNoAnnotations() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);
            when(adapter.getAnnotationTypeName()).thenReturn(Token.class.getName());

            var result = sut.onValidate(adapter, jcas.getCas());

            assertThat(result).isEmpty();
        }

        @Test
        void testReturnsErrorForCrossSentenceAnnotation() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);
            when(adapter.getAnnotationTypeName()).thenReturn(Annotation.class.getName());

            addAnnotation(0, 32); // Crosses sentences

            var result = sut.onValidate(adapter, jcas.getCas());

            // Expecting 2 errors: DocumentAnnotation (0-48) and our annotation (0-32)
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(
                    pair -> pair.getLeft().getMessage().contains("Crossing sentence boundaries"));
        }

        @Test
        void testReturnsNoErrorForWithinSentenceAnnotation() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);
            when(adapter.getAnnotationTypeName()).thenReturn(Token.class.getName());

            // Token annotations are within sentences by construction
            var result = sut.onValidate(adapter, jcas.getCas());

            assertThat(result).isEmpty();
        }

        @Test
        void testHandlesMixedAnnotations() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);
            when(adapter.getAnnotationTypeName()).thenReturn(Annotation.class.getName());

            addAnnotation(0, 15); // Within sentence 1 - OK
            addAnnotation(0, 32); // Crosses sentences - ERROR
            addAnnotation(16, 32); // Within sentence 2 - OK
            addAnnotation(10, 25); // Crosses sentences - ERROR

            var result = sut.onValidate(adapter, jcas.getCas());

            // Expecting 3 errors: DocumentAnnotation (0-48), and 2 crossing annotations
            assertThat(result).hasSize(3);
            assertThat(result).allMatch(
                    pair -> pair.getLeft().getMessage().contains("Crossing sentence boundaries"));
        }

        @Test
        void testHandlesAnnotationOutsideSentences() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.isCrossSentence()).thenReturn(false);
            when(adapter.getAnnotationTypeName()).thenReturn(Annotation.class.getName());

            jcas.reset();
            jcas.setDocumentText("No sentence annotations here.");
            addAnnotation(0, 10);

            var result = sut.onValidate(adapter, jcas.getCas());

            // Expecting 2: DocumentAnnotation and our annotation
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(pair -> pair.getLeft().getMessage()
                    .contains("Unable to determine any sentences"));
        }
    }
}
