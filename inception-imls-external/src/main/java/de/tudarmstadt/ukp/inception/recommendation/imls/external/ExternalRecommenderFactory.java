/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toJsonString;

import java.io.IOException;

import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommendationEngineFactory;

@Component
public class ExternalRecommenderFactory
    implements RecommendationEngineFactory
{

    private final Logger log = LoggerFactory.getLogger(getClass());

    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = 
            "de.tudarmstadt.ukp.inception.recommendation.imls.external.ExternalClassificationTool";

    private @Autowired RecommendationService recommendationService;
    
    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender) {
        ExternalRecommenderTraits traits = readTraits(aRecommender);
        traits.setRemoteUrl("http://localhost:30500");
        return new ExternalRecommender(aRecommender, traits);
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
        
        return (aLayer.isLockToTokenOffset() || aLayer.isMultipleTokens())
                && WebAnnoConst.SPAN_TYPE.equals(aLayer.getType());
    }

    @Override
    public org.apache.wicket.Component createTraitsEditor(String aId,
            IModel<Recommender> aModel)
    {
        return new ExternalRecommenderTraitsEditor(aId, aModel);
    }
    
    private ExternalRecommenderTraits readTraits(Recommender aRecommender) {
        ExternalRecommenderTraits traits = null;
        try {
            traits = fromJsonString(ExternalRecommenderTraits.class, aRecommender.getTraits());
        } catch (IOException e) {
            log.error("Error while reading traits", e);
        }

        if (traits == null) {
            traits = new ExternalRecommenderTraits();
        }

        return traits;
    }

    private void writeTraits(Recommender aRecommender, ExternalRecommenderTraits aTraits) {
        try {
            String json = toJsonString(aTraits);
            aRecommender.setTraits(json);
        } catch (IOException e) {
            log.error("Error while writing traits", e);
        }
    }
}
