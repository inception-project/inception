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
package de.tudarmstadt.ukp.inception.active.learning.log;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningRecommendationEvent;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSuggestionOfferedEvent;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.log.model.AnnotationDetails;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;


@Component
public class ActiveLearningSuggestionOfferedAdapter
    implements EventLoggingAdapter<ActiveLearningSuggestionOfferedEvent>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean accepts(Object aEvent)
    {
        return aEvent instanceof ActiveLearningRecommendationEvent;
    }

    @Override
    public long getDocument(ActiveLearningSuggestionOfferedEvent aEvent)
    {
        return aEvent.getDocument().getId();
    }

    @Override
    public long getProject(ActiveLearningSuggestionOfferedEvent aEvent)
    {
        return aEvent.getDocument().getProject().getId();
    }

    @Override
    public String getDetails(ActiveLearningSuggestionOfferedEvent aEvent)
    {
        try {
            Details details = new Details();
            details.ann = new AnnotationDetails();
            details.ann.setBegin(aEvent.getCurrentRecommendation().getBegin());
            details.ann.setEnd(aEvent.getCurrentRecommendation().getEnd());
            details.ann.setText(aEvent.getCurrentRecommendation().getCoveredText());
            details.ann.setType(aEvent.getLayer().getName());
            details.annotationFeature = aEvent.getAnnotationFeature();
            details.currentLabel = aEvent.getCurrentRecommendation().getLabel();
            details.confidence = aEvent.getCurrentRecommendation().getConfidence();
            details.recommenderId = aEvent.getCurrentRecommendation().getRecommenderId();

            List<String> allLabelList = aEvent.getAllRecommendations().stream()
                .map(ao -> ao.getLabel()).collect(Collectors.toList());
            details.allLabels = String.join(", ", allLabelList);
            return JSONUtil.toJsonString(details);
        }
        catch (IOException e) {
            log.error("Unable to log event [{}]", aEvent, e);
            return "<ERROR>";
        }
    }

    public static class Details
    {
        public AnnotationDetails ann;
        public String annotationFeature;
        public LearningRecordType userAction;
        public String currentLabel;
        public double confidence;
        public long recommenderId;
        public String allLabels;
    }
}
