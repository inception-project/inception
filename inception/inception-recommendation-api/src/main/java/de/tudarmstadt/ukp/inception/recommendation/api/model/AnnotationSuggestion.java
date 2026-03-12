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

import static de.tudarmstadt.ukp.inception.recommendation.api.model.AutoAcceptMode.NEVER;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.uima.cas.text.AnnotationPredicates;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.ExtractionContext;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.support.uima.Range;

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

    /**
     * Suggestion is a no-label duplicate of another no-label suggestion for the same layer and
     * position but a different feature. Since accepting any one of them creates the same empty
     * annotation, all but one are suppressed.
     */
    public static final int FLAG_DUPLICATE = 1 << 6;

    public static final int FLAG_ALL = FLAG_OVERLAP | FLAG_SKIPPED | FLAG_REJECTED
            | FLAG_TRANSIENT_ACCEPTED | FLAG_TRANSIENT_REJECTED | FLAG_TRANSIENT_CORRECTED
            | FLAG_DUPLICATE;

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

    public AnnotationSuggestion(Builder<?> aBuilder)
    {
        generation = aBuilder.generation;
        age = aBuilder.age;
        label = aBuilder.label;
        uiLabel = aBuilder.uiLabel;
        id = aBuilder.id;
        score = aBuilder.score;
        scoreExplanation = aBuilder.scoreExplanation;
        documentId = aBuilder.documentId;
        autoAcceptMode = aBuilder.autoAcceptMode != null ? aBuilder.autoAcceptMode : NEVER;
        hidingFlags = aBuilder.hidingFlags;
        correction = aBuilder.correction;
        correctionExplanation = aBuilder.correctionExplanation;

        recommenderId = aBuilder.recommenderId != null ? aBuilder.recommenderId
                : (aBuilder.recommender != null ? aBuilder.recommender.getId() : 0);

        recommenderName = aBuilder.recommenderName != null ? aBuilder.recommenderName
                : (aBuilder.recommender != null ? aBuilder.recommender.getName() : null);

        layerId = aBuilder.layerId != null ? aBuilder.layerId
                : (aBuilder.recommender != null && aBuilder.recommender.getLayer() != null
                        ? aBuilder.recommender.getLayer().getId()
                        : 0);

        feature = aBuilder.feature != null ? aBuilder.feature
                : (aBuilder.recommender != null && aBuilder.recommender.getFeature() != null
                        ? aBuilder.recommender.getFeature().getName()
                        : null);

        assert layerId > 0l : "Layer must be persisted (id > 0) but was [" + layerId + "]";
        assert recommenderId > 0l : "Recommender must be persisted (id > 0) but was "
                + recommenderId + "]";
        assert documentId > 0l : "Document must be persisted (id > 0) but was " + documentId + "]";
        assert feature != null : "Feature cannot be null";
        assert recommenderName != null : "Recommender name cannot be null";
        assert generation >= 0 : "Generation cannot be negative but was [" + generation + "]";
        assert age >= 0 : "Age cannot be negative but was [" + age + "]";
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
        if ((hidingFlags & FLAG_DUPLICATE) != 0) {
            sb.append("duplicate ");
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
        // We could add the source as EXTENSION_ID into the payload VID
        // Btw. we also seem to have layer and recommender ID redundantly here in both VIDs...!
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

    public static abstract class Builder<T extends Builder<?>>
    {
        protected int generation;
        protected int age;
        protected int id;
        protected Recommender recommender;
        protected Long recommenderId;
        protected String recommenderName;
        protected Long layerId;
        protected String feature;
        protected long documentId;
        protected String label;
        protected String uiLabel;
        protected double score;
        protected String scoreExplanation;
        protected AutoAcceptMode autoAcceptMode;
        protected int hidingFlags;
        protected boolean correction;
        protected String correctionExplanation;

        protected Builder()
        {
            // No initialization
        }

        public T withId(int aId)
        {
            id = aId;
            return (T) this;
        }

        public T withContext(ExtractionContext aCtx)
        {
            withGeneration(aCtx.getGeneration());
            withRecommender(aCtx.getRecommender());
            withLayer(aCtx.getLayer());
            withFeature(aCtx.getFeature());
            withDocument(aCtx.getDocument());
            return (T) this;
        }

        public T withGeneration(int aGeneration)
        {
            generation = aGeneration;
            return (T) this;
        }

        public T withAge(int aAge)
        {
            age = aAge;
            return (T) this;
        }

        public T withRecommender(Recommender aRecommender)
        {
            recommender = aRecommender;
            return (T) this;
        }

        public T withLayer(AnnotationLayer aLayer)
        {
            layerId = aLayer.getId();
            return (T) this;
        }

        public T withFeature(AnnotationFeature aFeature)
        {
            feature = aFeature.getName();
            return (T) this;
        }

        @Deprecated
        T withRecommenderId(long aRecommenderId)
        {
            recommenderId = aRecommenderId;
            return (T) this;
        }

        @Deprecated
        T withRecommenderName(String aRecommenderName)
        {
            recommenderName = aRecommenderName;
            return (T) this;
        }

        @Deprecated
        T withLayerId(long aLayerId)
        {
            layerId = aLayerId;
            return (T) this;
        }

        @Deprecated
        T withFeature(String aFeature)
        {
            feature = aFeature;
            return (T) this;
        }

        public T withDocument(SourceDocument aDocument)
        {
            documentId = aDocument.getId();
            return (T) this;
        }

        @Deprecated
        public T withDocument(long aDocumentId)
        {
            documentId = aDocumentId;
            return (T) this;
        }

        public T withLabel(String aLabel)
        {
            label = aLabel;
            return (T) this;
        }

        public T withUiLabel(String aUiLabel)
        {
            uiLabel = aUiLabel;
            return (T) this;
        }

        public T withScore(double aScore)
        {
            score = aScore;
            return (T) this;
        }

        public T withScoreExplanation(String aScoreExplanation)
        {
            scoreExplanation = aScoreExplanation;
            return (T) this;
        }

        public T withAutoAcceptMode(AutoAcceptMode aAutoAcceptMode)
        {
            autoAcceptMode = aAutoAcceptMode;
            return (T) this;
        }

        public T withHidingFlags(int aFlags)
        {
            hidingFlags = aFlags;
            return (T) this;
        }

        public T withCorrection(boolean aCorrection)
        {
            correction = aCorrection;
            return (T) this;
        }

        public T withCorrectionExplanation(String aCorrectionExplanation)
        {
            correctionExplanation = aCorrectionExplanation;
            return (T) this;
        }
    }
}
