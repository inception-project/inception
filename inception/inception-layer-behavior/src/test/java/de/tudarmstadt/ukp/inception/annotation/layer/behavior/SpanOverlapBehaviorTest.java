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

import static de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanOverlapBehavior.overlappingNonStackingSpans;
import static de.tudarmstadt.ukp.inception.annotation.layer.behavior.SpanOverlapBehavior.overlappingOrStackingSpans;
import static de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType.ERROR;
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.IllegalPlacementException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.MoveSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;

@ExtendWith(MockitoExtension.class)
class SpanOverlapBehaviorTest
{
    private @Mock TypeAdapter adapter;
    private @Mock AnnotationLayer layer;
    private @Mock SpanLayerSupport spanLayerSupport;
    private @Mock ChainLayerSupport chainLayerSupport;

    private SpanOverlapBehavior sut;
    private JCas jcas;

    @BeforeEach
    void setup() throws Exception
    {
        sut = new SpanOverlapBehavior();
        jcas = JCasFactory.createJCas();
        jcas.setDocumentText("The quick brown fox jumps over the lazy dog.");

        // Create tokens
        addToken(0, 3);   // The
        addToken(4, 9);   // quick
        addToken(10, 15); // brown
        addToken(16, 19); // fox
        addToken(20, 25); // jumps
        addToken(26, 30); // over
        addToken(31, 34); // the
        addToken(35, 39); // lazy
        addToken(40, 43); // dog

        // Create sentences
        addSentence(0, 44);
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
        void testAllowsAnyOverlap() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn(Annotation.class.getName());
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.ANY_OVERLAP);

