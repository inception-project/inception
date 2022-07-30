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

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.AbstractCodingAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.CodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

@Component
public class KrippendorffAlphaAgreementMeasureSupport
    extends AbstractCodingAgreementMeasureSupport<KrippendorffAlphaAgreementTraits>
{
    private final AnnotationSchemaService annotationService;

    public KrippendorffAlphaAgreementMeasureSupport(AnnotationSchemaService aAnnotationService)
    {
        super();
        annotationService = aAnnotationService;
    }

    @Override
    public String getName()
    {
        return "Krippendorff's Kappa (coding / nominal)";
    }

    @Override
    public AgreementMeasure<PairwiseAnnotationResult<CodingAgreementResult>> createMeasure(
            AnnotationFeature aFeature, KrippendorffAlphaAgreementTraits aTraits)
    {
        return new KrippendorffAlphaAgreementMeasure(aFeature, aTraits, annotationService);
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeature,
            IModel<KrippendorffAlphaAgreementTraits> aModel)
    {
        return new KrippendorffAlphaAgreementTraitsEditor(aId, aFeature, aModel);
    }

    @Override
    public KrippendorffAlphaAgreementTraits createTraits()
    {
        return new KrippendorffAlphaAgreementTraits();
    }
}
