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
package de.tudarmstadt.ukp.inception.recommendation.log;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderEvaluationResultEvent;

@Component
public class RecommenderEvaluationResultEventAdapter
    implements EventLoggingAdapter<RecommenderEvaluationResultEvent>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean accepts(Object aEvent)
    {
        return aEvent instanceof RecommenderEvaluationResultEvent;
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
    public String getDetails(RecommenderEvaluationResultEvent aEvent)
    {
        try {
            Details details = new Details();

            details.recommenderId = aEvent.getRecommender().getId();
            details.score = aEvent.getScore();
            details.active = aEvent.isActive();

            details.duration = aEvent.getDuration();
            details.threshold = aEvent.getRecommender().getThreshold();
            details.layer = aEvent.getRecommender().getLayer().getName();
            details.feature = aEvent.getRecommender().getFeature().getName();
            details.tool = aEvent.getRecommender().getTool();

            return JSONUtil.toJsonString(details);
        }
        catch (IOException e) {
            log.error("Unable to log event [{}]", aEvent, e);
            return "<ERROR>";
        }
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
        public double score;
    }
}
