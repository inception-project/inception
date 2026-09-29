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

import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.BOUNDARY_DISAGREEMENT;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.FEW_ITEMS;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.MARGINAL_ASYMMETRY;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.RATER_WITHOUT_ANNOTATIONS;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.SINGLE_CATEGORY;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.SKEWED_PREVALENCE;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.SPARSE_CATEGORIES;
import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.dkpro.statistics.agreement.coding.CodingAnnotationStudy;
import org.dkpro.statistics.agreement.coding.CohenKappaAgreement;
import org.dkpro.statistics.agreement.coding.PercentageAgreement;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementSummary;
import de.tudarmstadt.ukp.clarin.webanno.agreement.config.AgreementDiagnosticsPropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.ConfigurationSet;

class AgreementDiagnosticsTest
{
    @Test
    void thatBalancedStudyProducesNoDiagnostics()
    {
        var study = new CodingAnnotationStudy(2);
        addItems(study, 40, "A", "A");
        addItems(study, 40, "B", "B");
        addItems(study, 10, "A", "B");

        assertThat(analyze(study)).isEmpty();
    }

    @Test
    void thatSkewedPrevalenceIsDetected()
    {
        // The classic Kappa paradox: the raters agree on 94% of the items, but because one label
        // covers almost everything, Cohen's Kappa comes out far lower than the observed agreement.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 91, "OTHER", "OTHER");
        addItems(study, 3, "PER", "PER");
        addItems(study, 3, "OTHER", "PER");
        addItems(study, 3, "PER", "OTHER");

        assertThat(new PercentageAgreement(study).calculateAgreement()).isCloseTo(0.94,
                within(0.005));
        assertThat(new CohenKappaAgreement(study).calculateAgreement()).isLessThan(0.5);

        assertThat(typesOf(analyze(study))).contains(SKEWED_PREVALENCE);
    }

    @Test
    void thatSkewObservationNamesDominantAndRunnerUpLabels()
    {
        var study = new CodingAnnotationStudy(2);
        addItems(study, 91, "OTHER", "OTHER");
        addItems(study, 5, "PER", "PER");
        addItems(study, 2, "LOC", "LOC");

        var skew = analyze(study).stream() //
                .filter(d -> d.getType() == SKEWED_PREVALENCE) //
                .findFirst();

        assertThat(skew).isPresent();
        assertThat(skew.get().getObservation()) //
                .contains("OTHER") //
                .contains("PER") //
                .doesNotContain("LOC");
        assertThat(skew.get().getSubject()).contains("OTHER");
    }

    @Test
    void thatEquallyDominantLabelsAreAllNamed()
    {
        // A tie at the top can only reach the threshold once it has been lowered to 50% or less.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 45, "B", "B");
        addItems(study, 45, "A", "A");
        addItems(study, 10, "C", "C");

        var properties = new AgreementDiagnosticsPropertiesImpl();
        properties.setSkewThreshold(0.4);

        var diagnostics = new ArrayList<AgreementDiagnostic>();
        new AgreementDiagnostics(properties).analyzeStudy(study, diagnostics);

        var skew = diagnostics.stream() //
                .filter(d -> d.getType() == SKEWED_PREVALENCE) //
                .findFirst();

        assertThat(skew).isPresent();
        assertThat(skew.get().getObservation()) //
                .startsWith("The labels \"A\" and \"B\" each cover 45% of the 200") //
                .contains("the next most frequent (\"C\") covers 10%");
        assertThat(skew.get().getSubject()).get().asString().contains("A").contains("B");
    }

    @Test
    void thatDominantEmptyLabelIsStillReportedAsSkew()
    {
        // The empty label means the feature was left unset, but it is a category as far as the
        // measure is concerned and a dominant one depresses Kappa exactly like any other. Hiding it
        // would suppress a warning about a distortion that is really happening.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 334, "", "");
        addItems(study, 3, "_Grad_", "_Grad_");
        addItems(study, 1, "A", "A");

        var skew = analyze(study).stream() //
                .filter(d -> d.getType() == SKEWED_PREVALENCE) //
                .findFirst();

        assertThat(skew).isPresent();

        // The observation has to point at the unset feature rather than describing it as a label
        // named "", which would suggest rebalancing the label distribution instead of filling the
        // feature in.
        assertThat(skew.get().getObservation()) //
                .contains("No label at all") //
                .doesNotContain("One label (\"\")");
    }

    @Test
    void thatTinyRunnerUpShareIsNotReportedAsZero()
    {
        // A runner-up that rounds to 0% reads as a contradiction - the sentence names a label and
        // then says it does not occur.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 334, "OTHER", "OTHER");
        addItems(study, 1, "PER", "PER");

        var skew = analyze(study).stream() //
                .filter(d -> d.getType() == SKEWED_PREVALENCE) //
                .findFirst();

        assertThat(skew).isPresent();
        assertThat(skew.get().getObservation()) //
                .contains("less than 1%") //
                .doesNotContain("covers 0%");
    }

    @Test
    void thatNearTotalShareIsNotReportedAsHundredPercent()
    {
        // Rounding 334/335 up to "100%" claims nothing else was used, while the same sentence goes
        // on to name another label.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 334, "", "");
        addItems(study, 1, "_Grad_", "_Grad_");

        var skew = analyze(study).stream() //
                .filter(d -> d.getType() == SKEWED_PREVALENCE) //
                .findFirst();

        assertThat(skew).isPresent();
        assertThat(skew.get().getObservation()) //
                .contains("more than 99%") //
                .doesNotContain("covers 100%");
    }

    @Test
    void thatSingleCategoryAndSkewNeverDescribeTheSameData()
    {
        // The two notes make contradictory statements, so seeing both means they came from
        // different studies - different rater pairs - rather than from one. Neither threshold may
        // admit the other's data.
        var oneLabel = new CodingAnnotationStudy(2);
        addItems(oneLabel, 334, "", "");

        var twoLabels = new CodingAnnotationStudy(2);
        addItems(twoLabels, 334, "", "");
        addItems(twoLabels, 1, "_Grad_", "_Grad_");

        assertThat(typesOf(analyze(oneLabel))) //
                .contains(SINGLE_CATEGORY) //
                .doesNotContain(SKEWED_PREVALENCE);
        assertThat(typesOf(analyze(twoLabels))) //
                .contains(SKEWED_PREVALENCE) //
                .doesNotContain(SINGLE_CATEGORY);
    }

