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
package de.tudarmstadt.ukp.clarin.webanno.agreement.results;

import static java.lang.String.format;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementSummary;
import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementSummary.Skip;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

/**
 * Explains to the user why a rater pair produced no result on some documents, so that a "no data"
 * cell or a score resting on fewer documents than selected does not leave them guessing.
 */
public final class SkipDescriptions
{
    private SkipDescriptions()
    {
        // No instances
    }

    /**
     * @param aResult
     *            the summary of a rater pair.
     * @param aTraits
     *            the traits the calculation used.
     * @param aRater1
     *            the first rater of the pair.
     * @param aRater2
     *            the second rater of the pair.
     * @return one line per reason, preceded by a heading, or an empty string if no document was
     *         skipped.
     */
    public static String describeSkips(AgreementSummary aResult, DefaultAgreementTraits aTraits,
            User aRater1, User aRater2)
    {
        var skips = aResult.getSkips();
        if (skips.isEmpty()) {
            return "";
        }

        var text = new StringBuilder("Documents skipped:\n");
        for (var e : skips.entrySet()) {
            text.append(format("- %s: %d%n", describe(e.getKey(), aTraits, aRater1, aRater2),
                    e.getValue()));
        }
        return text.toString();
    }

    private static String describe(Skip aSkip, DefaultAgreementTraits aTraits, User aRater1,
            User aRater2)
    {
        return switch (aSkip.reason()) {
        // Likewise, the curation must have finished if the calculation is limited to finished
        // documents.
        case NOT_IN_CURATION_STATE -> aTraits != null && aTraits.isLimitToFinishedDocuments()
                ? "curation not finished"
                : "not in a curation state";
        // Only annotation documents in the states included in the calculation are loaded, so what
        // the rater lacks depends on whether the calculation is limited to finished documents.
        case NOT_ANNOTATED -> aTraits != null && aTraits.isLimitToFinishedDocuments()
                ? "not finished by " + uiName(aSkip.rater(), aRater1, aRater2)
                : "not worked on by " + uiName(aSkip.rater(), aRater1, aRater2);
        // A rater is only recorded if it was their data that could not be loaded.
        case FAILED -> aSkip.rater() != null ? "data of " + uiName(aSkip.rater(), aRater1, aRater2)
                + " could not be loaded (see log)" : "calculation failed (see log)";
        };
    }

    private static String uiName(String aRaterId, User aRater1, User aRater2)
    {
        if (aRater1.getUsername().equals(aRaterId)) {
            return aRater1.getUiName();
        }

        if (aRater2.getUsername().equals(aRaterId)) {
            return aRater2.getUiName();
        }

        return aRaterId;
    }
}
