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
package de.tudarmstadt.ukp.inception.recommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link RecommenderServiceAutoConfiguration}.
 * </p>
 */
@ConfigurationProperties("recommender")
public class RecommenderPropertiesImpl
    implements RecommenderProperties
{
    private boolean enabled;
    private boolean actionButtonsEnabled;
    private Messages messages = new Messages();

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean aEnabled)
    {
        enabled = aEnabled;
    }

    @Override
    public boolean isActionButtonsEnabled()
    {
        return actionButtonsEnabled;
    }

    public void setActionButtonsEnabled(boolean aActionButtonsEnabled)
    {
        actionButtonsEnabled = aActionButtonsEnabled;
    }

    @Override
    public Messages getMessages()
    {
        return messages;
    }

    public class Messages
    {
        private boolean noNewPredictionsAvailable = false;
        private boolean newPredictionsAvailable = false;
        private boolean evaluationSuccessful = false;
        private boolean evaluationFailed = true;
        private boolean nonTrainableRecommenderActivation = false;

        public boolean isNoNewPredictionsAvailable()
        {
            return noNewPredictionsAvailable;
        }

        public void setNoNewPredictionsAvailable(boolean aNoNewPredictionsAvailable)
        {
            noNewPredictionsAvailable = aNoNewPredictionsAvailable;
        }

        public boolean isNewPredictionsAvailable()
        {
            return newPredictionsAvailable;
        }

        public void setNewPredictionsAvailable(boolean aNewPredictionsAvailable)
        {
            newPredictionsAvailable = aNewPredictionsAvailable;
        }

        public boolean isEvaluationSuccessful()
        {
            return evaluationSuccessful;
        }

        public void setEvaluationSuccessful(boolean aEvaluationSuccessful)
        {
            evaluationSuccessful = aEvaluationSuccessful;
        }

        public boolean isEvaluationFailed()
        {
            return evaluationFailed;
        }

        public void setEvaluationFailed(boolean aEvaluationFailed)
        {
            evaluationFailed = aEvaluationFailed;
        }

        public boolean isNonTrainableRecommenderActivation()
        {
            return nonTrainableRecommenderActivation;
        }

        public void setNonTrainableRecommenderActivation(boolean aNonTrainableRecommenderActivation)
        {
            nonTrainableRecommenderActivation = aNonTrainableRecommenderActivation;
        }

    }
}
