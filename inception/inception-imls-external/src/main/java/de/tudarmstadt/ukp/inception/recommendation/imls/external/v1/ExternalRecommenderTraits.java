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
package de.tudarmstadt.ukp.inception.recommendation.imls.external.v1;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalRecommenderTraits
    implements Serializable
{
    private static final long serialVersionUID = -3109239605741337123L;

    private String remoteUrl;
    private boolean verifyCertificates = true;
    private boolean ranker;
    private TrainingCapability trainingCapability;

    @Deprecated
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private boolean trainable;

    public ExternalRecommenderTraits()
    {
        setTrainable(false);
    }

    public String getRemoteUrl()
    {
        return remoteUrl;
    }

    public void setRemoteUrl(String aRemoteUrl)
    {
        remoteUrl = aRemoteUrl;
    }

    @Deprecated
    public boolean isTrainable()
    {
        return trainable;
    }

    @Deprecated
    public void setTrainable(boolean aTrainable)
    {
        trainable = aTrainable;
        if (aTrainable) {
            trainingCapability = TrainingCapability.TRAINING_REQUIRED;
        }
        else {
            trainingCapability = TrainingCapability.TRAINING_NOT_SUPPORTED;
        }
    }

    public void setVerifyCertificates(boolean aVerifyCertificates)
    {
        verifyCertificates = aVerifyCertificates;
    }

    public boolean isVerifyCertificates()
    {
        return verifyCertificates;
    }

    public void setRanker(boolean aRanker)
    {
        ranker = aRanker;
    }

    public boolean isRanker()
    {
        return ranker;
    }

    public TrainingCapability getTrainingCapability()
    {
        return trainingCapability;
    }

    public void setTrainingCapability(TrainingCapability aTrainingCapability)
    {
        trainingCapability = aTrainingCapability;
    }
}