    @Test
    void thatConcentrationAcrossSeveralLabelsIsNotReportedAsSkew()
    {
        // Two labels sharing almost all the mass do not produce the Kappa paradox - only a single
        // dominant label does - so this must not be flagged.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 45, "A", "A");
        addItems(study, 45, "B", "B");
        addItems(study, 10, "C", "C");

        assertThat(typesOf(analyze(study))).doesNotContain(SKEWED_PREVALENCE);
    }

    @Test
    void thatSingleCategoryIsReportedInsteadOfSkew()
    {
        var study = new CodingAnnotationStudy(2);
        addItems(study, 50, "X", "X");

        assertThat(typesOf(analyze(study))) //
                .contains(SINGLE_CATEGORY) //
                .doesNotContain(SKEWED_PREVALENCE);
    }

    @Test
    void thatFewItemsAreDetected()
    {
        var study = new CodingAnnotationStudy(2);
        addItems(study, 6, "A", "A");
        addItems(study, 6, "B", "B");

        assertThat(typesOf(analyze(study))).contains(FEW_ITEMS);
    }

    @Test
    void thatMarginalAsymmetryIsDetected()
    {
        var study = new CodingAnnotationStudy(2);
        addItems(study, 40, "A", "B");
        addItems(study, 30, "A", "A");
        addItems(study, 30, "B", "B");

        assertThat(typesOf(analyze(study))).contains(MARGINAL_ASYMMETRY);
    }

    @Test
    void thatSymmetricMarginalsAreNotReportedEvenAtZeroThreshold()
    {
        // Identical marginals mean every gap is exactly zero. Reporting that as asymmetry would
        // both contradict itself ("0 percentage points apart") and name a "null" label, because no
        // category ever beats the initial worst gap.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 40, "A", "A");
        addItems(study, 40, "B", "B");

        var properties = new AgreementDiagnosticsPropertiesImpl();
        properties.setMarginalAsymmetryThreshold(0.0);

        var diagnostics = new ArrayList<AgreementDiagnostic>();
        new AgreementDiagnostics(properties).analyzeStudy(study, diagnostics);

        assertThat(typesOf(diagnostics)).doesNotContain(MARGINAL_ASYMMETRY);
    }

    @Test
    void thatMarginalAsymmetryNamesBothLabelsOfATwoLabelScheme()
    {
        // Rater 1 uses A for 70% of the items, rater 2 for 40%. B is off by the same 30 points in
        // the other direction - in floating point 0.7 - 0.4 and 0.6 - 0.3 still differ, though.
        // The two studies see the labels in opposite order, which must not change the note.
        var studyAB = new CodingAnnotationStudy(2);
        addItems(studyAB, 40, "A", "A");
        addItems(studyAB, 30, "A", "B");
        addItems(studyAB, 30, "B", "B");

        var studyBA = new CodingAnnotationStudy(2);
        addItems(studyBA, 30, "B", "B");
        addItems(studyBA, 30, "A", "B");
        addItems(studyBA, 40, "A", "A");

        var asymmetryAB = analyze(studyAB).stream() //
                .filter(d -> d.getType() == MARGINAL_ASYMMETRY) //
                .findFirst();
        var asymmetryBA = analyze(studyBA).stream() //
                .filter(d -> d.getType() == MARGINAL_ASYMMETRY) //
                .findFirst();

        assertThat(asymmetryAB).isPresent();
        assertThat(asymmetryAB.get().getObservation()) //
                .contains("the labels \"A\" and \"B\"") //
                .contains("30 percentage points");
        assertThat(asymmetryBA).isPresent();
        assertThat(asymmetryBA.get()).isEqualTo(asymmetryAB.get());
    }

    @Test
    void thatSparseCategoriesAreDetected()
    {
        var study = new CodingAnnotationStudy(2);
        addItems(study, 30, "A", "A");
        addItems(study, 30, "B", "B");
        study.addItem("C", "C");
        study.addItem("D", "D");

        var sparse = analyze(study).stream() //
                .filter(d -> d.getType() == SPARSE_CATEGORIES) //
                .findFirst();

        assertThat(sparse).isPresent();
        assertThat(sparse.get().getObservation()).contains("C").contains("D");
    }

    @Test
    void thatSparseCategoryListIsTruncatedWithoutStrayQuotes()
    {
        var study = new CodingAnnotationStudy(2);
        addItems(study, 30, "A", "A");
        addItems(study, 30, "B", "B");
        for (var label : asList("C", "D", "E", "F", "G")) {
            study.addItem(label, label);
        }

        var sparse = analyze(study).stream() //
                .filter(d -> d.getType() == SPARSE_CATEGORIES) //
                .findFirst();

        assertThat(sparse).isPresent();
        assertThat(sparse.get().getObservation()) //
                .endsWith("(\"C\", \"D\", \"E\" and 2 more).");
    }

    @Test
    void thatNullCategoriesAreIgnored()
    {
        // A null category means that the rater did not annotate the position at all, which only
        // happens when incomplete positions are included. That is not a label anybody used, so it
        // must not drive the label statistics. The nulls dominate here: counted as a category they
        // would cover over 45% of the assignments, while the labels actually used are balanced.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 10, "A", "A");
        addItems(study, 10, "B", "B");
        addItems(study, 50, "A", null);
        addItems(study, 50, null, "A");
        addItems(study, 50, "B", null);
        addItems(study, 50, null, "B");

        var diagnostics = analyze(study);

        assertThat(typesOf(diagnostics)).doesNotContain(SKEWED_PREVALENCE, SINGLE_CATEGORY);
        assertThat(diagnostics).extracting(AgreementDiagnostic::getObservation)
                .noneMatch(observation -> observation.contains("null"));
    }

