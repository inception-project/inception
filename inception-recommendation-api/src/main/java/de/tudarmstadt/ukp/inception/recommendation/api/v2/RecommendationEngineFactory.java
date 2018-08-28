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
package de.tudarmstadt.ukp.inception.recommendation.api.v2;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public interface RecommendationEngineFactory
{
    String getId();

    String getName();
    
    RecommendationEngine build(Recommender aRecommender);

    boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature);

    default Component createTraitsEditor(String aId, IModel<Recommender> aModel) {
        return new EmptyPanel(aId);
    }

    /**
     * @return True if the recommender is deprecated, i.e. users should not create new recommenders
     *         based on this factory
     */
    default boolean isDeprecated()
    {
        return false;
    }
}
