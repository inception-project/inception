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
package de.tudarmstadt.ukp.inception.assistant.embedding;

import static de.tudarmstadt.ukp.inception.assistant.config.AssistantEmbeddingProperties.AUTO_DETECT_DIMENSION;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmChatClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmChatClientExtensionPoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmEndpoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaLlmChatClient;

public class EmbeddingServiceImpl
    implements EmbeddingService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String OPT_NUM_CTX = "num_ctx";
    private static final String OPT_SEED = "seed";

    private final AssistantProperties properties;
    private final LlmChatClientExtensionPoint chatClientExtensionPoint;

    public EmbeddingServiceImpl(AssistantProperties aProperties,
            LlmChatClientExtensionPoint aChatClientExtensionPoint)
    {
        properties = aProperties;
        chatClientExtensionPoint = aChatClientExtensionPoint;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        autoDetectEmbeddingDimension();
    }

    @Override
    public int getDimension()
    {
        autoDetectEmbeddingDimension();
        return properties.getEmbedding().getDimension();
    }

    @Override
    public Optional<float[]> embed(String aQuery) throws IOException
    {
        var result = embed(new String[] { aQuery });

        if (result.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(result.get(0).getValue());
    }

    @Override
    public <T> List<Pair<T, float[]>> embed(Function<T, String> aExtractor, Iterable<T> aObjects)
        throws IOException
    {
        autoDetectEmbeddingDimension();

        var strings = new ArrayList<String>();
        var objects = new ArrayList<T>();
        for (var o : aObjects) {
            var s = removeEmptyLinesAndTrim(aExtractor.apply(o));

            if (s.isEmpty() || hasHighProportionOfShortSequences(s)
                    || hasHighProportionOfWhitespaceOrLineBreaks(s)) {
                continue;
            }

            strings.add(s);
            objects.add(o);
        }

        var vectors = client().embed(endpoint(), strings, embeddingOptions());

        var result = new ArrayList<Pair<T, float[]>>();
        for (var i = 0; i < vectors.size(); i++) {
            result.add(Pair.of(objects.get(i), vectors.get(i)));
        }
        return result;
    }

    @Override
    public List<Pair<String, float[]>> embed(String... aStrings) throws IOException
    {
        autoDetectEmbeddingDimension();

        var strings = new ArrayList<String>();
        for (var s : aStrings) {
            s = removeEmptyLinesAndTrim(s);

            if (s.isEmpty() || hasHighProportionOfShortSequences(s)
                    || hasHighProportionOfWhitespaceOrLineBreaks(s)) {
                continue;
            }

            strings.add(s);
        }

        var vectors = client().embed(endpoint(), strings, embeddingOptions());

        var result = new ArrayList<Pair<String, float[]>>();
        for (var i = 0; i < vectors.size(); i++) {
            result.add(Pair.of(strings.get(i), vectors.get(i)));
        }
        return result;
    }

    private LlmChatClient client()
    {
        // Provider is hardcoded to Ollama for now; once assistant config moves to UI-driven
        // traits, this becomes traits.getProviderId().
        return chatClientExtensionPoint.getExtension(OllamaLlmChatClient.ID) //
                .orElseThrow(() -> new IllegalStateException(
                        "Ollama LLM client not registered — is the inception-imls-ollama module on "
                                + "the classpath?"));
    }

    private LlmEndpoint endpoint()
    {
        return new LlmEndpoint(OllamaLlmChatClient.ID, properties.getUrl(),
                properties.getEmbedding().getModel(), null);
    }

    private Map<String, Object> embeddingOptions()
    {
        var options = new LinkedHashMap<String, Object>();
        options.put(OPT_NUM_CTX, properties.getEmbedding().getContextLength());
        options.put(OPT_SEED, properties.getEmbedding().getSeed());
        return options;
    }

    private void autoDetectEmbeddingDimension()
    {
        var embeddingProperties = properties.getEmbedding();
        synchronized (embeddingProperties) {
            if (embeddingProperties.getDimension() == AUTO_DETECT_DIMENSION) {
                try {
                    LOG.info("Contacting [{}] to auto-detect dimension of model [{}]...",
                            properties.getUrl(), embeddingProperties.getModel());
                    var vectors = client().embed(endpoint(),
                            List.of("We just need to know the dimension of the generated "
                                    + "embedding. Thanks!"),
                            null);
                    var dim = vectors.get(0).length;
                    embeddingProperties.setDimension(dim);
                    LOG.info("Auto-detected embedding dimension of model [{}]: {}",
                            embeddingProperties.getModel(), dim);
                }
                catch (Exception e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.warn("Unable to auto-detect embedding dimension - using default", e);
                    }
                    else {
                        LOG.warn("Unable to auto-detect embedding dimension - using default");
                    }
                    embeddingProperties.setDimension(1024);
                }
            }
        }
    }

    static String removeEmptyLinesAndTrim(String aString)
    {
        if (aString == null || aString.isEmpty()) {
            return aString;
        }

        return aString.lines() //
                .map(String::trim).filter(line -> !line.isEmpty()) // Remove empty lines
                .reduce((line1, line2) -> line1 + "\n" + line2) // Combine lines with line breaks
                .orElse(""); // Default to an empty string if no lines remain
    }

    static boolean hasHighProportionOfShortSequences(String aString)
    {
        if (aString == null || aString.isEmpty()) {
            return false;
        }

        // Split the string into words
        var words = aString.split("\\s+");

        // Count single and double-character words
        long shortWordCount = 0;
        for (var word : words) {
            if (word.length() <= 2) {
                shortWordCount++;
            }
        }

        // Consider "high proportion" to be more than 50%
        var shortWordProportion = (double) shortWordCount / words.length;
        return shortWordProportion > 0.5;
    }

    static boolean hasHighProportionOfWhitespaceOrLineBreaks(String aString)
    {
        if (aString == null || aString.isEmpty()) {
            return false;
        }

        var totalChars = aString.length();

        var whitespaceOrLineBreakCount = aString.chars()
                .filter(c -> Character.isWhitespace(c) || c == '\n' || c == '\r').count();

        // Consider "high proportion" to be more than 50%
        double proportion = (double) whitespaceOrLineBreakCount / totalChars;
        return proportion > 0.5;
    }
}
