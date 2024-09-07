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
package de.tudarmstadt.ukp.inception.recommendation.imls.external.v1;

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.SPAN_TYPE;

import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.config.ExternalRecommenderAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.config.ExternalRecommenderProperties;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ExternalRecommenderAutoConfiguration#externalRecommenderFactory}.
 * </p>
 */
public class ExternalRecommenderFactory
    extends RecommendationEngineFactoryImplBase<ExternalRecommenderTraits>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = "de.tudarmstadt.ukp.inception.recommendation.imls.external.ExternalClassificationTool";

    private final ExternalRecommenderProperties properties;

    public ExternalRecommenderFactory(ExternalRecommenderProperties aProperties)
    {
        properties = aProperties;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {
        ExternalRecommenderTraits traits = readTraits(aRecommender);
        return new ExternalRecommender(properties, aRecommender, traits);
    }

    @Override
    public String getName()
    {
        return "Remote classifier";
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        if (aLayer == null || aFeature == null) {
            return false;
        }

        return SPAN_TYPE.equals(aLayer.getType()) || RELATION_TYPE.equals(aLayer.getType());
    }

    @Override
    public ExternalRecommenderTraitsEditor createTraitsEditor(String aId,
            IModel<Recommender> aModel)
    {
        return new ExternalRecommenderTraitsEditor(aId, aModel);
    }

    @Override
    public ExternalRecommenderTraits createTraits()
    {
        return new ExternalRecommenderTraits();
    }

    @Override
    public boolean isEvaluable()
    {
        return false;
    }

    @Override
    public boolean isRanker(Recommender aRecommender)
    {
        var traits = readTraits(aRecommender);
        return traits.isRanker();
    }
}
