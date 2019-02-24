/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.inception.conceptlinking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("inception.entity-linking")
public class EntityLinkingProperties
{
    private int cacheSize = 1024;

    private int mentionContextSize = 5;
    private int candidateQueryLimit = 2500;
    private int candidateDisplayLimit = 100;
    private int candidateFrequencyThreshold = 25;
    private int signatureQueryLimit = Integer.MAX_VALUE;

    public int getCacheSize()
    {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize)
    {
        this.cacheSize = cacheSize;
    }

    public int getMentionContextSize()
    {
        return mentionContextSize;
    }

    public void setMentionContextSize(int mentionContextSize)
    {
        this.mentionContextSize = mentionContextSize;
    }

    public int getCandidateQueryLimit()
    {
        return candidateQueryLimit;
    }

    public void setCandidateQueryLimit(int candidateQueryLimit)
    {
        this.candidateQueryLimit = candidateQueryLimit;
    }

    public int getCandidateDisplayLimit()
    {
        return candidateDisplayLimit;
    }

    public void setCandidateDisplayLimit(int candidateDisplayLimit)
    {
        this.candidateDisplayLimit = candidateDisplayLimit;
    }

    public int getCandidateFrequencyThreshold()
    {
        return candidateFrequencyThreshold;
    }

    public void setCandidateFrequencyThreshold(int candidateFrequencyThreshold)
    {
        this.candidateFrequencyThreshold = candidateFrequencyThreshold;
    }

    public int getSignatureQueryLimit()
    {
        return signatureQueryLimit;
    }

    public void setSignatureQueryLimit(int signatureQueryLimit)
    {
        this.signatureQueryLimit = signatureQueryLimit;
    }
}

