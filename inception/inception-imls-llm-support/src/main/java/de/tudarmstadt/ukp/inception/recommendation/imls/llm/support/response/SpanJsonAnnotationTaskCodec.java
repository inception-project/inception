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

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage.Role.SYSTEM;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage.Role.USER;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ExtractionMode.MENTIONS_FROM_JSON;
import static java.util.Arrays.asList;
import static java.util.Collections.reverse;
import static java.util.Collections.sort;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.normalizeSpace;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.TextNode;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationTaskCodecQuery;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptContext;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.text.Trie;
import de.tudarmstadt.ukp.inception.support.text.WhitespaceNormalizingSanitizer;

public final class SpanJsonAnnotationTaskCodec
    implements AnnotationTaskCodec
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public String getId()
    {
        return getClass().getName();
    }

    @Override
    public boolean accepts(AnnotationTaskCodecQuery aContext)
    {
        return aContext.traits().getExtractionMode() == MENTIONS_FROM_JSON
                && !aContext.traits().isStructuredOutputSupported();
    }

    @Override
    public Map<String, MentionResult> generateExamples(RecommendationEngine aEngine, CAS aCas,
            int aNum)
    {
        var predictedType = aEngine.getPredictedType(aCas);
        var predictedFeature = aEngine.getPredictedFeature(aCas);

        var contextsWithLabels = generateContextsWithLabels(aCas, predictedType, predictedFeature);

        var labelsSeen = new HashSet<String>();
        var examples = new LinkedHashMap<String, MentionResult>();
        for (var contextWithLabels : contextsWithLabels) {
            // Stop once we have sufficient samples
            if (examples.size() >= aNum) {
                break;
            }

            // Skip if we already have examples for all the labels - except if there are null
            // labels, then we go on because we might otherwise end up only with a single
            // example
            if (!contextWithLabels.getValue().contains(null)
                    && labelsSeen.containsAll(contextWithLabels.getValue())) {
                continue;
            }

            var context = contextWithLabels.getKey();
            var mentions = new ArrayList<Mention>();
            for (var mention : aCas.<Annotation> select(predictedType).coveredBy(context)) {
                var label = FSUtil.getFeature(mention, predictedFeature, String.class);
                var text = normalizeSpace(mention.getCoveredText());
                mentions.add(new Mention(text, label, null));
            }
            var result = new MentionResult(mentions);

            examples.put(context.getCoveredText(), result);
            labelsSeen.addAll(contextWithLabels.getValue());
        }

        return examples;
    }

    @Override
    public Optional<ResponseFormat> getResponseFormat()
    {
        return Optional.of(ResponseFormat.JSON);
    }

    @Override
    public List<? extends ChatMessage> getFormatDefiningMessages(Recommender aRecommender,
            AnnotationSchemaService aSchemaService)
    {
        var staticMessages = new ArrayList<ChatMessage>();

        staticMessages.add(new ChatMessage(SYSTEM, """
                Your task is to identify and label mentions in the given document.
                The user will specify how to identify the mentions how to label them.
                Respond using JSON objects.
                """));

        var tagset = aRecommender.getFeature().getTagset();
        if (tagset != null) {
            var tagsMessageContent = new StringBuilder();

            if (tagset.isCreateTag()) {
                tagsMessageContent.append(
                        "You can use the following categories for the `label` or come up with a new one if none "
                                + "of the existing categories is appropriate:\n");
            }
            else {
                tagsMessageContent
                        .append("You assign either of the following categories to the `label`:\n");
            }

            for (var tag : aSchemaService.listTags(tagset)) {
                tagsMessageContent.append("* `").append(tag.getName()).append("`");
                if (isNotBlank(tag.getDescription())) {
                    tagsMessageContent.append(": " + tag.getDescription());
                }
                tagsMessageContent.append("\n");
            }
            staticMessages.add(new ChatMessage(SYSTEM, tagsMessageContent.toString()));
        }

        return staticMessages;
    }

    private List<Pair<Sentence, Set<String>>> generateContextsWithLabels(CAS aCas,
            Type predictedType, Feature predictedFeature)
    {
        var contextsWithLabels = new ArrayList<Pair<Sentence, Set<String>>>();
        for (var context : aCas.select(Sentence.class)) {
            if (isBlank(context.getCoveredText())) {
                continue;
            }

            var mentions = aCas.<Annotation> select(predictedType).coveredBy(context);
            if (mentions.isEmpty()) {
                continue;
            }

            var labels = new HashSet<String>();
            for (var mention : mentions) {
                labels.add(FSUtil.getFeature(mention, predictedFeature, String.class));
            }

            contextsWithLabels.add(Pair.of(context, labels));
        }

        sort(contextsWithLabels, comparing(e -> e.getValue().size()));
        reverse(contextsWithLabels);
        return contextsWithLabels;
    }

    @Override
    public List<? extends ChatMessage> encode(PromptContext aPromptContext, String aPrompt)
    {
        return asList( //
                new ChatMessage(SYSTEM, "# Context\n\n" + aPromptContext.getText()), //
                new ChatMessage(USER, aPrompt));
    }

    @Override
    public void decode(RecommendationEngine aEngine, CAS aCas, PromptContext aContext,
            String aResponse)
    {
        var mentions = extractMentionFromJson(aResponse);

        mentionsToPredictions(aEngine, aCas, aContext, mentions);
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
                            var fields = toList(item.fields());
                            if (fields.size() == 1) {
                                // Looks like this
                                // "politicians": [
                                // { "name": "President Livingston" },
                                // { "name": "John" },
                                // { "name": "Don Horny" }
                                // ]
                                var nestedFieldIterator = item.fields();
                                while (nestedFieldIterator.hasNext()) {
                                    var nestedEntry = nestedFieldIterator.next();
                                    if (nestedEntry.getValue().isTextual()) {
                                        mentions.add(Pair.of(nestedEntry.getValue().asText(),
                                                fieldEntry.getKey()));
                                    }
                                    break;
                                }
                            }
                            else if (fields.size() >= 2) {
                                // Looks like this
                                // "politicians": [
                                // { "text": "President Livingston", "type"="politician" },
                                // { "text": "John", "type"="politician" },
                                // { "text": "Don Horny", "type"="politician" }
                                // ]
                                String text = null;
                                String label = null;
                                if (item.get("text") instanceof TextNode tn) {
                                    text = tn.asText();
                                }
                                else if (item.get("name") instanceof TextNode tn) {
                                    text = tn.asText();
                                }
                                if (item.get("type") instanceof TextNode tn) {
                                    label = tn.asText();
                                }
                                else if (item.get("value") instanceof TextNode tn) {
                                    label = tn.asText();
                                }
                                else if (item.get("label") instanceof TextNode tn) {
                                    label = tn.asText();
                                }
                                else if (item.get("category") instanceof TextNode tn) {
                                    label = tn.asText();
                                }
                                if (isNotBlank(text) && isNotBlank(label)) {
                                    mentions.add(Pair.of(text, label));
                                }
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

    private <T> List<T> toList(Iterator<T> aIterator)
    {
        var list = new ArrayList<T>();
        aIterator.forEachRemaining(list::add);
        return list;
    }

    private void mentionsToPredictions(RecommendationEngine aEngine, CAS aCas,
            PromptContext aContext, List<Pair<String, String>> mentions)
    {
        var tokenType = getType(aCas, Token.class);
        var predictedType = aEngine.getPredictedType(aCas);
        var predictedFeature = aEngine.getPredictedFeature(aCas);
        var isPredictionFeature = aEngine.getIsPredictionFeature(aCas);

        var dict = new Trie<Pair<String, String>>(WhitespaceNormalizingSanitizer.factory());
        for (var mention : mentions) {
            try {
                dict.put(mention.getKey(), mention);
            }
            catch (IllegalArgumentException e) {
                // Ignore mentions that are not valid after sanitization
            }
        }

        var contextRange = aContext.getRange();
        var tokens = aCas.<Annotation> select(tokenType)
                .coveredBy(contextRange.getBegin(), contextRange.getEnd()).asList();
        var matchedMentions = new HashSet<String>();
        for (var token : tokens) {
            var match = dict.getNode(aCas.getDocumentText(), token.getBegin());
            if (match != null) {
                var begin = token.getBegin();
                var end = begin + match.matchLength;
                var text = match.node.value.getKey();
                var label = match.node.value.getValue();
                var prediction = aCas.createAnnotation(predictedType, begin, end);

                prediction.setBooleanValue(isPredictionFeature, true);
                if (isNotBlank(label)) {
                    prediction.setStringValue(predictedFeature, label);
                }

                aCas.addFsToIndexes(prediction);
                LOG.debug("Prediction generated [{}] -> [{}]", text, label);
                matchedMentions.add(text);
            }
        }

        for (var mention : mentions) {
            if (!matchedMentions.contains(mention.getKey())) {
                LOG.debug("Mention [{}] not found", mention.getKey());
            }
        }
    }
}
