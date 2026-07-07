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
import static org.assertj.core.api.Assertions.within;

import org.dkpro.statistics.agreement.aligning.AligningAnnotationStudy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.gamma.GammaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.aligning.FullAligningAgreementResult;

public class GammaAgreementMeasureTest
    extends AgreementMeasureTestSuite_ImplBase
{
    private AgreementMeasureSupport_ImplBase<DefaultAgreementTraits, //
            FullAligningAgreementResult, AligningAnnotationStudy> sut;
    private DefaultAgreementTraits traits;

    @Override
    @BeforeEach
    public void setup()
    {
        super.setup();

        sut = new GammaAgreementMeasureSupport();
        traits = sut.createTraits();
    }

    @Test
    public void multiLinkWithRoleLabelDifference() throws Exception
    {
        var result = multiLinkWithRoleLabelDifferenceTest(sut);

        // The link hosts are zero-width annotations which gamma cannot handle, so the study is
        // empty
        assertThat(result.getAgreement()).isNaN();
    }

    @Test
    public void twoEmptyCasTest() throws Exception
    {
        var result = twoEmptyCasTest(sut);

        assertThat(result.getAgreement()).isNaN();
    }

    @Test
    public void testTwoWithoutLabel_noExcludeIncomplete() throws Exception
    {
        traits.setExcludeIncomplete(false);

        var result = twoWithoutLabelTest(sut, traits);

        assertThat(result.getAgreement()).isCloseTo(0.3548, within(0.001));
    }

    @Test
    public void fullSingleCategoryAgreementWithTagsetTest() throws Exception
    {
        var result = fullSingleCategoryAgreementWithTagset(sut, traits);

        assertThat(result.getAgreement()).isCloseTo(1.0, within(0.01));
    }

    @Test
    public void multiValueStringPartialAgreementTest() throws Exception
    {
        var result = multiValueStringPartialAgreement(sut);

        assertThat(result.getAgreement()).isCloseTo(0.2214, within(0.001));
    }

    @Test
    public void selfOverlappingAgreementTest() throws Exception
    {
        var result = selfOverlappingAgreement(sut);

        assertThat(result.getAgreement()).isCloseTo(0.5788, within(0.001));
    }
}
