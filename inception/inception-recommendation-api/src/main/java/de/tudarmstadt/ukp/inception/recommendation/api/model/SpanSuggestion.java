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

    public SpanSuggestion(int aId, Recommender aRecommender, long aLayerId, String aFeature,
            String aDocumentName, Offset aOffset, String aCoveredText, String aLabel,
            String aUiLabel, double aScore, String aScoreExplanation)
    {
        this(aId, aRecommender.getId(), aRecommender.getName(), aLayerId, aFeature, aDocumentName,
                aOffset.getBegin(), aOffset.getEnd(), aCoveredText, aLabel, aUiLabel, aScore,
                aScoreExplanation);
    }

    public SpanSuggestion(int aId, long aRecommenderId, String aRecommenderName, long aLayerId,
            String aFeature, String aDocumentName, int aBegin, int aEnd, String aCoveredText,
            String aLabel, String aUiLabel, double aScore, String aScoreExplanation)
    {
        super(aId, aRecommenderId, aRecommenderName, aLayerId, aFeature, aDocumentName, aLabel,
                aUiLabel, aScore, aScoreExplanation);

        position = new Offset(aBegin, aEnd);
        coveredText = aCoveredText;
    }

    /**
     * Copy constructor.
     *
     * @param aObject
     *            The annotationObject to copy
     */
    public SpanSuggestion(SpanSuggestion aObject)
    {
        super(aObject);

        position = new Offset(aObject.position.getBegin(), aObject.position.getEnd());
        coveredText = aObject.coveredText;
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
        return new ToStringBuilder(this).append("id", id).append("recommenderId", recommenderId)
                .append("recommenderName", recommenderName).append("layerId", layerId)
                .append("feature", feature).append("documentName", documentName)
                .append("position", position).append("coveredText", coveredText)
                .append("label", label).append("uiLabel", uiLabel).append("score", score)
                .append("confindenceExplanation", scoreExplanation).append("visible", isVisible())
                .append("reasonForHiding", getReasonForHiding()).toString();
    }
}
