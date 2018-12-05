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
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class AnnotationSuggestion
    implements Serializable, Comparable<AnnotationSuggestion>
{
    private static final long serialVersionUID = -1145787227041121442L;

    private final int id;

    private final long recommenderId;
    private final String recommenderName;
    private final long layerId;
    private final String feature;

    private final String documentName;
    private final String documentUri;

    private final int begin;
    private final int end;
    private final String coveredText;

    private final String label;
    private final String uiLabel;
    private final double confidence;

    private boolean visible = true;

    public AnnotationSuggestion(int aId, long aRecommenderId, String aRecommenderName,
            long aLayerId, String aFeature, String aDocumentName, String aDocumentUri, int aBegin,
            int aEnd, String aCoveredText, String aLabel, String aUiLabel, double aConfidence)
    {
        label = aLabel;
        uiLabel = aUiLabel;
        id = aId;
        layerId = aLayerId;
        feature = aFeature;
        recommenderName = aRecommenderName;
        confidence = aConfidence;
        recommenderId = aRecommenderId;
        begin = aBegin;
        end = aEnd;
        coveredText = aCoveredText;
        documentName = aDocumentName;
        documentUri = aDocumentUri;
    }

    /**
     * Copy constructor.
     *
     * @param aObject
     *            The annotationObject to copy
     */
    public AnnotationSuggestion(AnnotationSuggestion aObject)
    {
        label = aObject.label;
        uiLabel = aObject.uiLabel;
        id = aObject.id;
        layerId = aObject.layerId;
        feature = aObject.feature;
        recommenderName = aObject.recommenderName;
        confidence = aObject.confidence;
        recommenderId = aObject.recommenderId;
        begin = aObject.begin;
        end = aObject.end;
        coveredText = aObject.coveredText;
        documentName = aObject.documentName;
        documentUri = aObject.documentUri;
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

    public int getId()
    {
        return id;
    }

    public String getLabel()
    {
        return label;
    }

    public String getUiLabel()
    {
        return uiLabel;
    }

    public long getLayerId()
    {
        return layerId;
    }
    
    public String getFeature()
    {
        return feature;
    }

    public String getRecommenderName()
    {
        return recommenderName;
    }

    public double getConfidence()
    {
        return confidence;
    }

    public long getRecommenderId()
    {
        return recommenderId;
    }

    public String getDocumentName()
    {
        return documentName;
    }

    /**
     * @deprecated Better use {@link #getBegin()} and {@link #getEnd()}
     */
    @Deprecated
    public Offset getOffset()
    {
        return new Offset(begin, end);
    }

    public void setVisible(boolean aVisible)
    {
        visible = aVisible;
    }

    public boolean isVisible()
    {
        return visible;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("id", id).append("label", label)
                .append("uiLabel", uiLabel).append("feature", feature)
                .append("recommenderName", recommenderName).append("confidence", confidence)
                .append("recommenderId", recommenderId).append("begin", begin).append("end", end)
                .append("coveredText", coveredText).append("documentName", documentName)
                .append("documentUri", documentUri).append("visible", visible).toString();
    }

    @Override
    public int hashCode()
    {
        // The recommenderId captures uniquely the project, layer and feature, so we do not have to
        // check them separately
        return Objects.hash(id, recommenderId, documentName);
    }

    @Override
    public boolean equals(Object o)
    {
        // The recommenderId captures uniquely the project, layer and feature, so we do not have to
        // check them separately
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AnnotationSuggestion that = (AnnotationSuggestion) o;
        return id == that.id && recommenderId == that.recommenderId
                && documentName.equals(that.documentName);
    }

    @Override
    public int compareTo(AnnotationSuggestion aAo)
    {
        if (aAo == null) {
            return 1;
        }
        if (this.equals(aAo)) {
            return 0;
        }
        if (this.getOffset().compareTo(aAo.getOffset()) != 0) {
            return this.getOffset().compareTo(aAo.getOffset());
        }
        if (this.getId() < aAo.getId()) {
            return -1;
        }
        if (this.getId() > aAo.getId()) {
            return 1;
        }
        if (this.getRecommenderId() < aAo.getId()) {
            return -1;
        }
        if (this.getRecommenderId() > aAo.getId()) {
            return 1;
        }
        if (this.getDocumentName().hashCode() < aAo.getDocumentName().hashCode()) {
            return -1;
        }
        if (this.getDocumentName().hashCode() > aAo.getDocumentName().hashCode()) {
            return 1;
        }

        return 0;
    }
}
