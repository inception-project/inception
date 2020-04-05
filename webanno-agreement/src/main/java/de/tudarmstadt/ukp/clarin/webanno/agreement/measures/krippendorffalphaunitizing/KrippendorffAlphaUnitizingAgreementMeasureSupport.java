/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalphaunitizing;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;

import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.danekja.java.util.function.serializable.SerializableSupplier;
import org.dkpro.statistics.agreement.unitizing.IUnitizingAnnotationStudy;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AggreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupport_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing.PairwiseUnitizingAgreementTable;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.unitizing.UnitizingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

@Component
public class KrippendorffAlphaUnitizingAgreementMeasureSupport
    extends AgreementMeasureSupport_ImplBase<
            KrippendorffAlphaUnitizingAgreementTraits, 
            PairwiseAnnotationResult<UnitizingAgreementResult>, 
            IUnitizingAnnotationStudy>
{
    private final AnnotationSchemaService annotationService;
    
    public KrippendorffAlphaUnitizingAgreementMeasureSupport(
            AnnotationSchemaService aAnnotationService)
    {
        super();
        annotationService = aAnnotationService;
    }
    
    @Override
    public String getName()
    {
        return "Krippendorff's Alpha (unitizing / character offsets)";
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        AnnotationLayer layer = aFeature.getLayer();
        
        if (SPAN_TYPE.equals(layer.getType())) {
            return true;
        }
        
        return false;
    }

    @Override
    public AggreementMeasure<PairwiseAnnotationResult<UnitizingAgreementResult>> createMeasure(
            AnnotationFeature aFeature, KrippendorffAlphaUnitizingAgreementTraits aTraits)
    {
        return new KrippendorffAlphaUnitizingAgreementMeasure(aFeature, aTraits, annotationService);
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
    public Panel createResultsPanel(String aId,
            IModel<PairwiseAnnotationResult<UnitizingAgreementResult>> aResults,
            SerializableSupplier<Map<String, List<CAS>>> aCasMapSupplier)
    {
        return new PairwiseUnitizingAgreementTable(aId, aResults);
    }
}
