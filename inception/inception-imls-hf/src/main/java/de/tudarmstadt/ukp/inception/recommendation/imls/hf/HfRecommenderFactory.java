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
package de.tudarmstadt.ukp.inception.recommendation.imls.hf;

import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;

import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.client.HfInferenceClient;

public class HfRecommenderFactory
    extends RecommendationEngineFactoryImplBase<HfRecommenderTraits>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = "de.tudarmstadt.ukp.inception.recommendation.imls.hf.HfRecommenderFactory";

    private final HfInferenceClient hfInferenceClient;

    public HfRecommenderFactory(HfInferenceClient aHfInferenceClient)
    {
        hfInferenceClient = aHfInferenceClient;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Hugging Face Recommender";
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {
        HfRecommenderTraits traits = readTraits(aRecommender);
        return new HfRecommender(aRecommender, traits, hfInferenceClient);
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer)
    {
        if (aLayer == null) {
            return false;
        }

        return SpanLayerSupport.TYPE.equals(aLayer.getType());
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        if (aFeature == null) {
            return false;
        }
        return accepts(aFeature.getLayer()) && TYPE_NAME_STRING.equals(aFeature.getType());
    }

    @Override
    public HfRecommenderTraitsEditor createTraitsEditor(String aId, IModel<Recommender> aModel)
    {
        return new HfRecommenderTraitsEditor(aId, aModel);
    }

    @Override
    public HfRecommenderTraits createTraits()
    {
        return new HfRecommenderTraits();
    }

    @Override
    public boolean isEvaluable()
    {
        return false;
    }
}