    @Test
    void thatItemsTheMeasureDoesNotScoreAreLeftOutOfTheLabelStatistics()
    {
        // Only one rater annotated most positions, always with the same label. A measure like
        // Krippendorff's alpha cannot pair those single values and scores only the balanced rest,
        // so the label statistics must not report that one label dominates.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 20, "A", "A");
        addItems(study, 20, "B", "B");
        addItems(study, 400, "A", null);

        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        agreementDiagnostics.tally(result(study, 440, 400, false), tally);

        assertThat(tally.getItemCount()).isEqualTo(40);
        assertThat(tally.getUnscoredItemCount()).isEqualTo(400);
        assertThat(tally.getCategoryCounts()).containsEntry("A", 40).containsEntry("B", 40);

        var diagnostics = analyze(tally);

        assertThat(typesOf(diagnostics)).doesNotContain(SKEWED_PREVALENCE) //
                .contains(BOUNDARY_DISAGREEMENT);
        assertThat(diagnostics).filteredOn(d -> d.getType() == BOUNDARY_DISAGREEMENT) //
                .extracting(AgreementDiagnostic::getObservation) //
                .singleElement().asString() //
                .contains("ignores the 400 item(s)") //
                .contains("remaining 40");
    }

    @Test
    void thatItemsTheMeasureScoresStayInTheLabelStatistics()
    {
        // The same data as above, but for a measure that does score items with a single value -
        // there the dominant label is part of what the score is based on.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 20, "A", "A");
        addItems(study, 20, "B", "B");
        addItems(study, 400, "A", null);

        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        agreementDiagnostics.tally(result(study, 440, 400, true), tally);

        assertThat(tally.getItemCount()).isEqualTo(440);
        assertThat(tally.getUnscoredItemCount()).isZero();

        var diagnostics = analyze(tally);

        assertThat(typesOf(diagnostics)).contains(SKEWED_PREVALENCE, BOUNDARY_DISAGREEMENT);
        assertThat(diagnostics).extracting(AgreementDiagnostic::getObservation)
                .noneMatch(observation -> observation.contains("ignores"));
    }

    @Test
    void thatSingleValueItemsDoNotCauseMarginalAsymmetry()
    {
        // Rater 1 annotated additional positions with "A". Counted, they would put rater 1's share
        // of "A" 21 points above rater 2's. On the items the measure scores, both use "A" and "B"
        // equally.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 40, "A", "A");
        addItems(study, 40, "B", "B");
        addItems(study, 60, "A", null);

        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        agreementDiagnostics.tally(result(study, 140, 60, false), tally);

        assertThat(typesOf(analyze(tally))).doesNotContain(MARGINAL_ASYMMETRY);
    }

    @Test
    void thatScoreRestingOnNoItemIsReportedAsFewItems()
    {
        // The raters never gave a value to the same position, so a measure that ignores single
        // values has nothing to score, even though the study has plenty of items.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 50, "A", null);
        addItems(study, 50, null, "B");

        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        agreementDiagnostics.tally(result(study, 100, 100, false), tally);

        var diagnostics = analyze(tally);

        assertThat(diagnostics).filteredOn(d -> d.getType() == FEW_ITEMS) //
                .extracting(AgreementDiagnostic::getObservation) //
                .singleElement().asString() //
                .contains("only 0 item(s)");
    }

    @Test
    void thatItemsWithTwoOfThreeValuesAreStillScored()
    {
        // With three raters, an item that two of them annotated can be paired and therefore counts
        // towards the score, even though the third rater gave it no value.
        var study = new CodingAnnotationStudy(3);
        study.addItem("A", "A", null);
        study.addItem("B", null, null);

        var tally = new AgreementDataTally();
        tally.add(study, false);

        assertThat(tally.getItemCount()).isEqualTo(1);
        assertThat(tally.getUnscoredItemCount()).isEqualTo(1);
        assertThat(tally.getCategoryCounts()).containsOnlyKeys("A");
    }

    @Test
    void thatUnscoredItemsAreMerged()
    {
        var study = new CodingAnnotationStudy(2);
        addItems(study, 3, "A", "A");
        addItems(study, 2, "A", null);

        var first = new AgreementDataTally();
        first.add(study, false);
        var second = new AgreementDataTally();
        second.add(study, false);
        first.add(second);

        assertThat(first.getItemCount()).isEqualTo(6);
        assertThat(first.getUnscoredItemCount()).isEqualTo(4);
    }

    @Test
    void thatRaterWithoutAnnotationsIsReportedFromASingleDocument()
    {
        // A single such document may already be why the rater appears to disagree, and there is
        // no share that could tell a rater who has not started from one with nothing to annotate.
        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        for (var i = 0; i < 9; i++) {
            agreementDiagnostics.tally(documentAnnotatedBy("alice", "bob"), tally);
        }
        agreementDiagnostics.tally(documentAnnotatedBy("alice"), tally);

        var diagnostics = agreementDiagnostics.analyze(tally, null, RATER_NAMES);

        assertThat(diagnostics).filteredOn(d -> d.getType() == RATER_WITHOUT_ANNOTATIONS) //
                .extracting(AgreementDiagnostic::getObservation) //
                .containsExactly("No annotations to compare from Bob (in 1 of 10 documents).");
    }

    @Test
    void thatBoundaryDisagreementIsNotReportedAlongsideRaterWithoutAnnotations()
    {
        // Bob has not annotated one of the documents, so all of its positions are incomplete and
        // the boundary share reaches the threshold. The cause is not a disagreement about where to
        // annotate, and it is already reported as a rater without annotations.
        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        for (var i = 0; i < 2; i++) {
            agreementDiagnostics.tally(documentAnnotatedBy("alice", "bob"), tally);
        }

        var aliceOnly = new ArrayList<ConfigurationSet>();
        for (var i = 0; i < 40; i++) {
            var set = mock(ConfigurationSet.class);
            when(set.getCasGroupIds()).thenReturn(Set.of("alice"));
            aliceOnly.add(set);
        }
        var unannotatedByBob = mock(FullCodingAgreementResult.class);
        when(unannotatedByBob.isEmpty()).thenReturn(true);
        when(unannotatedByBob.getCasGroupIds()).thenReturn(List.of("alice", "bob"));
        when(unannotatedByBob.getRelevantSets()).thenReturn(aliceOnly);
        when(unannotatedByBob.getIncompleteSetsByPosition()).thenReturn(aliceOnly);
        agreementDiagnostics.tally(unannotatedByBob, tally);

        var boundaryOnly = new ArrayList<AgreementDiagnostic>();
        agreementDiagnostics.analyzeTally(tally, boundaryOnly);
        assertThat(typesOf(boundaryOnly)).contains(BOUNDARY_DISAGREEMENT);

        assertThat(typesOf(agreementDiagnostics.analyze(tally, null, RATER_NAMES)))
                .contains(RATER_WITHOUT_ANNOTATIONS) //
                .doesNotContain(BOUNDARY_DISAGREEMENT);
    }

