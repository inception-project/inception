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
package de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.Configuration;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.internal.AID;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.span.SpanPosition;

class ThresholdBasedMergeStrategyTest
{
    private SpanPosition position = null;

    @Test
    void testWithZeroConfidence()
    {
        var minUsers = 0;
        var minConfidence = 0.0;

        {
            var best = makeConfiguration("A", "B");
            var secondBest = makeConfiguration("C");
            assertThat(calculate(minUsers, minConfidence, best, secondBest)).containsSame(best);
        }

        {
            var best = makeConfiguration("A");
            var secondBest = makeConfiguration("B");
            assertThat(calculate(minUsers, minConfidence, best, secondBest)) //
                    .as("A tie is always considered as DISPUTED") //
                    .isEmpty();
        }

        {
            var best = makeConfiguration("A");
            assertThat(calculate(minUsers, minConfidence, best)).containsSame(best);
        }

        {
            assertThat(calculate(minUsers, minConfidence)).isEmpty();
        }
    }

    @Test
    void testWithOneConfidence()
    {
        var minUsers = 0;
        var minConfidence = 1.0;

        {
            var best = makeConfiguration("A", "B");
            var secondBest = makeConfiguration("C");
            assertThat(calculate(minUsers, minConfidence, best, secondBest))
                    .as("With confidence threshold 1, and non-unanimouse vote is DISPUTED")
                    .isEmpty();
        }

        {
            var best = makeConfiguration("A");
            var secondBest = makeConfiguration("B");
            assertThat(calculate(minUsers, minConfidence, best, secondBest)) //
                    .as("A tie is always considered as DISPUTED") //
                    .isEmpty();
        }

        {
            var best = makeConfiguration("A");
            assertThat(calculate(minUsers, minConfidence, best)).containsSame(best);
        }

        {
            assertThat(calculate(minUsers, minConfidence)).isEmpty();
        }
    }

    @Test
    void testWithPointFiveConfidence()
    {
        var minUsers = 0;
        var minConfidence = 0.5;

        {
            var best = makeConfiguration("A", "B");
            var secondBest = makeConfiguration("C");
            assertThat(calculate(minUsers, minConfidence, best, secondBest)) //
                    .as("Best has twice the votes of second best") //
                    .containsSame(best);
        }

        {
            var best = makeConfiguration("A", "B", "C");
            var secondBest = makeConfiguration("D", "E");
            assertThat(calculate(minUsers, minConfidence, best, secondBest)) //
                    .as("Best has less than twice the votes of second best") //
                    .isEmpty();
        }

        {
            var best = makeConfiguration("A");
            var secondBest = makeConfiguration("B");
            assertThat(calculate(minUsers, minConfidence, best, secondBest)) //
                    .as("A tie is always considered as DISPUTED") //
                    .isEmpty();
        }

        {
            var best = makeConfiguration("A");
            assertThat(calculate(minUsers, minConfidence, best)).containsSame(best);
        }

        {
            assertThat(calculate(minUsers, minConfidence)).isEmpty();
        }
    }

    private Optional<Configuration> calculate(int aUserThreshold, double aConfidenceThreshold,
            Configuration... aConfigurations)
    {
        ConfigurationSet cfgSet = new ConfigurationSet(position);
        for (Configuration cfg : aConfigurations) {
            cfgSet.addConfiguration(cfg);
        }

        ThresholdBasedMergeStrategy sut = new ThresholdBasedMergeStrategy(aUserThreshold,
                aConfidenceThreshold);
        return sut.chooseConfigurationToMerge(null, cfgSet);
    }

    private Configuration makeConfiguration(String... aAnnototors)
    {
        Configuration cfg = new Configuration(position);
        for (String annotator : aAnnototors) {
            cfg.add(annotator, new AID(0));
        }
        return cfg;
    }
}
