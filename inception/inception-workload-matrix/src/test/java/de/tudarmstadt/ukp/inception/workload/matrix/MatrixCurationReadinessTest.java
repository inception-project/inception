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
package de.tudarmstadt.ukp.inception.workload.matrix;

import static de.tudarmstadt.ukp.inception.workload.matrix.MatrixCurationReadiness.isReadyForCuration;
import static de.tudarmstadt.ukp.inception.workload.matrix.MatrixCurationReadiness.warning;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The static-assignment (matrix) readiness rule: every annotator must have finished or locked the
 * document, and at least one must actually have finished it.
 */
class MatrixCurationReadinessTest
{
    @Nested
    @DisplayName("All annotators must finish or lock the document")
    class Readiness
    {
        @ParameterizedTest(name = "finished={0}, ignored={1}, annotators={2} -> ready")
        @CsvSource({ //
                "1, 0, 1", // the only annotator finished
                "3, 0, 3", // everyone finished
                "2, 1, 3", // some finished, the rest locked
                "1, 2, 3", // one finished, the rest locked
        })
        void readyWhenEveryAnnotatorAccountedForAndSomebodyFinished(long aFinished, long aIgnored,
                long aAnnotators)
        {
            assertThat(isReadyForCuration(aFinished, aIgnored, aAnnotators)).isTrue();
        }

        @ParameterizedTest(name = "finished={0}, ignored={1}, annotators={2} -> not ready")
        @CsvSource({ //
                "0, 0, 1", // nobody did anything
                "1, 0, 3", // only one of three finished
                "2, 0, 3", // still one annotator outstanding
                "0, 2, 3", // two locked, one outstanding, nobody finished
        })
        void notReadyWhileAnnotationIsOutstanding(long aFinished, long aIgnored, long aAnnotators)
        {
            assertThat(isReadyForCuration(aFinished, aIgnored, aAnnotators)).isFalse();
        }

        @Test
        @DisplayName("a document every annotator merely locked is not ready - there is no data")
        void notReadyWhenAllAnnotatorsOnlyLockedTheDocument()
        {
            // The counts satisfy the threshold (0 + 3 >= 3), but locking without ever opening the
            // document leaves no CAS to initialize the curation CAS from.
            assertThat(isReadyForCuration(0, 3, 3)).isFalse();
        }

        @ParameterizedTest(name = "annotators={0}")
        @ValueSource(longs = { 0, 1, 3 })
        @DisplayName("a project without any finished annotation is never ready")
        void notReadyWithoutAnyFinishedAnnotation(long aAnnotators)
        {
            assertThat(isReadyForCuration(0, 0, aAnnotators)).isFalse();
        }

        @Test
        @DisplayName("zero annotators must not make every document trivially ready")
        void notReadyWhenProjectHasNoAnnotators()
        {
            // Reachable by importing a project without its users/permissions. The counts are scoped
            // to the current annotators, so they are all zero and would satisfy the threshold.
            assertThat(isReadyForCuration(0, 0, 0)).isFalse();
        }

        @Test
        @DisplayName("data from former annotators still counts")
        void readyWhenFinishedCountExceedsAnnotatorCount()
        {
            // Annotators that left the project keep their data, so the finished count can exceed
            // the
            // number of annotators currently holding the role. The extension does not feed the rule
            // such counts today, but the rule itself has to hold for them.
            assertThat(isReadyForCuration(2, 0, 1)).isTrue();
            assertThat(isReadyForCuration(1, 0, 0)).isTrue();
        }
    }

    @Nested
    @DisplayName("Warnings describe the shortfall consistently with the predicate")
    class Warnings
    {
        @Test
        void warningReportsTheNumberOfOutstandingAnnotators()
        {
            var w = warning(1, 0, 3);

            assertThat(w.missingAnnotatorCount()).isEqualTo(2);
            assertThat(w.requiredAnnotatorCount()).isEqualTo(3);
            assertThat(w.message()).contains("2 of the 3");
            assertThat(w.shortLabel()).isEqualTo("2/3");
        }

        @Test
        @DisplayName("nobody finished means every annotator is outstanding")
        void warningsReportTheWholeHeadcountWhenNothingIsFinished()
        {
            assertThat(isReadyForCuration(0, 3, 3)).isFalse();
            assertThat(warning(0, 3, 3).shortLabel()).isEqualTo("3/3");
        }

        @Test
        @DisplayName("a project without annotators reports 0/0 rather than inventing one")
        void warningsDoNotInventAnnotatorsForAnEmptyProject()
        {
            // Reachable by removing every annotator after the document was started. The explanation
            // lives in the message, not in the numbers.
            assertThat(isReadyForCuration(0, 0, 0)).isFalse();
            assertThat(warning(0, 0, 0).shortLabel()).isEqualTo("0/0");
        }

        @Test
        @DisplayName("a warning never reports a negative shortfall")
        void warningsNeverReportNegativeMissingCounts()
        {
            // More annotators finished than the project currently has. The extension scopes both
            // counts to the same population, so it cannot produce this today.
            assertThat(warning(2, 0, 1).missingAnnotatorCount()).isNotNegative();
        }

        @Test
        @DisplayName("a warning never claims that no annotation data exists")
        void nothingFinishedWarningDoesNotClaimThereIsNoData()
        {
            // The finished count only covers users currently holding the annotator role, so a
            // document finished exclusively by annotators that have since left reads as 0 finished
            // while curation opens it with their data (CurationDocumentService#listCuratableUsers).
            assertThat(warning(0, 0, 2).message()) //
                    .doesNotContain("there is no annotation data") //
                    .doesNotContain("No annotator has marked");
        }

        @Test
        @DisplayName("the wording names locks and former annotators as possible causes")
        void nothingFinishedWarningNamesTheMatrixSpecificCauses()
        {
            assertThat(warning(0, 0, 3).message()) //
                    .contains("3 annotators") //
                    .contains("curating incomplete data");
        }

        @Test
        @DisplayName("a project without annotators gets its own explanation")
        void nothingFinishedWarningExplainsAnEmptyProject()
        {
            assertThat(warning(0, 0, 0).message()) //
                    .contains("no annotators") //
                    .doesNotContain("none of the 0");
        }
    }
}
