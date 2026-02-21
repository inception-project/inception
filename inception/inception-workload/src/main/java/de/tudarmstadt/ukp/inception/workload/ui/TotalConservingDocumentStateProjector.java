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

import static de.tudarmstadt.ukp.inception.workload.ui.RegressionUtils.calculateRegression;
import static java.lang.Math.max;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.emptyList;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.log.api.EventRepository.DocumentStateSnapshot;
import de.tudarmstadt.ukp.inception.workload.ui.RegressionUtils.Point;
import de.tudarmstadt.ukp.inception.workload.ui.RegressionUtils.RegressionResult;

public class TotalConservingDocumentStateProjector
{
    /**
     * Defines the priority order for processing states.
     * <p>
     * We process states in REVERSE order of this list (from CURATION_FINISHED down to ANNOTATION).
     * This ensures that "Finished/Stable" states are calculated and rounded first, keeping their
     * lines smooth. Any rounding noise or overflow is pushed into the "Expendable" states (NEW).
     */
    private static final List<SourceDocumentState> EXPENDABLE_ORDER = List.of(
            SourceDocumentState.NEW, // Most expendable (Buffer)
            SourceDocumentState.ANNOTATION_IN_PROGRESS, //
            SourceDocumentState.ANNOTATION_FINISHED, //
            SourceDocumentState.CURATION_IN_PROGRESS, //
            SourceDocumentState.CURATION_FINISHED // Least expendable (Stable)
    );

    /**
     * Projects future states while ensuring the Total Document Count remains constant.
     *
     * @param aHistory
     *            The full list of historical snapshots (ordered oldest-first).
     * @param aProjectionDurationDays
     *            How far into the future to project (e.g., 30 days).
     * @param aLookbackDurationDays
     *            How far back into history to look for the trend (e.g., consider only last 14
     *            days). Pass null to use the entire history.
     * @return A list of projected snapshots.
     */
    public List<DocumentStateSnapshot> generate(List<DocumentStateSnapshot> aHistory,
            int aProjectionDurationDays, Integer aLookbackDurationDays)
    {
        if (aHistory == null || aHistory.size() < 2) {
            return emptyList();
        }

        var remainderState = SourceDocumentState.NEW;

        // 1. Get Baseline Data from the latest snapshot
        var latestSnapshot = aHistory.get(aHistory.size() - 1);
        var fixedTotalCount = latestSnapshot.counts().values().stream()
                .mapToLong(Integer::longValue).sum();

        var relevantHistory = filterHistory(aHistory, aLookbackDurationDays);
        var baselineDate = relevantHistory.get(0).day();
        var lastKnownDate = latestSnapshot.day();
        var lastKnownCounts = latestSnapshot.counts();

        // 2. Build Models for all states except the remainder (NEW)
        var models = new HashMap<SourceDocumentState, RegressionResult>();
        var allStates = latestSnapshot.counts().keySet();
        for (var state : allStates) {
            if (!state.equals(remainderState)) {
                models.put(state, buildModelForState(relevantHistory, state, baselineDate));
            }
        }

        var projections = new ArrayList<DocumentStateSnapshot>();

        for (var i = 1; i <= aProjectionDurationDays; i++) {
            var futureDate = lastKnownDate.plus(i, DAYS);
            var futureCounts = new HashMap<SourceDocumentState, Integer>();
            long currentSum = 0;

            // 3. PRIORITY CALCULATION LOOP
            // We iterate in REVERSE order (Stable -> Volatile).
            // This ensures Curation/Finished states get "first dibs" on rounding naturally.
            for (int k = EXPENDABLE_ORDER.size() - 1; k >= 0; k--) {
                var state = EXPENDABLE_ORDER.get(k);

                // Skip NEW (it's the calculated remainder)
                if (state.equals(remainderState)) {
                    continue;
                }

                // If we don't have a model for this state (e.g. rarely used state), skip
                if (!models.containsKey(state)) {
                    continue;
                }

                // A. Calculate Projection (Anchored to Last Known Value)
                // We project relative to the current actual count to prevent initial drops/jumps.
                double startValue = lastKnownCounts.getOrDefault(state, 0).doubleValue();
                double slope = models.get(state).slope();
                double rawPrediction = startValue + (slope * i);

                // B. Round NATURALLY (Standard Rounding)
                // This prevents jitter. 45.4 stays 45. 45.6 stays 46.
                // We do NOT use dithering/error-accumulation here to keep lines smooth.
                int count = (int) Math.round(Math.max(0, rawPrediction));

                futureCounts.put(state, count);
                currentSum += count;
            }

            // 4. Calculate Remainder (NEW) as the buffer
            long remainder = fixedTotalCount - currentSum;

            if (remainder < 0) {
                // OVERFLOW: The stable states rounded up too much (or trend is too aggressive).
                // We set NEW to 0, and then shave the excess off the others using Waterfall.
                futureCounts.put(remainderState, 0);

                // Resolve overflow by reducing from expendable states first
                resolveOverflowWaterfall(futureCounts, fixedTotalCount);
            }
            else {
                // UNDERFLOW: All rounding differences and remaining docs go here.
                futureCounts.put(remainderState, (int) remainder);
            }

            projections.add(new DocumentStateSnapshot(futureDate, futureCounts));
        }

        // Filter projections: only include snapshots where counts change day-to-day.
        // Always include the first and last projection in the range.
        if (projections.isEmpty()) {
            return projections;
        }

        var filtered = new ArrayList<DocumentStateSnapshot>();
        filtered.add(projections.get(0));

        for (int i = 1; i < projections.size() - 1; i++) {
            var prev = projections.get(i - 1).counts();
            var curr = projections.get(i).counts();
            if (!countsEqual(prev, curr)) {
                filtered.add(projections.get(i));
            }
        }

        if (projections.size() > 1) {
            filtered.add(projections.get(projections.size() - 1));
        }

        return filtered;
    }

