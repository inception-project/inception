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
package de.tudarmstadt.ukp.inception.recommendation.imls.weblicht;

import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.chains.WeblichtChainService;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.config.WeblichtRecommenderAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.traits.WeblichtRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.traits.WeblichtRecommenderTraitsEditor;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WeblichtRecommenderAutoConfiguration#weblichtRecommenderFactory}.
 * </p>
 */
public class WeblichtRecommenderFactoryImpl
    extends RecommendationEngineFactoryImplBase<WeblichtRecommenderTraits>
    implements WeblichtRecommenderFactory
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = "de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.WeblichtRecommender";

    public static final String DEFAULT_WEBLICHT_URL = "https://weblicht.sfs.uni-tuebingen.de/WaaS/api/1.0/chain/process";

    private final WeblichtChainService chainService;

    @Autowired
    public WeblichtRecommenderFactoryImpl(WeblichtChainService aChainService)
    {
        chainService = aChainService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {
        WeblichtRecommenderTraits traits = readTraits(aRecommender);
        return new WeblichtRecommender(aRecommender, traits, chainService);
    }

    @Override
    public String getName()
    {
        return "WebLicht recommender";
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer)
    {
        if (aLayer == null) {
            return false;
        }

        if (Lemma.class.getName().equals(aLayer.getName())) {
            return true;
        }

        if (POS.class.getName().equals(aLayer.getName())) {
            return true;
        }

        if (NamedEntity.class.getName().equals(aLayer.getName())) {
            return true;
        }

        return false;
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        if (aFeature == null) {
            return false;
        }

        if (!accepts(aFeature.getLayer())) {
            return false;
        }

        var layer = aFeature.getLayer();
        if (Lemma._TypeName.equals(layer.getName())) {
            if (Lemma._FeatName_value.equals(aFeature.getName())) {
                return true;
            }
        }

        if (POS._TypeName.equals(layer.getName())) {
            if (POS._FeatName_PosValue.equals(aFeature.getName())) {
                return true;
            }
            if (POS._FeatName_coarseValue.equals(aFeature.getName())) {
                return true;
            }
        }

        if (NamedEntity._TypeName.equals(layer.getName())) {
            if (NamedEntity._FeatName_value.equals(aFeature.getName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public WeblichtRecommenderTraitsEditor createTraitsEditor(String aId,
            IModel<Recommender> aModel)
    {
        return new WeblichtRecommenderTraitsEditor(aId, aModel);
    }

    @Override
    public WeblichtRecommenderTraits createTraits()
    {
        WeblichtRecommenderTraits traits = new WeblichtRecommenderTraits();
        traits.setUrl(DEFAULT_WEBLICHT_URL);
        return traits;
    }

    @Override
    public boolean isEvaluable()
    {
        return false;
    }
}