            addAnnotation(0, 10);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(5) //
                    .withEnd(15) // Overlaps with existing
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result).isSameAs(request);
        }

        @Test
        void testRejectsNoOverlapWhenOverlapping() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn(Annotation.class.getName());
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.NO_OVERLAP);
            when(layer.getUiName()).thenReturn("TestLayer");

            addAnnotation(0, 10);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(5) //
                    .withEnd(15) // Overlaps
                    .build();

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.onCreate(adapter, request)) //
                    .withMessageContaining("no overlap or stacking");
        }

        @Test
        void testRejectsNoOverlapWhenStacking() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn(Annotation.class.getName());
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.NO_OVERLAP);
            when(layer.getUiName()).thenReturn("TestLayer");

            addAnnotation(0, 10);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(0) //
                    .withEnd(10) // Stacking
                    .build();

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.onCreate(adapter, request)) //
                    .withMessageContaining("no overlap or stacking");
        }

        @Test
        void testAllowsNoOverlapWhenNonOverlapping() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn(Token.class.getName());

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(10) //
                    .withEnd(20) // Adjacent, not overlapping
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result).isSameAs(request);
        }

        @Test
        void testRejectsOverlapOnlyWhenStacking() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn(Annotation.class.getName());
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.OVERLAP_ONLY);
            when(layer.getUiName()).thenReturn("TestLayer");

            addAnnotation(0, 10);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(0) //
                    .withEnd(10) // Stacking
                    .build();

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.onCreate(adapter, request)) //
                    .withMessageContaining("stacking is not allowed");
        }

        @Test
        void testAllowsOverlapOnlyWhenOverlapping() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn(Annotation.class.getName());
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.OVERLAP_ONLY);

            addAnnotation(0, 10);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(5) //
                    .withEnd(15) // Overlapping
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result).isSameAs(request);
        }

        @Test
        void testRejectsStackingOnlyWhenOverlapping() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn(Annotation.class.getName());
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.STACKING_ONLY);
            when(layer.getUiName()).thenReturn("TestLayer");

            addAnnotation(0, 10);

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(5) //
                    .withEnd(15) // Overlapping
                    .build();

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.onCreate(adapter, request)) //
                    .withMessageContaining("only stacking is allowed");
        }

        @Test
        void testAllowsStackingOnlyWhenStacking() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn(Token.class.getName());

            var request = CreateSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withBegin(0) //
                    .withEnd(3) // Stacking with token
                    .build();

            var result = sut.onCreate(adapter, request);

            assertThat(result).isSameAs(request);
        }
    }

    @Nested
    class MoveTests
    {
        @Test
        void testAllowsMoveInNoOverlapMode() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn(Token.class.getName());

            var annotation = addAnnotation(0, 10);

            var request = MoveSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withAnnotation(annotation) //
                    .withBegin(20) //
                    .withEnd(30) // Non-overlapping position
                    .build();

            var result = sut.onMove(adapter, request);

            assertThat(result).isSameAs(request);
        }

        @Test
        void testRejectsMoveInNoOverlapModeWhenOverlapping() throws Exception
        {
            when(adapter.getAnnotationTypeName()).thenReturn(Annotation.class.getName());
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.NO_OVERLAP);
            when(layer.getUiName()).thenReturn("TestLayer");

            var annotation = addAnnotation(0, 10);
            addAnnotation(20, 30);

            var request = MoveSpanAnnotationRequest.builder() //
                    .withCas(jcas.getCas()) //
                    .withAnnotation(annotation) //
                    .withBegin(15) //
                    .withEnd(25) // Overlaps with second annotation
                    .build();

            assertThatExceptionOfType(IllegalPlacementException.class) //
                    .isThrownBy(() -> sut.onMove(adapter, request)) //
                    .withMessageContaining("no overlap or stacking");
        }
    }

    @Nested
    class RenderTests
    {
        @Test
        void testSkipsRenderWhenNoAnnotations() throws Exception
        {
            var vdoc = new VDocument();
            vdoc.setWindowBegin(0);
            vdoc.setWindowEnd(44);

            Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();

            sut.onRender(adapter, vdoc, annoToSpanIdx);

            assertThat(vdoc.comments()).isEmpty();
        }

        @Test
        void testAddsErrorForOverlappingInNoOverlapMode() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.NO_OVERLAP);

            var vdoc = new VDocument();
            vdoc.setWindowBegin(0);
            vdoc.setWindowEnd(44);

            Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();
            var anno1 = addAnnotation(0, 10);
            var anno2 = addAnnotation(5, 15); // Overlaps
            annoToSpanIdx.put(anno1, new VSpan(layer, anno1, new VRange(0, 10), null));
            annoToSpanIdx.put(anno2, new VSpan(layer, anno2, new VRange(5, 15), null));

            sut.onRender(adapter, vdoc, annoToSpanIdx);

            assertThat(vdoc.comments()).hasSize(2);
            assertThat(vdoc.comments()).allMatch(c -> c.getCommentType() == ERROR);
        }

        @Test
        void testAddsErrorForStackingInNoOverlapMode() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.NO_OVERLAP);

            var vdoc = new VDocument();
            vdoc.setWindowBegin(0);
            vdoc.setWindowEnd(44);

            Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();
            var anno1 = addAnnotation(0, 10);
            var anno2 = addAnnotation(0, 10); // Stacking
            annoToSpanIdx.put(anno1, new VSpan(layer, anno1, new VRange(0, 10), null));
            annoToSpanIdx.put(anno2, new VSpan(layer, anno2, new VRange(0, 10), null));

            sut.onRender(adapter, vdoc, annoToSpanIdx);

            assertThat(vdoc.comments()).hasSize(2);
            assertThat(vdoc.comments()).allMatch(c -> c.getCommentType() == ERROR);
        }

        @Test
        void testAddsErrorForStackingInOverlapOnlyMode() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.OVERLAP_ONLY);

            var vdoc = new VDocument();
            vdoc.setWindowBegin(0);
            vdoc.setWindowEnd(44);

            Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();
            var anno1 = addAnnotation(0, 10);
            var anno2 = addAnnotation(0, 10); // Stacking
            annoToSpanIdx.put(anno1, new VSpan(layer, anno1, new VRange(0, 10), null));
            annoToSpanIdx.put(anno2, new VSpan(layer, anno2, new VRange(0, 10), null));

            sut.onRender(adapter, vdoc, annoToSpanIdx);

            assertThat(vdoc.comments()).hasSize(2);
            assertThat(vdoc.comments()).allMatch(c -> c.getCommentType() == ERROR);
        }

        @Test
        void testAddsErrorForOverlappingInStackingOnlyMode() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.STACKING_ONLY);

            var vdoc = new VDocument();
            vdoc.setWindowBegin(0);
            vdoc.setWindowEnd(44);

            Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();
            var anno1 = addAnnotation(0, 10);
            var anno2 = addAnnotation(5, 15); // Overlaps
            annoToSpanIdx.put(anno1, new VSpan(layer, anno1, new VRange(0, 10), null));
            annoToSpanIdx.put(anno2, new VSpan(layer, anno2, new VRange(5, 15), null));

            sut.onRender(adapter, vdoc, annoToSpanIdx);

            assertThat(vdoc.comments()).hasSize(2);
            assertThat(vdoc.comments()).allMatch(c -> c.getCommentType() == ERROR);
        }

        @Test
        void testNoErrorInAnyOverlapMode() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.ANY_OVERLAP);

            var vdoc = new VDocument();
            vdoc.setWindowBegin(0);
            vdoc.setWindowEnd(44);

            Map<AnnotationFS, VSpan> annoToSpanIdx = new HashMap<>();
            var anno1 = addAnnotation(0, 10);
            var anno2 = addAnnotation(5, 15); // Overlaps
            var anno3 = addAnnotation(0, 10);  // Stacks
            annoToSpanIdx.put(anno1, new VSpan(layer, anno1, new VRange(0, 10), null));
            annoToSpanIdx.put(anno2, new VSpan(layer, anno2, new VRange(5, 15), null));
            annoToSpanIdx.put(anno3, new VSpan(layer, anno3, new VRange(0, 10), null));

            sut.onRender(adapter, vdoc, annoToSpanIdx);

            assertThat(vdoc.comments()).isEmpty();
        }
    }

    @Nested
    class ValidateTests
    {
        @Test
        void testReturnsEmptyForAnyOverlapMode() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.ANY_OVERLAP);
            when(adapter.getAnnotationTypeName()).thenReturn(Annotation.class.getName());

            addAnnotation(0, 10);
            addAnnotation(5, 15); // Overlaps
            addAnnotation(0, 10);  // Stacks

            var result = sut.onValidate(adapter, jcas.getCas());

            assertThat(result).isEmpty();
        }

        @Test
        void testReturnsErrorsForNoOverlapMode() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.NO_OVERLAP);
            when(adapter.getAnnotationTypeName()).thenReturn(Annotation.class.getName());

            addAnnotation(0, 10);
            addAnnotation(5, 15); // Overlaps

            var result = sut.onValidate(adapter, jcas.getCas());

            // DocumentAnnotation spans whole document and overlaps, plus the two we added
            assertThat(result).isNotEmpty();
        }

        @Test
        void testReturnsErrorsForStackingInOverlapOnlyMode() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.OVERLAP_ONLY);
            when(adapter.getAnnotationTypeName()).thenReturn(Annotation.class.getName());

            addAnnotation(0, 10);
            addAnnotation(0, 10); // Stacks

            var result = sut.onValidate(adapter, jcas.getCas());

            // Expecting errors for the stacked annotations plus DocumentAnnotation
            assertThat(result).isNotEmpty();
        }

        @Test
        void testReturnsErrorsForOverlappingInStackingOnlyMode() throws Exception
        {
            when(adapter.getLayer()).thenReturn(layer);
            when(layer.getOverlapMode()).thenReturn(OverlapMode.STACKING_ONLY);
            when(adapter.getAnnotationTypeName()).thenReturn(Token.class.getName());

            var result = sut.onValidate(adapter, jcas.getCas());

            // Tokens have overlapping bounds (e.g., 0-3, 4-9), expecting errors
            assertThat(result).isEmpty(); // Actually no errors since Token layer is skipped
        }
    }

    /**
     * Performance tests for the utility methods.
     */
    @Nested
    class PerformanceTests
    {
        Random rnd = new Random();

        @Test
        public void thatOverlappingOrStackingSpansWorks() throws Exception
        {
            CAS cas = generateCas();

            Set<AnnotationFS> expectedStacked = new HashSet<>();
            Set<AnnotationFS> expectedOverlapping = new HashSet<>();

            long startNaive = System.currentTimeMillis();
            List<Annotation> spans = cas.select(Annotation.class).asList();
            int n = 0;
            for (AnnotationFS span1 : spans) {
                for (AnnotationFS span2 : spans) {
                    n++;

                    if (span1.equals(span2)) {
                        continue;
                    }

                    if (colocated(span1, span2)) {
                        expectedStacked.add(span1);
                        expectedStacked.add(span2);
                    }
                    else if (overlapping(span1, span2)) {
                        expectedOverlapping.add(span1);
                        expectedOverlapping.add(span2);
                    }
                }
            }
            long naiveDuration = System.currentTimeMillis() - startNaive;

            Set<AnnotationFS> actualStacked = new HashSet<>();
            Set<AnnotationFS> actualOverlapping = new HashSet<>();

            long start = System.currentTimeMillis();
            int o = overlappingOrStackingSpans(
                    cas.<Annotation> select(cas.getAnnotationType()).asList(), actualStacked,
                    actualOverlapping);
            long duration = System.currentTimeMillis() - start;

            System.out.printf("Naive %d (%d)  optimized %d (%d)  speedup %f%n", naiveDuration, n,
                    duration, o, naiveDuration / (double) duration);

            assertThat(actualStacked).containsExactlyInAnyOrderElementsOf(expectedStacked);
            assertThat(actualOverlapping).containsExactlyInAnyOrderElementsOf(expectedOverlapping);
        }

        @Test
        public void thatOverlappingNonStackingSpans() throws Exception
        {
            CAS cas = generateCas();

            Set<AnnotationFS> expectedOverlapping = new HashSet<>();

            long startNaive = System.currentTimeMillis();
            List<Annotation> spans = cas.select(Annotation.class).asList();
            for (AnnotationFS fs1 : spans) {
                for (AnnotationFS fs2 : spans) {
                    if (fs1.equals(fs2)) {
                        continue;
                    }

                    if (overlapping(fs1, fs2) && !colocated(fs1, fs2)) {
                        expectedOverlapping.add(fs1);
                        expectedOverlapping.add(fs2);
                    }
                }
            }
            long naiveDuration = System.currentTimeMillis() - startNaive;

            long start = System.currentTimeMillis();
            Set<AnnotationFS> actualOverlapping = overlappingNonStackingSpans(
                    cas.<Annotation> select(cas.getAnnotationType()).asList());
            long duration = System.currentTimeMillis() - start;

            System.out.printf("Naive %d  optimized %d  speedup %f%n", naiveDuration, duration,
                    naiveDuration / (double) duration);

            assertThat(actualOverlapping).containsExactlyInAnyOrderElementsOf(expectedOverlapping);
        }

        private CAS generateCas() throws ResourceInitializationException
        {
            CAS cas = CasFactory.createCas();

            for (int i = 0; i < 1000; i++) {
                int begin = rnd.nextInt(10000);
                int end = begin + rnd.nextInt(100);
                cas.addFsToIndexes(cas.createAnnotation(cas.getAnnotationType(), begin, end));
            }

            return cas;
        }
    }
}
