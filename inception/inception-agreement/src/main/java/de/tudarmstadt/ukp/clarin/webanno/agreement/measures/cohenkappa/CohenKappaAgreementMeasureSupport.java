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

import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureCapability.CHANCE_CORRECTED;
import static java.util.EnumSet.of;

import java.util.Optional;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementConcern;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementConcerns;
import de.tudarmstadt.ukp.clarin.webanno.agreement.diagnostics.AgreementDiagnostic;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureCapability;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.AbstractCodingAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class CohenKappaAgreementMeasureSupport
    extends AbstractCodingAgreementMeasureSupport<DefaultAgreementTraits>
{
    public static final String ID = "CohenKappa";

    private final AnnotationSchemaService annotationService;
    private final DiffAdapterRegistry diffAdapterRegistry;

    public CohenKappaAgreementMeasureSupport(AnnotationSchemaService aAnnotationService,
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
        return "Cohen's Kappa (coding)";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("Chance-corrected agreement between exactly two raters on categorical "
                + "labels. The classic choice for two-annotator studies.");
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
        return new CohenKappaAgreementMeasure(aFeature, aTraits, annotationService,
                diffAdapterRegistry);
    }

    @Override
    public Optional<AgreementConcern> getConcern(AgreementDiagnostic aDiagnostic)
    {
        switch (aDiagnostic.getType()) {
        case SKEWED_PREVALENCE:
            return Optional.of(new AgreementConcern(aDiagnostic,
                    "When one label dominates, the agreement expected by chance approaches the "
                            + "agreement actually observed. Kappa divides by the difference between "
                            + "the two, so it can report a low score even though the raters agree on "
                            + "almost every item - the so-called Kappa paradox.",
                    "Compare against the raw observed agreement, and consider Gwet's AC1, which "
                            + "estimates chance agreement in a way that is robust to this kind of "
                            + "skew."));

        case MARGINAL_ASYMMETRY:
            return Optional.of(new AgreementConcern(aDiagnostic,
                    "Cohen's Kappa derives expected agreement from each rater's individual label "
                            + "distribution. When raters apply the scheme at different rates, this "
                            + "lowers the agreement expected by chance, so Kappa does not count "
                            + "the difference as disagreement and can report a higher score than "
                            + "measures that pool the marginals.",
                    "Compare against Fleiss' Kappa or Krippendorff's Alpha, which pool the "
                            + "marginals across raters and count the difference in label usage as "
                            + "disagreement. A clear gap between the scores points to systematic "
                            + "rater bias - check whether the annotation guidelines leave this "
                            + "label ambiguous."));

        default:
            return AgreementConcerns.generic(aDiagnostic);
        }
    }

    @Override
    public boolean isSupportingMoreThanTwoRaters()
    {
        return false;
    }
}
