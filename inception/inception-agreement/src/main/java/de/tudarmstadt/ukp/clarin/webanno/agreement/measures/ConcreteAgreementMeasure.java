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

import org.dkpro.statistics.agreement.IAgreementMeasure;
import org.dkpro.statistics.agreement.coding.CohenKappaAgreement;
import org.dkpro.statistics.agreement.coding.FleissKappaAgreement;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import org.dkpro.statistics.agreement.coding.KrippendorffAlphaAgreement;
import org.dkpro.statistics.agreement.distance.NominalDistanceFunction;

/**
 * @deprecated No longer to be used and going to be removed soon. This is replaced by the pluggable
 *             mechanism provided by {@link AgreementMeasureSupport}.
 */
@Deprecated
public enum ConcreteAgreementMeasure
{
    COHEN_KAPPA_AGREEMENT(false), FLEISS_KAPPA_AGREEMENT(false),
    KRIPPENDORFF_ALPHA_NOMINAL_AGREEMENT(true);

    private final boolean nullValueSupported;

    ConcreteAgreementMeasure(boolean aNullValueSupported)
    {
        nullValueSupported = aNullValueSupported;
    }

    public IAgreementMeasure make(ICodingAnnotationStudy aStudy)
    {
        switch (this) {
        case COHEN_KAPPA_AGREEMENT:
            return new CohenKappaAgreement(aStudy);
        case FLEISS_KAPPA_AGREEMENT:
            return new FleissKappaAgreement(aStudy);
        case KRIPPENDORFF_ALPHA_NOMINAL_AGREEMENT:
            return new KrippendorffAlphaAgreement(aStudy, new NominalDistanceFunction());
        default:
            throw new IllegalArgumentException();
        }
    }

    public boolean isNullValueSupported()
    {
        return nullValueSupported;
    }
}
