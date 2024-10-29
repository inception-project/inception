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

import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.AbstractCodingAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class KrippendorffAlphaAgreementMeasureSupport
    extends AbstractCodingAgreementMeasureSupport<DefaultAgreementTraits>
{
    public static final String ID = "KrippendorffAlpha";

    private final AnnotationSchemaService annotationService;

    public KrippendorffAlphaAgreementMeasureSupport(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Krippendorff's Alpha (coding / nominal)";
    }

    @Override
    public AgreementMeasure<FullCodingAgreementResult> createMeasure(AnnotationFeature aFeature,
            DefaultAgreementTraits aTraits)
    {
        return new KrippendorffAlphaAgreementMeasure(aFeature, aTraits, annotationService);
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeature,
            IModel<DefaultAgreementTraits> aModel)
    {
        return new KrippendorffAlphaAgreementTraitsEditor(aId, aFeature, aModel);
    }

    @Override
    public DefaultAgreementTraits createTraits()
    {
        return new DefaultAgreementTraits();
    }

    @Override
    public boolean isSupportingMoreThanTwoRaters()
    {
        return true;
    }
}
