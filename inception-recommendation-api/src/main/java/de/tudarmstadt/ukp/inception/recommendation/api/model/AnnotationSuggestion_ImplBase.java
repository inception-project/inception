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

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public abstract class AnnotationSuggestion_ImplBase
{
    public static final String EXTENSION_ID = "recommendationEditorExtension";
    
    /**
     * Suggestion is overlapping with an existing annotation
     */
    public static final int FLAG_OVERLAP = 1 << 0;
    /**
     * Suggestion has been skipped (from learning history)
     */
    public static final int FLAG_SKIPPED = 1 << 1;
    /**
     * Suggestion has been rejected (from learning history)
     */
    public static final int FLAG_REJECTED = 1 << 2;
    /**
     * User has accepted the suggestion and prediction has not re-run yet (which would reinitialize
     * the visbility state)
     */
    public static final int FLAG_TRANSIENT_ACCEPTED = 1 << 3;
    /**
     * User has rejected the suggestion and prediction has not re-run yet (which would reinitialize
     * the visbility state)
     */
    public static final int FLAG_TRANSIENT_REJECTED = 1 << 4;
    /**
     * User has corrected the suggestion and prediction has not re-run yet (which would reinitialize
     * the visbility state)
     */
    public static final int FLAG_TRANSIENT_CORRECTED = 1 << 5;
    public static final int FLAG_ALL = FLAG_OVERLAP | FLAG_SKIPPED | FLAG_REJECTED
                | FLAG_TRANSIENT_ACCEPTED | FLAG_TRANSIENT_REJECTED | FLAG_TRANSIENT_CORRECTED;
    protected final int id;
    protected final long recommenderId;
    protected final String recommenderName;
    protected final long layerId;
    protected final String feature;
    protected final String documentName;
    protected final String label;
    protected final String uiLabel;
    protected final double confidence;
    protected final String confidenceExplanation;
    private int hidingFlags = 0;

    public AnnotationSuggestion_ImplBase(int aId, long aRecommenderId, String aRecommenderName,
            long aLayerId, String aFeature, String aDocumentName, String aLabel, String aUiLabel,
            double aConfidence, String aConfidenceExplanation)
    {
        label = aLabel;
        uiLabel = aUiLabel;
        id = aId;
        layerId = aLayerId;
        feature = aFeature;
        recommenderName = aRecommenderName;
        confidence = aConfidence;
        confidenceExplanation = aConfidenceExplanation;
        recommenderId = aRecommenderId;
        documentName = aDocumentName;
    }
    
    public AnnotationSuggestion_ImplBase(AnnotationSuggestion_ImplBase aObject)
    {
        label = aObject.label;
        uiLabel = aObject.uiLabel;
        id = aObject.id;
        layerId = aObject.layerId;
        feature = aObject.feature;
        recommenderName = aObject.recommenderName;
        confidence = aObject.confidence;
        confidenceExplanation = aObject.confidenceExplanation;
        recommenderId = aObject.recommenderId;
        documentName = aObject.documentName;
    }

    public int getId()
    {
        return id;
    }

    /**
     * Get the annotation's label, might be null if this is a suggestion for an annotation but not
     * for a specific label.
     * 
     * @return the label value or null
     */
    @Nullable
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

    public Optional<String> getConfidenceExplanation()
    {
        return Optional.ofNullable(confidenceExplanation);
    }

    public long getRecommenderId()
    {
        return recommenderId;
    }

    public String getDocumentName()
    {
        return documentName;
    }

    public void hide(int aFlags)
    {
        hidingFlags |= aFlags;
    }

    public void show(int aFlags)
    {
        hidingFlags &= ~aFlags;
    }

    public String getReasonForHiding()
    {
        StringBuilder sb = new StringBuilder();
        if ((hidingFlags & FLAG_OVERLAP) != 0) {
            sb.append("overlapping ");
        }
        if ((hidingFlags & FLAG_REJECTED) != 0) {
            sb.append("rejected ");
        }
        if ((hidingFlags & FLAG_SKIPPED) != 0) {
            sb.append("skipped ");
        }
        if ((hidingFlags & FLAG_TRANSIENT_ACCEPTED) != 0) {
            sb.append("transient-accepted ");
        }
        if ((hidingFlags & FLAG_TRANSIENT_REJECTED) != 0) {
            sb.append("transient-rejected ");
        }
        return sb.toString();
    }

    public boolean isVisible()
    {
        return hidingFlags == 0;
    }

    public VID getVID()
    {
        return new VID(EXTENSION_ID, layerId, (int) recommenderId, id, VID.NONE, VID.NONE);
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
        SpanSuggestion that = (SpanSuggestion) o;
        return id == that.id && recommenderId == that.recommenderId
                && documentName.equals(that.documentName);
    }

    /**
     * Determine if the given label is equal to this object's label or if they are both null
     * 
     * @return true if both labels are null or equal
     */
    public boolean labelEquals(String aLabel)
    {
        return (aLabel == null && label == null) || (label != null && label.equals(aLabel));
    }
    
    public abstract Position getPosition();
}
