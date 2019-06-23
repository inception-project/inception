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

import java.util.List;

import org.apache.uima.cas.CAS;
import org.lappsgrid.client.ServiceClient;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.io.lif.internal.DKPro2Lif;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
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

            for (View view : container.getViews()) {
                view.addContains(
                        Discriminators.Uri.SENTENCE,
                        "INCEpTION",
                        "Sentence"
                );

                view.addContains(
                        Discriminators.Uri.TOKEN,
                        "INCEpTION",
                        "Token"
                );
            }

            String request = new Data<>(Discriminators.Uri.LIF, container).asJson();
            String response = client.execute(request);

            DataContainer result = Serializer.parse(response, DataContainer.class);

            aCas.reset();
            new Lif2DKPro().convert(result.getPayload(), aCas.getJCas());
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
    public boolean requiresTraining()
    {
        return false;
    }
}
