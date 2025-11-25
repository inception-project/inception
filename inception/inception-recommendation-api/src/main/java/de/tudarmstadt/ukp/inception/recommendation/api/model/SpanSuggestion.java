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

import org.apache.commons.lang3.builder.ToStringBuilder;

public class SpanSuggestion
    extends AnnotationSuggestion
    implements Serializable
{
    private static final long serialVersionUID = -1904645143661843249L;

    private final Offset position;
    private final String coveredText;

    private SpanSuggestion(Builder builder)
    {
        super(builder);

        position = builder.position;
        coveredText = builder.coveredText;
    }

    // Getter and setter

    public String getCoveredText()
    {
        return coveredText;
    }

    public int getBegin()
    {
        return position.getBegin();
    }

    public int getEnd()
    {
        return position.getEnd();
    }

    @Override
    public int getWindowBegin()
    {
        return position.getBegin();
    }

    @Override
    public int getWindowEnd()
    {
        return position.getEnd();
    }

    @Override
    public Offset getPosition()
    {
        return position;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this) //
                .append("id", id) //
                .append("generation", generation) //
                .append("age", getAge()) //
                .append("recommenderId", recommenderId) //
                .append("recommenderName", recommenderName) //
                .append("layerId", layerId) //
                .append("feature", feature) //
                .append("documentId", documentId) //
                .append("position", position) //
                .append("coveredText", coveredText) //
                .append("label", label) //
                .append("uiLabel", uiLabel) //
                .append("score", score) //
                .append("confindenceExplanation", scoreExplanation) //
                .append("visible", isVisible()) //
                .append("reasonForHiding", getReasonForHiding()) //
                .append("autoAcceptMode", getAutoAcceptMode()) //
                .toString();
    }

    @Override
    public AnnotationSuggestion assignId(int aId)
    {
        return toBuilder().withId(aId).build();
    }

    public static Builder builder()
    {
        return new Builder();
    }

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
                .withDocument(documentId) //
                .withLabel(label) //
                .withUiLabel(uiLabel) //
                .withScore(score) //
                .withScoreExplanation(scoreExplanation) //
                .withCorrection(correction) //
                .withCorrectionExplanation(correctionExplanation) //
                .withPosition(position) //
                .withCoveredText(coveredText) //
                .withAutoAcceptMode(getAutoAcceptMode()) //
                .withHidingFlags(getHidingFlags());
    }

    public static final class Builder
        extends AnnotationSuggestion.Builder<Builder>
    {
        private Offset position;
        private String coveredText;

        private Builder()
        {
        }

        public Builder withPosition(int aBegin, int aEnd)
        {
            position = new Offset(aBegin, aEnd);
            return this;
        }

        public Builder withPosition(Offset aPosition)
        {
            position = aPosition;
            return this;
        }

        public Builder withCoveredText(String aCoveredText)
        {
            coveredText = aCoveredText;
            return this;
        }

        public SpanSuggestion build()
        {
            return new SpanSuggestion(this);
        }
    }
}
