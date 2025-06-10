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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits;

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptContextGenerator.VAR_EXAMPLES;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptContextGenerator.VAR_TAGS;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptContextGenerator.getPromptContextGenerator;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseExtractor.getResponseExtractor;
import static de.tudarmstadt.ukp.inception.scheduling.ProgressScope.SCOPE_UNITS;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.NonTrainableRecommenderEngineImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.JinjaPromptRenderer;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.MentionsSample;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseExtractor;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * @deprecated No longer used. We use the {@link ChatBasedLlmRecommenderImplBase} now.
 * @forRemoval 37.0
 */
@Deprecated(since = "36.0")
public abstract class LlmRecommenderImplBase<T extends LlmRecommenderTraits>
    extends NonTrainableRecommenderEngineImplBase
{
    private static final int MAX_FEW_SHOT_EXAMPLES = 10;

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected final T traits;
    protected final AnnotationSchemaService schemaService;
    protected final JinjaPromptRenderer promptRenderer;

    public LlmRecommenderImplBase(Recommender aRecommender, T aTraits,
            AnnotationSchemaService aSchemaService)
    {
        super(aRecommender);

        traits = aTraits;
        schemaService = aSchemaService;
        promptRenderer = new JinjaPromptRenderer();
    }

    protected Map<String, Object> prepareGlobalBindings(CAS aCas)
    {
        var globalBindings = new LinkedHashMap<String, Object>();

        provideExamples(globalBindings, aCas);
        provideTagSet(globalBindings);

        return globalBindings;
    }

    private void provideTagSet(Map<String, Object> globalBindings)
    {
        var tagset = getRecommender().getFeature().getTagset();
        if (tagset != null) {
            var tags = schemaService.listTags(tagset).stream() //
                    .collect(toMap( //
                            tag -> tag.getName(), //
                            tag -> Objects.toString(tag.getDescription(), "")));
            globalBindings.put(VAR_TAGS, tags);
        }
    }

    private void provideExamples(Map<String, Object> globalBindings, CAS aCas)
    {
        var responseExtractor = getResponseExtractor(traits);
        var examples = generateExamples(aCas, responseExtractor);
        globalBindings.put(VAR_EXAMPLES, examples);
    }

    private List<MentionsSample> generateExamples(CAS aCas, ResponseExtractor responseExtractor)
    {
        var examples = responseExtractor.generateExamples(this, aCas, MAX_FEW_SHOT_EXAMPLES);

        var mentionSamples = new ArrayList<MentionsSample>();
        for (var e : examples.entrySet()) {
            var sample = new MentionsSample(e.getKey());
            for (var m : e.getValue().getMentions()) {
                sample.addMention(m.getCoveredText(), m.getLabel());
            }
        }

        return mentionSamples;
    }

    @Override
    public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        if (StringUtils.isBlank(traits.getPrompt())) {
            throw new RecommendationException("No prompt has been provided");
        }

        var globalBindings = prepareGlobalBindings(aCas);

        var responseExtractor = getResponseExtractor(traits);

        var contexts = getPromptContextGenerator(traits.getPromptingMode()) //
                .generate(this, aCas, aBegin, aEnd, globalBindings).toList();

        try (var progress = aContext.getMonitor().openScope(SCOPE_UNITS, contexts.size())) {
            contexts.forEach(promptContext -> {
                try {
                    var prompt = promptRenderer.render(traits.getPrompt(), promptContext);
                    var response = exchange(prompt);
                    responseExtractor.extractMentions(this, aCas, promptContext, response);
                }
                catch (IOException e) {
                    aContext.log(LogMessage.warn(getRecommender().getName(),
                            "Remote failed to respond: %s", getRootCauseMessage(e)));
                    LOG.error("Remote failed to respond: {}", getRootCauseMessage(e));
                }
            });
        }

        return new Range(aBegin, aEnd);
    }

    protected abstract String exchange(String aPrompt) throws IOException;
}
