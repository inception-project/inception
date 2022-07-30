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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures.cohenkappa;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementUtils.makeCodingStudy;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.getDiffAdapters;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toCollection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.dkpro.statistics.agreement.IAgreementMeasure;
import org.dkpro.statistics.agreement.coding.CohenKappaAgreement;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.CodingAgreementMeasure_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.CodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

public class CohenKappaAgreementMeasure
    extends CodingAgreementMeasure_ImplBase<DefaultAgreementTraits>
{
    private final AnnotationSchemaService annotationService;

    public CohenKappaAgreementMeasure(AnnotationFeature aFeature, DefaultAgreementTraits aTraits,
            AnnotationSchemaService aAnnotationService)
    {
        super(aFeature, aTraits);
        annotationService = aAnnotationService;
    }

    @Override
    public CodingAgreementResult calculatePairAgreement(Map<String, List<CAS>> aCasMap)
    {
        AnnotationFeature feature = getFeature();
        DefaultAgreementTraits traits = getTraits();

        List<DiffAdapter> adapters = getDiffAdapters(annotationService, asList(feature.getLayer()));

        CasDiff diff = doDiff(adapters, traits.getLinkCompareBehavior(), aCasMap);

        Set<String> tagset = annotationService.listTags(feature.getTagset()).stream()
                .map(Tag::getName).collect(toCollection(LinkedHashSet::new));

        CodingAgreementResult agreementResult = makeCodingStudy(diff, feature.getLayer().getName(),
                feature.getName(), tagset, true, aCasMap);

        IAgreementMeasure agreement = new CohenKappaAgreement(agreementResult.getStudy());

        if (agreementResult.getStudy().getItemCount() == 0) {
            agreementResult.setAgreement(Double.NaN);
        }
        else if (agreementResult.getObservedCategories().size() == 1) {
            agreementResult.setAgreement(1.0d);
        }
        else {
            agreementResult.setAgreement(agreement.calculateAgreement());
        }

        return agreementResult;
    }
}
