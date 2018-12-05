/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.active.learning.strategy;

import java.io.Serializable;

import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;

public class RecommendationDifference implements Serializable
{

    private static final long serialVersionUID = -8453152434815622376L;
    private double difference;
    private AnnotationObject recommendation1;
    private AnnotationObject recommendation2;

    public RecommendationDifference(double difference, AnnotationObject recommendation1,
            AnnotationObject recommendation2)
    {
        this.difference = difference;
        this.recommendation1 = recommendation1;
        this.recommendation2 = recommendation2;
    }

    public RecommendationDifference(double difference, AnnotationObject recommendation1)
    {
        this.difference = difference;
        this.recommendation1 = recommendation1;
        this.recommendation2 = null;
    }

    public double getDifference()
    {
        return difference;
    }

    public AnnotationObject getRecommendation1()
    {
        return recommendation1;
    }

    public AnnotationObject getRecommendation2()
    {
        return recommendation2;
    }
}
