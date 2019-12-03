/*
 * Copyright 2017
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

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class AnnotationSuggestion extends AnnotationSuggestion_ImplBase
    implements Serializable
{
    private static final long serialVersionUID = -1904645143661843249L;

    final int begin;
    final int end;
    final String coveredText;

    public AnnotationSuggestion(int aId, long aRecommenderId, String aRecommenderName,
        long aLayerId, String aFeature, String aDocumentName, int aBegin, int aEnd,
        String aCoveredText, String aLabel, String aUiLabel, double aConfidence,
        String aConfidenceExplanation)
    {
        super(aId, aRecommenderId, aRecommenderName, aLayerId, aFeature, aDocumentName, aLabel,
                aUiLabel, aConfidence, aConfidenceExplanation);
        
        begin = aBegin;
        end = aEnd;
        coveredText = aCoveredText;
    }

    /**
     * Copy constructor.
     *
     * @param aObject
     *            The annotationObject to copy
     */
    public AnnotationSuggestion(AnnotationSuggestion aObject)
    {
        super(aObject);
        
        begin = aObject.begin;
        end = aObject.end;
        coveredText = aObject.coveredText;
    }

    // Getter and setter

    public String getCoveredText()
    {
        return coveredText;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    /**
     * @deprecated Better use {@link #getBegin()} and {@link #getEnd()}
     */
    @Deprecated
    public Offset getOffset()
    {
        return new Offset(begin, end);
    }
    
    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("id", id).append("recommenderId", recommenderId)
                .append("recommenderName", recommenderName).append("layerId", layerId)
                .append("feature", feature).append("documentName", documentName)
                .append("begin", begin).append("end", end)
                .append("coveredText", coveredText).append("label", label)
                .append("uiLabel", uiLabel).append("confidence", confidence)
                .append("confindenceExplanation", confidenceExplanation)
                .append("visible", isVisible())
                .append("reasonForHiding", getReasonForHiding()).toString();
    }
}