    @Test
    void thatRatersWithAnnotationsInEveryDocumentAreNotReported()
    {
        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        agreementDiagnostics.tally(documentAnnotatedBy("alice", "bob"), tally);
        agreementDiagnostics.tally(documentAnnotatedBy("alice", "bob"), tally);

        assertThat(typesOf(agreementDiagnostics.analyze(tally, null, RATER_NAMES)))
                .doesNotContain(RATER_WITHOUT_ANNOTATIONS);
    }

    @Test
    void thatEveryRaterWithoutAnnotationsIsNamedForASingleDocument()
    {
        // Nobody annotated anything the comparison looks at. That, too, is only reported as what
        // it is rather than guessed at.
        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        agreementDiagnostics.tally(documentAnnotatedBy(), tally);

        assertThat(agreementDiagnostics.analyze(tally, null, RATER_NAMES))
                .filteredOn(d -> d.getType() == RATER_WITHOUT_ANNOTATIONS) //
                .extracting(AgreementDiagnostic::getObservation) //
                .containsExactly("No annotations to compare from Alice and Bob.");
    }

    @Test
    void thatRatersWithoutAnnotationsAreCountedAcrossMergedTallies()
    {
        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var first = new AgreementDataTally();
        agreementDiagnostics.tally(documentAnnotatedBy("alice"), first);
        var second = new AgreementDataTally();
        agreementDiagnostics.tally(documentAnnotatedBy("alice"), second);
        agreementDiagnostics.tally(documentAnnotatedBy("alice", "bob"), second);
        first.add(second);

        assertThat(first.getDocumentCount()).isEqualTo(3);
        assertThat(first.getEmptyDocumentCounts()).containsExactly(entry("bob", 2));
    }

    @Test
    void thatMergedRaterWithoutAnnotationsNamesTheRatersOfAllComparisons()
    {
        var merged = new RaterWithoutAnnotationsDiagnostic(Map.of("Bob", 1), 10)
                .mergeWith(new RaterWithoutAnnotationsDiagnostic(Map.of("Alice", 3), 8));

        assertThat(merged.getObservation()).isEqualTo(
                "No annotations to compare from Alice and Bob in at least one comparison.");
    }

    @Test
    void thatFewItemsAreReportedWhenTheFeatureWasNeverSet()
    {
        // A feature left unset reaches the study as the empty label. The score on such a study is
        // degenerate, so the note on how little it rests on must still appear.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 5, "", "");

        var diagnostics = analyze(study);

        assertThat(typesOf(diagnostics)).contains(FEW_ITEMS, SINGLE_CATEGORY);
        assertThat(diagnostics).extracting(AgreementDiagnostic::getObservation)
                .contains("No annotation has a label - the feature was always left unset.");
    }

    @Test
    void thatUnsetLabelIsNotListedAsEmptyQuotes()
    {
        var study = new CodingAnnotationStudy(2);
        addItems(study, 30, "A", "A");
        addItems(study, 30, "B", "B");
        study.addItem("", "");

        assertThat(analyze(study)).extracting(AgreementDiagnostic::getObservation) //
                .anyMatch(observation -> observation.contains("(no label)")) //
                .noneMatch(observation -> observation.contains("\"\""));
    }

    @Test
    void thatZeroMinimumBasisNeverReportsAnUndefinedShare()
    {
        // Another implementation of the properties might return zero. A share over zero positions
        // is undefined and must not surface as "NaN%" in the text shown to the user. This pins
        // down the behavior rather than one particular safeguard: each check currently rules out
        // a zero denominator on its own - here, no incomplete position means nothing is reported -
        // and the lower bound AgreementDiagnostics puts on the minimum basis is a second line of
        // defense for checks added later.
        var properties = new AgreementDiagnosticsPropertiesImpl()
        {
            @Override
            public int getMinimumBasisForShare()
            {
                return 0;
            }
        };

        var study = new CodingAnnotationStudy(2);
        addItems(study, 40, "A", "A");
        addItems(study, 40, "B", "B");

        var agreementDiagnostics = new AgreementDiagnostics(properties);
        var tally = new AgreementDataTally();
        agreementDiagnostics.tally(result(study, 0, 0), tally);

        var diagnostics = new ArrayList<AgreementDiagnostic>();
        agreementDiagnostics.analyzeTally(tally, diagnostics);

        assertThat(typesOf(diagnostics)).doesNotContain(BOUNDARY_DISAGREEMENT);
        assertThat(diagnostics).extracting(AgreementDiagnostic::getObservation)
                .noneMatch(observation -> observation.contains("NaN"));
    }

    @Test
    void thatManySmallDocumentsAreNotReportedAsFewItems()
    {
        // Each document on its own falls under the few-items limit, but the study spans 20 of them
        // and rests on 200 items in total. Diagnosing per document would permanently advise the
        // user to "include more documents", which adding documents can never resolve.
        var tally = new AgreementDataTally();
        for (var i = 0; i < 20; i++) {
            var study = new CodingAnnotationStudy(2);
            addItems(study, 5, "A", "A");
            addItems(study, 5, "B", "B");
            tally.add(study);
        }

        assertThat(tally.getItemCount()).isEqualTo(200);
        assertThat(typesOf(analyze(tally))).doesNotContain(FEW_ITEMS);
    }

