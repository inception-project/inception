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
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;

/**
 * Event generated when a suggestion is offered to the user via the active learning sidebar as part
 * of an active learning sampling strategy.
 */
public class ActiveLearningSuggestionOfferedEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = -2741267700429534514L;

    private final SourceDocument document;
    private final SpanSuggestion currentRecommendation;
    private final String user;
    private final AnnotationLayer layer;
    private final String annotationFeature;
    private final List<SpanSuggestion> allRecommendations;

    public ActiveLearningSuggestionOfferedEvent(Object aSource, SourceDocument aDocument,
            SpanSuggestion aCurrentRecommendation, String aUser, AnnotationLayer aLayer,
            String aAnnotationFeature, List<SpanSuggestion> aAllRecommendations)
    {
        super(aSource);
        document = aDocument;
        currentRecommendation = aCurrentRecommendation;
        user = aUser;
        layer = aLayer;
        annotationFeature = aAnnotationFeature;
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

    public List<SpanSuggestion> getAllRecommendations()
    {
        return allRecommendations;
    }
}
