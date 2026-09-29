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

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Tag.COMPLETE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Tag.DIFFERENCE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Tag.INCOMPLETE_POSITION;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.Tag.USED;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.StreamSupport;

import org.dkpro.statistics.agreement.IAnnotationUnit;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementSummary;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDataTally;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalpha.KrippendorffAlphaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;

public class KrippendorffAlphaNominalAgreementMeasureTest
    extends AgreementMeasureTestSuite_ImplBase
{
    private AgreementMeasureSupport<DefaultAgreementTraits, //
            FullCodingAgreementResult, ICodingAnnotationStudy> sut;
    private DefaultAgreementTraits traits;

    @Override
    @BeforeEach
    public void setup()
    {
        super.setup();

        sut = new KrippendorffAlphaAgreementMeasureSupport(annotationService, diffAdapterRegistry);
        traits = sut.createTraits();
    }

    @Test
    public void multiLinkWithRoleLabelDifference() throws Exception
    {
        when(annotationService.listSupportedFeatures(any(Project.class))).thenReturn(features);

        var result = multiLinkWithRoleLabelDifferenceTest(sut);

        var diff = result.getDiff();

        diff.print(System.out);

        assertThat(diff.size()).isEqualTo(3);
        assertThat(diff.getDifferingConfigurationSets()).isEmpty();
        assertThat(diff.getIncompleteConfigurationSets()).hasSize(2);

        assertThat(result.getAgreement()).isNaN();
    }

    /**
     * The exclusion count shown in the lower half of the pairwise agreement table is derived as
     * {@code relevant - used}, and its tooltip explains that number as the sum of the incomplete
     * and stacked sets. That explanation is only truthful if those counts actually add up to it.
     * <p>
     * This lives here rather than on one of the Kappa measures because Krippendorff's Alpha is one
     * of the few measures that honours the {@code excludeIncomplete} trait (and exposes it in a
     * traits editor). The measures that hard-code it cannot exercise the second mode at all, so
     * parameterising their tests over the trait only runs the same branch twice.
     */
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void thatExcludedSetsAreFullyAccountedFor(boolean aExcludeIncomplete) throws Exception
    {
        traits.setExcludeIncomplete(aExcludeIncomplete);

        var result = twoWithoutLabelTest(sut, traits);
        var summary = AgreementSummary.of(result);

        var excluded = summary.getRelevantSetCount() - summary.getUsedSetCount();

        // Mirrors what the tooltip lists: incomplete sets only count towards the exclusion when
        // the computation actually excluded them. When they were included instead, they are still
        // tagged incomplete but excluded nothing by the setting - yet Alpha does not score items
        // with a value from only one rater, so the measure itself excludes those.
        var breakdown = summary.getPluralitySets() + summary.getUnscoredSetCount();
        if (summary.isExcludeIncomplete()) {
            breakdown += summary.getIncompleteSetsByPosition() + summary.getIncompleteSetsByLabel();
        }

        assertThat(excluded) //
                .as("excluded sets are fully explained by the tooltip breakdown") //
                .isEqualTo(breakdown);

        // Guard against the assertion above holding trivially: the fixture has incomplete sets in
        // both modes, and the trait has to decide whether they were scored or dropped.
        assertThat(summary.getIncompleteSetsByPosition()) //
                .as("the fixture contributes incomplete sets in either mode") //
                .isEqualTo(2);
        // Both incomplete sets have a value from only one of the two raters, so Alpha scores
        // neither of them, whether or not the setting excludes them.
        assertThat(summary.getUsedSetCount()) //
                .as("incomplete sets with a single value are never scored by Alpha") //
                .isEqualTo(2);
        assertThat(summary.getUnscoredSetCount()) //
                .as("the measure ignores the incomplete sets only when the setting keeps them") //
                .isEqualTo(aExcludeIncomplete ? 0 : 2);

        // The table and the diagnostics notes must agree on how many items the measure ignored
        var tally = new AgreementDataTally();
        tally.add(result.getStudy(), result.isScoringSingleValueItems());
        assertThat(summary.getUnscoredSetCount()) //
                .as("the table reports the same unscored count as the diagnostics") //
                .isEqualTo(tally.getUnscoredItemCount());
    }

    @Test
    public void twoEmptyCasTest() throws Exception
    {
        var result = twoEmptyCasTest(sut);

        var diff = result.getDiff();

        assertThat(diff.size()).isEqualTo(0);
        assertThat(diff.getDifferingConfigurationSets()).isEmpty();
        assertThat(diff.getIncompleteConfigurationSets()).isEmpty();

        assertThat(result.getAgreement()).isNaN();
        assertThat(result.getIncompleteSetsByPosition()).isEmpty();
    }

    // @Test
    // public void singleNoDifferencesWithAdditionalCasTest() throws Exception
    // {
    // var result = singleNoDifferencesWithAdditionalCasTest(sut);
    //
    // CodingAgreementResult result1 = agreement.getStudy("user1", "user2");
    // assertEquals(0, result1.getTotalSetCount());
    // assertEquals(0, result1.getIrrelevantSets().size());
    // assertEquals(0, result1.getRelevantSetCount());
    //
    // CodingAgreementResult result2 = agreement.getStudy("user1", "user3");
    // assertEquals(1, result2.getTotalSetCount());
    // assertEquals(0, result2.getIrrelevantSets().size());
    // assertEquals(1, result2.getRelevantSetCount());
    //
    // assertEquals(NaN, agreement.getStudy("user1", "user2").getAgreement(), 0.01);
    // assertEquals(NaN, agreement.getStudy("user1", "user3").getAgreement(), 0.01);
    // assertEquals(NaN, agreement.getStudy("user2", "user3").getAgreement(), 0.01);
    // }

    @Test
    public void testTwoWithoutLabel_noExcludeIncomplete() throws Exception
    {
        traits.setExcludeIncomplete(false);

        var result = twoWithoutLabelTest(sut, traits);

        assertThat(result.getStudy().getItems())
                .extracting(item -> StreamSupport.stream(item.getUnits().spliterator(), false)
                        .map(IAnnotationUnit::getCategory).toList())
                .containsExactly( //
                        asList("", ""), //
                        asList("", null), //
                        asList(null, ""), //
                        asList("A", "B"));

        assertThat(result.getAllSets()).hasSize(4);
        assertThat(result.getIrrelevantSets()).isEmpty();
        // the following two counts are zero because the incomplete sets are not excluded!
        assertThat(result.getIncompleteSetsByPosition()) //
                .extracting(ConfigurationSet::getCasGroupIds) //
                .containsExactly( //
                        Set.of("user1"), //
                        Set.of("user2"));
        assertThat(result.getIncompleteSetsByLabel()).isEmpty();
        assertThat(result.getSetsWithDifferences()) //
                .extracting(ConfigurationSet::getCasGroupIds) //
                .containsExactly( //
                        Set.of("user1", "user2"));
        assertThat(result.getRelevantSets()) //
                .extracting(ConfigurationSet::getCasGroupIds, ConfigurationSet::getTags) //
                .containsExactly( //
                        tuple(Set.of("user1", "user2"), Set.of(COMPLETE, USED)), //
                        tuple(Set.of("user1"), Set.of(INCOMPLETE_POSITION, USED)), //
                        tuple(Set.of("user2"), Set.of(INCOMPLETE_POSITION, USED)), //
                        tuple(Set.of("user1", "user2"), Set.of(DIFFERENCE, COMPLETE, USED)));
        assertThat(result.getAgreement()).isCloseTo(0.4, within(0.01));
        // The two single-value items are part of the study, but alpha cannot pair their values
        assertThat(result.isScoringSingleValueItems()).isFalse();
    }

    @Test
    public void fullSingleCategoryAgreementWithTagsetTest() throws Exception
    {
        var tagset = new TagSet(project, "tagset");
        var tag1 = new Tag(tagset, "+");
        var tag2 = new Tag(tagset, "-");
        when(annotationService.listTags(tagset)).thenReturn(asList(tag1, tag2));
        when(annotationService.listSupportedFeatures(any(Project.class))).thenReturn(features);

        var result = fullSingleCategoryAgreementWithTagset(sut, traits);

        var item1 = result.getStudy().getItem(0);
        assertThat(item1.getUnit(0).getCategory()).isEqualTo("+");

        assertThat(result.getAllSets()).hasSize(1);
        assertThat(result.getIrrelevantSets()).isEmpty();
        assertThat(result.getIncompleteSetsByPosition()).isEmpty();
        assertThat(result.getIncompleteSetsByLabel()).isEmpty();
        assertThat(result.getSetsWithDifferences()).isEmpty();
        assertThat(result.getRelevantSets()) //
                .extracting(ConfigurationSet::getCasGroupIds, ConfigurationSet::getTags) //
                .containsExactly( //
                        tuple(Set.of("user1", "user2"), Set.of(COMPLETE, USED)));
        assertThat(result.getRelevantSets()).hasSize(1);
        assertThat(result.getAgreement()).isCloseTo(1.0, within(0.01));
    }
}
