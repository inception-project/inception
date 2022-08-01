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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.danekja.java.util.function.serializable.SerializableSupplier;
import org.dkpro.statistics.agreement.IAnnotationStudy;
import org.springframework.beans.factory.BeanNameAware;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public interface AgreementMeasureSupport<//
        T extends DefaultAgreementTraits, //
        R extends Serializable, //
        S extends IAnnotationStudy>
    extends BeanNameAware
{
    String getId();

    String getName();

    /**
     * Checks whether the given feature is supported by the current agreement measure support.
     * 
     * @param aFeature
     *            a feature definition.
     * @return whether the given feature is supported by the current agreement measure support.
     */
    boolean accepts(AnnotationFeature aFeature);

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
    default Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeature,
            IModel<T> aModel)
    {
        return new EmptyPanel(aId);
    }

    AgreementMeasure<R> createMeasure(AnnotationFeature aFeature, T aTraits);

    T createTraits();

    Panel createResultsPanel(String aId, IModel<R> aResults,
            SerializableSupplier<Map<String, List<CAS>>> aCasMapSupplier);
}
