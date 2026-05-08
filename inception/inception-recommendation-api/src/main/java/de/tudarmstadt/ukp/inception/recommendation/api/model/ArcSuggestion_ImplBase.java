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

public abstract class ArcSuggestion_ImplBase<P extends ArcPosition_ImplBase<?>>
    extends AnnotationSuggestion
    implements Serializable
{
    private static final long serialVersionUID = -4873732473868120957L;

    protected final P position;

    protected ArcSuggestion_ImplBase(Builder<?, P> builder)
    {
        super(builder);

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

    public abstract Builder<? extends Builder, ?> toBuilder();

    public static abstract class Builder<T extends Builder<?, ?>, P extends ArcPosition_ImplBase<?>>
        extends AnnotationSuggestion.Builder<T>
    {
        protected P position;

        protected Builder()
        {
        }

        public T withPosition(P aPosition)
        {
            position = aPosition;
            return (T) this;
        }

        public abstract ArcSuggestion_ImplBase build();
    }
}
