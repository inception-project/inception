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
package de.tudarmstadt.ukp.inception.recommendation.log;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderEvaluationResultEvent;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RecommenderServiceAutoConfiguration#recommenderEvaluationResultEventAdapter}.
 * </p>
 */
public class RecommenderEvaluationResultEventAdapter
    implements EventLoggingAdapter<RecommenderEvaluationResultEvent>
{
    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return RecommenderEvaluationResultEvent.class.isAssignableFrom(aEvent);
    }

    @Override
    public long getProject(RecommenderEvaluationResultEvent aEvent)
    {
        return aEvent.getRecommender().getProject().getId();
    }

    @Override
    public String getAnnotator(RecommenderEvaluationResultEvent aEvent)
    {
        return aEvent.getUser();
    }

    @Override
    public String getUser(RecommenderEvaluationResultEvent aEvent)
    {
        return aEvent.getUser();
    }

    @Override
    public String getDetails(RecommenderEvaluationResultEvent aEvent) throws IOException
    {
        var details = new Details();

        details.recommenderId = aEvent.getRecommender().getId();

        EvaluationResult result = aEvent.getResult();
        details.accuracy = result.computeAccuracyScore();
        details.f1 = result.computeF1Score();
        details.precision = result.computePrecisionScore();
        details.recall = result.computeRecallScore();

        details.trainSetSize = result.getTrainingSetSize();
        details.testSetSize = result.getTestSetSize();

        details.active = aEvent.isActive();
        details.duration = aEvent.getDuration();
        details.threshold = aEvent.getRecommender().getThreshold();
        details.layer = aEvent.getRecommender().getLayer().getName();
        details.feature = aEvent.getRecommender().getFeature().getName();
        details.tool = aEvent.getRecommender().getTool();

        return JSONUtil.toJsonString(details);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Details
    {
        // Recommender configuration
        public Long recommenderId;
        public String layer;
        public String feature;
        public String tool;
        public double threshold;

        // Evaluation process telemetry
        public long duration;

        // Evaluation results
        public boolean active;
        public double accuracy;
        public double f1;
        public double precision;
        public double recall;

        // Used data
        public int trainSetSize;
        public int testSetSize;
    }
}
