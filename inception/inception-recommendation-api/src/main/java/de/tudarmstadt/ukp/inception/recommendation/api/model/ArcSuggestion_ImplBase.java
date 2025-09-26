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

public abstract class ArcSuggestion_ImplBase<P extends ArcPosition_ImplBase<?>>
    extends AnnotationSuggestion
    implements Serializable
{
    private static final long serialVersionUID = -4873732473868120957L;

    protected final P position;

    protected ArcSuggestion_ImplBase(Builder<?, P> builder)
    {
        super(builder.id, builder.generation, builder.age, builder.recommenderId,
                builder.recommenderName, builder.layerId, builder.feature, builder.documentId,
                builder.label, builder.uiLabel, builder.score, builder.scoreExplanation,
                builder.autoAcceptMode, builder.hidingFlags, builder.correction,
                builder.correctionExplanation);

        position = builder.position;
    }

    // Getter and setter

    @Override
    public P getPosition()
    {
        return position;
    }

    // The begin of the window is min(source.begin, target.begin)
    // The end of the window is max(source.end, target.end)
    // This is mostly used to optimize the viewport when rendering

    @Override
    public int getWindowBegin()
    {
        return Math.min(position.getSourceBegin(), position.getTargetBegin());
    }

    @Override
    public int getWindowEnd()
    {
        return Math.max(position.getSourceEnd(), position.getTargetEnd());
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
                .append("windowBegin", getWindowBegin()) //
                .append("windowEnd", getWindowEnd()) //
                .append("label", label) //
                .append("uiLabel", uiLabel) //
                .append("score", score) //
                .append("confindenceExplanation", scoreExplanation) //
                .append("visible", isVisible()) //
                .append("reasonForHiding", getReasonForHiding()) //
                .toString();
    }

    @Override
    public ArcSuggestion_ImplBase assignId(int aId)
    {
        return toBuilder().withId(aId).build();
    }

    public abstract Builder toBuilder();

    public static abstract class Builder<T extends Builder<?, ?>, P extends ArcPosition_ImplBase<?>>
    {
        protected int generation;
        protected int age;
        protected int id;
        protected long recommenderId;
        protected String recommenderName;
        protected long layerId;
        protected String feature;
        protected long documentId;
        protected String label;
        protected String uiLabel;
        protected double score;
        protected String scoreExplanation;
        protected P position;
        protected AutoAcceptMode autoAcceptMode;
        protected int hidingFlags;
        private boolean correction;
        protected String correctionExplanation;

        protected Builder()
        {
        }

        public T withId(int aId)
        {
            this.id = aId;
            return (T) this;
        }

        public T withGeneration(int aGeneration)
        {
            this.generation = aGeneration;
            return (T) this;
        }

        public T withAge(int aAge)
        {
            this.age = aAge;
            return (T) this;
        }

        public T withRecommender(Recommender aRecommender)
        {
            this.recommenderId = aRecommender.getId();
            this.recommenderName = aRecommender.getName();
            this.feature = aRecommender.getFeature().getName();
            this.layerId = aRecommender.getLayer().getId();
            return (T) this;
        }

        @Deprecated
        T withRecommenderId(long aRecommenderId)
        {
            this.recommenderId = aRecommenderId;
            return (T) this;
        }

        @Deprecated
        T withRecommenderName(String aRecommenderName)
        {
            this.recommenderName = aRecommenderName;
            return (T) this;
        }

        @Deprecated
        T withLayerId(long aLayerId)
        {
            this.layerId = aLayerId;
            return (T) this;
        }

        @Deprecated
        T withFeature(String aFeature)
        {
            this.feature = aFeature;
            return (T) this;
        }

        public T withDocument(SourceDocument aDocument)
        {
            this.documentId = aDocument.getId();
            return (T) this;
        }

        @Deprecated
        public T withDocument(long aDocumentId)
        {
            this.documentId = aDocumentId;
            return (T) this;
        }

        public T withLabel(String aLabel)
        {
            this.label = aLabel;
            return (T) this;
        }

        public T withUiLabel(String aUiLabel)
        {
            this.uiLabel = aUiLabel;
            return (T) this;
        }

        public T withScore(double aScore)
        {
            this.score = aScore;
            return (T) this;
        }

        public T withScoreExplanation(String aScoreExplanation)
        {
            this.scoreExplanation = aScoreExplanation;
            return (T) this;
        }

        public T withPosition(P aPosition)
        {
            this.position = aPosition;
            return (T) this;
        }

        public T withAutoAcceptMode(AutoAcceptMode aAutoAcceptMode)
        {
            this.autoAcceptMode = aAutoAcceptMode;
            return (T) this;
        }

        public T withHidingFlags(int aFlags)
        {
            this.hidingFlags = aFlags;
            return (T) this;
        }

        public T withCorrection(boolean aCorrection)
        {
            this.correction = aCorrection;
            return (T) this;
        }

        public T withCorrectionExplanation(String aCorrectionExplanation)
        {
            this.correctionExplanation = aCorrectionExplanation;
            return (T) this;
        }

        public abstract ArcSuggestion_ImplBase build();
    }
}
