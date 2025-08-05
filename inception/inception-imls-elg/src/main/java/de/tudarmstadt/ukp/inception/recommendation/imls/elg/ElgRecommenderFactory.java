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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg;

import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;

import java.util.Optional;

import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgSession;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.service.ElgService;

public class ElgRecommenderFactory
    extends RecommendationEngineFactoryImplBase<ElgRecommenderTraits>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = "de.tudarmstadt.ukp.inception.recommendation.imls.elg.ElgRecommenderFactory";

    private final ElgService elgService;

    public ElgRecommenderFactory(ElgService aElgService)
    {
        elgService = aElgService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "European Language Grid Recommender";
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {
        ElgRecommenderTraits traits = readTraits(aRecommender);
        Optional<ElgSession> session = elgService.getSession(aRecommender.getProject());
        if (!session.isPresent()) {
            throw new IllegalArgumentException("Not signed in to ELG");
        }
        return new ElgRecommender(aRecommender, traits, elgService, session.get());
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
    public ElgRecommenderTraitsEditor createTraitsEditor(String aId, IModel<Recommender> aModel)
    {
        return new ElgRecommenderTraitsEditor(aId, aModel);
    }

    @Override
    public ElgRecommenderTraits createTraits()
    {
        return new ElgRecommenderTraits();
    }

    @Override
    public boolean isEvaluable()
    {
        return false;
    }
}
