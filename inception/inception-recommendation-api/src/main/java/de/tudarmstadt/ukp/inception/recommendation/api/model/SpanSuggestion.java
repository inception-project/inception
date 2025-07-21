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

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class SpanSuggestion
    extends AnnotationSuggestion
    implements Serializable
{
    private static final long serialVersionUID = -1904645143661843249L;

    private final Offset position;
    private final String coveredText;

    private SpanSuggestion(Builder builder)
    {
        super(builder.id, builder.generation, builder.age, builder.recommenderId,
                builder.recommenderName, builder.layerId, builder.feature, builder.documentId,
                builder.label, builder.uiLabel, builder.score, builder.scoreExplanation,
                builder.autoAcceptMode, builder.hidingFlags, builder.correction,
                builder.correctionExplanation);

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
    {
        private int id;
        private int generation;
        private int age;
        private long recommenderId;
        private String recommenderName;
        private long layerId;
        private String feature;
        private long documentId;
        private String label;
        private String uiLabel;
        private double score;
        private String scoreExplanation;
        private Offset position;
        private String coveredText;
        private AutoAcceptMode autoAcceptMode = AutoAcceptMode.NEVER;
        private int hidingFlags;
        private boolean correction;
        private String correctionExplanation;

        private Builder()
        {
        }

        public Builder withId(int aId)
        {
            this.id = aId;
            return this;
        }

        public Builder withAge(int aAge)
        {
            this.age = aAge;
            return this;
        }

        public Builder withGeneration(int aGeneration)
        {
            this.generation = aGeneration;
            return this;
        }

        public Builder withRecommender(Recommender aRecommender)
        {
            this.recommenderId = aRecommender.getId();
            this.recommenderName = aRecommender.getName();
            this.feature = aRecommender.getFeature().getName();
            this.layerId = aRecommender.getLayer().getId();
            return this;
        }

        @Deprecated
        Builder withRecommenderId(long aRecommenderId)
        {
            this.recommenderId = aRecommenderId;
            return this;
        }

        @Deprecated
        Builder withRecommenderName(String aRecommenderName)
        {
            this.recommenderName = aRecommenderName;
            return this;
        }

        @Deprecated
        Builder withLayerId(long aLayerId)
        {
            this.layerId = aLayerId;
            return this;
        }

        @Deprecated
        Builder withFeature(String aFeature)
        {
            this.feature = aFeature;
            return this;
        }

        public Builder withDocument(SourceDocument aDocument)
        {
            this.documentId = aDocument.getId();
            return this;
        }

        public Builder withDocument(long aDocumentId)
        {
            this.documentId = aDocumentId;
            return this;
        }

        public Builder withLabel(String aLabel)
        {
            this.label = aLabel;
            return this;
        }

        public Builder withUiLabel(String aUiLabel)
        {
            this.uiLabel = aUiLabel;
            return this;
        }

        public Builder withScore(double aScore)
        {
            this.score = aScore;
            return this;
        }

        public Builder withScoreExplanation(String aScoreExplanation)
        {
            this.scoreExplanation = aScoreExplanation;
            return this;
        }

        public Builder withPosition(int aBegin, int aEnd)
        {
            this.position = new Offset(aBegin, aEnd);
            return this;
        }

        public Builder withPosition(Offset aPosition)
        {
            this.position = aPosition;
            return this;
        }

        public Builder withCoveredText(String aCoveredText)
        {
            this.coveredText = aCoveredText;
            return this;
        }

        public Builder withAutoAcceptMode(AutoAcceptMode aAutoAcceptMode)
        {
            this.autoAcceptMode = aAutoAcceptMode;
            return this;
        }

        public Builder withHidingFlags(int aFlags)
        {
            this.hidingFlags = aFlags;
            return this;
        }

        public Builder withCorrection(boolean aCorrection)
        {
            this.correction = aCorrection;
            return this;
        }

        public Builder withCorrectionExplanation(String aCorrectionExplanation)
        {
            this.correctionExplanation = aCorrectionExplanation;
            return this;
        }

        public SpanSuggestion build()
        {
            return new SpanSuggestion(this);
        }
    }
}
