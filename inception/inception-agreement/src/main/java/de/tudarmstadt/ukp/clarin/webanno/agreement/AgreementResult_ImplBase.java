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
package de.tudarmstadt.ukp.clarin.webanno.agreement;

import static java.util.Collections.emptyList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnostic;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public class AgreementResult_ImplBase
    implements Serializable
{
    private static final long serialVersionUID = -342895455350245379L;

    private final AnnotationFeature feature;
    private final DefaultAgreementTraits traits;

    private Aggregation aggregation;

    public AgreementResult_ImplBase(AnnotationFeature aFeature, DefaultAgreementTraits aTraits)
    {
        feature = aFeature;
        traits = aTraits;
    }

    public AnnotationFeature getFeature()
    {
        return feature;
    }

    public DefaultAgreementTraits getTraits()
    {
        return traits;
    }

    /**
     * @return the individual computations this result is composed of - one per rater pair, per
     *         document, or whatever else the concrete result compares. Each is counted once by the
     *         diagnostics aggregation below.
     */
    protected Collection<AgreementSummary> getComparisonResults()
    {
        return emptyList();
    }

    /**
     * @param aComparison
     *            one of the {@link #getComparisonResults() comparisons}.
     * @param aRaterNames
     *            maps the CAS group id of a rater to the name shown to the user.
     * @return how the comparison is named to the user, e.g. by its rater pair, or empty if the
     *         result has no way of naming it.
     */
    protected Optional<String> describeComparison(AgreementSummary aComparison,
            Function<String, String> aRaterNames)
    {
        return Optional.empty();
    }

    /**
     * Invalidates the aggregated diagnostics. Subclasses call this whenever the comparisons change,
     * i.e. while the calculation is still merging results into this one.
     */
    protected void invalidateDiagnostics()
    {
        aggregation = null;
    }

    /**
     * @return the data characteristics observed across the comparisons, most widespread first.
     */
    public List<AgreementDiagnostic> getDiagnostics()
    {
        return aggregation().diagnostics();
    }

    /**
     * @param aDiagnostic
     *            a diagnostic reported by {@link #getDiagnostics()}.
     * @return in how many comparisons the characteristic was observed.
     */
    public int getDiagnosticCount(AgreementDiagnostic aDiagnostic)
    {
        return aggregation().counts().getOrDefault(aDiagnostic, 0);
    }

    /**
     * @return the number of comparisons that produced a result, i.e. the denominator for
     *         {@link #getDiagnosticCount}. A comparison produced a result if it yielded a score or
     *         exhibits a characteristic. Comparisons that were entirely skipped - e.g. pairs with
     *         the curator while no document is in a curation state - are not counted, as "2 of 10
     *         rater pairs" would otherwise overstate how many pairs actually had data. Counting
     *         every comparison that exhibits a characteristic keeps the numerator within the
     *         denominator.
     */
    public int getComparisonCount()
    {
        return aggregation().comparisonCount();
    }

    /**
     * @param aDiagnostic
     *            a diagnostic reported by {@link #getDiagnostics()}, or by one of the comparisons.
     * @return whether every comparison that produced a result exhibits the characteristic. Such a
     *         characteristic says nothing about any comparison in particular.
     */
    public boolean isCommonToAllComparisons(AgreementDiagnostic aDiagnostic)
    {
        return getDiagnosticCount(aDiagnostic) >= getComparisonCount();
    }

    /**
     * @param aDiagnostic
     *            a diagnostic reported by {@link #getDiagnostics()}.
     * @param aRaterNames
     *            maps the CAS group id of a rater to the name shown to the user.
     * @return the names of the comparisons that exhibit the characteristic, in the order the
     *         comparisons were made. Comparisons the result cannot name are left out.
     */
    public List<String> getComparisonsExhibiting(AgreementDiagnostic aDiagnostic,
            Function<String, String> aRaterNames)
    {
        return aggregation().exhibitors().getOrDefault(aDiagnostic, emptyList()).stream() //
                .map(comparison -> describeComparison(comparison, aRaterNames)) //
                .flatMap(Optional::stream) //
                .toList();
    }

    /**
     * @param aDiagnostic
     *            a diagnostic reported by {@link #getDiagnostics()}.
     * @return the smallest and largest value measured for this characteristic across the
     *         comparisons that exhibit it, or empty if it is not a quantitative characteristic.
     *         <p>
     *         Diagnostics are grouped by type (and subject) so they can be counted per comparison,
     *         which means the representative instance carries only one comparison's measurement.
     *         Callers that report the characteristic for the study as a whole must use this range
     *         rather than that single value, which would otherwise be presented as if it held for
     *         all comparisons.
     */
    public Optional<double[]> getDiagnosticValueRange(AgreementDiagnostic aDiagnostic)
    {
        return Optional.ofNullable(aggregation().ranges().get(aDiagnostic)) //
                .map(double[]::clone);
    }

    /**
     * The comparisons no longer change once the calculation has finished, while the UI asks for the
     * diagnostics several times per request. So they are aggregated once, on first use, and kept
     * with the result.
     */
    private Aggregation aggregation()
    {
        if (aggregation == null) {
            aggregation = aggregateDiagnostics();
        }

        return aggregation;
    }

    private Aggregation aggregateDiagnostics()
    {
        // Count each characteristic once per comparison that exhibits it, regardless of how many
        // of its parts did. "3 of 10 comparisons" is the statement the UI wants to make.
        var counts = new LinkedHashMap<AgreementDiagnostic, Integer>();
        var representatives = new HashMap<AgreementDiagnostic, AgreementDiagnostic>();
        var ranges = new HashMap<AgreementDiagnostic, double[]>();
        var exhibitors = new HashMap<AgreementDiagnostic, List<AgreementSummary>>();
        var comparisonCount = 0;
        for (var result : getComparisonResults()) {
            var diagnostics = result.getDiagnostics();

            if (result.getUsableAgreementsCount() > 0 || !diagnostics.isEmpty()) {
                comparisonCount++;
            }

            for (var diagnostic : diagnostics) {
                counts.merge(diagnostic, 1, Integer::sum);
                exhibitors.computeIfAbsent(diagnostic, _k -> new ArrayList<>()).add(result);
                representatives.merge(diagnostic, diagnostic, AgreementDiagnostic::mergeWith);

                var value = diagnostic.getValue();
                if (!Double.isNaN(value)) {
                    ranges.merge(diagnostic, new double[] { value, value },
                            (a, b) -> new double[] { Math.min(a[0], b[0]), Math.max(a[1], b[1]) });
                }
            }
        }

        // A map keeps the first of two equal keys, so the representatives are put into a fresh one
        // rather than into the map holding the counts.
        var aggregated = new LinkedHashMap<AgreementDiagnostic, Integer>();
        counts.forEach(
                (diagnostic, count) -> aggregated.put(representatives.get(diagnostic), count));

        var sorted = aggregated.entrySet().stream() //
                .sorted(Map.Entry.<AgreementDiagnostic, Integer> comparingByValue().reversed()) //
                .map(Map.Entry::getKey) //
                .toList();

        return new Aggregation(sorted, aggregated, ranges, exhibitors, comparisonCount);
    }

    private record Aggregation(List<AgreementDiagnostic> diagnostics,
            Map<AgreementDiagnostic, Integer> counts, Map<AgreementDiagnostic, double[]> ranges,
            Map<AgreementDiagnostic, List<AgreementSummary>> exhibitors, int comparisonCount)
        implements Serializable
    {
        private static final long serialVersionUID = 1L;
    }
}
