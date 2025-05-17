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
    private String url = "http://localhost:11434";
    private String nickname = "INCEpTION";

    private final AssistantChatProperties chat = new AssistantChatPropertiesImpl();
    private final AssistantEmbeddingProperties embedding = new AssistantEmbeddingPropertiesImpl();
    private final AssitantUserGuidePropertiesImpl userGuide = new AssitantUserGuidePropertiesImpl();
    private final AssistantToolPropertiesImpl tool = new AssistantToolPropertiesImpl();
    private final AssistantDocumentIndexProperties documentIndex;

    @Autowired
    public AssistantPropertiesImpl(AssistantDocumentIndexProperties aDocumentIndex)
    {
        documentIndex = aDocumentIndex;
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
    public AssistantChatProperties getChat()
    {
        return chat;
    }

    @Override
    public AssistantEmbeddingProperties getEmbedding()
    {
        return embedding;
    }

    @Override
    public AssitantUserGuideProperties getUserGuide()
    {
        return userGuide;
    }

    public AssistantToolPropertiesImpl getTool()
    {
        return tool;
    }

    @Override
    public AssistantDocumentIndexProperties getDocumentIndex()
    {
        return documentIndex;
    }

    public class AssitantUserGuidePropertiesImpl
        implements AssitantUserGuideProperties
    {
        private int maxChunks = 3;
        private double minScore = 0.8;
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
        private String model = "llama3.2";
        private final Set<String> capabilities = new HashSet<>();
        private String encoding = "cl100k_base";
        private int contextLength = AUTO_DETECT;
        private double topP = 0.3;
        private int topK = 25;
        private double repeatPenalty = 1.1;
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
        private String model = "granite-embedding";

        private int seed = 0xDEADBEEF;
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

    public static class AssistantToolPropertiesImpl
        implements AssistantToolProperties
    {
        private String model = "granite3-dense";

        @Override
        public String getModel()
        {
            return model;
        }

        public void setModel(String aModel)
        {
            model = aModel;
        }
    }
}
