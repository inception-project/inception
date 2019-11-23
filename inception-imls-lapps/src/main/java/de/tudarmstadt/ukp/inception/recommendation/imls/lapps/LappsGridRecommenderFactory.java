/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.imls.lapps;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridRecommenderTraitsEditor.NER_FEATURE;
import static de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridRecommenderTraitsEditor.NER_LAYER;
import static de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridRecommenderTraitsEditor.POS_FEATURE;
import static de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridRecommenderTraitsEditor.POS_LAYER;

import org.apache.wicket.model.IModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridRecommenderTraits;
import de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridRecommenderTraitsEditor;

@Component
@ConditionalOnProperty(prefix = "recommenders.lappsgrid", name = "enabled",
        matchIfMissing = true)
public class LappsGridRecommenderFactory
    extends RecommendationEngineFactoryImplBase<LappsGridRecommenderTraits>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = "de.tudarmstadt.ukp.inception.recommendation.imls.lapps.LappsGridRecommender";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {
        LappsGridRecommenderTraits traits = readTraits(aRecommender);
        return new LappsGridRecommender(aRecommender, traits);
    }

    @Override
    public String getName()
    {
        return "LAPPS Grid recommender";
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        if (aLayer == null || aFeature == null || !SPAN_TYPE.equals(aLayer.getType())) {
            return false;
        }

        String layer = aLayer.getName();
        String feature = aFeature.getName();
        AnchoringMode anchoring = aLayer.getAnchoringMode();

        boolean isNer = NER_LAYER.equals(layer) && NER_FEATURE.equals(feature);
        boolean isPos = POS_LAYER.equals(layer) && POS_FEATURE.equals(feature);

        return (isNer && anchoring == AnchoringMode.TOKENS) ||
               (isPos && anchoring == AnchoringMode.SINGLE_TOKEN);
    }

    @Override
    public LappsGridRecommenderTraitsEditor createTraitsEditor(String aId,
            IModel<Recommender> aModel)
    {
        return new LappsGridRecommenderTraitsEditor(aId, aModel);
    }

    @Override
    public LappsGridRecommenderTraits createTraits()
    {
        return new LappsGridRecommenderTraits();
    }

    @Override
    public boolean isEvaluable()
    {
        return false;
    }
}
