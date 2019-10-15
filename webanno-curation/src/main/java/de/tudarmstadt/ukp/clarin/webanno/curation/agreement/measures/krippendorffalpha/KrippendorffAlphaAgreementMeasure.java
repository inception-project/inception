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
package de.tudarmstadt.ukp.clarin.webanno.curation.agreement.measures.krippendorffalpha;

import static de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils.makeCodingStudy;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.getAdapters;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.dkpro.statistics.agreement.IAgreementMeasure;
import org.dkpro.statistics.agreement.InsufficientDataException;
import org.dkpro.statistics.agreement.coding.KrippendorffAlphaAgreement;
import org.dkpro.statistics.agreement.distance.NominalDistanceFunction;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.results.coding.CodingAggreementMeasure_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.results.coding.CodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public class KrippendorffAlphaAgreementMeasure
    extends CodingAggreementMeasure_ImplBase<KrippendorffAlphaAgreementTraits>
{
    private final AnnotationSchemaService annotationService;

    public KrippendorffAlphaAgreementMeasure(AnnotationFeature aFeature,
            KrippendorffAlphaAgreementTraits aTraits, AnnotationSchemaService aAnnotationService)
    {
        super(aFeature, aTraits);
        annotationService = aAnnotationService;
    }

    @Override
    public CodingAgreementResult calculatePairAgreement(
            Map<String, List<CAS>> aCasMap)
    {
        AnnotationFeature feature = getFeature();
        KrippendorffAlphaAgreementTraits traits = getTraits();
        
        List<DiffAdapter> adapters = getAdapters(annotationService, feature.getProject());

        CasDiff diff = doDiff(asList(feature.getLayer().getName()), adapters,
                traits.getLinkCompareBehavior(), aCasMap);

        CodingAgreementResult agreementResult = makeCodingStudy(diff,
                feature.getLayer().getName(), feature.getName(), traits.isExcludeIncomplete(),
                aCasMap);

        IAgreementMeasure agreement = new KrippendorffAlphaAgreement(agreementResult.getStudy(),
                new NominalDistanceFunction());

        if (agreementResult.getStudy().getItemCount() > 0) {
            try {
                agreementResult.setAgreement(agreement.calculateAgreement());
            }
            catch (InsufficientDataException e) {
                agreementResult.setAgreement(Double.NaN);
            }
        }
        else {
            agreementResult.setAgreement(Double.NaN);
        }

        return agreementResult;
    }
}
