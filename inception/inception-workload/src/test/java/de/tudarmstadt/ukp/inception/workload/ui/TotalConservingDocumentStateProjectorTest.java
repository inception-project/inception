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
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.log.api.EventRepository.DocumentStateSnapshot;

public class TotalConservingDocumentStateProjectorTest
{
    private TotalConservingDocumentStateProjector sut;

    @BeforeEach
    void setup()
    {
        sut = new TotalConservingDocumentStateProjector();
    }

    private Instant day(int offsetDays)
    {
        return LocalDate.of(2025, 1, 1).plusDays(offsetDays).atStartOfDay().toInstant(UTC);
    }

    private DocumentStateSnapshot snapshot(int offsetDays, SourceDocumentState state, int count)
    {
        var counts = new HashMap<SourceDocumentState, Integer>();
        counts.put(state, count);
        return new DocumentStateSnapshot(day(offsetDays), counts);
    }

    private DocumentStateSnapshot snapshot(int offsetDays, Map<SourceDocumentState, Integer> counts)
    {
        return new DocumentStateSnapshot(day(offsetDays), new HashMap<>(counts));
    }

    @Test
    public void testInsufficientHistoryReturnsEmpty()
    {
        var result = sut.generate(emptyList(), 5, null);
        assertThat(result).isEmpty();

        var single = asList(snapshot(0, ANNOTATION_FINISHED, 5));
        var result2 = sut.generate(single, 5, null);
        assertThat(result2).isEmpty();
    }

    @Test
    public void testPreservesTotalAcrossProjections()
    {
        // Two-state example where totals are fixed in the latest snapshot
        var history = new ArrayList<DocumentStateSnapshot>();
        history.add(snapshot(0, Map.of( //
                ANNOTATION_FINISHED, 2, //
                CURATION_FINISHED, 1, //
                NEW, 7)));
        history.add(snapshot(1, Map.of( //
                ANNOTATION_FINISHED, 3, //
                CURATION_FINISHED, 2, //
                NEW, 5)));

        // fixed total is taken from latest snapshot -> 3+2+5 = 10
        var projections = sut.generate(history, 3, null);

        assertThat(projections).hasSize(3);
        for (var p : projections) {
            var sum = p.counts().values().stream().mapToInt(Integer::intValue).sum();
            assertThat(sum).isEqualTo(10);
            // remainder state must be non-negative
            assertThat(p.counts().getOrDefault(NEW, 0)).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    public void testScalingWhenProjectedExceedsTotal()
    {
        // Create history that will produce very large projected values for the non-remainder states
        // Latest snapshot total will be 200 (100 + 100)
        var history = new ArrayList<DocumentStateSnapshot>();

        // Day 0: only NEW=100
        history.add(snapshot(0, Map.of( //
                NEW, 100)));

        // Day 1: ANNOTATION_FINISHED=100, CURATION_FINISHED=100 (total=200)
        history.add(snapshot(1, Map.of( //
                ANNOTATION_FINISHED, 100, //
                CURATION_FINISHED, 100)));

        // Project 2 days ahead; models for ANNOTATION/CURATION will predict large values -> should
        // trigger scaling
        var projections = sut.generate(history, 2, null);

        assertThat(projections).hasSize(2);

        var fixedTotal = 200;
        for (var p : projections) {
            var sum = p.counts().values().stream().mapToInt(Integer::intValue).sum();
            assertThat(sum).isEqualTo(fixedTotal);
            // New (remainder) should be clamped to 0 in the over-projection case
            assertThat(p.counts().getOrDefault(NEW, 0)).isEqualTo(0);
        }
    }

    @Test
    public void testFiltersUnchangedProjections()
    {
        var history = new ArrayList<DocumentStateSnapshot>();

        var counts0 = Map.of(ANNOTATION_FINISHED, 3, CURATION_FINISHED, 2, NEW, 5);
        var counts1 = Map.of(ANNOTATION_FINISHED, 3, CURATION_FINISHED, 2, NEW, 5);
        history.add(snapshot(0, counts0));
        history.add(snapshot(1, counts1));

        var projections = sut.generate(history, 7, null);

        // identical daily projections -> only first and last should be present
        assertThat(projections).hasSize(2);
        assertThat(projections.get(0).day()).isEqualTo(day(2));
        assertThat(projections.get(1).day()).isEqualTo(day(8));

        // totals preserved
        for (var p : projections) {
            var sum = p.counts().values().stream().mapToInt(Integer::intValue).sum();
            assertThat(sum).isEqualTo(10);
        }
    }
}
