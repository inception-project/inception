/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.uima.cas.text.AnnotationFS;

public class RelationSuggestion
    extends AnnotationSuggestion
    implements Serializable
{
    private static final long serialVersionUID = -1904645143661843249L;

    private final RelationPosition position;

    public RelationSuggestion(int aId, long aRecommenderId, String aRecommenderName,
            long aLayerId, String aFeature, String aDocumentName, AnnotationFS aSource,
            AnnotationFS aTarget, String aLabel, String aUiLabel,
            double aConfidence, String aConfidenceExplanation)
    {
        super(aId, aRecommenderId, aRecommenderName, aLayerId, aFeature, aDocumentName, aLabel,
                aUiLabel, aConfidence, aConfidenceExplanation);
        
        position = new RelationPosition(getAddr(aSource), getAddr(aTarget));
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
    
    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("id", id).append("recommenderId", recommenderId)
                .append("recommenderName", recommenderName).append("layerId", layerId)
                .append("feature", feature).append("documentName", documentName)
                .append("position", position)
                .append("label", label).append("uiLabel", uiLabel).append("confidence", confidence)
                .append("confindenceExplanation", confidenceExplanation)
                .append("visible", isVisible()).append("reasonForHiding", getReasonForHiding())
                .toString();
    }
}
