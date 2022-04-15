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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.dkpro.statistics.agreement.unitizing.IUnitizingAnnotationStudy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalphaunitizing.KrippendorffAlphaUnitizingAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalphaunitizing.KrippendorffAlphaUnitizingAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing.UnitizingAgreementResult;

public class KrippendorffAlphaUnitizingAgreementMeasureTest
    extends AgreementMeasureTestSuite_ImplBase
{
    private AgreementMeasureSupport_ImplBase<KrippendorffAlphaUnitizingAgreementTraits, //
            PairwiseAnnotationResult<UnitizingAgreementResult>, IUnitizingAnnotationStudy> sut;
    private KrippendorffAlphaUnitizingAgreementTraits traits;

    @Override
    @BeforeEach
    public void setup()
    {
        super.setup();

        sut = new KrippendorffAlphaUnitizingAgreementMeasureSupport(annotationService);
        traits = sut.createTraits();
    }

    @Test
    public void multiLinkWithRoleLabelDifference() throws Exception
    {
        var agreement = multiLinkWithRoleLabelDifferenceTest(sut);

        var result = agreement.getStudy("user1", "user2");

        assertEquals(0.0, result.getAgreement(), 0.00001d);
    }

    @Test
    public void twoEmptyCasTest() throws Exception
    {
        var agreement = twoEmptyCasTest(sut);

        var result = agreement.getStudy("user1", "user2");

        assertThat(result.getAgreement()).isNaN();
    }

    @Test
    public void singleNoDifferencesWithAdditionalCasTest() throws Exception
    {
        var agreement = singleNoDifferencesWithAdditionalCasTest(sut);

        assertThat(agreement.getStudy("user1", "user2").getAgreement()).isNaN();
        assertThat(agreement.getStudy("user1", "user3").getAgreement()).isEqualTo(-4.5d);
        assertThat(agreement.getStudy("user2", "user3").getAgreement()).isEqualTo(-4.5d);
    }

    @Test
    public void testTwoWithoutLabel_noExcludeIncomplete() throws Exception
    {
        traits.setExcludeIncomplete(false);

        var agreement = twoWithoutLabelTest(sut, traits);

        var result = agreement.getStudy("user1", "user2");

        assertEquals(0.0, result.getAgreement(), 0.01);
    }

    @Test
    public void fullSingleCategoryAgreementWithTagsetTest() throws Exception
    {
        var agreement = fullSingleCategoryAgreementWithTagset(sut, traits);

        var result = agreement.getStudy("user1", "user2");

        assertEquals(1.0, result.getAgreement(), 0.01);
    }

    @Test
    public void twoDocumentsNoOverlapTest() throws Exception
    {
        var agreement = twoDocumentsNoOverlap(sut, traits);

        var result = agreement.getStudy("user1", "user2");

        assertEquals(-0.0714, result.getAgreement(), 0.001);
    }

    @Test
    public void multiValueStringPartialAgreementTest() throws Exception
    {
        var agreement = multiValueStringPartialAgreement(sut);

        var result = agreement.getStudy("user1", "user2");

        assertEquals(0.4893, result.getAgreement(), 0.001);
    }
}
