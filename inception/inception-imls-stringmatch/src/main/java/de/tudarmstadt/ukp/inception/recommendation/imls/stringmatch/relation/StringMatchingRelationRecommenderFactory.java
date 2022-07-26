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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.relation;

import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.RELATION_TYPE;

import org.apache.uima.cas.CAS;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.config.StringMatchingRecommenderAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.relation.settings.StringMatchingRelationRecommenderTraitsEditor;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link StringMatchingRecommenderAutoConfiguration#stringMatchingRelationRecommenderFactory}.
 * </p>
 */
public class StringMatchingRelationRecommenderFactory
    extends RecommendationEngineFactoryImplBase<StringMatchingRelationRecommenderTraits>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = "StringMatchingRelationRecommender";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {
        StringMatchingRelationRecommenderTraits traits = readTraits(aRecommender);
        StringMatchingRelationRecommender recommender = new StringMatchingRelationRecommender(
                aRecommender, traits);

        return recommender;
    }

    @Override
    public String getName()
    {
        return "String Matcher for relations";
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        if (aLayer == null || aFeature == null) {
            return false;
        }

        return RELATION_TYPE.equals(aLayer.getType()) && !aLayer.isAllowStacking()
                && (CAS.TYPE_NAME_STRING.equals(aFeature.getType()) || aFeature.isVirtualFeature());
    }

    @Override
    public StringMatchingRelationRecommenderTraitsEditor createTraitsEditor(String aId,
            IModel<Recommender> aModel)
    {
        return new StringMatchingRelationRecommenderTraitsEditor(aId, aModel);
    }

    @Override
    public StringMatchingRelationRecommenderTraits createTraits()
    {
        return new StringMatchingRelationRecommenderTraits();
    }
}
