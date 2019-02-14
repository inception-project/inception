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
package de.tudarmstadt.ukp.inception.recommendation.imls.lapps;

import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.lappsgrid.client.ServiceClient;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.lif.internal.DKPro2Lif;
import de.tudarmstadt.ukp.dkpro.core.io.lif.internal.Lif2DKPro;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;

public class LappsRecommender
    implements RecommendationEngine
{
    private static final Logger LOG = LoggerFactory.getLogger(LappsRecommender.class);

    private final Recommender recommender;
    private final LappsRecommenderTraits traits;
    private final ServiceClient client;

    public LappsRecommender(Recommender aRecommender, LappsRecommenderTraits aTraits)
    {
        recommender = aRecommender;
        traits = aTraits;
        client = buildClient();
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
        throws RecommendationException
    {
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        try {
            for (Token t : JCasUtil.select(aCas.getJCas(), Token.class)) {
                t.setPos(null);
            }

            String metadataJson = client.getMetadata();
            Data<Object> data = Serializer.parse(metadataJson);
            ServiceMetadata metadata = new ServiceMetadata((Map) data.getPayload());

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

            Type type = CasUtil.getType(aCas, getPredictedType());

            Feature feature = type.getFeatureByBaseName(recommender.getFeature().getName());
        } catch (Exception e) {
            throw new RecommendationException("Cannot predict", e);
        }
    }

    @Override
    public double evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
            throws RecommendationException
    {
        return 0;
    }

    private ServiceClient buildClient()
    {
        // String url = "http://eldrad.cs-i.brandeis.edu:8080/service_manager/invoker/brandeis_eldrad_grid_1:opennlp.postagger_2.0.1";
        // String url = "http://vassar.lappsgrid.org/invoker/anc:gate.tagger_2.3.0";
        // String url = "http://eldrad.cs-i.brandeis.edu:8080/service_manager/invoker/brandeis_eldrad_grid_1:uima.dkpro.opennlp.namedentityrecognizer_0.0.1";
        String url = "http://eldrad.cs-i.brandeis.edu:8080/service_manager/invoker/brandeis_eldrad_grid_1:uima.dkpro.stanfordnlp.postagger_0.0.1";

        try {
            return new ServiceClient(url, "tester", "tester");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String featureToDiscriminator()
    {
        return "";
    }

    @Override
    public String getPredictedType()
    {
        return recommender.getLayer().getName();
    }

    @Override
    public String getPredictedFeature()
    {
        return recommender.getFeature().getName();
    }
}
