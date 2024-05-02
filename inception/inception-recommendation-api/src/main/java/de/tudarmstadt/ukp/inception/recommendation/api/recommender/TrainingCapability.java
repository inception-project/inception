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
package de.tudarmstadt.ukp.inception.recommendation.api.recommender;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TrainingCapability
{
    /**
     * {@link RecommendationEngine} does not support training. Calling
     * {@link RecommendationEngine#train} may be a no-op at best or result in an exception at worst.
     */
    @JsonProperty("training-not-supported")
    TRAINING_NOT_SUPPORTED,

    /**
     * {@link RecommendationEngine} supports training but does not require it. Thus,
     * {@link RecommendationEngine#predict} can be called even if there was not training data.
     * {@link RecommendationEngine#isReadyForPrediction} may return {@code true}, even if
     * {@link RecommendationEngine#train} has not been called before. It also means that prediction
     * will be called repeatedly (i.e. data predicted once will not be cached indefinitely as with
     * {link {@link #TRAINING_NOT_SUPPORTED}}).
     */
    @JsonProperty("training-supported")
    TRAINING_SUPPORTED,

    /**
     * {@link RecommendationEngine} requires training. {@link RecommendationEngine#train} must be
     * called to initialize a context before {@link RecommendationEngine#predict} can be used.
     */
    @JsonProperty("training-required")
    TRAINING_REQUIRED
}
