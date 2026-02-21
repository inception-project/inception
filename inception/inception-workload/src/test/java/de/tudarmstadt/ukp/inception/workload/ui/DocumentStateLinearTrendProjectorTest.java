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
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.log.api.EventRepository.DocumentStateSnapshot;

public class DocumentStateLinearTrendProjectorTest
{
    private LinearDocumentStateProjector sut;

    @BeforeEach
    void setup()
    {
        sut = new LinearDocumentStateProjector();
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

    @Test
    public void testInsufficientHistoryReturnsEmpty()
    {
        var result = sut.generate(emptyList(), 5, null);
        assertTrue(result.isEmpty());

        var single = asList(snapshot(0, ANNOTATION_FINISHED, 5));
        var result2 = sut.generate(single, 5, null);
        assertTrue(result2.isEmpty());
    }

    @Test
    public void testLinearIncreaseProjection()
    {
        // create simple increasing counts for ANNOTATION_FINISHED: 1,2,3 at days 0,1,2
        var history = new ArrayList<DocumentStateSnapshot>();
        history.add(snapshot(0, ANNOTATION_FINISHED, 1));
        history.add(snapshot(1, ANNOTATION_FINISHED, 2));
        history.add(snapshot(2, ANNOTATION_FINISHED, 3));

        var projections = sut.generate(history, 3, null);

        // Expect linear increase by ~1 per day -> projected counts 4,5,6
        assertThat(projections) //
                .extracting(p -> p.counts().get(ANNOTATION_FINISHED)) //
                .containsExactly(4, 5, 6);
    }

    @Test
    public void testLookbackLimitsHistory()
    {
        var history = new ArrayList<DocumentStateSnapshot>();
        // older slow increase
        history.add(snapshot(0, CURATION_FINISHED, 1));
        history.add(snapshot(1, CURATION_FINISHED, 2));
        history.add(snapshot(2, CURATION_FINISHED, 3));
        // recent jump (Day 10)
        history.add(snapshot(10, CURATION_FINISHED, 50));

        // Slope should be calculated between Day 2 (Count 3) and Day 10 (Count 50).
        // Slope = (50 - 3) / (10 - 2) = 47 / 8 = ~5.875 per day.
        var projections = sut.generate(history, 2, 2);

        // We expect values ~56 and ~62 (50 + 5.875, 50 + 11.75)
        // This asserts the logic actually ignored the slow start (Day 0 & 1)
        assertThat(projections) //
                .hasSize(2) //
                .extracting(p -> p.counts().get(CURATION_FINISHED)) //
                .containsExactly(56, 62);
    }

    @Test
    public void testFiltersUnchangedProjections()
    {
        // constant counts -> slope 0 -> projections identical each day
        var history = new ArrayList<DocumentStateSnapshot>();
        history.add(snapshot(0, ANNOTATION_FINISHED, 5));
        history.add(snapshot(1, ANNOTATION_FINISHED, 5));

        var projections = sut.generate(history, 5, null);

        // should emit only first and last projection (day 2 and day 6)
        assertThat(projections).hasSize(2);
        assertThat(projections.get(0).day()).isEqualTo(day(2));
        assertThat(projections.get(1).day()).isEqualTo(day(6));

        assertThat(projections) // counts should be constant
                .extracting(p -> p.counts().get(ANNOTATION_FINISHED)).containsExactly(5, 5);
    }
}
