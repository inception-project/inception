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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama;

import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;

import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaClient;

public class OllamaRecommenderFactory
    extends RecommendationEngineFactoryImplBase<OllamaRecommenderTraits>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = "de.tudarmstadt.ukp.inception.recommendation.imls.ollama.OllamaRecommenderFactory";

    private final OllamaClient client;

    public OllamaRecommenderFactory(OllamaClient aClient)
    {
        client = aClient;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Ollama Recommender";
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {
        OllamaRecommenderTraits traits = readTraits(aRecommender);
        return new OllamaRecommender(aRecommender, traits, client);
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        return SPAN_TYPE.equals(aFeature.getLayer().getType())
                && TYPE_NAME_STRING.equals(aFeature.getType());
    }

    @Override
    public OllamaRecommenderTraitsEditor createTraitsEditor(String aId, IModel<Recommender> aModel)
    {
        return new OllamaRecommenderTraitsEditor(aId, aModel);
    }

    @Override
    public OllamaRecommenderTraits createTraits()
    {
        return new OllamaRecommenderTraits();
    }

    @Override
    public boolean isEvaluable()
    {
        return false;
    }

    @Override
    public boolean isMultipleRecommendationProvider()
    {
        return false;
    }
}
