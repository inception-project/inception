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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalpha;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.CodingStudyUtils.makeCodingStudy;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static java.lang.Double.NaN;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toCollection;

import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.dkpro.statistics.agreement.InsufficientDataException;
import org.dkpro.statistics.agreement.coding.KrippendorffAlphaAgreement;
import org.dkpro.statistics.agreement.distance.NominalDistanceFunction;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.CodingAgreementMeasure_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class KrippendorffAlphaAgreementMeasure
    extends CodingAgreementMeasure_ImplBase<DefaultAgreementTraits>
{
    private final AnnotationSchemaService annotationService;
    private final DiffAdapterRegistry diffAdapterRegistry;

    public KrippendorffAlphaAgreementMeasure(AnnotationFeature aFeature,
            DefaultAgreementTraits aTraits, AnnotationSchemaService aAnnotationService,
            DiffAdapterRegistry aDiffAdapterRegistry)
    {
        super(aFeature, aTraits);
        annotationService = aAnnotationService;
        diffAdapterRegistry = aDiffAdapterRegistry;
    }

    @Override
    public FullCodingAgreementResult getAgreement(Map<String, CAS> aCasMap)
    {
        var feature = getFeature();
        var traits = getTraits();

        var adapters = diffAdapterRegistry.getDiffAdapters(asList(feature.getLayer()));

        var diff = doDiff(adapters, aCasMap);

        var tagset = annotationService.listTags(feature.getTagset()).stream() //
                .map(Tag::getName) //
                .collect(toCollection(LinkedHashSet::new));

        var agreementResult = makeCodingStudy(diff, feature.getLayer().getName(), feature.getName(),
                tagset, traits.isExcludeIncomplete(), aCasMap);

        var measure = createMeasure(agreementResult);

        if (agreementResult.isEmpty()) {
            agreementResult.setAgreement(NaN);
        }
        else {
            try {
                agreementResult.setAgreement(measure.calculateAgreement());
            }
            catch (InsufficientDataException e) {
                agreementResult.setAgreement(NaN);
            }
        }

        return agreementResult;
    }

    private KrippendorffAlphaAgreement createMeasure(FullCodingAgreementResult agreementResult)
    {
        return new KrippendorffAlphaAgreement(agreementResult.getStudy(),
                new NominalDistanceFunction())
        {
            @Override
            public double calculateAgreement()
            {
                // See https://github.com/dkpro/dkpro-statistics/issues/35
                double D_O = calculateObservedDisagreement();
                double D_E = calculateExpectedDisagreement();
                if (D_O == 0.0 && D_E == 0.0) {
                    if (study.getItemCount() == 0) {
                        return Double.NaN;
                    }
                    else {
                        return 1.0;
                    }
                }
                return 1.0 - (D_O / D_E);
            }
        };
    }
}
