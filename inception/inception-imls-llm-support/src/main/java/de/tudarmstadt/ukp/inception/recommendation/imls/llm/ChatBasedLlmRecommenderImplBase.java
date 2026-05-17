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
import static java.util.Collections.emptyList;
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

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.NonTrainableRecommenderEngineImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatOptions;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmChatClientExtensionPoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmEndpoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.JinjaPromptRenderer;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderTraits;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.Range;
import tools.jackson.databind.JsonNode;

public abstract class ChatBasedLlmRecommenderImplBase<T extends LlmRecommenderTraits>
    extends NonTrainableRecommenderEngineImplBase
{
    private static final int MAX_FEW_SHOT_EXAMPLES = 10;

    private static final String OPT_TEMPERATURE = "temperature";
    private static final String OPT_TOP_P = "top_p";
    private static final String OPT_SEED = "seed";

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected final T traits;
    protected final AnnotationSchemaService schemaService;
    protected final AnnotationTaskCodecExtensionPoint taskCodecExtensionPoint;
    protected final LlmChatClientExtensionPoint chatClientExtensionPoint;
    protected final JinjaPromptRenderer promptRenderer;

    public ChatBasedLlmRecommenderImplBase(Recommender aRecommender, T aTraits,
            AnnotationSchemaService aSchemaService,
            AnnotationTaskCodecExtensionPoint aResponseExtractorExtensionPoint,
            LlmChatClientExtensionPoint aChatClientExtensionPoint)
    {
        super(aRecommender);

        traits = aTraits;
        schemaService = aSchemaService;
        taskCodecExtensionPoint = aResponseExtractorExtensionPoint;
        chatClientExtensionPoint = aChatClientExtensionPoint;
        promptRenderer = new JinjaPromptRenderer();
    }

    /**
     * Identifier of the {@code LlmChatClient} that handles this recommender's provider. Must match
     * the {@code getId()} of the registered adapter.
     */
    protected abstract String getProviderId();

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

    protected String exchange(List<ChatMessage> aPrompt, ResponseFormat aResponseformat,
            JsonNode aJsonSchema)
        throws IOException
    {
        var providerId = getProviderId();
        var client = chatClientExtensionPoint.getExtension(providerId)
                .orElseThrow(() -> new IOException("No LLM client registered for provider ["
                        + providerId + "] - is the corresponding module enabled?"));

        var endpoint = new LlmEndpoint(providerId, traits.getUrl(), traits.getModel(),
                traits.getAuthentication());

        var options = recommenderDefaults(traits.getOptions());
        var chatOptions = new ChatOptions(aResponseformat, aJsonSchema, emptyList(), options);

        var result = client.chat(endpoint, aPrompt, chatOptions);
        LOG.trace("[{}] response: [{}]", providerId, result.message().content());
        return result.message().content();
    }

    /**
     * Apply recommender-use-case defaults to the option bag without mutating the traits-owned map:
     * deterministic seed for reproducibility, and {@code temperature=0} unless the user has already
     * set either temperature or top_p.
     */
    private static Map<String, Object> recommenderDefaults(Map<String, Object> aOptions)
    {
        var out = new LinkedHashMap<String, Object>();
        if (aOptions != null) {
            out.putAll(aOptions);
        }
        if (!out.containsKey(OPT_TEMPERATURE) && !out.containsKey(OPT_TOP_P)) {
            out.put(OPT_TEMPERATURE, 0.0d);
        }
        out.putIfAbsent(OPT_SEED, 0xdeadbeef);
        return out;
    }
}
