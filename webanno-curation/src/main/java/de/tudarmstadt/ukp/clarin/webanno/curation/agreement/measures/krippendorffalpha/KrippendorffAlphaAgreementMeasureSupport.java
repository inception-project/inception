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
package de.tudarmstadt.ukp.clarin.webanno.curation.agreement.measures.krippendorffalpha;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.measures.AggreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.measures.AgreementMeasureSupport_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

@Component
public class KrippendorffAlphaAgreementMeasureSupport
    extends AgreementMeasureSupport_ImplBase<KrippendorffAlphaAgreementTraits>
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
        return "Krippendorff's Kappa (nominal)";
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        AnnotationLayer layer = aFeature.getLayer();
        
        if (
                SPAN_TYPE.equals(layer.getType()) && 
                layer.getAttachFeature() != null
        ) {
            return true;
        }
        
        return false;
    }

    @Override
    public AggreementMeasure createMeasure(AnnotationFeature aFeature,
            KrippendorffAlphaAgreementTraits aTraits)
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
