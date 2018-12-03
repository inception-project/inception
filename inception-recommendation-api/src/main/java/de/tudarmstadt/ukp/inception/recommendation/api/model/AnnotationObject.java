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

public class AnnotationObject
    implements Serializable, Comparable<AnnotationObject>
{
    private static final long serialVersionUID = -1145787227041121442L;

    private final TokenObject token;
    private final int id;
    private final String label;
    private final String uiLabel;
    private final String feature;
    private final String source;
    private final double confidence;
    private final long recommenderId;
    private boolean visible = false;

    public AnnotationObject(TokenObject aToken, String aLabel, String aUiLabel, int aId,
        String aFeature, String aSource, double aConfidence, long aRecommenderId)
    {
        token = aToken;
        label = aLabel;
        uiLabel = aUiLabel;
        id = aId;
        feature = aFeature;
        source = aSource;
        confidence = aConfidence;
        recommenderId = aRecommenderId;
    }

    /**
     * Copy constructor.
     *
     * @param ao The annotationObject to copy
     */
    public AnnotationObject(AnnotationObject ao)
    {
        this(ao.token, ao.label, ao.uiLabel, ao.id, ao.feature, ao.source, ao.confidence,
            ao.recommenderId);
    }

    // Getter and setter

    public Offset getOffset()
    {
        return token.offset;
    }

    public String getCoveredText()
    {
        return token.getCoveredText();
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

    public String getFeature()
    {
        return feature;
    }

    public String getSource()
    {
        return source;
    }

    public double getConfidence()
    {
        return confidence;
    }

    public long getRecommenderId()
    {
        return recommenderId;
    }

    public TokenObject getTokenObject()
    {
        return token;
    }

    public String getDocumentName()
    {
        return token.documentName;
    }

    public void setVisible(boolean aVisible) { visible = aVisible; }

    public boolean isVisible() { return visible; }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AnnotationObject that = (AnnotationObject) o;
        return id == that.id && recommenderId == that.recommenderId
            && token.documentName.equals(that.getDocumentName());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, recommenderId, token.documentURI);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AnnotationObject{");
        sb.append("token=").append(token);
        sb.append(", id=").append(id);
        sb.append(", label='").append(label).append('\'');
        sb.append(", uiLabel='").append(uiLabel).append('\'');
        sb.append(", feature='").append(feature).append('\'');
        sb.append(", source='").append(source).append('\'');
        sb.append(", confidence=").append(confidence);
        sb.append(", recommenderId=").append(recommenderId);
        sb.append(", documentUri=").append(token.documentURI);
        sb.append(", visible=").append(visible);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo(AnnotationObject aAo)
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
