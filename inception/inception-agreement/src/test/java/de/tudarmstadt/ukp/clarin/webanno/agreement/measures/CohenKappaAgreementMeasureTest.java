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
import static java.lang.Double.NaN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.StreamSupport;

import org.dkpro.statistics.agreement.IAnnotationUnit;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.cohenkappa.CohenKappaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class CohenKappaAgreementMeasureTest
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

        sut = new CohenKappaAgreementMeasureSupport(annotationService);
        traits = sut.createTraits();
    }

    @Test
    public void multiLinkWithRoleLabelDifference() throws Exception
    {
        when(annotationService.listSupportedFeatures(any(Project.class))).thenReturn(features);

        var result = multiLinkWithRoleLabelDifferenceTest(sut);

        var diff = result.getDiff();

        // diff.print(System.out);

        assertEquals(3, diff.size());
        assertThat(diff.getDifferingConfigurationSets()).isEmpty();
        assertEquals(2, diff.getIncompleteConfigurationSets().size());

        assertEquals(NaN, result.getAgreement(), 0.00001d);
    }

    @Test
    public void twoEmptyCasTest() throws Exception
    {
        var result = twoEmptyCasTest(sut);

        var diff = result.getDiff();

        assertEquals(0, diff.size());
        assertThat(diff.getDifferingConfigurationSets()).isEmpty();
        assertThat(diff.getIncompleteConfigurationSets()).isEmpty();

        assertEquals(NaN, result.getAgreement(), 0.000001d);
        assertThat(result.getIncompleteSetsByPosition()).isEmpty();
    }

    @Test
    public void threeCasesWithAnnotationOnlyInThird() throws Exception
    {
        var result = threeCasesWithAnnotationOnlyInThird(sut);

        assertThat(result.getAllSets()).hasSize(1);
        assertThat(result.getIrrelevantSets()).isEmpty();
        assertThat(result.getRelevantSets()).hasSize(1);
        assertThat(result.getAgreement()).isNaN();
    }

    @Test
    public void twoWithoutLabelTest() throws Exception
    {
        var result = twoWithoutLabelTest(sut, traits);

        // result.getDiff().print(System.out);

        assertThat(result.getStudy().getItems())
                .extracting(item -> StreamSupport.stream(item.getUnits().spliterator(), false)
                        .map(IAnnotationUnit::getCategory).toList())
                .containsExactly( //
                        asList("", ""), //
                        asList("A", "B"));

        assertThat(result.getAllSets()).hasSize(4);
        assertThat(result.getIrrelevantSets()).isEmpty();
        assertThat(result.getIncompleteSetsByPosition()) //
                .extracting(ConfigurationSet::getCasGroupIds, ConfigurationSet::getTags) //
                .containsExactly( //
                        tuple(Set.of("user1"), Set.of(INCOMPLETE_POSITION)), //
                        tuple(Set.of("user2"), Set.of(INCOMPLETE_POSITION)));
        assertThat(result.getIncompleteSetsByLabel()).isEmpty();
        assertThat(result.getSetsWithDifferences()) //
                .extracting(ConfigurationSet::getCasGroupIds, ConfigurationSet::getTags) //
                .containsExactly( //
                        tuple(Set.of("user1", "user2"), Set.of(DIFFERENCE, COMPLETE, USED)));
        assertThat(result.getRelevantSets()) //
                .extracting(ConfigurationSet::getCasGroupIds, ConfigurationSet::getTags) //
                .containsExactly( //
                        tuple(Set.of("user1", "user2"), Set.of(COMPLETE, USED)), //
                        tuple(Set.of("user1"), Set.of(INCOMPLETE_POSITION)), //
                        tuple(Set.of("user2"), Set.of(INCOMPLETE_POSITION)), //
                        tuple(Set.of("user1", "user2"), Set.of(DIFFERENCE, COMPLETE, USED)));

        assertEquals(0.333, result.getAgreement(), 0.01);
    }

    @Test
    public void fullSingleCategoryAgreementWithTagsetTest() throws Exception
    {
        var result = fullSingleCategoryAgreementWithTagset(sut, traits);

        var item1 = result.getStudy().getItem(0);
        assertEquals("+", item1.getUnit(0).getCategory());

        assertThat(result.getAllSets()).hasSize(1);
        assertThat(result.getIrrelevantSets()).isEmpty();
        assertThat(result.getIncompleteSetsByPosition()).isEmpty();
        assertThat(result.getIncompleteSetsByLabel()).isEmpty();
        assertThat(result.getSetsWithDifferences()).isEmpty();
        assertThat(result.getRelevantSets()).hasSize(1);
        assertEquals(1.0, result.getAgreement(), 0.01);
    }
}
