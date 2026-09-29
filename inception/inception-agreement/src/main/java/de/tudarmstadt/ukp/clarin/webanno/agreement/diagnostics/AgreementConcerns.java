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
package de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics;

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;

/**
 * Interpretations of {@link AgreementDiagnostic}s that hold regardless of which measure was used.
 * Measures build on these and override the ones they have something more specific to say about.
 */
public class AgreementConcerns
{
    private AgreementConcerns()
    {
        // No instances
    }

    /**
     * Interprets a diagnostic in a way that is valid for any agreement measure.
     *
     * @param aDiagnostic
     *            the observed data characteristic.
     * @return the concern, or empty if this characteristic has no measure-independent implication.
     */
    public static Optional<AgreementConcern> generic(AgreementDiagnostic aDiagnostic)
    {
        switch (aDiagnostic.getType()) {
        case FEW_ITEMS:
            return Optional.of(new AgreementConcern(aDiagnostic,
                    "Scores computed on so few items vary widely by chance, so small differences "
                            + "between annotator pairs should not be over-interpreted.",
                    "Include more documents in the calculation if you can."));

        case SPARSE_CATEGORIES:
            return Optional.of(new AgreementConcern(aDiagnostic,
                    "These labels were used so rarely that how consistently the raters apply "
                            + "them rests on very little evidence. A single disagreement on a rare "
                            + "label can shift the score noticeably.",
                    "Include more documents in the calculation if you can, or consider merging "
                            + "rare labels in your annotation scheme."));

        case BOUNDARY_DISAGREEMENT:
            // Which measures score the segmentation instead depends on the layer, which is not
            // known here - see suggestSegmentationMeasures.
            return Optional.of(new AgreementConcern(aDiagnostic,
                    "A substantial part of the disagreement is about where to annotate rather "
                            + "than about which label to use. A coding measure only compares labels "
                            + "at matching positions, so it does not capture this kind of "
                            + "disagreement."));

        case UNUSABLE_DOCUMENTS:
            return Optional.of(new AgreementConcern(aDiagnostic,
                    "The score covers a much smaller part of the documents both raters worked on "
                            + "than it appears to. Such a document produces no score if the raters "
                            + "left nothing that the measure can compare - for example because the "
                            + "feature was never set, or because they never annotated the same "
                            + "positions while incomplete positions are excluded - or if its data "
                            + "could not be processed.",
                    "Check whether the selected feature is actually used in these documents. "
                            + "Documents that could not be processed are listed with the pair's "
                            + "result, and the details are in the log."));

        case RATER_WITHOUT_ANNOTATIONS:
            return Optional.of(new AgreementConcern(aDiagnostic,
                    "The data cannot tell why: the rater may not have started on these documents "
                            + "yet, or there may have been nothing to annotate. Either way, the "
                            + "positions the other raters annotated there are ones this rater did "
                            + "not annotate, which bears on the score and on the share of positions "
                            + "annotated by only some of the raters.",
                    "Check whether the rater has actually worked on these documents. Limiting the "
                            + "calculation to finished documents leaves out documents that are "
                            + "still in progress."));

        case SINGLE_CATEGORY:
            if (aDiagnostic instanceof AffectedDocumentsDiagnostic) {
                return Optional.of(new AgreementConcern(aDiagnostic,
                        "With no variation in the labels there is nothing for a chance-corrected "
                                + "measure to correct for, so the scores of these documents are "
                                + "degenerate rather than meaningful. The overall score is the "
                                + "average of the per-document scores, so they still count "
                                + "towards it like any other document.",
                        "Check these documents. If they genuinely use only one label, limit the "
                                + "calculation to the other documents to see how much they "
                                + "affect the score."));
            }

            return Optional.of(new AgreementConcern(aDiagnostic,
                    "With no variation in the labels there is nothing for a chance-corrected "
                            + "measure to correct for, so the score is degenerate rather than "
                            + "meaningful.",
                    "Check whether the correct feature was selected for the calculation."));

        default:
            // SKEWED_PREVALENCE and MARGINAL_ASYMMETRY have no measure-independent reading - what
            // they imply depends entirely on how the measure models chance agreement.
            return Optional.empty();
        }
    }

    /**
     * Suggests measures that score the segmentation itself, for data that shows
     * {@link AgreementDiagnosticType#BOUNDARY_DISAGREEMENT}. Such measures only exist for some
     * layers, so the caller passes those that are actually applicable - suggesting a measure the
     * user cannot select would only send them looking for it.
     *
     * @param aMeasureNames
     *            the names of the applicable measures that score the segmentation.
     * @return the suggestion, or empty if there is no such measure.
     */
    public static Optional<String> suggestSegmentationMeasures(List<String> aMeasureNames)
    {
        if (aMeasureNames.isEmpty()) {
            return Optional.empty();
        }

        var names = aMeasureNames.size() == 1 ? aMeasureNames.get(0)
                : String.join(", ", aMeasureNames.subList(0, aMeasureNames.size() - 1)) + " or "
                        + aMeasureNames.get(aMeasureNames.size() - 1);

        return Optional.of(format(
                "A measure that scores the segmentation itself is a better fit for this pattern: "
                        + "%s.",
                names));
    }
}
