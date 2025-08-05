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
package de.tudarmstadt.ukp.inception.curation.merge.strategy;

import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.internal.AID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanPosition;

class ThresholdBasedMergeStrategyTest
{
    private SpanPosition position = null;

    @Test
    void testWithZeroConfidence()
    {
        var minUsers = 0;
        var minConfidence = 0.0;
        var topRanks = 1;

        {
            var best = makeConfiguration("A", "B");
            var secondBest = makeConfiguration("C");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best, secondBest)) //
                    .containsExactly(best);
        }

        {
            var best = makeConfiguration("A");
            var secondBest = makeConfiguration("B");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best, secondBest)) //
                    .as("A tie is always considered as DISPUTED") //
                    .isEmpty();
        }

        {
            var best = makeConfiguration("A");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best)) //
                    .containsExactly(best);
        }

        {
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP)).isEmpty();
        }
    }

    @Test
    void testWithOneConfidence()
    {
        var minUsers = 0;
        var minConfidence = 1.0;
        var topRanks = 1;

        {
            var best = makeConfiguration("A", "B");
            var secondBest = makeConfiguration("C");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best, secondBest))
                    .as("With confidence threshold 1, and non-unanimouse vote is DISPUTED")
                    .isEmpty();
        }

        {
            var best = makeConfiguration("A");
            var secondBest = makeConfiguration("B");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best, secondBest)) //
                    .as("A tie is always considered as DISPUTED") //
                    .isEmpty();
        }

        {
            var best = makeConfiguration("A");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best)) //
                    .containsExactly(best);
        }

        {
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP)).isEmpty();
        }
    }

    @Test
    void testWithPointFiveConfidence()
    {
        var minUsers = 0;
        var minConfidence = 0.5;
        var topRanks = 1;

        {
            var best = makeConfiguration("A", "B");
            var secondBest = makeConfiguration("C");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best, secondBest)) //
                    .as("Best has twice the votes of second best") //
                    .containsExactly(best);
        }

        {
            var best = makeConfiguration("A", "B", "C");
            var secondBest = makeConfiguration("D", "E");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best, secondBest)) //
                    .as("Best has more than half of the total votes") //
                    .containsExactly(best);
        }

        {
            var best = makeConfiguration("A");
            var secondBest = makeConfiguration("B");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best, secondBest)) //
                    .as("A tie is always considered as DISPUTED") //
                    .isEmpty();
        }

        {
            var best = makeConfiguration("A");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best)) //
                    .containsExactly(best);
        }

        {
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP)).isEmpty();
        }
    }

    @Test
    void testWithPointFiveConfidenceWithMultipleResults()
    {
        var minUsers = 0;
        var minConfidence = 0.4;
        var topRanks = 2;

        {
            var best = makeConfiguration("A", "B");
            var secondBest = makeConfiguration("C");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best, secondBest)) //
                    .as("Second-best does not have at least 50% of the total votes") //
                    .containsExactly(best);
        }

        {
            var best = makeConfiguration("A", "B", "C");
            var secondBest = makeConfiguration("D", "E");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best, secondBest)) //
                    .as("Second-best does have 40% of the total votes") //
                    .containsExactly(best, secondBest);
        }

        {
            var best = makeConfiguration("A", "B", "C");
            var secondBest = makeConfiguration("D", "E");
            assertThat(calculate(minUsers, minConfidence, topRanks, NO_OVERLAP, best, secondBest)) //
                    .as("No stacking allowed") //
                    .containsExactly(best);
        }

        {
            var best = makeConfiguration("A");
            var secondBest = makeConfiguration("B");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best, secondBest)) //
                    .as("A tie is allowed when topRanks is not 1") //
                    .containsExactly(best, secondBest);
        }

        {
            var best = makeConfiguration("A");
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP, best)) //
                    .containsExactly(best);
        }

        {
            assertThat(calculate(minUsers, minConfidence, topRanks, ANY_OVERLAP)).isEmpty();
        }
    }

    private List<Configuration> calculate(int aUserThreshold, double aConfidenceThreshold,
            int aTopRanks, OverlapMode aOverlapMode, Configuration... aConfigurations)
    {
        var cfgSet = new ConfigurationSet(position);
        for (var cfg : aConfigurations) {
            cfgSet.addConfiguration(cfg);
        }

        var layer = new AnnotationLayer();
        layer.setOverlapMode(aOverlapMode);

        var sut = ThresholdBasedMergeStrategy.builder() //
                .withUserThreshold(aUserThreshold) //
                .withConfidenceThreshold(aConfidenceThreshold) //
                .withTopRanks(aTopRanks) //
                .build();
        return sut.chooseConfigurationsToMerge(null, cfgSet, layer);
    }

    private Configuration makeConfiguration(String... aAnnototors)
    {
        var cfg = new Configuration(position);
        for (var annotator : aAnnototors) {
            cfg.add(annotator, new AID(0));
        }
        return cfg;
    }
}
