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
package de.tudarmstadt.ukp.inception.recommendation.imls.external.v2;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getRealCas;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineCapability.TRAINING_NOT_SUPPORTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineCapability.TRAINING_REQUIRED;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineCapability;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v2.api.Document;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v2.api.ExternalRecommenderV2Api;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v2.api.FormatConverter;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v2.config.ExternalRecommenderProperties;

public class ExternalRecommender
    extends RecommendationEngine
{
    private final static Logger LOG = LoggerFactory.getLogger(ExternalRecommender.class);

    public static final RecommenderContext.Key<Boolean> KEY_TRAINING_COMPLETE = new RecommenderContext.Key<>(
            "training_complete");

    private final ExternalRecommenderProperties properties;
    private final ExternalRecommenderTraits traits;
    private final ExternalRecommenderV2Api api;

    public ExternalRecommender(ExternalRecommenderProperties aProperties, Recommender aRecommender,
            ExternalRecommenderTraits aTraits)
    {
        super(aRecommender);

        properties = aProperties;
        traits = aTraits;
        api = new ExternalRecommenderV2Api(URI.create(traits.getRemoteUrl()),
                properties.getConnectTimeout());
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses) throws RecommendationException
    {

    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        CAS cas = getRealCas(aCas);

        if (aContext.getUser().isEmpty()) {
            LOG.warn("No user found in context, skipping predictions...");
            return;
        }

        String userName = aContext.getUser().get().getUsername();
        long version = 0;

        FormatConverter converter = new FormatConverter();
        String layerName = recommender.getLayer().getName();
        String featureName = recommender.getFeature().getName();
        String projectName = recommender.getProject().getName();

        Document request = converter.documentFromCas(cas, layerName, featureName, version);

        String modelName = projectName + "_" + userName + "_" + recommender.getName();

        String classifierName = traits.getClassifierInfo().getName();
        Optional<Document> response = api.predict(classifierName, modelName, request);
        if (response.isEmpty()) {
            LOG.error("Could not obtain predictions, skipping...");
            return;
        }

        converter.loadIntoCas(response.get(), layerName, featureName, cas);
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
        throws RecommendationException
    {
        return null;
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        if (traits.isTrainable()) {
            return aContext.get(KEY_TRAINING_COMPLETE).orElse(false);
        }
        else {
            return true;
        }
    }

    @Override
    public int estimateSampleCount(List<CAS> aCasses)
    {
        return 0;
    }

    @Override
    public RecommendationEngineCapability getTrainingCapability()
    {
        if (traits.isTrainable()) {
            //
            // return TRAINING_SUPPORTED;
            // We need to get at least one training CAS because we need to extract the type system
            return TRAINING_REQUIRED;
        }
        else {
            return TRAINING_NOT_SUPPORTED;
        }
    }

}
