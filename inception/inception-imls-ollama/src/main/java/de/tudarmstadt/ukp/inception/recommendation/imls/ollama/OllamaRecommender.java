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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.loader.ResourceLocator;
import com.hubspot.jinjava.loader.ResourceNotFoundException;

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.NonTrainableRecommenderEngineImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaGenerateRequest;
import de.tudarmstadt.ukp.inception.rendering.model.Range;

public class OllamaRecommender
    extends NonTrainableRecommenderEngineImplBase
{
    private static final String VAR_TEXT = "text";
    private static final String VAR_SENTENCE = "sentence";
    private static final String VAR_DOCUMENT = "document";

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
    public Range predict(RecommenderContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        switch (traits.getProcessingMode()) {
        case PER_ANNOTATION:
            return predictPerAnnotation(aContext, aCas, aBegin, aEnd);
        case PER_SENTENCE:
            return predictPerSentence(aContext, aCas, aBegin, aEnd);
        case PER_DOCUMENT:
            return predictPerDocument(aContext, aCas, aBegin, aEnd);
        default:
            throw new RecommendationException(
                    "Unsupported mode [" + traits.getProcessingMode() + "]");
        }
    }

    private Range predictPerDocument(RecommenderContext aContext, CAS aCas, int aBegin, int aEnd)
    {
        var bindings = Map.of(VAR_TEXT, aCas.getDocumentText());
        var prompt = jinjava.render(traits.getPrompt(), bindings);

        try {
            var candidate = aCas.getDocumentAnnotation();

            var response = generate(prompt);

            extractPredictions(candidate, response);
        }
        catch (IOException e) {
            LOG.error("Ollama [{}] failed to respond: {}", traits.getModel(),
                    ExceptionUtils.getRootCauseMessage(e));
        }

        return new Range(aBegin, aEnd);
    }

    private void extractPredictions(AnnotationFS aCandidate, String aResponse)
    {
        switch (traits.getExtractionMode()) {
        case RESPONSE_AS_LABEL:
            predictResultAsLabel(aCandidate, aResponse);
            break;
        case MENTIONS_FROM_JSON:
            var mentions = extractMentionFromJson(aCandidate, aResponse);
            mentionsToPredictions(aCandidate, mentions);
            break;
        default:
            throw new IllegalArgumentException(
                    "Unsupported extraction mode [" + traits.getExtractionMode() + "]");
        }
    }

    private ArrayList<Pair<String, String>> extractMentionFromJson(AnnotationFS aCandidate,
            String aResponse)
    {
        var mentions = new ArrayList<Pair<String, String>>();
        try {
            // Ollama JSON mode always returns a JSON object
            // See:
            // https://github.com/jmorganca/ollama/commit/5cba29b9d666854706a194805c9d66518fe77545#diff-a604f7ba9b7f66dd7b59a9e884d3c82c96e5269fee85c906a7cca5f0c3eff7f8R30-R57
            var rootNode = JSONUtil.getObjectMapper().readTree(aResponse);

            var fieldIterator = rootNode.fields();
            while (fieldIterator.hasNext()) {
                var fieldEntry = fieldIterator.next();
                if (fieldEntry.getValue().isArray()) {
                    for (var item : fieldEntry.getValue()) {
                        if (item.isTextual()) {
                            // Looks like this
                            // "Person": ["John"],
                            // "Location": ["diner", "Starbucks"]
                            mentions.add(Pair.of(item.asText(), fieldEntry.getKey()));
                        }
                        if (item.isObject()) {
                            // Looks like this
                            // "politicians": [
                            // { "name": "President Livingston" },
                            // { "name": "John" },
                            // { "name": "Don Horny" }
                            // ]
                            var subFieldIterator = item.fields();
                            while (subFieldIterator.hasNext()) {
                                var subEntry = subFieldIterator.next();
                                if (subEntry.getValue().isTextual()) {
                                    mentions.add(Pair.of(subEntry.getValue().asText(),
                                            fieldEntry.getKey()));
                                }
                                // We assume that the first item is the most relevant one (the
                                // mention) so we do not get a bad mention in cases like this:
                                // {
                                // "name": "Don Horny",
                                // "affiliation": "Lord of Darkness"
                                // }
                                break;
                            }
                        }
                    }
                }

                // Looks like this
                // "John": {"type": "PERSON"},
                // "diner": {"type": "LOCATION"},
                // "Starbucks": {"type": "LOCATION"}
                if (fieldEntry.getValue().isObject()) {
                    mentions.add(Pair.of(fieldEntry.getKey(), null));
                }

                // Looks like this
                // "John": "politician",
                // "President Livingston": "politician",
                // "minister of foreign affairs": "politician",
                // "Don Horny": "politician"
                if (fieldEntry.getValue().isTextual()) {
                    mentions.add(Pair.of(fieldEntry.getKey(), fieldEntry.getValue().asText()));
                }
            }
        }
        catch (IOException e) {
            LOG.error("Unable to extract mentions - not valid JSON: [" + aResponse + "]");
        }
        return mentions;
    }

    private void mentionsToPredictions(AnnotationFS aCandidate, List<Pair<String, String>> mentions)
    {
        var cas = aCandidate.getCAS();
        var text = aCandidate.getCoveredText();
        var predictedType = getPredictedType(cas);
        var predictedFeature = getPredictedFeature(cas);
        var isPredictionFeature = getIsPredictionFeature(cas);

        for (var entry : mentions) {
            var mention = entry.getKey();
            if (mention.isBlank()) {
                LOG.debug("Blank mention ignored");
                continue;
            }

            var label = entry.getValue();
            var lastIndex = 0;
            var index = text.indexOf(mention, lastIndex);
            var hitCount = 0;
            while (index >= 0) {
                int begin = aCandidate.getBegin() + index;
                var prediction = cas.createAnnotation(predictedType, begin,
                        begin + mention.length());
                prediction.setBooleanValue(isPredictionFeature, true);
                if (label != null) {
                    prediction.setStringValue(predictedFeature, label);
                }
                cas.addFsToIndexes(prediction);
                LOG.debug("Prediction generated [{}] -> [{}]", mention, label);
                hitCount++;

                lastIndex = index + mention.length();
                index = text.indexOf(mention, lastIndex);

                if (hitCount > text.length() / mention.length()) {
                    LOG.error(
                            "Mention detection seems to have entered into an endless loop - aborting");
                    break;
                }
            }

            if (hitCount == 0) {
                LOG.debug("Mention [{}] not found", mention);
            }
        }
    }

    private void predictResultAsLabel(AnnotationFS aCandidate, String aResponse)
    {
        var aCas = aCandidate.getCAS();

        var predictedType = getPredictedType(aCas);
        var predictedFeature = getPredictedFeature(aCas);
        var isPredictionFeature = getIsPredictionFeature(aCas);

        var prediction = aCas.createAnnotation(predictedType, aCandidate.getBegin(),
                aCandidate.getEnd());
        prediction.setFeatureValueFromString(predictedFeature, aResponse);
        prediction.setBooleanValue(isPredictionFeature, true);
        aCas.addFsToIndexes(prediction);

        LOG.debug("Prediction generated [{}] -> [{}]", prediction.getCoveredText(), aResponse);
    }

    private Range predictPerSentence(RecommenderContext aContext, CAS aCas, int aBegin, int aEnd)
    {
        var candidateType = CasUtil.getAnnotationType(aCas, Sentence.class);

        for (var candidate : selectOverlapping(aCas, candidateType, aBegin, aEnd)) {
            var bindings = Map.of(VAR_TEXT, candidate.getCoveredText());
            var prompt = jinjava.render(traits.getPrompt(), bindings);

            try {
                var response = generate(prompt);

                extractPredictions(candidate, response);
            }
            catch (IOException e) {
                LOG.error("Ollama [{}] failed to respond: {}", traits.getModel(),
                        ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return new Range(aBegin, aEnd);
    }

    private Range predictPerAnnotation(RecommenderContext aContext, CAS aCas, int aBegin, int aEnd)
    {
        var predictedType = getPredictedType(aCas);

        for (var candidate : selectOverlapping(aCas, predictedType, aBegin, aEnd)) {
            String sentence = aCas.select(Sentence.class).covering(candidate)
                    .map(Sentence::getCoveredText).findFirst().orElse("");
            var bindings = Map.of( //
                    VAR_TEXT, candidate.getCoveredText(), //
                    VAR_SENTENCE, sentence);
            var prompt = jinjava.render(traits.getPrompt(), bindings);

            try {
                var response = generate(prompt);

                extractPredictions(candidate, response);
            }
            catch (IOException e) {
                LOG.error("Ollama [{}] failed to respond: {}", traits.getModel(),
                        ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return new Range(aBegin, aEnd);
    }

    private String generate(String prompt) throws IOException
    {
        LOG.trace("Asking ollama [{}]: [{}]", traits.getModel(), prompt);
        var request = OllamaGenerateRequest.builder() //
                .withModel(traits.getModel()) //
                .withPrompt(prompt) //
                .withFormat(traits.getFormat()) //
                .withRaw(traits.isRaw()) //
                .withStream(false) //
                .build();
        var response = client.generate(traits.getUrl(), request).trim();
        LOG.trace("Ollama [{}] responds: [{}]", traits.getModel(), response);
        return response;
    }
}
