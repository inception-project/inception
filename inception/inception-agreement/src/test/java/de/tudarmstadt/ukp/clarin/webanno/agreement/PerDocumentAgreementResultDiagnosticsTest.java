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

import static de.tudarmstadt.ukp.clarin.webanno.agreement.SkipReason.NOT_ANNOTATED;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnosticType.SKEWED_PREVALENCE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.dkpro.statistics.agreement.coding.CodingAnnotationStudy;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.agreement.config.AgreementDiagnosticsPropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnostics;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

class PerDocumentAgreementResultDiagnosticsTest
{
    private static final String TYPE = "custom.Span";
    private static final String FEATURE = "value";

    @Test
    void thatDiagnosticsAreAggregatedAcrossDocuments()
    {
        // The per-document computation treats every document as its own complete study, so a
        // characteristic exhibited by three documents has to be reported as "3 of 3" rather than
        // being dropped because the result is not a pairwise one.
        var result = new PerDocumentAgreementResult(feature(), new DefaultAgreementTraits());
        for (var name : List.of("doc1", "doc2", "doc3")) {
            result.mergeResult(document(name), skewedSummary());
        }

        assertThat(result.getComparisonCount()).isEqualTo(3);

        var diagnostics = result.getDiagnostics();
        assertThat(diagnostics).extracting(d -> d.getType()).contains(SKEWED_PREVALENCE);

        var skew = diagnostics.stream() //
                .filter(d -> d.getType() == SKEWED_PREVALENCE) //
                .findFirst() //
                .orElseThrow();
        assertThat(result.getDiagnosticCount(skew)).isEqualTo(3);
    }

    @Test
    void thatUnaffectedDocumentsAreNotCounted()
    {
        var result = new PerDocumentAgreementResult(feature(), new DefaultAgreementTraits());
        result.mergeResult(document("skewed"), skewedSummary());
        result.mergeResult(document("balanced"), balancedSummary());

        var skew = result.getDiagnostics().stream() //
                .filter(d -> d.getType() == SKEWED_PREVALENCE) //
                .findFirst() //
                .orElseThrow();

        assertThat(result.getComparisonCount()).isEqualTo(2);
        assertThat(result.getDiagnosticCount(skew)).isEqualTo(1);
    }

    @Test
    void thatDocumentsWithoutScoreAreNotCountedAsComparisons()
    {
        // A document that produced no score and exhibits nothing did not take part in the
        // comparison, so it must not inflate the "N of M documents" denominator.
        var result = new PerDocumentAgreementResult(feature(), new DefaultAgreementTraits());
        result.mergeResult(document("skewed"), skewedSummary());
        result.mergeResult(document("balanced"), balancedSummary());
        result.mergeResult(document("skipped"),
                AgreementSummary.skipped(feature().getLayer(), feature(), NOT_ANNOTATED, "rater1"));

        assertThat(result.getComparisonCount()).isEqualTo(2);
    }

    @Test
    void thatDocumentsWithoutScoreDoNotSpoilScoreStatistics()
    {
        var result = new PerDocumentAgreementResult(feature(), new DefaultAgreementTraits());
        result.mergeResult(document("doc1"), new AgreementSummary(TYPE, FEATURE, 0.4));
        result.mergeResult(document("doc2"), new AgreementSummary(TYPE, FEATURE, 0.8));
        result.mergeResult(document("skipped"),
                AgreementSummary.skipped(feature().getLayer(), feature(), NOT_ANNOTATED, "rater1"));

        var stats = result.getAgreementScoreStats();
        assertThat(stats.getN()).isEqualTo(2);
        assertThat(stats.getMean()).isCloseTo(0.6, within(1e-9));
        assertThat(stats.getVariance()).isNotNaN();
        assertThat(result.getDocuments()).hasSize(3);
    }

    @Test
    void thatResultWithoutDocumentsReportsNoDiagnostics()
    {
        var result = new PerDocumentAgreementResult(feature(), new DefaultAgreementTraits());

        assertThat(result.getDiagnostics()).isEmpty();
        assertThat(result.getComparisonCount()).isZero();
    }

    private static AgreementSummary skewedSummary()
    {
        var study = new CodingAnnotationStudy(2);
        addItems(study, 91, "OTHER", "OTHER");
        addItems(study, 3, "PER", "PER");
        addItems(study, 3, "OTHER", "PER");
        addItems(study, 3, "PER", "OTHER");

        return analyzedSummary(study);
    }

    private static AgreementSummary balancedSummary()
    {
        var study = new CodingAnnotationStudy(2);
        addItems(study, 40, "A", "A");
        addItems(study, 40, "B", "B");
        addItems(study, 10, "A", "B");

        return analyzedSummary(study);
    }

    private static AgreementSummary analyzedSummary(CodingAnnotationStudy aStudy)
    {
        var diagnostics = new AgreementDiagnostics(new AgreementDiagnosticsPropertiesImpl());
        // Both raters annotated at least one position, so neither is reported as having nothing
        // to compare
        var shared = new ConfigurationSet(null);
        shared.addCasGroupId("rater1");
        shared.addCasGroupId("rater2");
        var full = new FullCodingAgreementResult(TYPE, FEATURE, null, aStudy,
                List.of("rater1", "rater2"), List.of(shared), false);

        // A single document is the whole study here, so the thresholds are applied right away -
        // this mirrors what CalculatePerDocumentAgreementTask does.
        var summary = AgreementSummary.of(full, diagnostics);
        summary.analyzeMerged(diagnostics);
        return summary;
    }

    private static void addItems(CodingAnnotationStudy aStudy, int aCount, String... aValues)
    {
        for (var i = 0; i < aCount; i++) {
            aStudy.addItem((Object[]) aValues);
        }
    }

    private static SourceDocument document(String aName)
    {
        var document = new SourceDocument();
        document.setName(aName);
        return document;
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
}
