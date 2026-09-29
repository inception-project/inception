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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures.fleisskappa;

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

public class FleissKappaAgreementMeasureSupport
    extends AbstractCodingAgreementMeasureSupport<DefaultAgreementTraits>
{
    public static final String ID = "FleissKappa";

    private final AnnotationSchemaService annotationService;
    private final DiffAdapterRegistry diffAdapterRegistry;

    public FleissKappaAgreementMeasureSupport(AnnotationSchemaService aAnnotationService,
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
        return "Fleiss' Kappa (coding)";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of("Chance-corrected agreement on categorical labels generalised to any "
                + "number of raters. The multi-rater counterpart of Cohen's Kappa.");
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
        return new FleissKappaAgreementMeasure(aFeature, aTraits, annotationService,
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
                    "Compare against the raw observed agreement, and consider Gwet's AC2, which "
                            + "supports more than two raters and estimates chance agreement in a "
                            + "way that is robust to this kind of skew."));

        case MARGINAL_ASYMMETRY:
            return Optional.of(new AgreementConcern(aDiagnostic,
                    "The raters applied the labelling scheme at noticeably different rates. This "
                            + "measure pools the marginals across raters, so the difference in "
                            + "label usage counts as disagreement and lowers the score. Cohen's "
                            + "Kappa would report a higher score on the same data because it does "
                            + "not count this rater bias against the raters.",
                    "Check whether the annotation guidelines leave this label ambiguous."));

        default:
            return AgreementConcerns.generic(aDiagnostic);
        }
    }

    @Override
    public boolean isSupportingMoreThanTwoRaters()
    {
        return true;
    }
}
