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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;

@Component
public class OpenNlpDoccatRecommenderFactory
    extends RecommendationEngineFactoryImplBase<OpenNlpDoccatRecommenderTraits>
{
    private Logger log = LoggerFactory.getLogger(getClass());

    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = 
            "de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat.OpenNlpDoccatRecommenderFactory";
    
    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender) {
        OpenNlpDoccatRecommenderTraits traits = new OpenNlpDoccatRecommenderTraits();
        return new OpenNlpDoccatRecommender(aRecommender, traits);
    }

    @Override
    public String getName()
    {
        return "Sentence Classifier (OpenNLP Document Categorizer)";
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        if (aLayer == null || aFeature == null) {
            return false;
        }
        
        return (asList(AnchoringMode.SENTENCES).contains(aLayer.getAnchoringMode()))
                && !aLayer.isCrossSentence() && SPAN_TYPE.equals(aLayer.getType())
                && CAS.TYPE_NAME_STRING.equals(aFeature.getType()) || aFeature.isVirtualFeature();
    }

    @Override
    public OpenNlpDoccatRecommenderTraits createTraits()
    {
        return new OpenNlpDoccatRecommenderTraits();
    }
}
