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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.BOUNDARY_DISAGREEMENT;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.FEW_ITEMS;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.MARGINAL_ASYMMETRY;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.RATER_WITHOUT_ANNOTATIONS;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.SINGLE_CATEGORY;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.SKEWED_PREVALENCE;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.SPARSE_CATEGORIES;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.UNUSABLE_DOCUMENTS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementConcern;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementConcerns;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnostic;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.cohenkappa.CohenKappaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.fleisskappa.FleissKappaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.gamma.GammaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.gwetac1.GwetAC1AgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.gwetac2.GwetAC2AgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalpha.KrippendorffAlphaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalphaunitizing.KrippendorffAlphaUnitizingAgreementMeasureSupport;

/**
 * Checks the interpretations each measure attaches to the data characteristics reported by the
 * diagnostics - that every characteristic is routed to the intended text, and that the advice given
 * is actually available for the measure that gives it.
 */
class AgreementMeasureConcernsTest
{
    /**
     * The characteristics each measure interprets itself. Every other characteristic must fall
     * through to the generic, measure-independent interpretation.
     */
    private static final Map<String, Set<AgreementDiagnosticType>> OVERRIDES = Map.of( //
            CohenKappaAgreementMeasureSupport.ID, EnumSet.of(SKEWED_PREVALENCE, MARGINAL_ASYMMETRY),
            FleissKappaAgreementMeasureSupport.ID,
            EnumSet.of(SKEWED_PREVALENCE, MARGINAL_ASYMMETRY), //
            GwetAC1AgreementMeasureSupport.ID, EnumSet.of(SKEWED_PREVALENCE), //
            GwetAC2AgreementMeasureSupport.ID, EnumSet.of(SKEWED_PREVALENCE), //
            KrippendorffAlphaAgreementMeasureSupport.ID,
            EnumSet.of(SKEWED_PREVALENCE, MARGINAL_ASYMMETRY), //
            GammaAgreementMeasureSupport.ID, EnumSet.noneOf(AgreementDiagnosticType.class), //
            KrippendorffAlphaUnitizingAgreementMeasureSupport.ID,
            EnumSet.noneOf(AgreementDiagnosticType.class));

    static Stream<AgreementMeasureSupport<?, ?, ?>> measures()
    {
        return Stream.of( //
                new CohenKappaAgreementMeasureSupport(null, null), //
                new FleissKappaAgreementMeasureSupport(null, null), //
                new GwetAC1AgreementMeasureSupport(null, null), //
                new GwetAC2AgreementMeasureSupport(null, null), //
                new KrippendorffAlphaAgreementMeasureSupport(null, null), //
                new GammaAgreementMeasureSupport(), //
                new KrippendorffAlphaUnitizingAgreementMeasureSupport());
    }

    static Stream<Arguments> measuresAndTypes()
    {
        return measures().flatMap(m -> Arrays.stream(AgreementDiagnosticType.values())
                .map(t -> Arguments.of(m.getId(), t, m)));
    }