    @Test
    void thatSkewIsJudgedOnTheWholeStudyRatherThanOneDocument()
    {
        // One document is dominated by a single label (90%), the others use a different label.
        // Across the study the dominant label covers well under half of the assignments, so the
        // skew note must not claim that the study is skewed - even though the document on its own
        // would trigger it. It may only point at that document.
        var skewedDoc = new CodingAnnotationStudy(2);
        addItems(skewedDoc, 27, "OTHER", "OTHER");
        addItems(skewedDoc, 3, "PER", "PER");

        // Guard against the test passing vacuously: judged on its own, the skewed document must
        // actually be reported as skewed (and not merely as consisting of a single label).
        var skewedDocTally = new AgreementDataTally();
        skewedDocTally.add(skewedDoc);
        assertThat(typesOf(analyze(skewedDocTally))).contains(SKEWED_PREVALENCE);

        // The same document merged with the rest of the study, the way the calculation merges
        // per-document summaries into the summary of a rater pair.
        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var summary = summaryOf(skewedDoc, agreementDiagnostics, "skewed.txt");
        for (var i = 0; i < 2; i++) {
            var otherDoc = new CodingAnnotationStudy(2);
            addItems(otherDoc, 15, "PER", "PER");
            addItems(otherDoc, 15, "LOC", "LOC");
            summary.merge(summaryOf(otherDoc, agreementDiagnostics, "other" + i + ".txt"));
        }
        summary.analyzeMerged(agreementDiagnostics);

        assertThat(summary.getDiagnostics()) //
                .filteredOn(d -> d.getType() == SKEWED_PREVALENCE) //
                .extracting(AgreementDiagnostic::getObservation) //
                .containsExactly("In 1 of the 3 scored documents, a single label covers 85% or "
                        + "more of the assigned labels: \"skewed.txt\".");
    }

    @Test
    void thatAnalyzedSummaryKeepsItsDiagnostics()
    {
        // The tally is dropped once analyzed. Analyzing again or merging into the summary must
        // neither fail nor lose the diagnostics that were derived from it.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 91, "OTHER", "OTHER");
        addItems(study, 3, "PER", "PER");
        addItems(study, 3, "OTHER", "PER");
        addItems(study, 3, "PER", "OTHER");

        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var summary = summaryOf(study, agreementDiagnostics);
        summary.analyzeMerged(agreementDiagnostics);
        assertThat(typesOf(summary.getDiagnostics())).contains(SKEWED_PREVALENCE);

        summary.analyzeMerged(agreementDiagnostics);
        summary.merge(summaryOf(study, agreementDiagnostics));

        assertThat(typesOf(summary.getDiagnostics())).contains(SKEWED_PREVALENCE);
    }

    @Test
    void thatQuantitiesInTheObservationDescribeTheWholeStudy()
    {
        // The numbers quoted in a note have to be the study's, not those of whichever document
        // happened to be analyzed first.
        var tally = new AgreementDataTally();
        for (var i = 0; i < 3; i++) {
            var study = new CodingAnnotationStudy(2);
            addItems(study, 31, "OTHER", "OTHER");
            addItems(study, 2, "PER", "PER");
            tally.add(study);
        }

        var skew = analyze(tally).stream() //
                .filter(d -> d.getType() == SKEWED_PREVALENCE) //
                .findFirst();

        assertThat(skew).isPresent();
        // 3 documents x 66 assignments, not the 66 of a single document.
        assertThat(skew.get().getObservation()).contains("198");
    }

    @Test
    void thatMergingSummariesAlsoMergesTheirTallies()
    {
        var first = new AgreementDataTally();
        var firstStudy = new CodingAnnotationStudy(2);
        addItems(firstStudy, 5, "A", "A");
        first.add(firstStudy);

        var second = new AgreementDataTally();
        var secondStudy = new CodingAnnotationStudy(2);
        addItems(secondStudy, 7, "B", "B");
        second.add(secondStudy);

        first.add(second);

        assertThat(first.getItemCount()).isEqualTo(12);
        assertThat(first.getAssignmentCount()).isEqualTo(24);
        assertThat(first.getCategoryCounts()).containsEntry("A", 10).containsEntry("B", 14);
    }

