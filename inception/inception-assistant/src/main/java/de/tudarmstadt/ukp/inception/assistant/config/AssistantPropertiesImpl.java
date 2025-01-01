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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("assistant")
public class AssistantPropertiesImpl
    implements AssistantProperties
{
    private String url = "http://localhost:11434";

    private boolean forceRebuildUserManualIndex = false;
    private int maxUserManualPassages = 3;
    private double minUserManualPassageRelevance = 0.8;

    private AssistantChatProperties chat = new AssistantChatPropertiesImpl();
    private AssistantEmbeddingProperties embedding = new AssistantEmbeddingPropertiesImpl();

    @Value("${inception.dev:false}") // Inject system property or use default if not provided
    private boolean devMode;

    @Override
    public boolean isDevMode()
    {
        return devMode;
    }

    public void setDevMode(boolean aDevMode)
    {
        devMode = aDevMode;
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
    public boolean isForceRebuildUserManualIndex()
    {
        return forceRebuildUserManualIndex;
    }

    public void setForceRebuildUserManualIndex(boolean aForceRebuildUserManualIndex)
    {
        forceRebuildUserManualIndex = aForceRebuildUserManualIndex;
    }

    @Override
    public int getMaxUserManualPassages()
    {
        return maxUserManualPassages;
    }

    public void setMaxUserManualPassages(int aMaxUserManualPassages)
    {
        maxUserManualPassages = aMaxUserManualPassages;
    }

    @Override
    public double getMinUserManualPassageRelevance()
    {
        return minUserManualPassageRelevance;
    }

    public void setMinUserManualPassageRelevance(double aMinUserManualPassageRelevance)
    {
        minUserManualPassageRelevance = aMinUserManualPassageRelevance;
    }

    @Override
    public AssistantChatProperties getChat()
    {
        return chat;
    }

    public void setChat(AssistantChatProperties aChat)
    {
        chat = aChat;
    }

    @Override
    public AssistantEmbeddingProperties getEmbedding()
    {
        return embedding;
    }

    public void setEmbedding(AssistantEmbeddingProperties aEmbedding)
    {
        embedding = aEmbedding;
    }

    public static class AssistantChatPropertiesImpl
        implements AssistantChatProperties
    {
        private String model = "llama3.2";

        private double topP = 0.3;
        private int topK = 25;
        private double repeatPenalty = 1.1;
        private double temperature = 0.1;
        private int contextLength = 4096;
        private String encoding = "cl100k_base";

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
        private String model = "granite-embedding";

        private int seed = 0xDEADBEEF;
        private double topP = 1.0;
        private int topK = 1000;
        private double repeatPenalty = 1.0;
        private double temperature = 0.0;
        private int contextLength = 768;
        private int batchSize = 16;
        private String encoding = "cl100k_base";
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

        public void setDimension(int aDimension)
        {
            dimension = aDimension;
        }
    }
}
