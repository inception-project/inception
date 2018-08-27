/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.recommendation.api.v2;

import java.util.List;

import org.apache.uima.cas.CAS;

public interface RecommendationEngine {
    /**
     * Given training data in {@code aCasses}, train a model. In order to save data between
     * This method must not mutate {@code aCasses} in any way.
     * @param aContext The context of the recommender
     * @param aCasses The training data
     */
    void train(RecommenderContext aContext, List<CAS> aCasses);
    void predict(RecommenderContext aContext, CAS aCas);

    /**
     * Evaluates the performance of a recommender by splitting the data given in {@code aCasses}
     * in training and test sets by using {@code aDataSplitter}, training on the training śet
     * and measuring performance on unseen data on the training set. This method must not
     * mutate {@code aCasses} in any way.
     * @param aCasses The CASses containing target annotations
     * @param aDataSplitter The splitter which determines which annotations belong to which set
     * @return Score measuring the performance of predicting on the test set
     */
    double evaluate(List<CAS> aCasses, DataSplitter aDataSplitter);

    default boolean isEvaluable()
    {
        return true;
    }
}
