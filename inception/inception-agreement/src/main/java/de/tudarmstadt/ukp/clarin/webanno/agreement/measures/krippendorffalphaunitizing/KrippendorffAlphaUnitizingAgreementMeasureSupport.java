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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalphaunitizing;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.dkpro.statistics.agreement.unitizing.IUnitizingAnnotationStudy;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupport_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing.FullUnitizingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing.PairwiseUnitizingAgreementTable;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@Component
public class KrippendorffAlphaUnitizingAgreementMeasureSupport
    extends AgreementMeasureSupport_ImplBase<//
            KrippendorffAlphaUnitizingAgreementTraits, //
            FullUnitizingAgreementResult, //
            IUnitizingAnnotationStudy>
{
    public KrippendorffAlphaUnitizingAgreementMeasureSupport(
            AnnotationSchemaService aAnnotationService)
    {
        super();
    }

    @Override
    public String getName()
    {
        return "Krippendorff's Alpha (unitizing / character offsets)";
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        var layer = aFeature.getLayer();

        if (SpanLayerSupport.TYPE.equals(layer.getType())) {
            return true;
        }

        return false;
    }

    @Override
    public AgreementMeasure<FullUnitizingAgreementResult> createMeasure(AnnotationFeature aFeature,
            KrippendorffAlphaUnitizingAgreementTraits aTraits)
    {
        return new KrippendorffAlphaUnitizingAgreementMeasure(aFeature, aTraits);
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeature,
            IModel<KrippendorffAlphaUnitizingAgreementTraits> aModel)
    {
        return new KrippendorffAlphaUnitizingAgreementTraitsEditor(aId, aFeature, aModel);
    }

    @Override
    public KrippendorffAlphaUnitizingAgreementTraits createTraits()
    {
        return new KrippendorffAlphaUnitizingAgreementTraits();
    }

    @Override
    public Panel createResultsPanel(String aId, IModel<PairwiseAnnotationResult> aResults)
    {
        return new PairwiseUnitizingAgreementTable(aId, aResults);
    }
}
