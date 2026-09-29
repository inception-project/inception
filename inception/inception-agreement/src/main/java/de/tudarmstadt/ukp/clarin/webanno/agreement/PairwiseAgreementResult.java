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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnostics;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public class PairwiseAgreementResult
    extends AgreementResult_ImplBase
{
    private static final long serialVersionUID = -6943850667308982795L;

    private final Set<String> raters = new TreeSet<>();
    private final Map<String, AgreementSummary> results = new LinkedHashMap<>();

    public PairwiseAgreementResult(AnnotationFeature aFeature, DefaultAgreementTraits aTraits)
    {
        super(aFeature, aTraits);
    }

    public Set<String> getRaters()
    {
        return raters;
    }

    public AgreementSummary getResult(String aAnnotator1, String aAnnotator2)
    {
        return results.get(makeKey(aAnnotator1, aAnnotator2));
    }

    public void mergeResult(String aAnnotator1, String aAnnotator2, AgreementSummary aRes)
    {
        invalidateDiagnostics();

        raters.add(aAnnotator1);
        raters.add(aAnnotator2);

        var existingRes = getResult(aAnnotator1, aAnnotator2);
        if (existingRes != null) {
            existingRes.merge(aRes);
        }
        else {
            results.put(makeKey(aAnnotator1, aAnnotator2), aRes);
        }
    }

    /**
     * Runs the diagnostics that can only be evaluated once all documents of a rater pair have been
     * merged. Call this after the last {@link #mergeResult} - before that, a pair's document counts
     * are still incomplete and any share derived from them would be wrong.
     *
     * @param aDiagnostics
     *            the configured analyzer.
     */
    public void analyzeMerged(AgreementDiagnostics aDiagnostics)
    {
        analyzeMerged(aDiagnostics, Function.identity());
    }

    /**
     * Runs the diagnostics that can only be evaluated once all documents of a rater pair have been
     * merged. Call this after the last {@link #mergeResult} - before that, a pair's document counts
     * are still incomplete and any share derived from them would be wrong.
     *
     * @param aDiagnostics
     *            the configured analyzer.
     * @param aRaterNames
     *            maps the CAS group id of a rater to the name shown to the user.
     */
    public void analyzeMerged(AgreementDiagnostics aDiagnostics,
            Function<String, String> aRaterNames)
    {
        invalidateDiagnostics();
        results.values().forEach(result -> result.analyzeMerged(aDiagnostics, aRaterNames));
    }

    @Override
    protected Collection<AgreementSummary> getComparisonResults()
    {
        // The unit of comparison here is the rater pair, so each pair counts once towards a
        // characteristic no matter how many documents within it exhibited the characteristic.
        return results.values();
    }

    @Override
    protected Optional<String> describeComparison(AgreementSummary aComparison,
            Function<String, String> aRaterNames)
    {
        var raters = aComparison.getCasGroupIds();
        if (raters.size() != 2) {
            return Optional.empty();
        }

        return Optional
                .of(aRaterNames.apply(raters.get(0)) + " ↔ " + aRaterNames.apply(raters.get(1)));
    }

    private String makeKey(String aKey1, String aKey2)
    {
        String key;
        if (aKey1.compareTo(aKey2) > 0) {
            key = aKey1 + aKey2;
        }
        else {
            key = aKey2 + aKey1;
        }
        return key;
    }
}
