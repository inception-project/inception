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
package de.tudarmstadt.ukp.inception.recommendation.api.event;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;

public class RecommendationRejectedEvent
    extends ApplicationEvent
    implements TransientAnnotationStateChangedEvent
{
    private static final long serialVersionUID = 4618078923202025558L;

    private final SourceDocument document;
    private final String user;
    private final AnnotationSuggestion suggestion;
    private final AnnotationFeature feature;

    public RecommendationRejectedEvent(Object aSource, SourceDocument aDocument, String aUser,
            AnnotationFeature aFeature, AnnotationSuggestion aSuggestion)
    {
        super(aSource);

        document = aDocument;
        user = aUser;
        feature = aFeature;
        suggestion = aSuggestion;
    }

    @Override
    public SourceDocument getDocument()
    {
        return document;
    }

    @Override
    public String getUser()
    {
        return user;
    }

    public AnnotationSuggestion getSuggestion()
    {
        return suggestion;
    }

    public AnnotationFeature getFeature()
    {
        return feature;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("RecommendationRejectedEvent [");
        if (document != null) {
            builder.append("docID=");
            builder.append(document.getId());
            builder.append(", user=");
            builder.append(user);
            builder.append(", ");
        }
        builder.append(", feature=");
        builder.append(feature.getName());
        builder.append(", suggestion=");
        builder.append(suggestion);
        builder.append("]");
        return builder.toString();
    }
}