    /**
     * Resolves overflow by reducing counts in a specific priority order (Waterfall), ensuring that
     * upstream states (ANNOTATION) absorb the error before downstream states (CURATION) are
     * touched.
     */
    private void resolveOverflowWaterfall(Map<SourceDocumentState, Integer> aCounts,
            long aTargetTotal)
    {
        long currentTotal = aCounts.values().stream().mapToLong(Integer::longValue).sum();
        long excess = currentTotal - aTargetTotal;

        if (excess <= 0) {
            return;
        }

        // Iterate through states from "Most Expendable" (NEW/ANNOTATION) to "Least Expendable"
        for (SourceDocumentState state : EXPENDABLE_ORDER) {
            if (!aCounts.containsKey(state)) {
                continue;
            }

            int count = aCounts.get(state);
            if (count > 0) {
                int reduction = (int) Math.min(count, excess);
                aCounts.put(state, count - reduction);
                excess -= reduction;
            }
            if (excess == 0) {
                break;
            }
        }

        // Safety Fallback: If EXPENDABLE_ORDER didn't cover all states (e.g. custom states),
        // remove from arbitrary remaining states.
        if (excess > 0) {
            for (var entry : aCounts.entrySet()) {
                int count = entry.getValue();
                if (count > 0) {
                    int reduction = (int) Math.min(count, excess);
                    entry.setValue(count - reduction);
                    excess -= reduction;
                    if (excess == 0) {
                        break;
                    }
                }
            }
        }
    }

    private List<DocumentStateSnapshot> filterHistory(List<DocumentStateSnapshot> aHistory,
            Integer aLookbackDays)
    {
        if (aLookbackDays == null) {
            return aHistory;
        }
        var cutoff = aHistory.get(aHistory.size() - 1).day().minus(aLookbackDays, DAYS);

        var filtered = aHistory.stream() //
                .filter(s -> !s.day().isBefore(cutoff)) //
                .toList();

        return filtered.size() < 2 ? aHistory.subList(max(0, aHistory.size() - 2), aHistory.size())
                : filtered;
    }

    private RegressionResult buildModelForState(List<DocumentStateSnapshot> aHistory,
            SourceDocumentState aState, Instant aBaselineDate)
    {
        var points = new ArrayList<Point>();
        for (var s : aHistory) {
            var x = DAYS.between(aBaselineDate, s.day());
            var y = s.counts().getOrDefault(aState, 0);
            points.add(new Point(x, y));
        }
        return calculateRegression(points);
    }

    private boolean countsEqual(Map<SourceDocumentState, Integer> a,
            Map<SourceDocumentState, Integer> b)
    {
        var keys = new HashSet<SourceDocumentState>();
        keys.addAll(a.keySet());
        keys.addAll(b.keySet());
        for (var k : keys) {
            var va = a.getOrDefault(k, 0);
            var vb = b.getOrDefault(k, 0);
            if (!Objects.equals(va, vb)) {
                return false;
            }
        }
        return true;
    }
}
