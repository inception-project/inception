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
package de.tudarmstadt.ukp.inception.assistant.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("assistant")
public class AssistantPropertiesImpl
    implements AssistantProperties
{
    /** URL of the Ollama service. */
    private String url = "http://localhost:11434";

    /** The name by which the assistant identifies itself. */
    private String nickname = "INCEpTION";

    /** API key sent to the Ollama service, when required. Empty by default. */
    private String apiKey = null;

    /**
     * Whether an extra LLM call should be made to generate a summary line for thinking sections.
     */
    private boolean summarizeThoughts = false;

    private final AssistantChatPropertiesImpl chat = new AssistantChatPropertiesImpl();
    private final AssistantEmbeddingPropertiesImpl embedding = new AssistantEmbeddingPropertiesImpl();
    private final AssistantUserGuidePropertiesImpl userGuide = new AssistantUserGuidePropertiesImpl();

    private @Autowired AssistantDocumentIndexProperties documentIndex;

    public void setApiKey(String aApiKey)
    {
        apiKey = aApiKey;
    }

    @Override
    public String getApiKey()
    {
        return apiKey;
    }

    @Override
    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String aNickname)
    {
        nickname = aNickname;
    }

    public void setSummarizeThoughts(boolean aSummarizeThoughts)
    {
        summarizeThoughts = aSummarizeThoughts;
    }

    @Override
    public boolean isSummarizeThoughts()
    {
        return summarizeThoughts;
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    public void setUrl(String aAssistantUrl)
    {
        url = aAssistantUrl;
    }

    @Override
    public AssistantChatPropertiesImpl getChat()
    {
        return chat;
    }

    @Override
    public AssistantEmbeddingPropertiesImpl getEmbedding()
    {
        return embedding;
    }

    @Override
    public AssistantUserGuidePropertiesImpl getUserGuide()
    {
        return userGuide;
    }

    public void setDocumentIndex(AssistantDocumentIndexProperties aDocumentIndex)
    {
        documentIndex = aDocumentIndex;
    }

    @Override
    public AssistantDocumentIndexProperties getDocumentIndex()
    {
        return documentIndex;
    }

    public static class AssistantUserGuidePropertiesImpl
        implements AssitantUserGuideProperties
    {
        /**
         * Maximum number of relevant chunks from the user guide to pass to the LLM service.
         */
        private int maxChunks = 3;

        /**
         * Minimum relevance score for chunks to be considered. Should be a positive number not
         * larger than {@code 1.0}.
         */
        private double minScore = 0.8;

        /** Whether to re-build the user manual index when the application starts. */
        private boolean rebuildIndexOnBoot = false;

        @Override
        public int getMaxChunks()
        {
            return maxChunks;
        }

        public void setMaxChunks(int aCount)
        {
            maxChunks = aCount;
        }

        @Override
        public double getMinScore()
        {
            return minScore;
        }

        public void setMinScore(double aScore)
        {
            minScore = aScore;
        }

        @Override
        public boolean isRebuildIndexOnBoot()
        {
            return rebuildIndexOnBoot;
        }

        public void setRebuildIndexOnBoot(boolean aFlag)
        {
            rebuildIndexOnBoot = aFlag;
        }
    }

    public static class AssistantChatPropertiesImpl
        implements AssistantChatProperties
    {
        /** The model used to drive the chat functionality of the assistant. */
        private String model = "llama3.2";

        /**
         * The capabilities of the model. Relevant values are {@code auto}, {@code completion} and
         * {@code tools}. With {@code auto} the system queries the LLM service during startup to
         * determine the model's capabilities; setting this manually overrides the detection and
         * avoids the startup query.
         */
        // Default ([auto]) is declared in META-INF/additional-spring-configuration-metadata.json
        // because the metadata processor cannot read instance initializer blocks.
        private final Set<String> capabilities = new HashSet<>();

        /**
         * The token encoding used by the model. Used to estimate token counts so that text can be
         * chunked accurately. Leave at the default if in doubt.
         */
        private String encoding = "cl100k_base";

        /**
         * The context length supported by the model. Controls how much chat history is preserved.
         * With {@code 0} (the default), the LLM service is queried during startup to detect the
         * value automatically. Setting it manually avoids the startup query.
         */
        private int contextLength = AUTO_DETECT;

        /**
         * Diversity of output: tokens are sampled from the smallest set whose probabilities sum to
         * this threshold. Tune the balance between diversity (high, 0.9) and coherence (low,
         * 0.3-0.5).
         */
        private double topP = 0.3;

        /**
         * Number of top-ranked tokens considered for sampling during text generation. Tune the
         * balance between diversity (high &gt;= 50) and coherence (low, e.g. 5).
         */
        private int topK = 25;

        /**
         * Discourages the model from repeating the same words or phrases by reducing the
         * probability of already-generated tokens, promoting more varied and coherent output. Tune
         * the balance between less repetition (high &gt;= 1.5) and more repetition (low, 1.0).
         */
        private double repeatPenalty = 1.1;

        /**
         * Randomness of the model's output, by adjusting the probability distribution of the next
         * word. Tune the balance between more randomness (high &gt;= 1.0) and less randomness (low,
         * 0.5).
         */
        private double temperature = 0.1;

        {
            capabilities.add(AUTO_DETECT_CAPABILITIES);
        }

        public void setCapabilities(Collection<String> aCapabilities)
        {
            capabilities.clear();
            if (aCapabilities != null) {
                capabilities.addAll(aCapabilities);
            }
        }

        @Override
        public Set<String> getCapabilities()
        {
            return capabilities;
        }

        @Override
        public String getModel()
        {
            return model;
        }

        public void setModel(String aModel)
        {
            model = aModel;
        }

        @Override
        public double getTopP()
        {
            return topP;
        }

        public void setTopP(double aTopP)
        {
            topP = aTopP;
        }

        @Override
        public int getTopK()
        {
            return topK;
        }

        public void setTopK(int aTopK)
        {
            topK = aTopK;
        }

        @Override
        public double getRepeatPenalty()
        {
            return repeatPenalty;
        }

        public void setRepeatPenalty(double aRepeatPenalty)
        {
            repeatPenalty = aRepeatPenalty;
        }

        @Override
        public double getTemperature()
        {
            return temperature;
        }

        public void setTemperature(double aTemperature)
        {
            temperature = aTemperature;
        }

        @Override
        public int getContextLength()
        {
            return contextLength;
        }

        public void setContextLength(int aContextLength)
        {
            contextLength = aContextLength;
        }

        @Override
        public String getEncoding()
        {
            return encoding;
        }

        public void setEncoding(String aEncoding)
        {
            encoding = aEncoding;
        }
    }

    public static class AssistantEmbeddingPropertiesImpl
        implements AssistantEmbeddingProperties
    {
        /** The model used to drive the search functionality of the assistant. */
        private String model = "granite-embedding";

        /** Random number generator seed used to ensure repeatable retrieval results. */
        private int seed = 0xDEADBEEF;

        /**
         * Context length supported by the model in LLM tokens. Controls how much of a chunk is used
         * to calculate the embedding. Should not be lower than
         * {@code assistant.documents.chunk-size}.
         */
        private int contextLength = 768;

        /**
         * Maximum number of chunks sent together to the LLM service when generating embeddings.
         * Batching multiple chunks in a single request increases indexing speed.
         */
        private int batchSize = 16;

        /**
         * The token encoding used by the model. Used to estimate token counts so that text can be
         * chunked accurately. Leave at the default if in doubt.
         */
        private String encoding = "cl100k_base";

        /**
         * Dimension of the embedding vectors created by the model. With {@code 0} (the default),
         * the LLM service is queried during startup to auto-detect the vector size; setting this
         * manually is helpful when the LLM service may be unavailable during startup. Changing the
         * value requires indexes to be rebuilt. Dimensions higher than 1024 are not recommended due
         * to memory usage and reduced performance.
         */
        private int dimension = AUTO_DETECT_DIMENSION;

        @Override
        public String getModel()
        {
            return model;
        }

        public void setModel(String aModel)
        {
            model = aModel;
        }

        @Override
        public int getSeed()
        {
            return seed;
        }

        public void setSeed(int aSeed)
        {
            seed = aSeed;
        }

        @Override
        public int getContextLength()
        {
            return contextLength;
        }

        public void setContextLength(int aContextLength)
        {
            contextLength = aContextLength;
        }

        @Override
        public int getBatchSize()
        {
            return batchSize;
        }

        public void setBatchSize(int aBatchSize)
        {
            batchSize = aBatchSize;
        }

        @Override
        public String getEncoding()
        {
            return encoding;
        }

        public void setEncoding(String aEncoding)
        {
            encoding = aEncoding;
        }

        @Override
        public int getDimension()
        {
            return dimension;
        }

        @Override
        public void setDimension(int aDimension)
        {
            dimension = aDimension;
        }
    }
}
