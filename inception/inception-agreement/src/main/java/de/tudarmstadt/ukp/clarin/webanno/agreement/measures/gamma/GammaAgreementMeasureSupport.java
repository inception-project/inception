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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures.gamma;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.dkpro.statistics.agreement.aligning.AligningAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementResult_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.PerDocumentAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupport_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.aligning.FullAligningAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.perdoc.PerDocumentAgreementTable;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing.PairwiseUnitizingAgreementTable;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;

public class GammaAgreementMeasureSupport
    extends AgreementMeasureSupport_ImplBase<//
            DefaultAgreementTraits, //
            FullAligningAgreementResult, //
            AligningAnnotationStudy>
{
    public static final String ID = "Gamma";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Gamma (aligning / character offsets)";
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        return SpanLayerSupport.TYPE.equals(aLayer.getType());
    }

    @Override
    public AgreementMeasure<FullAligningAgreementResult> createMeasure(AnnotationLayer aLayer,
            AnnotationFeature aFeature, DefaultAgreementTraits aTraits)
    {
        return new GammaAgreementMeasure(aLayer, aFeature, aTraits);
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
