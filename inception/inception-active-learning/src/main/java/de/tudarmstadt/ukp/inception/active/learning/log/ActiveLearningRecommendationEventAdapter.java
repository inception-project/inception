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
package de.tudarmstadt.ukp.inception.active.learning.log;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.active.learning.config.ActiveLearningAutoConfiguration;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningRecommendationEvent;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.log.model.AnnotationDetails;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ActiveLearningAutoConfiguration#activeLearningRecommendationEventAdapter()}.
 * </p>
 */
public class ActiveLearningRecommendationEventAdapter
    implements EventLoggingAdapter<ActiveLearningRecommendationEvent>
{
    @Override
    public boolean accepts(Object aEvent)
    {
        return aEvent instanceof ActiveLearningRecommendationEvent;
    }

    @Override
    public long getDocument(ActiveLearningRecommendationEvent aEvent)
    {
        return aEvent.getDocument().getId();
    }

    @Override
    public long getProject(ActiveLearningRecommendationEvent aEvent)
    {
        return aEvent.getDocument().getProject().getId();
    }

    @Override
    public String getDetails(ActiveLearningRecommendationEvent aEvent) throws IOException
    {
        ActiveLearningRecommendationDetails details = new ActiveLearningRecommendationDetails();
        details.ann = new AnnotationDetails();
        details.ann.setBegin(aEvent.getCurrentRecommendation().getBegin());
        details.ann.setEnd(aEvent.getCurrentRecommendation().getEnd());
        details.ann.setText(aEvent.getCurrentRecommendation().getCoveredText());
        details.ann.setType(aEvent.getLayer().getName());
        details.annotationFeature = aEvent.getAnnotationFeature();
        details.userAction = aEvent.getAction();
        details.currentLabel = aEvent.getCurrentRecommendation().getLabel();
        details.score = aEvent.getCurrentRecommendation().getScore();
        details.recommenderId = aEvent.getCurrentRecommendation().getRecommenderId();

        List<String> allLabelList = aEvent.getAllRecommendations().stream().map(ao -> ao.getLabel())
                .collect(Collectors.toList());
        details.allLabels = String.join(", ", allLabelList);
        return JSONUtil.toJsonString(details);
    }

    public static class ActiveLearningRecommendationDetails
    {
        public AnnotationDetails ann;
        public String annotationFeature;
        public LearningRecordType userAction;
        public String currentLabel;
        public double score;
        public long recommenderId;
        public String allLabels;
    }
}
