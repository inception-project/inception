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
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.uima.cas.text.AnnotationPredicates;

import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

public abstract class AnnotationSuggestion
    implements Serializable
{
    private static final long serialVersionUID = -7137765759688480950L;

    public static final int NEW_ID = -1;

    public static final String EXTENSION_ID = "rec";

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
     * the visibility state)
     */
    public static final int FLAG_TRANSIENT_ACCEPTED = 1 << 3;

    /**
     * User has rejected the suggestion and prediction has not re-run yet (which would reinitialize
     * the visibility state)
     */
    public static final int FLAG_TRANSIENT_REJECTED = 1 << 4;

    /**
     * User has corrected the suggestion and prediction has not re-run yet (which would reinitialize
     * the visibility state)
     */
    public static final int FLAG_TRANSIENT_CORRECTED = 1 << 5;

    public static final int FLAG_ALL = FLAG_OVERLAP | FLAG_SKIPPED | FLAG_REJECTED
            | FLAG_TRANSIENT_ACCEPTED | FLAG_TRANSIENT_REJECTED | FLAG_TRANSIENT_CORRECTED;

    protected final int generation;
    protected final int id;
    protected final long recommenderId;
    protected final String recommenderName;
    protected final long layerId;
    protected final String feature;
    protected final long documentId;
    protected final String label;
    protected final String uiLabel;

    protected double score;
    protected String scoreExplanation;
    protected boolean correction;
    protected String correctionExplanation;

    private AutoAcceptMode autoAcceptMode;
    private int hidingFlags = 0;
    private int age = 0;

    public AnnotationSuggestion(int aId, int aGeneration, int aAge, long aRecommenderId,
            String aRecommenderName, long aLayerId, String aFeature, long aDocumentId,
            String aLabel, String aUiLabel, double aScore, String aScoreExplanation,
            AutoAcceptMode aAutoAcceptMode, int aHidingFlags, boolean aCorrection,
            String aCorrectionExplanation)
    {
        generation = aGeneration;
        age = aAge;
        label = aLabel;
        uiLabel = aUiLabel;
        id = aId;
        layerId = aLayerId;
        feature = aFeature;
        recommenderName = aRecommenderName;
        score = aScore;
        scoreExplanation = aScoreExplanation;
        recommenderId = aRecommenderId;
        documentId = aDocumentId;
        autoAcceptMode = aAutoAcceptMode != null ? aAutoAcceptMode : AutoAcceptMode.NEVER;
        hidingFlags = aHidingFlags;
        correction = aCorrection;
        correctionExplanation = aCorrectionExplanation;
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

    public double getScore()
    {
        return score;
    }

    public void setScore(double aScore)
    {
        score = aScore;
    }

    public Optional<String> getScoreExplanation()
    {
        return Optional.ofNullable(scoreExplanation);
    }

    public void setScoreExplanation(String aScoreExplanation)
    {
        scoreExplanation = aScoreExplanation;
    }

    /**
     * @return whether the suggestion is a correction suggestion for an existing annotation.
     *         Corrections should not be hidden for overlap with an existing annotation unless the
     *         label matches.
     */
    public boolean isCorrection()
    {
        return correction;
    }

    public void setCorrection(boolean aCorrection)
    {
        correction = aCorrection;
    }

    public Optional<String> getCorrectionExplanation()
    {
        return Optional.ofNullable(correctionExplanation);
    }

    public void setCorrectionExplanation(String aCorrectionExplanation)
    {
        correctionExplanation = aCorrectionExplanation;
    }

    public long getRecommenderId()
    {
        return recommenderId;
    }

    public long getDocumentId()
    {
        return documentId;
    }

    public void hide(int aFlags)
    {
        hidingFlags |= aFlags;
    }

    public void show(int aFlags)
    {
        hidingFlags &= ~aFlags;
    }

    protected int getHidingFlags()
    {
        return hidingFlags;
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
        if ((hidingFlags & FLAG_TRANSIENT_CORRECTED) != 0) {
            sb.append("transient-corrected ");
        }
        return sb.toString();
    }

    public boolean isVisible()
    {
        return hidingFlags == 0;
    }

    public AutoAcceptMode getAutoAcceptMode()
    {
        return autoAcceptMode;
    }

    public void clearAutoAccept()
    {
        autoAcceptMode = AutoAcceptMode.NEVER;
    }

    public VID getVID()
    {
        var payload = new VID(layerId, (int) recommenderId, id).toString();
        return new VID(EXTENSION_ID, layerId, (int) recommenderId, id, payload);
    }

    @Override
    public int hashCode()
    {
        // The recommenderId captures uniquely the project, layer and feature, so we do not have to
        // check them separately
        return Objects.hash(id, recommenderId, documentId);
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
                && documentId == that.documentId;
    }

    /**
     * Determine if the given label is equal to this object's label or if they are both null
     * 
     * @param aLabel
     *            the label
     * @return true if both labels are null or equal
     */
    public boolean labelEquals(String aLabel)
    {
        return (aLabel == null && label == null) || (label != null && label.equals(aLabel));
    }

    public abstract Position getPosition();

    public abstract int getWindowBegin();

    public abstract int getWindowEnd();

    public boolean coveredBy(Range aRange)
    {
        if (Range.UNDEFINED.equals(aRange)) {
            return false;
        }

        return AnnotationPredicates.coveredBy(getWindowBegin(), getWindowEnd(), aRange.getBegin(),
                aRange.getEnd());
    }

    public boolean hideSuggestion(LearningRecordUserAction aAction)
    {
        switch (aAction) {
        case REJECTED:
            hide(FLAG_REJECTED);
            return true;
        case SKIPPED:
            hide(FLAG_SKIPPED);
            return true;
        default:
            // Nothing to do for the other cases.
            // ACCEPTED annotation are filtered out anyway because the overlap with a created
            // annotation and the same for CORRECTED
            return false;
        }
    }

    public int incrementAge()
    {
        age++;
        return age;
    }

    public int getAge()
    {
        return age;
    }

    public AnnotationSuggestion reconcileWith(AnnotationSuggestion aNewSuggestion)
    {
        incrementAge();
        setScore(aNewSuggestion.getScore());
        aNewSuggestion.getScoreExplanation().ifPresent(this::setScoreExplanation);
        setCorrection(aNewSuggestion.isCorrection());
        aNewSuggestion.getCorrectionExplanation().ifPresent(this::setCorrectionExplanation);
        return this;
    }

    /**
     * @return a clone of the current suggestion with the new ID. This is used when adding a
     *         suggestion to {@link Predictions} if the ID of the suggestion is set to
     *         {@link #NEW_ID}.
     * @param aId
     *            the ID of the suggestion.
     */
    abstract public AnnotationSuggestion assignId(int aId);
}
