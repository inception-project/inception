/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

public enum RecommendationEngineCapability 
{
    /**
     * {@link RecommendationEngine} does not support training. Calling 
     * {@link RecommendationEngine#train} may be a no-op at
     * best or result in an exception at worst.
     */
    TRAINING_NOT_SUPPORTED,

    /**
     * {@link RecommendationEngine} supports training but does not require it. Thus,
     * {@link RecommendationEngine#predict} can be called even if there was not training data.
     * {@link RecommendationEngine#isReadyForPrediction} may return {@code true}, even if
     * {@link RecommendationEngine#train} has not been called before.
     */
    TRAINING_SUPPORTED,
    
    /**
     * {@link RecommendationEngine} requires training. {@link RecommendationEngine#train} must
     * be called to initialize a context before {@link RecommendationEngine#predict} can be used.
     */
    TRAINING_REQUIRED
}
