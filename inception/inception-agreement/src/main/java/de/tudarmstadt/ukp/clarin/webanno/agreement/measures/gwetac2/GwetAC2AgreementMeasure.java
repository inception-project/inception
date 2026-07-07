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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures.gwetac2;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.CodingStudyUtils.makeCodingStudy;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static java.lang.Double.NaN;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toCollection;

import java.util.LinkedHashSet;
import java.util.Map;

import java.util.stream.StreamSupport;

import org.apache.uima.cas.CAS;
import org.dkpro.statistics.agreement.InsufficientDataException;
import org.dkpro.statistics.agreement.coding.GwetAC2Agreement;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import org.dkpro.statistics.agreement.distance.NominalDistanceFunction;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.CodingAgreementMeasure_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class GwetAC2AgreementMeasure
    extends CodingAgreementMeasure_ImplBase<DefaultAgreementTraits>
{
    private final AnnotationSchemaService annotationService;
    private final DiffAdapterRegistry diffAdapterRegistry;

    public GwetAC2AgreementMeasure(AnnotationFeature aFeature, DefaultAgreementTraits aTraits,
            AnnotationSchemaService aAnnotationService, DiffAdapterRegistry aDiffAdapterRegistry)
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

        if (agreementResult.isEmpty()) {
            agreementResult.setAgreement(NaN);
        }
        else if (agreementResult.getObservedCategories().size() == 1
                && hasCoAnnotatedItem(agreementResult.getStudy())) {
            // AC2 requires at least two categories to estimate chance agreement and would otherwise
            // throw. A study in which the raters co-annotated at least one item and all picked the
            // same single category is perfect agreement by definition - mirror Cohen's/Fleiss'
            // kappa here. If no item was annotated by two or more raters, there is no agreement to
            // observe and we fall through to NaN below.
            agreementResult.setAgreement(1.0d);
        }
        else {
            try {
                var measure = new GwetAC2Agreement(agreementResult.getStudy(),
                        new NominalDistanceFunction());
                agreementResult.setAgreement(measure.calculateAgreement());
            }
            catch (InsufficientDataException e) {
                agreementResult.setAgreement(NaN);
            }
        }

        return agreementResult;
    }

    private static boolean hasCoAnnotatedItem(ICodingAnnotationStudy aStudy)
    {
        return StreamSupport.stream(aStudy.getItems().spliterator(), false)
                .anyMatch(item -> item.getRaterCount() >= 2);
    }
}
