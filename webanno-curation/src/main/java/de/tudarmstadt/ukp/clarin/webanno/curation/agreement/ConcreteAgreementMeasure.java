/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.curation.agreement;

import de.tudarmstadt.ukp.dkpro.statistics.agreement.IAgreementMeasure;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.CohenKappaAgreement;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.FleissKappaAgreement;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.KrippendorffAlphaAgreement;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.distance.NominalDistanceFunction;

public enum ConcreteAgreementMeasure
{
    COHEN_KAPPA_AGREEMENT(false),
    FLEISS_KAPPA_AGREEMENT(false), 
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
