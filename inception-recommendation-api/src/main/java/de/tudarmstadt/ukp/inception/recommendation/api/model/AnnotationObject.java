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
    private static final double DEFAULT_CONFIDENCE = 1.0;

    private TokenObject token;
    private int id;
    private String label;
    private String uiLabel;
    private String feature;
    private String source;
    private double confidence;
    private long recommenderId;
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
     * Creates an AnnotationObject with default confidence
     */
    public AnnotationObject(TokenObject aToken, String aLabel, String aUiLabel, int aId,
                            String aFeature, String aSource, long aRecommenderId)
    {
        this(aToken, aLabel, aUiLabel, aId, aFeature, aSource, DEFAULT_CONFIDENCE, aRecommenderId);
    }

    /**
     * Creates an AnnotationObject with null label and uiLabel
     */
    public AnnotationObject(TokenObject aToken, int aId, String aFeature,
                            String aSource, double aConfidence, long aRecommenderId)
    {
        this(aToken, null, null, aId, aFeature, aSource, aConfidence, aRecommenderId);
    }

    /**
     * Creates an AnnotationObject with default confidence and null label and uiLabel
     */
    public AnnotationObject(TokenObject aToken, int aId, String aFeature, String aSource,
        long aRecommenderId)
    {
        this(aToken, null, null, aId, aFeature, aSource, aRecommenderId);
    }

    /**
     * Creates an AnnotationObject with default confidence and null label, uiLabel and source
     */
    public AnnotationObject(TokenObject aToken, int aId, String aFeature, long aRecommenderId)
    {
        this(aToken, null, null, aId, aFeature, null, aRecommenderId);
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

    public void setOffset(Offset aOffset)
    {
        token.setOffset(aOffset);
    }

    public String getCoveredText()
    {
        return token.getCoveredText();
    }

    public void setCoveredText(String aCoveredText)
    {
        token.setCoveredText(aCoveredText);
    }

    public int getId()
    {
        return id;
    }

    public void setId(int aId)
    {
        id = aId;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String aLabel)
    {
        label = aLabel;
    }

    public String getUiLabel()
    {
        return uiLabel;
    }

    public void setUiLabel(String aUiLabel)
    {
        uiLabel = aUiLabel;
    }

    public String getFeature()
    {
        return feature;
    }

    public void setFeature(String aFeature)
    {
        feature = aFeature;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String aSource)
    {
        source = aSource;
    }

    public double getConfidence()
    {
        return confidence;
    }

    public void setConfidence(double aConfidence)
    {
        confidence = aConfidence;
    }

    public long getRecommenderId()
    {
        return recommenderId;
    }

    public void setRecommenderId(long aRecommenderId)
    {
        recommenderId = aRecommenderId;
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AnnotationObject that = (AnnotationObject) o;
        return id == that.id && recommenderId == that.recommenderId
            && token.documentURI.equals(that.getDocumentName());
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
