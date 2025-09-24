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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm;

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptContextGenerator.VAR_EXAMPLES;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptContextGenerator.getPromptContextGenerator;
import static de.tudarmstadt.ukp.inception.scheduling.ProgressScope.SCOPE_UNITS;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.NonTrainableRecommenderEngineImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.JinjaPromptRenderer;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderTraits;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public abstract class ChatBasedLlmRecommenderImplBase<T extends LlmRecommenderTraits>
    extends NonTrainableRecommenderEngineImplBase
{
    private static final int MAX_FEW_SHOT_EXAMPLES = 10;

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected final T traits;
    protected final AnnotationSchemaService schemaService;
    protected final AnnotationTaskCodecExtensionPoint taskCodecExtensionPoint;
    protected final JinjaPromptRenderer promptRenderer;

    public ChatBasedLlmRecommenderImplBase(Recommender aRecommender, T aTraits,
            AnnotationSchemaService aSchemaService,
            AnnotationTaskCodecExtensionPoint aResponseExtractorExtensionPoint)
    {
        super(aRecommender);

        traits = aTraits;
        schemaService = aSchemaService;
        taskCodecExtensionPoint = aResponseExtractorExtensionPoint;
        promptRenderer = new JinjaPromptRenderer();
    }

    protected Map<String, Object> prepareGlobalBindings(CAS aCas)
    {
        var globalBindings = new LinkedHashMap<String, Object>();

        provideExamples(globalBindings, aCas);

        return globalBindings;
    }

    private void provideExamples(Map<String, Object> globalBindings, CAS aCas)
    {
        var responseExtractor = taskCodecExtensionPoint.getExtension(recommender, traits).get();
        var examples = responseExtractor.generateExamples(this, aCas, MAX_FEW_SHOT_EXAMPLES);
        globalBindings.put(VAR_EXAMPLES, examples);
    }

    @Override
    public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        var codec = taskCodecExtensionPoint.getExtension(recommender, traits).get();

        var staticMessages = new ArrayList<ChatMessage>();

        staticMessages.addAll(codec.getFormatDefiningMessages(getRecommender(), schemaService));

        var responseformat = codec.getResponseFormat();
        var jsonSchema = codec.getJsonSchema(getRecommender(), schemaService, traits);

        var globalBindings = prepareGlobalBindings(aCas);
        var contexts = getPromptContextGenerator(traits.getPromptingMode()) //
                .generate(this, aCas, aBegin, aEnd, globalBindings) //
                .toList();

        try (var progress = aContext.getMonitor().openScope(SCOPE_UNITS, contexts.size())) {
            for (var promptContext : contexts) {
                if (aContext.getMonitor().isCancelled()) {
                    break;
                }

                progress.update(up -> up.increment());

                if (isBlank(promptContext.getText())) {
                    continue;
                }

                try {
                    var messages = new ArrayList<>(staticMessages);
                    messages.addAll(codec.encode(promptContext, traits.getPrompt()));
                    var response = exchange(messages, responseformat.orElse(null),
                            jsonSchema.orElse(null));
                    codec.decode(this, aCas, promptContext, response);
                }
                catch (IOException e) {
                    aContext.log(LogMessage.warn(getRecommender().getName(),
                            "Remote failed to respond: %s", getRootCauseMessage(e)));
                    LOG.error("Remote failed to respond: {}", getRootCauseMessage(e), e);
                }
            }
        }

        return new Range(aBegin, aEnd);
    }

    protected abstract String exchange(List<ChatMessage> aPrompt, ResponseFormat aResponseformat,
            JsonNode aJsonSchema)
        throws IOException;
}
