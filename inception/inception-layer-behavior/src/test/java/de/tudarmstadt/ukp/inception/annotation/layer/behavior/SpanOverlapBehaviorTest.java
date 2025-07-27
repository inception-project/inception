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
import static org.apache.uima.cas.text.AnnotationPredicates.colocated;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.Test;

public class SpanOverlapBehaviorTest
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
