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
import static java.lang.Math.round;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.log.api.EventRepository.DocumentStateSnapshot;
import de.tudarmstadt.ukp.inception.workload.ui.RegressionUtils.Point;
import de.tudarmstadt.ukp.inception.workload.ui.RegressionUtils.RegressionResult;

public class LinearDocumentStateProjector
{
    /**
     * Projects future document states based on a linear regression of recent history.
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

        // Filter History based on Lookback
        var lastKnownDate = aHistory.get(aHistory.size() - 1).day();
        List<DocumentStateSnapshot> relevantHistory;

        if (aLookbackDurationDays != null) {
            var cutoffDate = lastKnownDate.minus(aLookbackDurationDays, DAYS);
            relevantHistory = aHistory.stream() //
                    .filter(s -> !s.day().isBefore(cutoffDate)) //
                    .toList();

            // Edge case: If lookback is too short and filters everything out,
            // fallback to the last 2 snapshots or full history to avoid crash.
            if (relevantHistory.size() < 2) {
                relevantHistory = aHistory.size() >= 2
                        ? aHistory.subList(aHistory.size() - 2, aHistory.size())
                        : aHistory;
            }
        }
        else {
            relevantHistory = aHistory;
        }

        // Prepare Data for Regression (Group by State)
        var stateData = new HashMap<SourceDocumentState, List<Point>>();
        var baselineDate = relevantHistory.get(0).day();

        for (var snapshot : relevantHistory) {
            double x = DAYS.between(baselineDate, snapshot.day());
            snapshot.counts().forEach((state, count) -> {
                stateData.computeIfAbsent(state, k -> new ArrayList<>()) //
                        .add(new Point(x, count));
            });
        }

        // Calculate Regression Parameters (Slope & Intercept) per State
        var models = new HashMap<SourceDocumentState, RegressionResult>();
        for (var entry : stateData.entrySet()) {
            models.put(entry.getKey(), calculateRegression(entry.getValue()));
        }

        // Generate Projections
        var rawProjections = new ArrayList<DocumentStateSnapshot>();

        // Calculate 'x' relative to the baselineDate
        var daysFromBaselineToLast = DAYS.between(baselineDate, lastKnownDate);

        for (var i = 1; i <= aProjectionDurationDays; i++) {
            var futureDate = lastKnownDate.plus(i, DAYS);
            var futureX = daysFromBaselineToLast + i;

            var futureCounts = new HashMap<SourceDocumentState, Integer>();

            for (var entry : models.entrySet()) {
                var state = entry.getKey();
                var model = entry.getValue();

                var predictedY = (model.slope() * futureX) + model.intercept();

                // Clamp to 0 to ensure we don't return negative document counts
                var result = (int) max(0, round(predictedY));
                futureCounts.put(state, result);
            }

            rawProjections.add(new DocumentStateSnapshot(futureDate, futureCounts));
        }

        // Filter projections: only include snapshots where counts change day-to-day.
        // Always include the first and last projection in the range.
        if (rawProjections.isEmpty()) {
            return rawProjections;
        }

        var projections = new ArrayList<DocumentStateSnapshot>();
        projections.add(rawProjections.get(0));

        for (int i = 1; i < rawProjections.size() - 1; i++) {
            var prev = rawProjections.get(i - 1).counts();
            var curr = rawProjections.get(i).counts();
            if (!countsEqual(prev, curr)) {
                projections.add(rawProjections.get(i));
            }
        }

        // ensure last is present
        if (rawProjections.size() > 1) {
            projections.add(rawProjections.get(rawProjections.size() - 1));
        }

        return projections;
    }

    private boolean countsEqual(java.util.Map<SourceDocumentState, Integer> a,
            java.util.Map<SourceDocumentState, Integer> b)
    {
        // compare union of keys
        var keys = new java.util.HashSet<SourceDocumentState>();
        keys.addAll(a.keySet());
        keys.addAll(b.keySet());
        for (var k : keys) {
            var va = a.getOrDefault(k, 0);
            var vb = b.getOrDefault(k, 0);
            if (!java.util.Objects.equals(va, vb)) {
                return false;
            }
        }
        return true;
    }
}
