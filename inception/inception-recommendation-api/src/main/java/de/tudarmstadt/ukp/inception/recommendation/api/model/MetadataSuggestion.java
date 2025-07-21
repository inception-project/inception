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

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class MetadataSuggestion
    extends AnnotationSuggestion
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    private MetadataSuggestion(Builder builder)
    {
        super(builder.id, builder.generation, builder.age, builder.recommenderId,
                builder.recommenderName, builder.layerId, builder.feature, builder.documentId,
                builder.label, builder.uiLabel, builder.score, builder.scoreExplanation,
                builder.autoAcceptMode, builder.hidingFlags, builder.correction,
                builder.correctionExplanation);
    }

    @Override
    public MetadataPosition getPosition()
    {
        return MetadataPosition.INSTANCE;
    }

    @Override
    public int getWindowBegin()
    {
        return -1;
    }

    @Override
    public int getWindowEnd()
    {
        return -1;
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
        private AutoAcceptMode autoAcceptMode;
        private int hidingFlags;
        boolean correction;
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

        @Deprecated
        Builder withDocument(long aDocumentId)
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

        public MetadataSuggestion build()
        {
            return new MetadataSuggestion(this);
        }
    }
}
