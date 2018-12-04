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
package de.tudarmstadt.ukp.inception.active.learning.sidebar;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningService;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;

public interface ActiveLearningStrategy
{
    Optional<RecommendationDifference> updateRecommendations(LearningRecordService aRecordService,
            Date learnSkippedRecommendationTime);

    Optional<RecommendationDifference> generateRecommendationWithLowestDifference(
            LearningRecordService aRecordService, Date learnSkippedRecommendationTime,
            List<List<AnnotationObject>> aListOfRecommendationsForEachToken);

    boolean hasRecommendationWhichIsSkipped(LearningRecordService aRecordService,
            ActiveLearningService aActiveLearningService);

    Optional<AnnotationObject> generateRecommendationWithLowestConfidence(
            ActiveLearningService aActiveLearningService, JCas aJcas);

    boolean checkRecommendationExist(ActiveLearningService aActiveLearningService,
            LearningRecord aRecord);
}
