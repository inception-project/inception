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
package de.tudarmstadt.ukp.clarin.webanno.agreement.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class AgreementDiagnosticsPropertiesImplTest
{
    @Test
    void thatSharesOutsideTheUnitIntervalAreRejected()
    {
        var properties = new AgreementDiagnosticsPropertiesImpl();

        assertThatIllegalArgumentException().isThrownBy(() -> properties.setSkewThreshold(-0.1))
                .withMessageContaining("agreement.diagnostics.skew-threshold");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> properties.setMarginalAsymmetryThreshold(1.5));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> properties.setBoundaryDisagreementThreshold(Double.NaN));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> properties.setUnusableDocumentThreshold(-1));
    }

    @Test
    void thatSharesAtTheBoundsAreAccepted()
    {
        var properties = new AgreementDiagnosticsPropertiesImpl();

        properties.setSkewThreshold(1.0);
        properties.setMarginalAsymmetryThreshold(0.0);

        assertThat(properties.getSkewThreshold()).isEqualTo(1.0);
        assertThat(properties.getMarginalAsymmetryThreshold()).isZero();
    }

    @Test
    void thatCountsBelowTheirMinimumAreRejected()
    {
        var properties = new AgreementDiagnosticsPropertiesImpl();

        assertThatIllegalArgumentException().isThrownBy(() -> properties.setMinimumBasisForShare(0))
                .withMessageContaining("agreement.diagnostics.minimum-basis-for-share");
        assertThatIllegalArgumentException().isThrownBy(() -> properties.setFewItemsLimit(-1));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> properties.setSparseCategoryLimit(-1));

        properties.setFewItemsLimit(0);
        assertThat(properties.getFewItemsLimit()).isZero();
    }
}
