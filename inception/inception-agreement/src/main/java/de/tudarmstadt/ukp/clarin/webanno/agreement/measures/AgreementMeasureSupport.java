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

import static java.util.Collections.emptySet;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

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
     * @return a human-readable description of the measure, shown in the measure selection UI. The
     *         default is empty, so measures that do not provide one are simply shown without a
     *         description.
     */
    default Optional<String> getDescription()
    {
        return Optional.empty();
    }

    /**
     * @return the paradigm the measure operates in (coding / unitizing / aligning). This is the
     *         primary axis for grouping measures in the selection UI. The default is
     *         {@link AgreementMeasureParadigm#CODING}, which is the most common case.
     */
    default AgreementMeasureParadigm getParadigm()
    {
        return AgreementMeasureParadigm.CODING;
    }

    /**
     * @return the full set of capabilities (chance-corrected, multi-rater, weighted, ...) to render
     *         as badges/filters in the selection UI. This combines the capabilities a measure
     *         declares via {@link #getDeclaredCapabilities()} with
     *         {@link AgreementMeasureCapability#MULTI_RATER}, which is derived from
     *         {@link #isSupportingMoreThanTwoRaters()} so it has a single source of truth.
     *         Implementations should override {@link #getDeclaredCapabilities()} rather than this
     *         method.
     */
    default Set<AgreementMeasureCapability> getCapabilities()
    {
        var capabilities = EnumSet.noneOf(AgreementMeasureCapability.class);
        capabilities.addAll(getDeclaredCapabilities());
        if (isSupportingMoreThanTwoRaters()) {
            capabilities.add(AgreementMeasureCapability.MULTI_RATER);
        }
        return capabilities;
    }

    /**
     * @return the capabilities a measure declares directly. Override this to advertise capabilities
     *         other than {@link AgreementMeasureCapability#MULTI_RATER} (which is derived from
     *         {@link #isSupportingMoreThanTwoRaters()}). The default is empty.
     */
    default Set<AgreementMeasureCapability> getDeclaredCapabilities()
    {
        return emptySet();
    }

    /**
     * Checks whether the given layer/feature is supported by this measure. By default this is
     * derived from {@link #getInapplicabilityReason(AnnotationLayer, AnnotationFeature)} so that
     * acceptance and the explanation shown to the user can never disagree. Implementations should
     * override {@link #getInapplicabilityReason} rather than this method.
     *
     * @param aLayer
     *            a layer definition.
     * @param aFeature
     *            a feature definition.
     * @return whether the given feature is supported by the current agreement measure support.
     */
    default boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        return getInapplicabilityReason(aLayer, aFeature).isEmpty();
    }

    /**
     * Explains why this measure cannot be applied to the given layer/feature, for display in the
     * measure selection UI. Returns an empty {@link Optional} when the measure <em>is</em>
     * applicable. This is the single source of truth for applicability: {@link #accepts} is derived
     * from it.
     *
     * @param aLayer
     *            a layer definition.
     * @param aFeature
     *            a feature definition (may be null for position-based measures).
     * @return the reason the measure is not applicable, or empty if it is applicable.
     */
    default Optional<String> getInapplicabilityReason(AnnotationLayer aLayer,
            AnnotationFeature aFeature)
    {
        return Optional.empty();
    }

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
