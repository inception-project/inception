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
package de.tudarmstadt.ukp.clarin.webanno.curation.agreement.measures.fleisskappa;

import static de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils.makeStudy;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.getAdapters;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.measures.AggreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.IAgreementMeasure;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.coding.FleissKappaAgreement;

public class FleissKappaAgreementMeasure
    implements AggreementMeasure
{
    private final AnnotationFeature feature;
    private final DefaultAgreementTraits traits;
    private final AnnotationSchemaService annotationService;
    
    public FleissKappaAgreementMeasure(AnnotationFeature aFeature,
            DefaultAgreementTraits aTraits, AnnotationSchemaService aAnnotationService)
    {
        feature = aFeature;
        traits = aTraits;
        annotationService = aAnnotationService;
    }

    @Override
    public AgreementResult getAgreement(Map<String, List<CAS>> aCasMap)
    {
        List<DiffAdapter> adapters = getAdapters(annotationService, feature.getProject());

        DiffResult diff = doDiff(asList(feature.getLayer().getName()), adapters,
                traits.getLinkCompareBehavior(), aCasMap);

        AgreementResult agreementResult = makeStudy(diff, feature.getLayer().getName(),
                feature.getName(), true, aCasMap);

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
