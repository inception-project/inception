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

import static de.tudarmstadt.ukp.clarin.webanno.agreement.SkipReason.FAILED;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.SkipReason.NOT_ANNOTATED;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.SkipReason.NOT_IN_CURATION_STATE;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.BOUNDARY_DISAGREEMENT;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.UNUSABLE_DOCUMENTS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Tag.INCOMPLETE_POSITION;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.dkpro.statistics.agreement.coding.CodingAnnotationStudy;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.agreement.config.AgreementDiagnosticsPropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnostic;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnostics;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.SparseCategoriesDiagnostic;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

class PairwiseAgreementResultDiagnosticsTest
{
    private static final String TYPE = "custom.Span";
    private static final String FEATURE = "value";

    @Test
    void thatUnusableDocumentsAreReportedAfterMerging()
    {
        // 30 documents of which only 9 produced a score - well past the 30% default threshold.
        var result = resultWith(9, 21);
        result.analyzeMerged(diagnostics());

        assertThat(typesOf(result)).contains(UNUSABLE_DOCUMENTS);
        assertThat(observationOf(result)).contains("9").contains("30");
    }

    @Test
    void thatDocumentsOnlyOneRaterWorkedOnAreNotCounted()
    {
        // Where each document goes to only some of the annotators, most documents of a pair were
        // worked on by one of them only. That is how the work was distributed, not lost data.
        var result = resultWith(9, 0);
        for (var i = 0; i < 21; i++) {
            result.mergeResult("rater1", "rater2", AgreementSummary.skipped(feature().getLayer(),
                    feature(), NOT_ANNOTATED, "rater2"));
        }
        result.analyzeMerged(diagnostics());

        assertThat(typesOf(result)).doesNotContain(UNUSABLE_DOCUMENTS);
    }

    @Test
    void thatDocumentsThatFailedAreCounted()
    {
        var result = resultWith(9, 0);
        for (var i = 0; i < 21; i++) {
            result.mergeResult("rater1", "rater2",
                    AgreementSummary.skipped(feature().getLayer(), feature(), FAILED, "rater2"));
        }
        result.analyzeMerged(diagnostics());

        assertThat(typesOf(result)).contains(UNUSABLE_DOCUMENTS);
        assertThat(observationOf(result)).contains("9 of the 30 documents both raters worked on");
    }

    @Test
    void thatFullyUsableSelectionIsNotReported()
    {
        var result = resultWith(30, 0);
        result.analyzeMerged(diagnostics());

        assertThat(typesOf(result)).doesNotContain(UNUSABLE_DOCUMENTS);
    }

    @Test
    void thatSmallSelectionsAreReported()
    {
        // The share of documents is not an estimate, so the minimum basis for share does not
        // apply - "only 1 of 3 documents produced a score" is worth knowing even on 3 documents.
        // Two unusable documents is also exactly the minimum at which this is reported.
        var result = resultWith(1, 2);
        result.analyzeMerged(diagnostics());

        assertThat(typesOf(result)).contains(UNUSABLE_DOCUMENTS);
        assertThat(observationOf(result)).contains("1").contains("3");
    }

    @Test
    void thatASingleUnusableDocumentIsNotReported()
    {
        // Half the selection is unusable, well past the threshold, but a single unscored document
        // is too common to be worth a note.
        var result = resultWith(1, 1);
        result.analyzeMerged(diagnostics());

        assertThat(typesOf(result)).doesNotContain(UNUSABLE_DOCUMENTS);
    }

    @Test
    void thatUnusableDocumentsAreReportedExactlyAtTheThreshold()
    {
        // 3 of 10 documents without a score = 30%, the default threshold, which is inclusive.
        var result = resultWith(7, 3);
        result.analyzeMerged(diagnostics());

        assertThat(typesOf(result)).contains(UNUSABLE_DOCUMENTS);
    }

    @Test
    void thatUnusableDocumentsAreNotReportedJustBelowTheThreshold()
    {
        // 3 of 11 documents without a score = 27%.
        var result = resultWith(8, 3);
        result.analyzeMerged(diagnostics());

        assertThat(typesOf(result)).doesNotContain(UNUSABLE_DOCUMENTS);
    }

    @Test
    void thatNothingIsReportedWithoutTheAnalyzer()
    {
        var result = resultWith(9, 21);
        result.analyzeMerged(null);

        assertThat(typesOf(result)).isEmpty();
    }

    @Test
    void thatValueRangeSpansAllPairsExhibitingTheCharacteristic()
    {
        // Diagnostics are grouped by type so they can be counted per rater pair, which means the
        // representative instance carries only the first pair's measurement. The range must still
        // reflect what every pair measured.
        var result = new PairwiseAgreementResult(feature(), new DefaultAgreementTraits());
        result.mergeResult("a", "b", summaryWith(boundaryDisagreement(0.40)));
        result.mergeResult("c", "d", summaryWith(boundaryDisagreement(0.85)));

        var diagnostic = result.getDiagnostics().get(0);

        assertThat(result.getDiagnosticCount(diagnostic)).isEqualTo(2);
        assertThat(result.getDiagnosticValueRange(diagnostic)) //
                .hasValueSatisfying(range -> assertThat(range).containsExactly(0.40, 0.85));
    }

