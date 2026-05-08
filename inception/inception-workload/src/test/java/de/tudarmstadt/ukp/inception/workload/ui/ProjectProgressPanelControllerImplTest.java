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
package de.tudarmstadt.ukp.inception.workload.ui;

import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.NEW;
import static de.tudarmstadt.ukp.inception.workload.ui.ProjectProgressPanelControllerImpl.backprojectToDaily;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.log.api.EventRepository.DocumentStateSnapshot;

public class ProjectProgressPanelControllerImplTest
{
    private Instant day(int offsetDays)
    {
        return LocalDate.of(2025, 1, 1).plusDays(offsetDays).atStartOfDay().toInstant(UTC);
    }

    private DocumentStateSnapshot snapshot(int offsetDays, Map<SourceDocumentState, Integer> counts)
    {
        return new DocumentStateSnapshot(day(offsetDays), new HashMap<>(counts));
    }

    @Test
    public void testBackprojectToDaily_emptyHistory()
    {
        var result = backprojectToDaily(emptyList(), 30);
        assertThat(result).isEmpty();
    }

    @Test
    public void testBackprojectToDaily_singleSnapshot()
    {
        var single = asList(snapshot(0, Map.of(ANNOTATION_FINISHED, 5)));
        var result = backprojectToDaily(single, 30);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).counts().get(ANNOTATION_FINISHED)).isEqualTo(5);
    }

    @Test
    public void testBackprojectToDaily_nullHistory()
    {
        var result = backprojectToDaily(null, 30);
        assertThat(result).isEmpty();
    }

    @Test
    public void testBackprojectToDaily_consecutiveDays()
    {
        // Already daily snapshots - but still fills full lookback window
        var history = asList( //
                snapshot(0, Map.of(ANNOTATION_FINISHED, 5)), //
                snapshot(1, Map.of(ANNOTATION_FINISHED, 6)), //
                snapshot(2, Map.of(ANNOTATION_FINISHED, 7)));

        var result = backprojectToDaily(history, 30);

        // Should have 31 days (30 days before day 2, plus day 2 itself)
        assertThat(result).hasSize(31);

        // Last 3 days should match original snapshots
        assertThat(result.get(28).counts().get(ANNOTATION_FINISHED)).isEqualTo(5);
        assertThat(result.get(29).counts().get(ANNOTATION_FINISHED)).isEqualTo(6);
        assertThat(result.get(30).counts().get(ANNOTATION_FINISHED)).isEqualTo(7);

        // Earlier days backward-filled from day 0
        assertThat(result.get(0).counts().get(ANNOTATION_FINISHED)).isEqualTo(5);
        assertThat(result.get(27).counts().get(ANNOTATION_FINISHED)).isEqualTo(5);
    }

    @Test
    public void testBackprojectToDaily_sparseSnapshots()
    {
        // Sparse history: day 0, then day 5
        var history = asList( //
                snapshot(0, Map.of(ANNOTATION_FINISHED, 10, NEW, 40)), //
                snapshot(5, Map.of(ANNOTATION_FINISHED, 15, NEW, 35)));

        var result = backprojectToDaily(history, 30);

        // Should have 31 days (30 days before day 5, plus day 5 itself)
        assertThat(result).hasSize(31);

        // Day 0 (index 25): snapshot at day 0
        assertThat(result.get(25).day()).isEqualTo(day(0));
        assertThat(result.get(25).counts().get(ANNOTATION_FINISHED)).isEqualTo(10);
        assertThat(result.get(25).counts().get(NEW)).isEqualTo(40);

        // Days 1-4 (indices 26-29): backward-fill from day 5
        for (int i = 26; i < 30; i++) {
            assertThat(result.get(i).counts().get(ANNOTATION_FINISHED)).isEqualTo(15);
            assertThat(result.get(i).counts().get(NEW)).isEqualTo(35);
        }

        // Day 5 (index 30): snapshot at day 5
        assertThat(result.get(30).day()).isEqualTo(day(5));
        assertThat(result.get(30).counts().get(ANNOTATION_FINISHED)).isEqualTo(15);
        assertThat(result.get(30).counts().get(NEW)).isEqualTo(35);

        // Earlier days (indices 0-24): backward-fill from day 0
        assertThat(result.get(0).counts().get(ANNOTATION_FINISHED)).isEqualTo(10);
        assertThat(result.get(0).counts().get(NEW)).isEqualTo(40);
    }

    @Test
    public void testBackprojectToDaily_multipleGaps()
    {
        // Multiple gaps: day 0, day 3, day 10
        var history = asList( //
                snapshot(0, Map.of(ANNOTATION_FINISHED, 5)), //
                snapshot(3, Map.of(ANNOTATION_FINISHED, 7)), //
                snapshot(10, Map.of(ANNOTATION_FINISHED, 12)));

        var result = backprojectToDaily(history, 30);

        // Should have 31 days (30 days before day 10, plus day 10 itself)
        assertThat(result).hasSize(31);

        // Day 0 (index 20): snapshot at day 0 (5)
        assertThat(result.get(20).counts().get(ANNOTATION_FINISHED)).isEqualTo(5);

        // Days 1-2 (indices 21-22): backward-fill from day 3 (7)
        assertThat(result.get(21).counts().get(ANNOTATION_FINISHED)).isEqualTo(7);
        assertThat(result.get(22).counts().get(ANNOTATION_FINISHED)).isEqualTo(7);

        // Day 3 (index 23): snapshot at day 3 (7)
        assertThat(result.get(23).counts().get(ANNOTATION_FINISHED)).isEqualTo(7);

        // Days 4-9 (indices 24-29): backward-fill from day 10 (12)
        assertThat(result.get(24).counts().get(ANNOTATION_FINISHED)).isEqualTo(12);
        assertThat(result.get(29).counts().get(ANNOTATION_FINISHED)).isEqualTo(12);

        // Day 10 (index 30): snapshot at day 10 (12)
        assertThat(result.get(30).counts().get(ANNOTATION_FINISHED)).isEqualTo(12);

        // Earlier days (indices 0-19): backward-fill from day 0 (5)
        assertThat(result.get(0).counts().get(ANNOTATION_FINISHED)).isEqualTo(5);
        assertThat(result.get(19).counts().get(ANNOTATION_FINISHED)).isEqualTo(5);
    }

    @Test
    public void testBackprojectToDaily_multipleStates()
    {
        // Test with multiple document states
        var history = asList( //
                snapshot(0, Map.of( //
                        ANNOTATION_FINISHED, 10, //
                        CURATION_FINISHED, 5, //
                        NEW, 35)), //
                snapshot(7, Map.of( //
                        ANNOTATION_FINISHED, 20, //
                        CURATION_FINISHED, 15, //
                        NEW, 15)));

        var result = backprojectToDaily(history, 30);

        // Should have 31 days (30 days before day 7, plus day 7 itself)
        assertThat(result).hasSize(31);

        // Day 0 (index 23): snapshot at day 0
        assertThat(result.get(23).counts().get(ANNOTATION_FINISHED)).isEqualTo(10);
        assertThat(result.get(23).counts().get(CURATION_FINISHED)).isEqualTo(5);
        assertThat(result.get(23).counts().get(NEW)).isEqualTo(35);

        // Days 1-6 (indices 24-29): backward-fill from day 7
        for (int i = 24; i < 30; i++) {
            assertThat(result.get(i).counts().get(ANNOTATION_FINISHED)).isEqualTo(20);
            assertThat(result.get(i).counts().get(CURATION_FINISHED)).isEqualTo(15);
            assertThat(result.get(i).counts().get(NEW)).isEqualTo(15);
        }

        // Day 7 (index 30): snapshot at day 7
        assertThat(result.get(30).counts().get(ANNOTATION_FINISHED)).isEqualTo(20);
        assertThat(result.get(30).counts().get(CURATION_FINISHED)).isEqualTo(15);
        assertThat(result.get(30).counts().get(NEW)).isEqualTo(15);

        // Earlier days (indices 0-22): backward-fill from day 0
        assertThat(result.get(0).counts().get(ANNOTATION_FINISHED)).isEqualTo(10);
        assertThat(result.get(0).counts().get(CURATION_FINISHED)).isEqualTo(5);
        assertThat(result.get(0).counts().get(NEW)).isEqualTo(35);
    }

    @Test
    public void testBackprojectToDaily_preservesTotal()
    {
        // Verify that totals are preserved across backprojection
        var history = asList( //
                snapshot(0, Map.of(ANNOTATION_FINISHED, 10, NEW, 40)), //
                snapshot(5, Map.of(ANNOTATION_FINISHED, 15, NEW, 35)));

        var result = backprojectToDaily(history, 30);

        // All 31 days should preserve total of 50
        for (int i = 0; i <= 30; i++) {
            var total = result.get(i).counts().values().stream().mapToInt(Integer::intValue).sum();
            assertThat(total).isEqualTo(50);
        }
    }
}
