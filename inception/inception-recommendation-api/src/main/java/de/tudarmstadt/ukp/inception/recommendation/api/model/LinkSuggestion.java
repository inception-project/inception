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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import java.io.Serializable;

public class LinkSuggestion
    extends ArcSuggestion_ImplBase<LinkPosition>
    implements Serializable
{
    private static final long serialVersionUID = -2724884621704905935L;

    public LinkSuggestion(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder);
    }

    @Override
    public Builder toBuilder()
    {
        return builder() //
                .withId(id) //
                .withGeneration(generation) //
                .withAge(getAge()) //
                .withRecommenderId(recommenderId) //
                .withRecommenderName(recommenderName) //
                .withLayerId(layerId) //
                .withFeature(feature) //
                .withDocumentName(documentName) //
                .withLabel(label) //
                .withUiLabel(uiLabel) //
                .withScore(score) //
                .withScoreExplanation(scoreExplanation) //
                .withPosition(position) //
                .withAutoAcceptMode(getAutoAcceptMode()) //
                .withHidingFlags(getHidingFlags());
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends ArcSuggestion_ImplBase.Builder<T, LinkPosition>
    {
        @Override
        public LinkSuggestion build()
        {
            return new LinkSuggestion(this);
        }
    }
}