    @Test
    void thatAggregationReflectsResultsMergedAfterItWasRead()
    {
        // The aggregation is kept once computed, so merging another result must invalidate it.
        var result = new PairwiseAgreementResult(feature(), new DefaultAgreementTraits());
        result.mergeResult("a", "b", summaryWith(boundaryDisagreement(0.40)));

        var diagnostic = result.getDiagnostics().get(0);
        assertThat(result.getDiagnosticCount(diagnostic)).isEqualTo(1);
        assertThat(result.getComparisonCount()).isEqualTo(1);

        result.mergeResult("c", "d", summaryWith(boundaryDisagreement(0.85)));

        assertThat(result.getDiagnosticCount(diagnostic)).isEqualTo(2);
        assertThat(result.getComparisonCount()).isEqualTo(2);
        assertThat(result.getDiagnosticValueRange(diagnostic)) //
                .hasValueSatisfying(range -> assertThat(range).containsExactly(0.40, 0.85));
    }

    @Test
    void thatValueRangeCannotBeChangedByTheCaller()
    {
        var result = new PairwiseAgreementResult(feature(), new DefaultAgreementTraits());
        result.mergeResult("a", "b", summaryWith(boundaryDisagreement(0.40)));
        result.mergeResult("c", "d", summaryWith(boundaryDisagreement(0.85)));

        var diagnostic = result.getDiagnostics().get(0);
        result.getDiagnosticValueRange(diagnostic).orElseThrow()[0] = 0;

        assertThat(result.getDiagnosticValueRange(diagnostic)) //
                .hasValueSatisfying(range -> assertThat(range).containsExactly(0.40, 0.85));
    }

    @Test
    void thatSparseLabelsOfAllPairsAreNamed()
    {
        // Diagnostics are grouped by type so they can be counted per rater pair. For sparse labels
        // the labels are the finding, so the note must name those of every pair rather than only
        // those of the first one - a label sparse in one pair need not be sparse in another.
        var result = new PairwiseAgreementResult(feature(), new DefaultAgreementTraits());
        result.mergeResult("a", "b", summaryWith(new SparseCategoriesDiagnostic(List.of("X"), 5)));
        result.mergeResult("c", "d",
                summaryWith(new SparseCategoriesDiagnostic(List.of("X", "Y"), 5)));

        assertThat(result.getDiagnostics()).hasSize(1);

        var diagnostic = result.getDiagnostics().get(0);

        assertThat(diagnostic.getObservation()) //
                .isEqualTo("2 label(s) are used on fewer than 5 items in at least one comparison "
                        + "(\"X\", \"Y\").");
        assertThat(result.getDiagnosticCount(diagnostic)).isEqualTo(2);
        // The per-pair counts of sparse labels would only distract from the labels named above.
        assertThat(result.getDiagnosticValueRange(diagnostic)).isEmpty();
    }

    @Test
    void thatEntirelySkippedPairsAreNotCountedAsComparisons()
    {
        // Pairs with the curator while nothing is in a curation state are skipped on every
        // document. They had no data, so they must neither be flagged as covering only part of
        // the selection nor inflate the "N of M rater pairs" denominator. Several documents are
        // used so that the coverage note would otherwise fire.
        var result = resultWith(3, 0);
        for (var i = 0; i < 5; i++) {
            result.mergeResult("rater1", "curator", AgreementSummary.skipped(feature().getLayer(),
                    feature(), NOT_IN_CURATION_STATE, null));
            result.mergeResult("rater2", "curator", AgreementSummary.skipped(feature().getLayer(),
                    feature(), NOT_IN_CURATION_STATE, null));
        }
        result.analyzeMerged(diagnostics());

        assertThat(typesOf(result)).doesNotContain(UNUSABLE_DOCUMENTS);
        assertThat(result.getComparisonCount()).isEqualTo(1);
    }

    @Test
    void thatPairsWithoutScoreButWithCharacteristicAreCountedAsComparisons()
    {
        // A pair that produced no score but is reported as affected must be part of the
        // denominator, or the UI would claim more affected pairs than there are pairs. Raters who
        // never annotate at the same positions produce no score, but their boundary disagreement
        // is still reported.
        var result = resultWith(3, 0);
        result.mergeResult("rater1", "curator", disjunctPositions(20));
        result.analyzeMerged(diagnostics());

        assertThat(typesOf(result)).contains(BOUNDARY_DISAGREEMENT);
        assertThat(result.getComparisonCount()).isEqualTo(2);
    }

    @Test
    void thatValueRangeIsEmptyForUnknownDiagnostic()
    {
        var result = new PairwiseAgreementResult(feature(), new DefaultAgreementTraits());

        assertThat(result.getDiagnosticValueRange(boundaryDisagreement(0.4))).isEmpty();
    }

