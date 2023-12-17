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
package de.tudarmstadt.ukp.inception.active.learning.event;

import java.util.List;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;

public class ActiveLearningRecommendationEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = -2741267700429534514L;

    private final SourceDocument document;
    private final SpanSuggestion currentRecommendation;
    private final String dataOwner;
    private final AnnotationLayer layer;
    private final String annotationFeature;
    private final LearningRecordUserAction action;
    private final List<? extends AnnotationSuggestion> allRecommendations;

    public ActiveLearningRecommendationEvent(Object aSource, SourceDocument aDocument,
            SpanSuggestion aCurrentRecommendation, String aDataOwner, AnnotationLayer aLayer,
            String aAnnotationFeature, LearningRecordUserAction aAction,
            List<? extends AnnotationSuggestion> aAllRecommendations)
    {
        super(aSource);
        document = aDocument;
        currentRecommendation = aCurrentRecommendation;
        dataOwner = aDataOwner;
        layer = aLayer;
        annotationFeature = aAnnotationFeature;
        action = aAction;
        allRecommendations = aAllRecommendations;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public SpanSuggestion getCurrentRecommendation()
    {
        return currentRecommendation;
    }

    public String getUser()
    {
        return dataOwner;
    }

    public AnnotationLayer getLayer()
    {
        return layer;
    }

    public String getAnnotationFeature()
    {
        return annotationFeature;
    }

    public LearningRecordUserAction getAction()
    {
        return action;
    }

    public List<? extends AnnotationSuggestion> getAllRecommendations()
    {
        return allRecommendations;
    }
}
