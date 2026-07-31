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
package de.tudarmstadt.ukp.inception.workload.dynamic;

import static de.tudarmstadt.ukp.inception.workload.dynamic.DynamicCurationReadiness.isReadyForCuration;
import static de.tudarmstadt.ukp.inception.workload.dynamic.DynamicCurationReadiness.warning;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The dynamic-assignment readiness rule: <i>enough</i> annotators must have finished the document -
 * not all of them - and at least one must actually have finished it.
 */
class DynamicCurationReadinessTest
{
    @Nested
    @DisplayName("Enough annotators must finish the document")
    class Readiness
    {
        @ParameterizedTest(name = "finished={0}, required={1} -> ready")
        @CsvSource({ //
                "1, 1", // exactly enough
                "3, 3", // exactly enough
                "4, 3", // more than enough
        })
        void readyWhenEnoughAnnotatorsFinished(long aFinished, long aRequired)
        {
            assertThat(isReadyForCuration(aFinished, aRequired)).isTrue();
        }

        @ParameterizedTest(name = "finished={0}, required={1} -> not ready")
        @CsvSource({ //
                "0, 1", // nobody finished
                "1, 3", // not enough yet
                "2, 3", // one short
        })
        void notReadyWhileTooFewAnnotatorsFinished(long aFinished, long aRequired)
        {
            assertThat(isReadyForCuration(aFinished, aRequired)).isFalse();
        }

        @Test
        @DisplayName("requiring zero annotators must not make every document trivially ready")
        void notReadyWhenNothingFinishedAndNothingRequired()
        {
            assertThat(isReadyForCuration(0, 0)).isFalse();
            assertThat(isReadyForCuration(1, 0)).isTrue();
        }
    }

    @Nested
    @DisplayName("Warnings describe the shortfall consistently with the predicate")
    class Warnings
    {
        @Test
        void warningReportsTheNumberOfOutstandingAnnotators()
        {
            var w = warning(1, 3);

            assertThat(w.missingAnnotatorCount()).isEqualTo(2);
            assertThat(w.requiredAnnotatorCount()).isEqualTo(3);
            assertThat(w.message()).contains("2 of the 3");
            assertThat(w.shortLabel()).isEqualTo("2/3");
        }

        @Test
        @DisplayName("nobody finished means the whole required count is outstanding")
        void warningsReportTheWholeRequirementWhenNothingIsFinished()
        {
            assertThat(isReadyForCuration(0, 3)).isFalse();
            assertThat(warning(0, 3).shortLabel()).isEqualTo("3/3");
        }

        @Test
        @DisplayName("a project requiring nobody reports 0/0 rather than inventing an annotator")
        void warningsDoNotInventAnnotatorsWhenNoneAreRequired()
        {
            // The settings form enforces a minimum of 1, but imported or hand-edited traits can
            // still carry a zero.
            assertThat(isReadyForCuration(0, 0)).isFalse();
            assertThat(warning(0, 0).shortLabel()).isEqualTo("0/0");
        }

        @Test
        @DisplayName("a partial shortfall is reported as the plain difference")
        void warningsReportThePlainDifference()
        {
            assertThat(isReadyForCuration(1, 3)).isFalse();
            assertThat(warning(1, 3).missingAnnotatorCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("a warning never claims that no annotation data exists")
        void nothingFinishedWarningDoesNotClaimThereIsNoData()
        {
            assertThat(warning(0, 2).message()).doesNotContain("there is no annotation data");
        }

        @Test
        @DisplayName("the wording names the required count rather than a headcount")
        void nothingFinishedWarningNamesTheRequiredCount()
        {
            assertThat(warning(0, 2).message()) //
                    .contains("2 are required") //
                    .contains("curating incomplete data");
        }

        @Test
        @DisplayName("a project requiring no annotators gets its own explanation")
        void nothingFinishedWarningExplainsAZeroThreshold()
        {
            assertThat(warning(0, 0).message()) //
                    .contains("requires no annotator") //
                    .doesNotContain("0 are required");
        }
    }
}