    @Test
    void thatBoundaryDisagreementIsReportedWhenNoPositionIsShared()
    {
        // With incomplete positions excluded, raters who never annotate the same position leave
        // an empty study behind. That is the strongest case of boundary disagreement there is, so
        // the note must not depend on any labels having been assigned.
        var diagnostics = new AgreementDiagnostics(new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        diagnostics.tally(result(new CodingAnnotationStudy(2), 40, 40), tally);

        assertThat(tally.getRelevantPositionCount()).isEqualTo(40);
        assertThat(typesOf(analyze(tally))).contains(BOUNDARY_DISAGREEMENT);
    }

    @Test
    void thatDocumentsWithoutSharedPositionsCountTowardsTheBoundaryShare()
    {
        // One document is entirely disjunct, the other mostly agrees on the positions. Across the
        // study, 55 of the 100 positions are incomplete - leaving out the disjunct document would
        // report only 5 of 50 and suppress the note exactly where boundary disagreement dominates.
        var diagnostics = new AgreementDiagnostics(new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();

        diagnostics.tally(result(new CodingAnnotationStudy(2), 50, 50), tally);

        var sharedDoc = new CodingAnnotationStudy(2);
        addItems(sharedDoc, 25, "A", "A");
        addItems(sharedDoc, 20, "B", "B");
        diagnostics.tally(result(sharedDoc, 50, 5), tally);

        assertThat(tally.getRelevantPositionCount()).isEqualTo(100);
        assertThat(tally.getIncompletePositionCount()).isEqualTo(55);
        assertThat(typesOf(analyze(tally))).contains(BOUNDARY_DISAGREEMENT);
    }

    // The thresholds are inclusive: a share that reaches the threshold is reported, one just
    // below it is not. The admin guide documents them this way, so the boundaries are pinned down.

    @Test
    void thatSkewIsReportedExactlyAtTheThreshold()
    {
        // 170 of 200 assignments = 85%, the default skew threshold.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 85, "OTHER", "OTHER");
        addItems(study, 15, "PER", "PER");

        assertThat(typesOf(analyze(study))).contains(SKEWED_PREVALENCE);
    }

    @Test
    void thatSkewIsNotReportedJustBelowTheThreshold()
    {
        // 168 of 200 assignments = 84%.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 84, "OTHER", "OTHER");
        addItems(study, 16, "PER", "PER");

        assertThat(typesOf(analyze(study))).doesNotContain(SKEWED_PREVALENCE);
    }

    @Test
    void thatMarginalAsymmetryIsReportedExactlyAtTheThreshold()
    {
        // Rater 1 uses A for 60% of the items, rater 2 for 40% - a gap of 20 percentage points,
        // the default threshold. In floating point 0.6 - 0.4 is 0.19999999999999996, so this also
        // guards against the boundary being missed by rounding error.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 40, "A", "A");
        addItems(study, 20, "A", "B");
        addItems(study, 40, "B", "B");

        assertThat(typesOf(analyze(study))).contains(MARGINAL_ASYMMETRY);
    }

    @Test
    void thatMarginalAsymmetryIsNotReportedJustBelowTheThreshold()
    {
        // Rater 1 uses A for 60% of the items, rater 2 for 41% - 19 percentage points apart.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 41, "A", "A");
        addItems(study, 19, "A", "B");
        addItems(study, 40, "B", "B");

        assertThat(typesOf(analyze(study))).doesNotContain(MARGINAL_ASYMMETRY);
    }

    @Test
    void thatBoundaryDisagreementIsReportedExactlyAtTheThreshold()
    {
        // 20 of 100 positions incomplete = 20%, the default threshold.
        var tally = new AgreementDataTally();
        new AgreementDiagnostics(new AgreementDiagnosticsPropertiesImpl())
                .tally(result(new CodingAnnotationStudy(2), 100, 20), tally);

        assertThat(typesOf(analyze(tally))).contains(BOUNDARY_DISAGREEMENT);
    }

    @Test
    void thatBoundaryDisagreementIsNotReportedJustBelowTheThreshold()
    {
        var tally = new AgreementDataTally();
        new AgreementDiagnostics(new AgreementDiagnosticsPropertiesImpl())
                .tally(result(new CodingAnnotationStudy(2), 100, 19), tally);

        assertThat(typesOf(analyze(tally))).doesNotContain(BOUNDARY_DISAGREEMENT);
    }

    @Test
    void thatCriterionStatesTheConfiguredThreshold()
    {
        var properties = new AgreementDiagnosticsPropertiesImpl();
        properties.setBoundaryDisagreementThreshold(0.25);
        var analyzer = new AgreementDiagnostics(properties);

        assertThat(analyzer
                .describeCriterion(new AgreementDiagnostic(BOUNDARY_DISAGREEMENT, "", 0.3, null)))
                        .hasValue("Only reported where 25% or more of the positions were "
                                + "annotated by only some of the raters.");
        assertThat(analyzer.describeCriterion(new AgreementDiagnostic(FEW_ITEMS, "", 5, null)))
                .hasValue("Only reported where the score rests on fewer than 30 items.");
        assertThat(analyzer
                .describeCriterion(new AgreementDiagnostic(MARGINAL_ASYMMETRY, "", 0.3, null)))
                        .hasValue("Only reported where the raters used a label at rates 20 or "
                                + "more percentage points apart.");
    }

    @Test
    void thatCriterionOfAffectedDocumentsIsAboutTheShareOfDocuments()
    {
        // The observation already says what each document exhibits, so the criterion must be the
        // share of documents - not the per-document threshold of the characteristic.
        var analyzer = new AgreementDiagnostics(new AgreementDiagnosticsPropertiesImpl());

        assertThat(analyzer.describeCriterion(new AffectedDocumentsDiagnostic(SKEWED_PREVALENCE,
                "a single label covers 85% or more of the assigned labels", List.of("a.txt"), 1,
                2))).hasValue(
                        "Only reported where 25% or more of the scored documents show "
                                + "this on their own, while the data as a whole does not.");
    }

    @Test
    void thatNotesWithoutThresholdHaveNoCriterion()
    {
        var analyzer = new AgreementDiagnostics(new AgreementDiagnosticsPropertiesImpl());

        assertThat(analyzer.describeCriterion(new AgreementDiagnostic(SINGLE_CATEGORY, "", 1, "A")))
                .isEmpty();
        assertThat(analyzer
                .describeCriterion(new RaterWithoutAnnotationsDiagnostic(Map.of("Bob", 1), 2)))
                        .isEmpty();
        // Names its limit in the observation already
        assertThat(analyzer.describeCriterion(new SparseCategoriesDiagnostic(List.of("A"), 5)))
                .isEmpty();
    }

    @Test
    void thatSharedBoundariesAreNotReportedEvenAtZeroThreshold()
    {
        // Every position was annotated by all raters. Reporting that as boundary disagreement
        // would contradict itself ("0% of the positions were annotated by only some").
        var properties = new AgreementDiagnosticsPropertiesImpl();
        properties.setBoundaryDisagreementThreshold(0.0);
        var analyzer = new AgreementDiagnostics(properties);

        var tally = new AgreementDataTally();
        analyzer.tally(result(new CodingAnnotationStudy(2), 100, 0), tally);

        var diagnostics = new ArrayList<AgreementDiagnostic>();
        analyzer.analyzeTally(tally, diagnostics);

        assertThat(typesOf(diagnostics)).doesNotContain(BOUNDARY_DISAGREEMENT);
    }

    @Test
    void thatFewItemsIsNotReportedAtTheLimit()
    {
        // The limit is the number of items *below* which the note fires - 30 items is enough.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 15, "A", "A");
        addItems(study, 15, "B", "B");

        assertThat(typesOf(analyze(study))).doesNotContain(FEW_ITEMS);
    }

    @Test
    void thatFewItemsIsReportedJustBelowTheLimit()
    {
        var study = new CodingAnnotationStudy(2);
        addItems(study, 15, "A", "A");
        addItems(study, 14, "B", "B");

        assertThat(typesOf(analyze(study))).contains(FEW_ITEMS);
    }

    @Test
    void thatSparseCategoryIsNotReportedAtTheLimit()
    {
        // "C" is used on exactly 5 items - the limit is the count *below* which a label is sparse.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 40, "A", "A");
        addItems(study, 40, "B", "B");
        addItems(study, 3, "C", "C");
        addItems(study, 2, "C", "A");

        assertThat(typesOf(analyze(study))).doesNotContain(SPARSE_CATEGORIES);
    }

    @Test
    void thatSparseCategoryIsReportedJustBelowTheLimit()
    {
        // "C" is used on 4 items. Both raters assigned it on each, 8 times in total - but that is
        // still evidence from only 4 items.
        var study = new CodingAnnotationStudy(2);
        addItems(study, 40, "A", "A");
        addItems(study, 40, "B", "B");
        addItems(study, 4, "C", "C");

        assertThat(typesOf(analyze(study))).contains(SPARSE_CATEGORIES);
    }

    @Test
    void thatSparseCategoryIsNotHiddenByTheNumberOfRaters()
    {
        // Five raters agreeing on "C" for a single item assign it five times, which would reach
        // the limit if assignments were counted.
        var study = new CodingAnnotationStudy(5);
        for (var i = 0; i < 40; i++) {
            study.addItem("A", "A", "A", "A", "A");
            study.addItem("B", "B", "B", "B", "B");
        }
        study.addItem("C", "C", "C", "C", "C");

        assertThat(typesOf(analyze(study))).contains(SPARSE_CATEGORIES);
    }

    @Test
    void thatDocumentsUsingOnlyOneLabelAreNamedWhenThePooledDataIsBalanced()
    {
        // Each document scores 1.0 through the single-category shortcut, but pooled, the labels
        // are split 50/50 - so only the documents on their own show why the average is perfect.
        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        agreementDiagnostics.tally(scoredResult(itemsOf(10, "A", "A"), 1.0), "doc1.txt", tally);
        agreementDiagnostics.tally(scoredResult(itemsOf(10, "B", "B"), 1.0), "doc2.txt", tally);

        assertThat(agreementDiagnostics.analyze(tally, null)) //
                .filteredOn(d -> d.getType() == SINGLE_CATEGORY) //
                .singleElement() //
                .isInstanceOf(AffectedDocumentsDiagnostic.class) //
                .extracting(AgreementDiagnostic::getObservation) //
                .isEqualTo("In 2 of the 2 scored documents, all annotations use the same label: "
                        + "\"doc1.txt\" and \"doc2.txt\".");
    }

    @Test
    void thatRaterBiasCancellingOutAcrossDocumentsIsReported()
    {
        // In the first document, rater 1 prefers "A" and rater 2 prefers "B"; in the second, it is
        // the other way around. Pooled, both raters use both labels equally often.
        var first = new CodingAnnotationStudy(2);
        addItems(first, 6, "A", "A");
        addItems(first, 8, "A", "B");
        addItems(first, 6, "B", "B");
        var second = new CodingAnnotationStudy(2);
        addItems(second, 6, "A", "A");
        addItems(second, 8, "B", "A");
        addItems(second, 6, "B", "B");

        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        agreementDiagnostics.tally(scoredResult(first, 0.2), "first.txt", tally);
        agreementDiagnostics.tally(scoredResult(second, 0.2), "second.txt", tally);

        var asymmetry = agreementDiagnostics.analyze(tally, null).stream() //
                .filter(d -> d.getType() == MARGINAL_ASYMMETRY) //
                .toList();

        assertThat(asymmetry).singleElement().isInstanceOf(AffectedDocumentsDiagnostic.class);
        assertThat(((AffectedDocumentsDiagnostic) asymmetry.get(0)).getDocumentNames())
                .containsExactly("first.txt", "second.txt");
    }

    @Test
    void thatFewAffectedDocumentsAreNotReported()
    {
        // One document of ten uses a single label - below the default threshold of 25%
        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        agreementDiagnostics.tally(scoredResult(itemsOf(10, "A", "A"), 1.0), "single.txt", tally);
        for (var i = 0; i < 9; i++) {
            var study = itemsOf(10, "A", "A");
            addItems(study, 10, "B", "B");
            agreementDiagnostics.tally(scoredResult(study, 1.0), "doc" + i + ".txt", tally);
        }

        assertThat(typesOf(agreementDiagnostics.analyze(tally, null)))
                .doesNotContain(SINGLE_CATEGORY);
    }

    @Test
    void thatDocumentsWithoutScoreAreNotCountedAsAffected()
    {
        // A document without a score does not go into the average, so it cannot distort it. That
        // it produced no score is what the note on unusable documents is for.
        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        agreementDiagnostics.tally(scoredResult(itemsOf(10, "A", "A"), Double.NaN), "a.txt", tally);
        agreementDiagnostics.tally(scoredResult(itemsOf(10, "B", "B"), Double.NaN), "b.txt", tally);
        var mixed = itemsOf(10, "A", "A");
        addItems(mixed, 10, "B", "B");
        agreementDiagnostics.tally(scoredResult(mixed, 1.0), "c.txt", tally);

        assertThat(tally.getScoredDocumentCount()).isEqualTo(1);
        assertThat(tally.getAffectedDocumentCount(SINGLE_CATEGORY)).isZero();
        assertThat(typesOf(agreementDiagnostics.analyze(tally, null)))
                .doesNotContain(SINGLE_CATEGORY);
    }

    @Test
    void thatAffectedDocumentsAreNotReportedWhenThePooledDataShowsTheSame()
    {
        // Both documents use only "A", so the pooled note already covers them
        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        agreementDiagnostics.tally(scoredResult(itemsOf(10, "A", "A"), 1.0), "a.txt", tally);
        agreementDiagnostics.tally(scoredResult(itemsOf(10, "A", "A"), 1.0), "b.txt", tally);

        assertThat(agreementDiagnostics.analyze(tally, null)) //
                .filteredOn(d -> d.getType() == SINGLE_CATEGORY) //
                .singleElement() //
                .isNotInstanceOf(AffectedDocumentsDiagnostic.class);
    }

    @Test
    void thatAffectedDocumentsAreNamedAcrossMergedTallies()
    {
        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var first = new AgreementDataTally();
        agreementDiagnostics.tally(scoredResult(itemsOf(10, "A", "A"), 1.0), "b.txt", first);
        var second = new AgreementDataTally();
        agreementDiagnostics.tally(scoredResult(itemsOf(10, "B", "B"), 1.0), "a.txt", second);
        first.add(second);

        assertThat(first.getScoredDocumentCount()).isEqualTo(2);
        assertThat(first.getAffectedDocumentNames(SINGLE_CATEGORY)).containsExactly("a.txt",
                "b.txt");
    }

    @Test
    void thatManyAffectedDocumentsAreSummarized()
    {
        assertThat(AgreementDiagnostics.listDocuments(List.of("a", "b", "c", "d", "e"), 5))
                .isEqualTo("\"a\", \"b\", \"c\" and 2 more");
        // Documents without a known name are still counted
        assertThat(AgreementDiagnostics.listDocuments(List.of("a"), 3))
                .isEqualTo("\"a\" and 2 more");
        assertThat(AgreementDiagnostics.listDocuments(List.of(), 3)).isEmpty();
    }

    @Test
    void thatMergedAffectedDocumentsNameTheDocumentsOfAllComparisons()
    {
        var merged = new AffectedDocumentsDiagnostic(SINGLE_CATEGORY,
                "all annotations use the same label", List.of("b.txt"), 1, 2)
                        .mergeWith(new AffectedDocumentsDiagnostic(SINGLE_CATEGORY,
                                "all annotations use the same label", List.of("a.txt", "b.txt"), 2,
                                4));

        assertThat(merged.getObservation()).isEqualTo("In at least one comparison, all annotations "
                + "use the same label in some of the scored documents: \"a.txt\" and \"b.txt\".");
    }

    @Test
    void thatRaterWithoutAnnotationsNamesTheDocuments()
    {
        var agreementDiagnostics = new AgreementDiagnostics(
                new AgreementDiagnosticsPropertiesImpl());
        var tally = new AgreementDataTally();
        agreementDiagnostics.tally(documentAnnotatedBy("alice", "bob"), "full.txt", tally);
        agreementDiagnostics.tally(documentAnnotatedBy("alice"), "empty.txt", tally);

        assertThat(agreementDiagnostics.analyze(tally, null, RATER_NAMES))
                .filteredOn(d -> d.getType() == RATER_WITHOUT_ANNOTATIONS) //
                .extracting(AgreementDiagnostic::getObservation) //
                .containsExactly("No annotations to compare from Bob "
                        + "(in 1 of 2 documents: \"empty.txt\").");
    }

    @Test
    void thatMergedRaterWithoutAnnotationsNamesTheDocumentsOfAllComparisons()
    {
        var merged = new RaterWithoutAnnotationsDiagnostic(Map.of("Bob", 1),
                Map.of("Bob", List.of("b.txt")), 10)
                        .mergeWith(new RaterWithoutAnnotationsDiagnostic(Map.of("Bob", 1),
                                Map.of("Bob", List.of("a.txt")), 8));

        assertThat(merged.getObservation()).isEqualTo("No annotations to compare from Bob "
                + "(in \"a.txt\" and \"b.txt\") in at least one comparison.");
    }

    private static final Function<String, String> RATER_NAMES = Map.of("alice", "Alice", "bob",
            "Bob")::get;

    /**
     * Builds the result of a document compared between "alice" and "bob" in which only the given
     * raters annotated anything that the comparison looks at.
     */
    private static FullCodingAgreementResult documentAnnotatedBy(String... aRaters)
    {
        var sets = new ArrayList<ConfigurationSet>();
        for (var rater : aRaters) {
            var set = mock(ConfigurationSet.class);
            when(set.getCasGroupIds()).thenReturn(Set.of(rater));
            sets.add(set);
        }

        var result = mock(FullCodingAgreementResult.class);
        when(result.isEmpty()).thenReturn(true);
        when(result.getCasGroupIds()).thenReturn(List.of("alice", "bob"));
        when(result.getRelevantSets()).thenReturn(sets);
        when(result.getIncompleteSetsByPosition()).thenReturn(List.of());
        return result;
    }

    private static FullCodingAgreementResult result(CodingAnnotationStudy aStudy,
            int aRelevantPositions, int aIncompletePositions)
    {
        return result(aStudy, aRelevantPositions, aIncompletePositions, true);
    }

    private static FullCodingAgreementResult result(CodingAnnotationStudy aStudy,
            int aRelevantPositions, int aIncompletePositions, boolean aScoringSingleValueItems)
    {
        var result = mock(FullCodingAgreementResult.class);
        when(result.isEmpty()).thenReturn(aStudy.getItemCount() == 0);
        when(result.getStudy()).thenReturn(aStudy);
        when(result.isScoringSingleValueItems()).thenReturn(aScoringSingleValueItems);
        when(result.getRelevantSets())
                .thenReturn(nCopies(aRelevantPositions, mock(ConfigurationSet.class)));
        when(result.getIncompleteSetsByPosition())
                .thenReturn(nCopies(aIncompletePositions, mock(ConfigurationSet.class)));
        return result;
    }

    private static FullCodingAgreementResult scoredResult(CodingAnnotationStudy aStudy,
            double aAgreement)
    {
        var result = result(aStudy, aStudy.getItemCount(), 0);
        when(result.getAgreement()).thenReturn(aAgreement);
        return result;
    }

    private static CodingAnnotationStudy itemsOf(int aCount, String aRater1, String aRater2)
    {
        var study = new CodingAnnotationStudy(2);
        addItems(study, aCount, aRater1, aRater2);
        return study;
    }

    private static AgreementSummary summaryOf(CodingAnnotationStudy aStudy,
            AgreementDiagnostics aDiagnostics)
    {
        return summaryOf(aStudy, aDiagnostics, null);
    }

    private static AgreementSummary summaryOf(CodingAnnotationStudy aStudy,
            AgreementDiagnostics aDiagnostics, String aDocumentName)
    {
        // Both raters annotated at least one position, so neither is reported as having nothing
        // to compare
        var shared = new ConfigurationSet(null);
        shared.addCasGroupId("rater1");
        shared.addCasGroupId("rater2");
        var result = new FullCodingAgreementResult("custom.Span", "value", null, aStudy,
                List.of("rater1", "rater2"), List.of(shared), false);
        return AgreementSummary.of(result, aDiagnostics, aDocumentName);
    }

    private static void addItems(CodingAnnotationStudy aStudy, int aCount, String aRater1,
            String aRater2)
    {
        for (var i = 0; i < aCount; i++) {
            aStudy.addItem(aRater1, aRater2);
        }
    }

    private static List<AgreementDiagnostic> analyze(CodingAnnotationStudy aStudy)
    {
        var diagnostics = new ArrayList<AgreementDiagnostic>();
        new AgreementDiagnostics(new AgreementDiagnosticsPropertiesImpl()) //
                .analyzeStudy(aStudy, diagnostics);
        return diagnostics;
    }

    private static List<AgreementDiagnostic> analyze(AgreementDataTally aTally)
    {
        var diagnostics = new ArrayList<AgreementDiagnostic>();
        new AgreementDiagnostics(new AgreementDiagnosticsPropertiesImpl()) //
                .analyzeTally(aTally, diagnostics);
        return diagnostics;
    }

    private static List<AgreementDiagnosticType> typesOf(List<AgreementDiagnostic> aDiagnostics)
    {
        return aDiagnostics.stream().map(AgreementDiagnostic::getType).toList();
    }
}
