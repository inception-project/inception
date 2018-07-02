/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.api;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public interface ClassificationToolFactory<C, T>
{
    /**
     * Get the ID of the classification tool.
     */
    String getId();
    
    /**
     * Get the name of the classification tool (a human readable name).
     */
    String getName();
    
    ClassificationTool<C> createTool(long aRecommenderId, String aFeature, AnnotationLayer aLayer,
        int aMaxPredictions);

    boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature);
    
    /**
     * Returns a Wicket component to configure the specific traits of this classifier. Note that
     * every {@link ClassificationToolFactory} has to return a <b>different class</b> here. So it is
     * not possible to simple return a Wicket {@link Panel} here, but it must be a subclass of
     * {@link Panel} used exclusively by the current {@link ClassificationToolFactory}. If this is
     * not done, then the traits editor in the UI will not be correctly updated when switching
     * between feature types!
     * 
     * @param aId
     *            a markup ID.
     * @param aRecommender
     *            a model holding the recommender for which the traits editor should be created.
     * @return the traits editor component .
     */
    default Panel createTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        return new EmptyPanel(aId);
    }
    
    /**
     * Read the traits for the given {@link Recommender}. If traits are supported, then this method
     * must be overwritten. A typical implementation would read the traits from a JSON string stored
     * in {@link Recommender#getTraits}, but it would also possible to load the traits from a
     * database table.
     * 
     * @param aRecommender
     *            the recommender whose traits should be obtained.
     * @return the traits.
     */
    default T readTraits(Recommender aRecommender)
    {
        return null;
    }

    /**
     * Write the traits for the given {@link Recommender}. If traits are supported, then this method
     * must be overwritten. A typical implementation would write the traits from to JSON string
     * stored in {@link Recommender#setTraits}, but it would also possible to store the traits from
     * a database table.
     * 
     * @param aRecommender
     *            the recommender whose traits should be written.
     * @param aTraits
     *            the traits.
     */
    default void writeTraits(Recommender aRecommender, T aTraits)
    {
        aRecommender.setTraits(null);
    }

    default boolean isSupported(AnnotationFeature aFeature){
        return true;
    }
}
