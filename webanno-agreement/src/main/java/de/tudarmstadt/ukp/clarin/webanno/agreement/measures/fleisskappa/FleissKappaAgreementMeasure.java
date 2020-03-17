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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures.fleisskappa;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementUtils.makeCodingStudy;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.getDiffAdapters;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.dkpro.statistics.agreement.IAgreementMeasure;
import org.dkpro.statistics.agreement.coding.FleissKappaAgreement;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.CodingAggreementMeasure_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.CodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public class FleissKappaAgreementMeasure
    extends CodingAggreementMeasure_ImplBase<DefaultAgreementTraits>
{
    private final AnnotationSchemaService annotationService;
    
    public FleissKappaAgreementMeasure(AnnotationFeature aFeature,
            DefaultAgreementTraits aTraits, AnnotationSchemaService aAnnotationService)
    {
        super(aFeature, aTraits);
        annotationService = aAnnotationService;
    }

    @Override
    public CodingAgreementResult calculatePairAgreement(
            Map<String, List<CAS>> aCasMap)
    {
        AnnotationFeature feature = getFeature();
        DefaultAgreementTraits traits = getTraits();
        
        List<DiffAdapter> adapters = getDiffAdapters(annotationService, asList(feature.getLayer()));

        CasDiff diff = doDiff(adapters, traits.getLinkCompareBehavior(), aCasMap);

        CodingAgreementResult agreementResult = makeCodingStudy(diff,
                feature.getLayer().getName(), feature.getName(), true, aCasMap);

        IAgreementMeasure agreement = new FleissKappaAgreement(agreementResult.getStudy());

        if (agreementResult.getStudy().getItemCount() > 0) {
            agreementResult.setAgreement(agreement.calculateAgreement());
        }
        else {
            agreementResult.setAgreement(Double.NaN);
        }

        return agreementResult;
    }
}
