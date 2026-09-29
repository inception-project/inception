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

/**
 * A characteristic of the annotated data that may affect how an agreement score should be read.
 * <p>
 * These describe the <em>data</em>, not the measure. Whether a given characteristic is actually a
 * problem depends on which measure was used, so the interpretation is left to the measure - see
 * {@code AgreementMeasureSupport#getConcern}.
 */
public enum AgreementDiagnosticType
{
    /**
     * One category dominates the label distribution. Chance-corrected measures of the Kappa family
     * become unstable under high skew: expected agreement approaches observed agreement, so the
     * score collapses towards zero even when raters agree on almost every item. This is the
     * so-called "Kappa paradox" (Feinstein &amp; Cicchetti 1990).
     */
    SKEWED_PREVALENCE,

    /**
     * The raters used the available categories with noticeably different frequencies. Measures that
     * pool the marginals (Fleiss' Kappa, Krippendorff's Alpha) count such rater bias as
     * disagreement. Cohen's Kappa uses each rater's own marginals instead, which lowers its
     * expected agreement, so it can report a higher score than the pooled measures.
     */
    MARGINAL_ASYMMETRY,

    /**
     * Some categories occur only a handful of times. How consistently the raters apply those labels
     * rests on too little evidence to be meaningful.
     */
    SPARSE_CATEGORIES,

    /**
     * Only one category was actually observed. Chance-corrected measures are undefined or
     * degenerate in this situation because there is no variation to correct for.
     */
    SINGLE_CATEGORY,

    /**
     * A large share of the positions were not annotated by all raters, i.e. the raters disagree
     * about <em>where</em> to annotate rather than about <em>which label</em> to apply. A coding
     * measure only sees the label disagreement and therefore answers a different question than the
     * one the data poses.
     */
    BOUNDARY_DISAGREEMENT,

    /**
     * Very few items were available for the computation, so the score carries a wide confidence
     * interval regardless of which measure was used.
     */
    FEW_ITEMS,

    /**
     * A large share of the documents both raters worked on did not produce a score at all, e.g.
     * because they contained nothing the measure could compare or because their data could not be
     * processed. The score therefore rests on a much smaller part of the shared documents than it
     * appears to. Documents only one of the raters worked on are not counted.
     */
    UNUSABLE_DOCUMENTS,

    /**
     * A rater has nothing that the comparison looks at in a document that is included in the
     * calculation, i.e. one they are working on or have finished. The data cannot tell why: they
     * may not have started yet, or there may have been nothing to annotate. Either way, every
     * position the other raters annotated in that document counts as one this rater left out.
     */
    RATER_WITHOUT_ANNOTATIONS;
}
