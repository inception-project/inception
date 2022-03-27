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
import org.apache.uima.cas.text.AnnotationFS;

public class RelationSuggestion
    extends AnnotationSuggestion
    implements Serializable
{
    private static final long serialVersionUID = -1904645143661843249L;

    private final RelationPosition position;

    public RelationSuggestion(int aId, Recommender aRecommender, long aLayerId, String aFeature,
            String aDocumentName, AnnotationFS aSource, AnnotationFS aTarget, String aLabel,
            String aUiLabel, double aScore, String aScoreExplanation)
    {
        this(aId, aRecommender.getId(), aRecommender.getName(), aLayerId, aFeature, aDocumentName,
                aSource.getBegin(), aSource.getEnd(), aTarget.getBegin(), aTarget.getEnd(), aLabel,
                aUiLabel, aScore, aScoreExplanation);
    }

    public RelationSuggestion(int aId, long aRecommenderId, String aRecommenderName, long aLayerId,
            String aFeature, String aDocumentName, int aSourceBegin, int aSourceEnd,
            int aTargetBegin, int aTargetEnd, String aLabel, String aUiLabel, double aScore,
            String aScoreExplanation)
    {
        super(aId, aRecommenderId, aRecommenderName, aLayerId, aFeature, aDocumentName, aLabel,
                aUiLabel, aScore, aScoreExplanation);

        position = new RelationPosition(aSourceBegin, aSourceEnd, aTargetBegin, aTargetEnd);
    }

    /**
     * Copy constructor.
     *
     * @param aObject
     *            The annotationObject to copy
     */
    public RelationSuggestion(RelationSuggestion aObject)
    {
        super(aObject);

        position = new RelationPosition(aObject.position);
    }

    // Getter and setter

    @Override
    public RelationPosition getPosition()
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
        return new ToStringBuilder(this).append("id", id).append("recommenderId", recommenderId)
                .append("recommenderName", recommenderName).append("layerId", layerId)
                .append("feature", feature).append("documentName", documentName)
                .append("position", position) //
                .append("windowBegin", getWindowBegin()).append("windowEnd", getWindowEnd()) //
                .append("label", label).append("uiLabel", uiLabel).append("score", score)
                .append("confindenceExplanation", scoreExplanation).append("visible", isVisible())
                .append("reasonForHiding", getReasonForHiding()).toString();
    }
}
