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
import java.util.stream.Collectors;

import de.tudarmstadt.ukp.inception.active.learning.config.ActiveLearningAutoConfiguration;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningRecommendationEvent;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.log.model.AnnotationDetails;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

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
    public boolean accepts(Class<?> aEvent)
    {
        return ActiveLearningRecommendationEvent.class.isAssignableFrom(aEvent);
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
        var ann = new AnnotationDetails();
        ann.setBegin(aEvent.getCurrentRecommendation().getBegin());
        ann.setEnd(aEvent.getCurrentRecommendation().getEnd());
        ann.setText(aEvent.getCurrentRecommendation().getCoveredText());
        ann.setType(aEvent.getLayer().getName());

        var allLabelList = aEvent.getAllRecommendations().stream() //
                .map(ao -> ao.getLabel()) //
                .collect(Collectors.joining(", "));

        var details = new ActiveLearningRecommendationDetails( //
                ann, //
                aEvent.getAnnotationFeature(), //
                aEvent.getAction(), //
                aEvent.getCurrentRecommendation().getLabel(), //
                aEvent.getCurrentRecommendation().getScore(), //
                aEvent.getCurrentRecommendation().getRecommenderId(), //
                allLabelList);

        return JSONUtil.toJsonString(details);
    }

    record ActiveLearningRecommendationDetails( //
            AnnotationDetails ann, //
            String annotationFeature, //
            LearningRecordUserAction userAction, //
            String currentLabel, //
            double score, //
            long recommenderId, //
            String allLabels)
    {}
}
