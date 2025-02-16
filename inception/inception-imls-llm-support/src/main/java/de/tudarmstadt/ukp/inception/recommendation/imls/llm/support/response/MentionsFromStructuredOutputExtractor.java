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

import static com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON;
import static com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.ChatMessage.Role.SYSTEM;
import static java.util.Collections.reverse;
import static java.util.Collections.sort;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.normalizeSpace;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.ChatMessage;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class MentionsFromStructuredOutputExtractor
    implements ResponseExtractor
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
                mentions.add(new Mention(text, label));
            }
            var result = new MentionResult(mentions);

            examples.put(context.getCoveredText(), result);
            labelsSeen.addAll(contextWithLabels.getValue());
        }

        return examples;
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
    public Optional<ResponseFormat> getResponseFormat()
    {
        return Optional.of(ResponseFormat.JSON);
    }

    @Override
    public Optional<JsonNode> getJsonSchema()
    {
        var generator = new SchemaGenerator(
                new SchemaGeneratorConfigBuilder(DRAFT_2020_12, PLAIN_JSON) //
                        .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT) //
                        .with(new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED))
                        .build());

        return Optional.of(generator.generateSchema(MentionResult.class));
    }

    @Override
    public List<? extends ChatMessage> getFormatDefiningMessages(Recommender aRecommender,
            AnnotationSchemaService aSchemaService)
    {
        var messages = new ArrayList<ChatMessage>();

        messages.add(new ChatMessage(SYSTEM, """
                Your task is to identify and label mentions in the given document.
                The user will specify how to identify the mentions how to label them.
                Assign the text of the mention to the `coveredText` property.
                Assign the label of the mention to the `label` property.
                """));

        var tagset = aRecommender.getFeature().getTagset();
        if (tagset != null) {
            var tags = aSchemaService.listTags(tagset);
            if (!tags.isEmpty()) {
                var tagsMessageContent = new StringBuilder();

                if (tagset.isCreateTag()) {
                    tagsMessageContent.append(
                            "You can use the following values for the `label` property or come up with a new one if none "
                                    + "of the existing values is appropriate:\n");
                }
                else {
                    tagsMessageContent.append(
                            "You assign either of the following values to the `label` property:\n");
                }

                for (var tag : tags) {
                    tagsMessageContent.append("* `" + tag.getName() + "`");
                    if (isNotBlank(tag.getDescription())) {
                        tagsMessageContent.append(": " + tag.getDescription());
                    }
                    tagsMessageContent.append("\n");
                }
                messages.add(new ChatMessage(SYSTEM, tagsMessageContent.toString()));
            }
        }

        return messages;
    }

    @Override
    public void extractMentions(RecommendationEngine aEngine, CAS aCas, PromptContext aContext,
            String aResponse)
        throws JsonProcessingException
    {
        var mapper = new ObjectMapper();
        var result = mapper.readValue(aResponse, MentionResult.class);

        var text = aContext.getText();
        var predictedType = aEngine.getPredictedType(aCas);
        var predictedFeature = aEngine.getPredictedFeature(aCas);
        var isPredictionFeature = aEngine.getIsPredictionFeature(aCas);

        for (var mention : result.getMentions()) {
            var coveredText = mention.getCoveredText();
            if (coveredText.isBlank()) {
                LOG.debug("Blank mention ignored");
                continue;
            }

            var label = mention.getLabel();
            var lastIndex = 0;
            var index = text.indexOf(coveredText, lastIndex);
            var hitCount = 0;
            while (index >= 0) {
                int begin = aContext.getRange().getBegin() + index;
                var prediction = aCas.createAnnotation(predictedType, begin,
                        begin + coveredText.length());
                prediction.setBooleanValue(isPredictionFeature, true);
                if (label != null) {
                    prediction.setStringValue(predictedFeature, label);
                }
                aCas.addFsToIndexes(prediction);
                LOG.debug("Prediction generated [{}] -> [{}]", coveredText, label);
                hitCount++;

                lastIndex = index + coveredText.length();
                index = text.indexOf(coveredText, lastIndex);

                if (hitCount > text.length() / coveredText.length()) {
                    LOG.error(
                            "Mention detection seems to have entered into an endless loop - aborting");
                    break;
                }
            }

            if (hitCount == 0) {
                LOG.debug("Mention [{}] not found", coveredText);
            }
        }
    }
}
