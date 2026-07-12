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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures.gwetac1;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureCapability.CHANCE_CORRECTED;
import static java.util.EnumSet.of;

import java.util.Optional;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureCapability;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.AbstractCodingAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class GwetAC1AgreementMeasureSupport
    extends AbstractCodingAgreementMeasureSupport<DefaultAgreementTraits>
{
    public static final String ID = "GwetAC1";

    private final AnnotationSchemaService annotationService;
    private final DiffAdapterRegistry diffAdapterRegistry;

    public GwetAC1AgreementMeasureSupport(AnnotationSchemaService aAnnotationService,
            DiffAdapterRegistry aDiffAdapterRegistry)
    {
        annotationService = aAnnotationService;
        diffAdapterRegistry = aDiffAdapterRegistry;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Gwet's AC1 (coding)";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("Chance-corrected agreement designed to be more robust than Kappa when "
                + "labels are highly imbalanced, avoiding the paradox of low scores despite high "
                + "observed agreement.");
    }

    @Override
    public Set<AgreementMeasureCapability> getDeclaredCapabilities()
    {
        return of(CHANCE_CORRECTED);
    }

    @Override
    public AgreementMeasure<FullCodingAgreementResult> createMeasure(AnnotationLayer aLayer,
            AnnotationFeature aFeature, DefaultAgreementTraits aTraits)
    {
        return new GwetAC1AgreementMeasure(aFeature, aTraits, annotationService,
                diffAdapterRegistry);
    }

    @Override
    public boolean isSupportingMoreThanTwoRaters()
    {
        // Gwet's AC1 does not implement DKPro's IMultiRaterAgreement, so the underlying
        // GwetAC1Agreement throws for studies with more than two raters. Unlike its weighted
        // extension AC2, AC1 is pairwise-only.
        return false;
    }
}
