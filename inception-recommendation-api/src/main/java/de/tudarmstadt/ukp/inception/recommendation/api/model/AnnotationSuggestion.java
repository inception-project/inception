/*
 * Copyright 2020
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

import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public interface AnnotationSuggestion
{
    String EXTENSION_ID = "recommendationEditorExtension";
    
    /**
     * Suggestion is overlapping with an existing annotation
     */
    int FLAG_OVERLAP = 1 << 0;
    
    /**
     * Suggestion has been skipped (from learning history)
     */
    int FLAG_SKIPPED = 1 << 1;
    
    /**
     * Suggestion has been rejected (from learning history)
     */
    int FLAG_REJECTED = 1 << 2;
    
    /**
     * User has accepted the suggestion and prediction has not re-run yet (which would reinitialize
     * the visibility state)
     */
    int FLAG_TRANSIENT_ACCEPTED = 1 << 3;
    
    /**
     * User has rejected the suggestion and prediction has not re-run yet (which would reinitialize
     * the visibility state)
     */
    int FLAG_TRANSIENT_REJECTED = 1 << 4;
    
    /**
     * User has corrected the suggestion and prediction has not re-run yet (which would reinitialize
     * the visibility state)
     */
    int FLAG_TRANSIENT_CORRECTED = 1 << 5;
    
    int FLAG_ALL = FLAG_OVERLAP | FLAG_SKIPPED | FLAG_REJECTED | FLAG_TRANSIENT_ACCEPTED
            | FLAG_TRANSIENT_REJECTED | FLAG_TRANSIENT_CORRECTED;

    int getId();

    /**
     * Get the annotation's label, might be null if this is a suggestion for an annotation but not
     * for a specific label.
     * 
     * @return the label value or null
     */
    String getLabel();

    String getUiLabel();

    long getLayerId();

    String getFeature();

    String getRecommenderName();

    double getConfidence();

    Optional<String> getConfidenceExplanation();

    long getRecommenderId();

    String getDocumentName();

    void hide(int aFlags);

    void show(int aFlags);

    String getReasonForHiding();

    boolean isVisible();

    VID getVID();

    /**
     * Determine if the given label is equal to this object's label or if they are both null
     * 
     * @return true if both labels are null or equal
     */
    boolean labelEquals(String aLabel);

    Position getPosition();
}
