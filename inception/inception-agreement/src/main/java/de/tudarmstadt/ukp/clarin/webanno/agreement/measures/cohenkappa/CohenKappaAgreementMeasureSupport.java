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

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.AbstractCodingAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.CodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

@Component
public class CohenKappaAgreementMeasureSupport
    extends AbstractCodingAgreementMeasureSupport<DefaultAgreementTraits>
{
    private final AnnotationSchemaService annotationService;

    public CohenKappaAgreementMeasureSupport(AnnotationSchemaService aAnnotationService)
    {
        super();
        annotationService = aAnnotationService;
    }

    @Override
    public String getName()
    {
        return "Cohen's Kappa (coding)";
    }

    @Override
    public AgreementMeasure<PairwiseAnnotationResult<CodingAgreementResult>> createMeasure(
            AnnotationFeature aFeature, DefaultAgreementTraits aTraits)
    {
        return new CohenKappaAgreementMeasure(aFeature, aTraits, annotationService);
    }
}
