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

import static java.util.stream.Collectors.joining;

import java.io.IOException;

import de.tudarmstadt.ukp.inception.active.learning.config.ActiveLearningAutoConfiguration;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningSuggestionOfferedEvent;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.log.model.AnnotationDetails;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ActiveLearningAutoConfiguration#activeLearningSuggestionOfferedAdapter}.
 * </p>
 */
public class ActiveLearningSuggestionOfferedAdapter
    implements EventLoggingAdapter<ActiveLearningSuggestionOfferedEvent>
{
    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return ActiveLearningSuggestionOfferedEvent.class.isAssignableFrom(aEvent);
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
    public String getDetails(ActiveLearningSuggestionOfferedEvent aEvent) throws IOException
    {
        var rec = aEvent.getCurrentRecommendation();

        var ann = new AnnotationDetails();
        ann.setBegin(rec.getBegin());
        ann.setEnd(rec.getEnd());
        ann.setText(rec.getCoveredText());
        ann.setType(aEvent.getLayer().getName());

        var allLabels = aEvent.getAllRecommendations().stream() //
                .map(ao -> ao.getLabel()) //
                .collect(joining(", "));

        var details = new Details( //
                ann, //
                aEvent.getAnnotationFeature(), //
                rec.getLabel(), //
                rec.getScore(), //
                rec.getRecommenderId(), //
                allLabels);

        return JSONUtil.toJsonString(details);
    }

    record Details( //
            AnnotationDetails ann, //
            String annotationFeature, //
            String currentLabel, //
            double score, //
            long recommenderId, //
            String allLabels)
    {}
}
