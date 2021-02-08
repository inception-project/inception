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
package de.tudarmstadt.ukp.inception.conceptlinking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link EntityLinkingServiceAutoConfiguration#entityLinkingProperties}.
 * </p>
 */
@ConfigurationProperties("knowledge-base.entity-linking")
public class EntityLinkingPropertiesImpl
    implements EntityLinkingProperties
{
    private int cacheSize = 1024;

    private int mentionContextSize = 5;
    private int candidateQueryLimit = 2500;
    private int candidateDisplayLimit = 100;
    private int signatureQueryLimit = Integer.MAX_VALUE;

    @Override
    public int getCacheSize()
    {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize)
    {
        this.cacheSize = cacheSize;
    }

    @Override
    public int getMentionContextSize()
    {
        return mentionContextSize;
    }

    public void setMentionContextSize(int mentionContextSize)
    {
        this.mentionContextSize = mentionContextSize;
    }

    @Override
    public int getCandidateQueryLimit()
    {
        return candidateQueryLimit;
    }

    public void setCandidateQueryLimit(int candidateQueryLimit)
    {
        this.candidateQueryLimit = candidateQueryLimit;
    }

    @Override
    public int getCandidateDisplayLimit()
    {
        return candidateDisplayLimit;
    }

    public void setCandidateDisplayLimit(int candidateDisplayLimit)
    {
        this.candidateDisplayLimit = candidateDisplayLimit;
    }

    @Override
    public int getSignatureQueryLimit()
    {
        return signatureQueryLimit;
    }

    public void setSignatureQueryLimit(int signatureQueryLimit)
    {
        this.signatureQueryLimit = signatureQueryLimit;
    }
}
