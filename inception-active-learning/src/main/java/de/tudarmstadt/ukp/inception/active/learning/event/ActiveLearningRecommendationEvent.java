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
package de.tudarmstadt.ukp.inception.active.learning.event;

import java.util.List;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;

public class ActiveLearningRecommendationEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = -2741267700429534514L;

    private final SourceDocument document;
    private final AnnotationSuggestion currentRecommendation;
    private final String user;
    private final AnnotationLayer layer;
    private final String annotationFeature;
    private final LearningRecordType action;
    private final List<AnnotationSuggestion> allRecommendations;

    public ActiveLearningRecommendationEvent(Object aSource, SourceDocument aDocument,
            AnnotationSuggestion aCurrentRecommendation, String aUser, AnnotationLayer aLayer,
            String aAnnotationFeature, LearningRecordType aAction,
            List<AnnotationSuggestion> aAllRecommendations)
    {
        super(aSource);
        document = aDocument;
        currentRecommendation = aCurrentRecommendation;
        user = aUser;
        layer = aLayer;
        annotationFeature = aAnnotationFeature;
        action = aAction;
        allRecommendations = aAllRecommendations;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public AnnotationSuggestion getCurrentRecommendation()
    {
        return currentRecommendation;
    }

    public String getUser()
    {
        return user;
    }

    public AnnotationLayer getLayer()
    {
        return layer;
    }

    public String getAnnotationFeature()
    {
        return annotationFeature;
    }

    public LearningRecordType getAction()
    {
        return action;
    }

    public List<AnnotationSuggestion> getAllRecommendations()
    {
        return allRecommendations;
    }
}