    @Test
    void thatGenericInterpretationCoversExactlyTheMeasureIndependentCharacteristics()
    {
        // Skew and marginal asymmetry have no measure-independent reading - what they imply
        // depends on how the measure models chance agreement. Everything else must be covered, so
        // that a measure without an override still explains it.
        var covered = EnumSet.noneOf(AgreementDiagnosticType.class);
        for (var type : AgreementDiagnosticType.values()) {
            AgreementConcerns.generic(diagnostic(type)).ifPresent(c -> covered.add(c.getType()));
        }

        assertThat(covered).containsExactlyInAnyOrder(FEW_ITEMS, SPARSE_CATEGORIES,
                BOUNDARY_DISAGREEMENT, UNUSABLE_DOCUMENTS, RATER_WITHOUT_ANNOTATIONS,
                SINGLE_CATEGORY);
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("measuresAndTypes")
    void thatConcernIsWellFormed(String aMeasure, AgreementDiagnosticType aType,
            AgreementMeasureSupport<?, ?, ?> aSupport)
    {
        var diagnostic = diagnostic(aType);

        aSupport.getConcern(diagnostic).ifPresent(concern -> {
            // The concern must describe the characteristic it was asked about - a switch case
            // falling through to the wrong text would otherwise go unnoticed.
            assertThat(concern.getDiagnostic()).isSameAs(diagnostic);
            assertThat(concern.getImplication()).isNotBlank();
            concern.getSuggestion().ifPresent(s -> assertThat(s).isNotBlank());
        });
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("measuresAndTypes")
    void thatOnlyOverriddenCharacteristicsDeviateFromTheGenericInterpretation(String aMeasure,
            AgreementDiagnosticType aType, AgreementMeasureSupport<?, ?, ?> aSupport)
    {
        var diagnostic = diagnostic(aType);
        var concern = aSupport.getConcern(diagnostic);
        var generic = AgreementConcerns.generic(diagnostic);

        if (OVERRIDES.get(aSupport.getId()).contains(aType)) {
            assertThat(concern).as("measure-specific interpretation").isPresent();
            assertThat(concern.map(AgreementConcern::getImplication))
                    .isNotEqualTo(generic.map(AgreementConcern::getImplication));
        }
        else {
            assertThat(concern.map(AgreementConcern::getImplication))
                    .isEqualTo(generic.map(AgreementConcern::getImplication));
            assertThat(concern.flatMap(AgreementConcern::getSuggestion))
                    .isEqualTo(generic.flatMap(AgreementConcern::getSuggestion));
        }
    }

    @Test
    void thatMultiRaterMeasuresDoNotRecommendAPairwiseOnlyMeasureForSkew()
    {
        // Fleiss' Kappa and Krippendorff's Alpha are used for studies with more than two raters.
        // Gwet's AC1 only supports two raters, so the advice must point to AC2 instead.
        var ac1 = new GwetAC1AgreementMeasureSupport(null, null);
        var ac2 = new GwetAC2AgreementMeasureSupport(null, null);
        assertThat(ac1.isSupportingMoreThanTwoRaters()).isFalse();
        assertThat(ac2.isSupportingMoreThanTwoRaters()).isTrue();

        for (var support : Set.of(new FleissKappaAgreementMeasureSupport(null, null),
                new KrippendorffAlphaAgreementMeasureSupport(null, null))) {
            var suggestion = support.getConcern(diagnostic(SKEWED_PREVALENCE))
                    .flatMap(AgreementConcern::getSuggestion) //
                    .orElseThrow();

            assertThat(suggestion).as(support.getId()) //
                    .contains("AC2") //
                    .doesNotContain("AC1");
        }
    }

    @Test
    void thatKrippendorffAlphaExplainsSkewInTermsOfAlpha()
    {
        var implication = new KrippendorffAlphaAgreementMeasureSupport(null, null)
                .getConcern(diagnostic(SKEWED_PREVALENCE)) //
                .map(AgreementConcern::getImplication) //
                .orElseThrow();

        assertThat(implication).contains("Alpha").doesNotContain("Kappa divides");
    }

    @Test
    void thatGwetMeasuresReassureRatherThanAdviseOnSkew()
    {
        // Skew is exactly the situation AC1 and AC2 were designed for - there is nothing to do.
        for (var support : Set.of(new GwetAC1AgreementMeasureSupport(null, null),
                new GwetAC2AgreementMeasureSupport(null, null))) {
            var concern = support.getConcern(diagnostic(SKEWED_PREVALENCE)).orElseThrow();

            assertThat(concern.getSuggestion()).as(support.getId()).isEmpty();
        }
    }

    @Test
    void thatSegmentationMeasuresAreOnlySuggestedWhenApplicable()
    {
        // Whether there is a measure scoring the segmentation depends on the layer - e.g. there is
        // none for relations - so the generic reading must not name any.
        assertThat(AgreementConcerns.generic(diagnostic(BOUNDARY_DISAGREEMENT))
                .flatMap(AgreementConcern::getSuggestion)).isEmpty();

        assertThat(AgreementConcerns.suggestSegmentationMeasures(List.of())).isEmpty();
        assertThat(AgreementConcerns.suggestSegmentationMeasures(List.of("Gamma")))
                .hasValueSatisfying(s -> assertThat(s).endsWith(": Gamma."));
        assertThat(AgreementConcerns
                .suggestSegmentationMeasures(List.of("Gamma", "Krippendorff's Alpha (unitizing)")))
                        .hasValueSatisfying(s -> assertThat(s)
                                .endsWith(": Gamma or Krippendorff's Alpha (unitizing)."));
    }

    private static AgreementDiagnostic diagnostic(AgreementDiagnosticType aType)
    {
        return new AgreementDiagnostic(aType, "observation", 0.5, "subject");
    }
}
