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

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.dkpro.statistics.agreement.IAnnotationStudy;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementResult_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.FullAgreementResult_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public interface AgreementMeasureSupport<//
        T extends DefaultAgreementTraits, //
        R extends FullAgreementResult_ImplBase<S>, //
        S extends IAnnotationStudy>
{
    String getId();

    String getName();

    /**
     * Checks whether the given feature is supported by the current agreement measure support.
     * 
     * @param aLayer
     *            a layer definition.
     * @param aFeature
     *            a feature definition.
     * @return whether the given feature is supported by the current agreement measure support.
     */
    boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature);

    /**
     * Returns a Wicket component to configure the specific traits of this measure.
     * 
     * @param aId
     *            a markup ID.
     * @param aFeature
     *            the feature which the agreement is configured to operate on.
     * @param aModel
     *            a model holding the measure settings.
     * @return the traits editor component .
     */
    default Panel createTraitsEditor(String aId, IModel<AnnotationLayer> aLayer,
            IModel<AnnotationFeature> aFeature, IModel<T> aModel)
    {
        return new EmptyPanel(aId);
    }

    default Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeature,
            IModel<T> aModel)
    {
        return createTraitsEditor(aId, aFeature.map(AnnotationFeature::getLayer), aFeature, aModel);
    }

    default AgreementMeasure<R> createMeasure(AnnotationFeature aFeature, T aTraits)
    {
        return createMeasure(aFeature.getLayer(), aFeature, aTraits);
    }

    AgreementMeasure<R> createMeasure(AnnotationLayer aLayer, AnnotationFeature aFeature,
            T aTraits);

    T createTraits();

    Panel createResultsPanel(String aId, IModel<? extends AgreementResult_ImplBase> aResults,
            DefaultAgreementTraits aDefaultAgreementTraits);

    boolean isSupportingMoreThanTwoRaters();
}
