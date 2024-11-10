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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response;

import static java.util.Collections.reverse;
import static java.util.Collections.sort;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.normalizeSpace;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptContext;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class MentionsFromJsonExtractor
    implements ResponseExtractor
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public List<MentionsSample> generate(RecommendationEngine aEngine, CAS aCas, int aNum)
    {
        var examples = generateSamples(aEngine, aCas, aNum);

        return new ArrayList<>(examples.values());
    }

    Map<String, MentionsSample> generateSamples(RecommendationEngine aEngine, CAS aCas, int aNum)
    {
        var predictedType = aEngine.getPredictedType(aCas);
        var predictedFeature = aEngine.getPredictedFeature(aCas);

        var examples = new LinkedHashMap<String, MentionsSample>();

        var sentencesAndLabels = new ArrayList<Pair<Sentence, Set<String>>>();
        for (var sentence : aCas.select(Sentence.class)) {
            if (isBlank(sentence.getCoveredText())) {
                continue;
            }

            var labels = new HashSet<String>();
            var mentions = aCas.<Annotation> select(predictedType).coveredBy(sentence);
            if (mentions.isEmpty()) {
                continue;
            }

            for (var mention : mentions) {
                labels.add(FSUtil.getFeature(mention, predictedFeature, String.class));
            }

            sentencesAndLabels.add(Pair.of(sentence, labels));
        }

        sort(sentencesAndLabels, comparing(e -> e.getValue().size()));
        reverse(sentencesAndLabels);

        var labelsSeen = new HashSet<String>();
        for (var sentenceAndLabels : sentencesAndLabels) {
            // Stop once we have sufficient samples
            if (examples.size() >= aNum) {
                break;
            }

            // Skip if we already have examples for all the labels - except if there are null
            // labels, then we go on because we might otherwise end up only with a single
            // example
            if (!sentenceAndLabels.getValue().contains(null)
                    && labelsSeen.containsAll(sentenceAndLabels.getValue())) {
                continue;
            }

            var sentence = sentenceAndLabels.getKey();
            var sample = new MentionsSample(sentence.getCoveredText());
            for (var mention : aCas.<Annotation> select(predictedType).coveredBy(sentence)) {
                var label = FSUtil.getFeature(mention, predictedFeature, String.class);
                var mentionText = mention.getCoveredText();
                mentionText = normalizeSpace(mentionText);
                sample.addMention(mentionText, label);
            }

            examples.put(sentence.getCoveredText(), sample);
            labelsSeen.addAll(sentenceAndLabels.getValue());
        }

        return examples;
    }

    @Override
    public void extract(RecommendationEngine aEngine, CAS aCas, PromptContext aContext,
            String aResponse)
    {
        var mentions = extractMentionFromJson(aResponse);
        mentionsToPredictions(aEngine, aCas, aContext.getCandidate(), mentions);
    }

    List<Pair<String, String>> extractMentionFromJson(String aResponse)
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
                        if (item.isTextual() || item.isNumber() || item.isBoolean()) {
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
                                break;
                            }
                        }
                    }
                }

                // Looks like this - typically generated from unlabelled few-shot examples
                // "John": null,
                // "diner": null,
                // "Starbucks": null
                if (fieldEntry.getValue().isNull()) {
                    mentions.add(Pair.of(fieldEntry.getKey(), null));
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

    private void mentionsToPredictions(RecommendationEngine aEngine, CAS aCas,
            AnnotationFS aCandidate, List<Pair<String, String>> mentions)
    {
        var text = aCandidate.getCoveredText();
        var predictedType = aEngine.getPredictedType(aCas);
        var predictedFeature = aEngine.getPredictedFeature(aCas);
        var isPredictionFeature = aEngine.getIsPredictionFeature(aCas);

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
                var prediction = aCas.createAnnotation(predictedType, begin,
                        begin + mention.length());
                prediction.setBooleanValue(isPredictionFeature, true);
                if (label != null) {
                    prediction.setStringValue(predictedFeature, label);
                }
                aCas.addFsToIndexes(prediction);
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
}
