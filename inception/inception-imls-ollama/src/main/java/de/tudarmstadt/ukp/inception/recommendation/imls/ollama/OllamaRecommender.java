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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectOverlapping;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.loader.ResourceLocator;
import com.hubspot.jinjava.loader.ResourceNotFoundException;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.NonTrainableRecommenderEngineImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.exceptions.OllamaBaseException;

public class OllamaRecommender
    extends NonTrainableRecommenderEngineImplBase
{
    private static final String VAR_TEXT = "text";

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final OllamaRecommenderTraits traits;

    private final OllamaAPI ollamaApi;
    private final Jinjava jinjava;

    public OllamaRecommender(Recommender aRecommender, OllamaRecommenderTraits aTraits)
    {
        super(aRecommender);

        traits = aTraits;

        var config = new JinjavaConfig();
        jinjava = new Jinjava(config);
        jinjava.setResourceLocator(new ResourceLocator()
        {
            @Override
            public String getString(String aFullName, Charset aEncoding,
                    JinjavaInterpreter aInterpreter)
                throws IOException
            {
                throw new ResourceNotFoundException("Couldn't find resource: " + aFullName);
            }
        });

        ollamaApi = new OllamaAPI(traits.getUrl());
    }

    @Override
    public Range predict(RecommenderContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        switch (traits.getProcessingMode()) {
        case PER_ANNOTATION:
            return predictPerAnnotation(aContext, aCas, aBegin, aEnd);
        case PER_SENTENCE:
            return predictPerSentence(aContext, aCas, aBegin, aEnd);
        default:
            throw new RecommendationException(
                    "Unsupported mode [" + traits.getProcessingMode() + "]");
        }
    }

    private Range predictPerSentence(RecommenderContext aContext, CAS aCas, int aBegin, int aEnd)
    {
        var candidateType = CasUtil.getAnnotationType(aCas, Sentence.class);
        var predictedType = getPredictedType(aCas);
        var predictedFeature = getPredictedFeature(aCas);
        var isPredictionFeature = getIsPredictionFeature(aCas);

        for (var candidate : selectOverlapping(aCas, candidateType, aBegin, aEnd)) {
            var bindings = Map.of(VAR_TEXT, candidate.getCoveredText());
            var prompt = jinjava.render(traits.getPrompt(), bindings);

            try {
                var response = ask(prompt);

                var prediction = aCas.createAnnotation(predictedType, candidate.getBegin(),
                        candidate.getEnd());
                prediction.setFeatureValueFromString(predictedFeature, response);
                prediction.setBooleanValue(isPredictionFeature, true);
                aCas.addFsToIndexes(prediction);
            }
            catch (OllamaBaseException | IOException e) {
                LOG.error("Ollama [{}] failed to respond: {}", traits.getModel(),
                        ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return new Range(aBegin, aEnd);
    }

    private Range predictPerAnnotation(RecommenderContext aContext, CAS aCas, int aBegin, int aEnd)
    {
        var predictedType = getPredictedType(aCas);
        var predictedFeature = getPredictedFeature(aCas);
        var isPredictionFeature = getIsPredictionFeature(aCas);

        for (var candidate : selectOverlapping(aCas, predictedType, aBegin, aEnd)) {
            var bindings = Map.of(VAR_TEXT, candidate.getCoveredText());
            var prompt = jinjava.render(traits.getPrompt(), bindings);

            try {
                var response = ask(prompt);

                var prediction = candidate;
                prediction.setFeatureValueFromString(predictedFeature, response);
                prediction.setBooleanValue(isPredictionFeature, true);
            }
            catch (OllamaBaseException | IOException e) {
                LOG.error("Ollama [{}] failed to respond: {}", traits.getModel(),
                        ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return new Range(aBegin, aEnd);
    }

    private String ask(String prompt) throws OllamaBaseException, IOException
    {
        LOG.trace("Asking ollama [{}]: [{}]", traits.getModel(), prompt);
        var response = ollamaApi.ask(traits.getModel(), prompt).trim();
        LOG.trace("Ollama [{}] responds: [{}]", traits.getModel(), response);
        return response;
    }
}
