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

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.dkpro.statistics.agreement.unitizing.IUnitizingAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementResult_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.PerDocumentAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupport_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.perdoc.PerDocumentAgreementTable;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing.FullUnitizingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing.PairwiseUnitizingAgreementTable;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;

public class KrippendorffAlphaUnitizingAgreementMeasureSupport
    extends AgreementMeasureSupport_ImplBase<//
            DefaultAgreementTraits, //
            FullUnitizingAgreementResult, //
            IUnitizingAnnotationStudy>
{
    public static final String ID = "KrippendorffAlphaUnitizing";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Krippendorff's Alpha (unitizing / character offsets)";
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        if (SpanLayerSupport.TYPE.equals(aLayer.getType())) {
            return true;
        }

        return false;
    }

    @Override
    public AgreementMeasure<FullUnitizingAgreementResult> createMeasure(AnnotationLayer aLayer,
            AnnotationFeature aFeature, DefaultAgreementTraits aTraits)
    {
        return new KrippendorffAlphaUnitizingAgreementMeasure(aLayer, aFeature, aTraits);
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationLayer> aLayer,
            IModel<AnnotationFeature> aFeature, IModel<DefaultAgreementTraits> aModel)
    {
        return new KrippendorffAlphaUnitizingAgreementTraitsEditor(aId, aFeature, aModel);
    }

    @Override
    public DefaultAgreementTraits createTraits()
    {
        return new DefaultAgreementTraits();
    }

    @Override
    public Panel createResultsPanel(String aId, IModel<? extends AgreementResult_ImplBase> aResults,
            DefaultAgreementTraits aTraits)
    {
        if (aResults.getObject() instanceof PairwiseAgreementResult) {
            return new PairwiseUnitizingAgreementTable(aId, (IModel) aResults, aTraits);
        }

        if (aResults.getObject() instanceof PerDocumentAgreementResult) {
            return new PerDocumentAgreementTable(aId, (IModel) aResults, aTraits);
        }

        return new EmptyPanel(aId);
    }

    @Override
    public boolean isSupportingMoreThanTwoRaters()
    {
        return true;
    }
}
