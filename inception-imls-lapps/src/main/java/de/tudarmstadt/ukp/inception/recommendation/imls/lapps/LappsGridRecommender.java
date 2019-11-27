/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.imls.lapps;

import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.dkpro.core.io.lif.internal.DKPro2Lif;
import org.dkpro.core.io.lif.internal.Lif2DKPro;
import org.lappsgrid.client.ServiceClient;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineCapability;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridRecommenderTraits;

public class LappsGridRecommender
    extends RecommendationEngine
{
    private static final Logger LOG = LoggerFactory.getLogger(LappsGridRecommender.class);

    private final LappsGridRecommenderTraits traits;
    private final ServiceClient client;

    public LappsGridRecommender(Recommender aRecommender, LappsGridRecommenderTraits aTraits)
    {
        super(aRecommender);

        traits = aTraits;
        client = buildClient();
    }
    
    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
    {
        // Training not supported
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        try {
            Container container = new Container();
            new DKPro2Lif().convert(aCas.getJCas(), container);

            String request = new Data<>(Discriminators.Uri.LIF, container).asJson();
            String response = client.execute(request);

            DataContainer result = Serializer.parse(response, DataContainer.class);
            
            aCas.reset();
            new Lif2DKPro().convert(result.getPayload(), aCas.getJCas());
            
            Feature isPredictionFeature = getIsPredictionFeature(aCas);
            for (AnnotationFS predictedAnnotation : select(aCas, getPredictedType(aCas))) {
                predictedAnnotation.setBooleanValue(isPredictionFeature, true);
            }
            
            // Drop the tokens we got from the remote service since their boundaries might not
            // match ours.
            select(aCas, getType(aCas, Token.class)).forEach(aCas::removeFsFromIndexes);
            
            // If the remote service did not return tokens (or if we didn't find them...), then
            // let's just re-add the tokens that we originally sent. We need the tokens later
            // when extracting the predicted annotations
            Type tokenType = getType(aCas, Token.class);
            if (select(aCas, getType(aCas, Token.class)).isEmpty()) {
                container.getView(0).getAnnotations().stream()
                    .filter(a -> Discriminators.Uri.TOKEN.equals(a.getAtType()))
                    .forEach(token -> {
                        AnnotationFS t = aCas.createAnnotation(tokenType,
                                token.getStart().intValue(), token.getEnd().intValue());
                        aCas.addFsToIndexes(t);
                    });
            }
        } catch (Exception e) {
            throw new RecommendationException("Cannot predict", e);
        }
    }
    
    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        EvaluationResult result = new EvaluationResult();
        result.setErrorMsg("Evaluation not supported (yet)");
        return result;
    }

    private ServiceClient buildClient()
    {
        String url = traits.getUrl();

        try {
            return new ServiceClient(url, "tester", "tester");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        return true;
    }

    public RecommendationEngineCapability getTrainingCapability()
    {
        return RecommendationEngineCapability.TRAINING_NOT_SUPPORTED;
    }
}
