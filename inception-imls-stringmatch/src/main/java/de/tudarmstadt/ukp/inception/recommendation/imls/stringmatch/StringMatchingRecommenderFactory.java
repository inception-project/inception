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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static java.util.Arrays.asList;

import org.apache.uima.cas.CAS;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.gazeteer.GazeteerService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.settings.StringMatchingRecommenderTraitsEditor;

@Component
public class StringMatchingRecommenderFactory
    extends RecommendationEngineFactoryImplBase<StringMatchingRecommenderTraits>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID =
        "de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommender";

    private final GazeteerService gazeteerService;
    
    public StringMatchingRecommenderFactory(GazeteerService aGazeteerService)
    {
        gazeteerService = aGazeteerService;
    }
    
    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {
        StringMatchingRecommenderTraits traits = new StringMatchingRecommenderTraits();
        return new StringMatchingRecommender(aRecommender, traits, gazeteerService);
    }

    @Override
    public String getName()
    {
        return "String Matcher";
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        if (aLayer == null || aFeature == null) {
            return false;
        }

        return (asList(SINGLE_TOKEN, TOKENS).contains(aLayer.getAnchoringMode()))
            && !aLayer.isCrossSentence() && SPAN_TYPE.equals(aLayer.getType())
            && (CAS.TYPE_NAME_STRING.equals(aFeature.getType()) || aFeature.isVirtualFeature());
    }
    
    @Override
    public StringMatchingRecommenderTraitsEditor createTraitsEditor(String aId,
            IModel<Recommender> aModel)
    {
        return new StringMatchingRecommenderTraitsEditor(aId, aModel);
    }

    @Override
    public StringMatchingRecommenderTraits createTraits()
    {
        return new StringMatchingRecommenderTraits();
    }
}