    @Test
    void thatSkippedDocumentsMergeWithComputedOnes()
    {
        // Measures that honour the excludeIncomplete trait pick up its default of false, while a
        // skipped document never ran a computation and so has no setting of its own. Both land in
        // the same rater pair as soon as one annotator is missing a single document, which is
        // ordinary rather than exotic - so merging them must not fail.
        var layer = new AnnotationLayer();
        layer.setName(TYPE);

        var result = new PairwiseAgreementResult(feature(), new DefaultAgreementTraits());
        result.mergeResult("rater1", "rater2",
                AgreementSummary.skipped(layer, feature(), NOT_ANNOTATED, "rater2"));
        result.mergeResult("rater1", "rater2", AgreementSummary.of(codingResult(false)));

        // The setting of the computation that actually ran has to survive the merge, otherwise the
        // tooltip would report sets as excluded that were in fact scored.
        assertThat(result.getResult("rater1", "rater2").isExcludeIncomplete()).isFalse();
    }

    @Test
    void thatSkippedOnlyPairsReportADefinedExclusionSetting()
    {
        var layer = new AnnotationLayer();
        layer.setName(TYPE);

        var summary = AgreementSummary.skipped(layer, feature(), NOT_ANNOTATED, "rater2");

        assertThat(summary.isExcludeIncomplete()).isTrue();
    }

    /**
     * Builds a coding result the way a measure honouring the {@code excludeIncomplete} trait would.
     */
    private static FullCodingAgreementResult codingResult(boolean aExcludeIncomplete)
    {
        var study = new CodingAnnotationStudy(2);
        study.addItem("A", "A");
        study.addItem("A", "B");

        return new FullCodingAgreementResult(TYPE, FEATURE, null, study,
                List.of("rater1", "rater2"), List.of(), aExcludeIncomplete);
    }

    /**
     * Builds a summary for raters who annotated the given number of positions, none of them shared
     * - so the study is empty and there is no score.
     */
    private static AgreementSummary disjunctPositions(int aPositions)
    {
        var sets = new ArrayList<ConfigurationSet>();
        for (var i = 0; i < aPositions; i++) {
            var set = new ConfigurationSet(null).addTags(INCOMPLETE_POSITION);
            set.addCasGroupId(i % 2 == 0 ? "rater1" : "curator");
            sets.add(set);
        }

        var result = new FullCodingAgreementResult(TYPE, FEATURE, null,
                new CodingAnnotationStudy(2), List.of("rater1", "curator"), sets, true);
        result.setAgreement(Double.NaN);
        return AgreementSummary.of(result, diagnostics());
    }

    private static AgreementDiagnostic boundaryDisagreement(double aValue)
    {
        return new AgreementDiagnostic(
                BOUNDARY_DISAGREEMENT, Math.round(aValue * 100)
                        + "% of the positions were annotated by only some of the " + "raters.",
                aValue, null);
    }

    private static AgreementSummary summaryWith(AgreementDiagnostic aDiagnostic)
    {
        var summary = new AgreementSummary(TYPE, FEATURE, 0.8);
        summary.addDiagnostic(aDiagnostic);
        return summary;
    }

    private static AnnotationFeature feature()
    {
        var layer = new AnnotationLayer();
        layer.setName(TYPE);

        var feature = new AnnotationFeature();
        feature.setName(FEATURE);
        feature.setLayer(layer);
        return feature;
    }

    private static AgreementDiagnostics diagnostics()
    {
        return new AgreementDiagnostics(new AgreementDiagnosticsPropertiesImpl());
    }

    /**
     * Builds a pairwise result for a single rater pair, merged from the given number of scored
     * documents and documents that both raters worked on but that produced no score - mirroring
     * what the calculation task does.
     */
    private static PairwiseAgreementResult resultWith(int aScored, int aUnscored)
    {
        var layer = new AnnotationLayer();
        layer.setName(TYPE);

        var feature = new AnnotationFeature();
        feature.setName(FEATURE);
        feature.setLayer(layer);

        var result = new PairwiseAgreementResult(feature, new DefaultAgreementTraits());

        for (var i = 0; i < aScored; i++) {
            result.mergeResult("rater1", "rater2", new AgreementSummary(TYPE, FEATURE, 0.8));
        }

        for (var i = 0; i < aUnscored; i++) {
            result.mergeResult("rater1", "rater2", new AgreementSummary(TYPE, FEATURE));
        }

        return result;
    }

    private static List<AgreementDiagnosticType> typesOf(PairwiseAgreementResult aResult)
    {
        return aResult.getDiagnostics().stream().map(AgreementDiagnostic::getType).toList();
    }

    private static String observationOf(PairwiseAgreementResult aResult)
    {
        return aResult.getDiagnostics().stream() //
                .filter(d -> d.getType() == UNUSABLE_DOCUMENTS) //
                .map(AgreementDiagnostic::getObservation) //
                .findFirst() //
                .orElseThrow();
    }
}
