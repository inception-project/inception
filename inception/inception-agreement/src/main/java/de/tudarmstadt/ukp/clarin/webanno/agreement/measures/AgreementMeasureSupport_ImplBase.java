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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.dkpro.statistics.agreement.IAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.agreement.FullAgreementResult_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public abstract class AgreementMeasureSupport_ImplBase<//
        T extends DefaultAgreementTraits, //
        R extends FullAgreementResult_ImplBase<S>, //
        S extends IAnnotationStudy>
    implements AgreementMeasureSupport<T, R, S>
{
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationLayer> aLayer,
            IModel<AnnotationFeature> aFeature, IModel<T> aModel)
    {
        return new DefaultAgreementTraitsEditor<DefaultAgreementTraits>(aId, aFeature,
                (IModel) aModel);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T createTraits()
    {
        return (T) new DefaultAgreementTraits();
    }
}
