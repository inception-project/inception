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
package de.tudarmstadt.ukp.inception.recommendation.api.recommender;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public interface RecommendationEngineFactory<T>
{
    String getId();

    String getName();

    /**
     * @return True if the recommender is deprecated, i.e. users should not create new recommenders
     *         based on this factory
     */
    boolean isDeprecated();

    default boolean isSynchronous(Recommender aRecommender)
    {
        return false;
    }

    default boolean isEvaluable()
    {
        return true;
    }

    /**
     * @return {@code true} if the recommender could generate more than one recommendation at a
     *         given position. If this is not the case (i.e. if {@code false} is returned, then
     *         there is no need to offer the option to cap the number of recommendations in the
     *         recommender settings.
     */
    default boolean isMultipleRecommendationProvider()
    {
        return true;
    }

    default boolean isModelExportSupported()
    {
        return false;
    }

    default String getExportModelName(Recommender aRecommender)
    {
        throw new UnsupportedOperationException("Model export not supported");
    }

    RecommendationEngine build(Recommender aRecommender);

    default boolean accepts(Recommender aRecommender)
    {
        if (aRecommender == null) {
            return false;
        }

        return accepts(aRecommender.getLayer()) && accepts(aRecommender.getFeature());
    }

    /**
     * @return whether a recommender may be applicable to the given layer. The recommender may still
     *         reject if an unacceptable feature is provided, so {@link #accepts(AnnotationFeature)}
     *         should also be used.
     */
    boolean accepts(AnnotationLayer aLayer);

    /**
     * @return whether a recommender may be applicable to the given feature.
     */
    boolean accepts(AnnotationFeature aFeature);

    T createTraits();

    AbstractTraitsEditor createTraitsEditor(String aId, IModel<Recommender> aModel);

    T readTraits(Recommender aRecommender);

    void writeTraits(Recommender aRecommender, T aTraits);

    /**
     * The task of a ranker is to provide ordered suggestions instead of scored suggestions. While a
     * ranker will usually set the score property of a suggestion, that score should not be
     * displayed prominently in the user interface.
     * 
     * @return {@code true} if the recommender is a ranker
     */
    default boolean isRanker(Recommender aRecommender)
    {
        return false;
    }

    default boolean isInteractive(Recommender aRecommender)
    {
        return false;
    }

    default Panel createInteractionPanel(String aId, IModel<Recommender> aModel)
    {
        return new EmptyPanel(aId);
    }
}
