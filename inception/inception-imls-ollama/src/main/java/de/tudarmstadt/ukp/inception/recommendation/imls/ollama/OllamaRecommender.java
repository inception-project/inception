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

import static de.tudarmstadt.ukp.inception.recommendation.imls.ollama.prompt.PromptContextGenerator.VAR_EXAMPLES;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.loader.ResourceLocator;
import com.hubspot.jinjava.loader.ResourceNotFoundException;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.NonTrainableRecommenderEngineImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaGenerateRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.prompt.PerAnnotationContextGenerator;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.prompt.PerDocumentContextGenerator;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.prompt.PerSentenceContextGenerator;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.prompt.PromptContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.prompt.PromptContextGenerator;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.response.MentionsFromJsonExtractor;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.response.ResponseAsLabelExtractor;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.response.ResponseExtractor;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class OllamaRecommender
    extends NonTrainableRecommenderEngineImplBase
{
    private static final int MAX_FEW_SHOT_EXAMPLES = 10;

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final OllamaRecommenderTraits traits;

    private final OllamaClient client;
    private final Jinjava jinjava;

    public OllamaRecommender(Recommender aRecommender, OllamaRecommenderTraits aTraits,
            OllamaClient aClient)
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

        client = aClient;
    }

    @Override
    public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        var responseExtractor = getResponseExtractor();
        var examples = responseExtractor.generate(this, aCas, MAX_FEW_SHOT_EXAMPLES);

        getPromptContextGenerator().generate(this, aCas, aBegin, aEnd).forEach(promptContext -> {
            try {
                var bindings = promptContext.getBindings();

                bindings.put(VAR_EXAMPLES, examples);

                var response = query(promptContext);

                responseExtractor.extract(this, aCas, promptContext, response);
            }
            catch (IOException e) {
                aContext.log(LogMessage.warn(getRecommender().getName(),
                        "Ollama [%s] failed to respond: %s", traits.getModel(),
                        ExceptionUtils.getRootCauseMessage(e)));
                LOG.error("Ollama [{}] failed to respond: {}", traits.getModel(),
                        ExceptionUtils.getRootCauseMessage(e));
            }
        });

        return new Range(aBegin, aEnd);
    }

    private String query(PromptContext aContext) throws IOException
    {
        var prompt = jinjava.render(traits.getPrompt(), aContext.getBindings());

        LOG.trace("Querying ollama [{}]: [{}]", traits.getModel(), prompt);
        var request = OllamaGenerateRequest.builder() //
                .withModel(traits.getModel()) //
                .withPrompt(prompt) //
                .withFormat(traits.getFormat()) //
                .withRaw(traits.isRaw()) //
                .withStream(false) //
                // FIXME: Make NUM_PREDICT accessible in UI
                .withOption(OllamaGenerateRequest.NUM_PREDICT, 300) //
                .build();
        var response = client.generate(traits.getUrl(), request).trim();
        LOG.trace("Ollama [{}] responds: [{}]", traits.getModel(), response);
        return response;
    }

    private PromptContextGenerator getPromptContextGenerator()
    {
        switch (traits.getPromptingMode()) {
        case PER_ANNOTATION:
            return new PerAnnotationContextGenerator();
        case PER_SENTENCE:
            return new PerSentenceContextGenerator();
        case PER_DOCUMENT:
            return new PerDocumentContextGenerator();
        default:
            throw new IllegalArgumentException(
                    "Unsupported mode [" + traits.getPromptingMode() + "]");
        }
    }

    private ResponseExtractor getResponseExtractor()
    {
        switch (traits.getExtractionMode()) {
        case RESPONSE_AS_LABEL:
            return new ResponseAsLabelExtractor();
        case MENTIONS_FROM_JSON:
            return new MentionsFromJsonExtractor();
        default:
            throw new IllegalArgumentException(
                    "Unsupported extraction mode [" + traits.getExtractionMode() + "]");
        }
    }
}
