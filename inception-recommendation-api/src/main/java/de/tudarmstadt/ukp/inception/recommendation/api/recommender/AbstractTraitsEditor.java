package de.tudarmstadt.ukp.inception.recommendation.api.recommender;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

/*
 * Copyright 2019
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
public class AbstractTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = -5826029092354401342L;

    public AbstractTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);
    }
    
    public Recommender getModelObject()
    {
        return (Recommender) getDefaultModelObject();
    }
}
