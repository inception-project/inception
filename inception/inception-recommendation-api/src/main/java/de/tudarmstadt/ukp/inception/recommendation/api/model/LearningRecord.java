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
import java.util.Date;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

@Entity
@Table(name = "learning_record")
public class LearningRecord
    implements Serializable
{
    private static final long serialVersionUID = -8487663728083806672L;
    private static final int TOKEN_TEXT_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "document")
    private SourceDocument sourceDocument;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "layer")
    private AnnotationLayer layer;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne
    @JoinColumn(name = "annotationFeature")
    private AnnotationFeature annotationFeature;

    // Character offsets describing the text this record refers to.
    // -1 mean the offset is not used for this particular record.
    // Offsets are used the following:
    // - For span suggestions, we use (offsetBegin, offsetEnd)
    // - For relation suggestions, we use
    // {Gov, (offsetBegin, offsetEnd)} -> {Dep, (offsetBegin2, offsetEnd2)}

    private int offsetBegin;
    private int offsetEnd;
    private int offsetBegin2 = -1;
    private int offsetEnd2 = -1;

    private String tokenText;
    private String annotation;

    @Type(type = "de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserActionType")
    private LearningRecordType userAction;

    private String user;

    @Type(type = "de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocationType")
    private LearningRecordChangeLocation changeLocation;

    @Type(type = "de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionTypeWrapper")
    private SuggestionType suggestionType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date actionDate = new Date();

    private LearningRecord(Builder builder)
    {
        this.id = builder.id;
        this.sourceDocument = builder.sourceDocument;
        this.layer = builder.layer;
        this.annotationFeature = builder.annotationFeature;
        this.offsetBegin = builder.offsetBegin;
        this.offsetEnd = builder.offsetEnd;
        this.offsetBegin2 = builder.offsetBegin2;
        this.offsetEnd2 = builder.offsetEnd2;
        this.tokenText = builder.tokenText;
        this.annotation = builder.annotation;
        this.userAction = builder.userAction;
        this.user = builder.user;
        this.changeLocation = builder.changeLocation;
        this.suggestionType = builder.suggestionType;
        this.actionDate = builder.actionDate;
    }

    public LearningRecord()
    {
        // Required for serialization/JPA
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long aId)
    {
        id = aId;
    }

    public SourceDocument getSourceDocument()
    {
        return sourceDocument;
    }

    public void setSourceDocument(SourceDocument aSourceDocument)
    {
        sourceDocument = aSourceDocument;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String aUser)
    {
        user = aUser;
    }

    public int getOffsetBegin()
    {
        return offsetBegin;
    }

    public void setOffsetBegin(int aOffsetCharacterBegin)
    {
        offsetBegin = aOffsetCharacterBegin;
    }

    public int getOffsetEnd()
    {
        return offsetEnd;
    }

    public void setOffsetEnd(int aOffsetCharacterEnd)
    {
        offsetEnd = aOffsetCharacterEnd;
    }

    public int getOffsetBegin2()
    {
        return offsetBegin2;
    }

    public void setOffsetBegin2(int aOffset2Begin)
    {
        offsetBegin2 = aOffset2Begin;
    }

    public int getOffsetEnd2()
    {
        return offsetEnd2;
    }

    public void setOffsetEnd2(int aOffset2End)
    {
        offsetEnd2 = aOffset2End;
    }

    public int getOffsetSourceBegin()
    {
        return offsetBegin;
    }

    public void setOffsetSourceBegin(int aSourceBeginOffset)
    {
        offsetBegin = aSourceBeginOffset;
    }

    public int getOffsetSourceEnd()
    {
        return offsetEnd;
    }

    public void setOffsetSourceEnd(int aSourceEndOffset)
    {
        offsetEnd = aSourceEndOffset;
    }

    public int getOffsetTargetBegin()
    {
        return offsetBegin2;
    }

    public void setOffsetTargetBegin(int aTargetBeginOffset)
    {
        offsetBegin2 = aTargetBeginOffset;
    }

    public int getOffsetTargetEnd()
    {
        return offsetEnd2;
    }

    public void setOffsetTargetEnd(int aTargetEndOffset)
    {
        offsetEnd2 = aTargetEndOffset;
    }

    public String getTokenText()
    {
        return tokenText;
    }

    public SuggestionType getSuggestionType()
    {
        return suggestionType;
    }

    public void setSuggestionType(SuggestionType aSuggestionType)
    {
        suggestionType = aSuggestionType;
    }

    public void setTokenText(String tokenText)
    {
        // Truncate the token text if it is too long
        int targetLength = Math.min(tokenText.length(), TOKEN_TEXT_LENGTH);
        this.tokenText = tokenText.substring(0, targetLength);
    }

    /**
     * @return annotation label, might be null if the recorded annotation was an annotation without
     *         label.
     */
    @Nullable
    public String getAnnotation()
    {
        return annotation;
    }

    public void setAnnotation(String aAnnotation)
    {
        annotation = aAnnotation;
    }

    public LearningRecordType getUserAction()
    {
        return userAction;
    }

    public void setUserAction(LearningRecordType aUserAction)
    {
        userAction = aUserAction;
    }

    public Date getActionDate()
    {
        return actionDate;
    }

    public void setActionDate(Date aActionDate)
    {
        actionDate = aActionDate;
    }

    public AnnotationLayer getLayer()
    {
        return layer;
    }

    public void setLayer(AnnotationLayer aLayer)
    {
        layer = aLayer;
    }

    public LearningRecordChangeLocation getChangeLocation()
    {
        return changeLocation;
    }

    public void setChangeLocation(LearningRecordChangeLocation aChangeLocation)
    {
        changeLocation = aChangeLocation;
    }

    public AnnotationFeature getAnnotationFeature()
    {
        return annotationFeature;
    }

    public void setAnnotationFeature(AnnotationFeature anAnnotationFeature)
    {
        annotationFeature = anAnnotationFeature;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LearningRecord that = (LearningRecord) o;

        if (!Objects.equals(suggestionType, that.suggestionType)) {
            return false;
        }

        if (offsetBegin != that.offsetBegin) {
            return false;
        }
        if (offsetEnd != that.offsetEnd) {
            return false;
        }

        if (SuggestionType.RELATION.equals(suggestionType)) {
            if (offsetBegin2 != that.offsetBegin2) {
                return false;
            }
            if (offsetEnd2 != that.offsetEnd2) {
                return false;
            }
        }

        if (!Objects.equals(sourceDocument, that.sourceDocument)) {
            return false;
        }
        if (!Objects.equals(layer, that.layer)) {
            return false;
        }
        if (!Objects.equals(annotation, that.annotation)) {
            return false;
        }
        if (!Objects.equals(annotationFeature, that.annotationFeature)) {
            return false;
        }
        return Objects.equals(user, that.user);
    }

    @Override
    public int hashCode()
    {
        int result = sourceDocument != null ? sourceDocument.hashCode() : 0;
        result = 31 * result + (layer != null ? layer.hashCode() : 0);
        result = 31 * result + (annotationFeature != null ? annotationFeature.hashCode() : 0);
        result = 31 * result + (suggestionType != null ? suggestionType.getId().hashCode() : 0);
        result = 31 * result + offsetBegin;
        result = 31 * result + offsetEnd;
        result = 31 * result + offsetBegin2;
        result = 31 * result + offsetEnd2;
        result = 31 * result + (annotation != null ? annotation.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private Long id;
        private SourceDocument sourceDocument;
        private AnnotationLayer layer;
        private AnnotationFeature annotationFeature;
        private int offsetBegin;
        private int offsetEnd;
        private int offsetBegin2 = -1;
        private int offsetEnd2 = -1;
        private String tokenText;
        private String annotation;
        private LearningRecordType userAction;
        private String user;
        private LearningRecordChangeLocation changeLocation;
        private SuggestionType suggestionType;
        private Date actionDate = new Date();

        private Builder()
        {
            // No instances
        }

        public Builder withId(Long aId)
        {
            id = aId;
            return this;
        }

        public Builder withSourceDocument(SourceDocument aSourceDocument)
        {
            sourceDocument = aSourceDocument;
            return this;
        }

        public Builder withLayer(AnnotationLayer aLayer)
        {
            layer = aLayer;
            return this;
        }

        public Builder withAnnotationFeature(AnnotationFeature aAnnotationFeature)
        {
            annotationFeature = aAnnotationFeature;
            return this;
        }

        public Builder withOffsetBegin(int aOffsetBegin)
        {
            offsetBegin = aOffsetBegin;
            return this;
        }

        public Builder withOffsetEnd(int aOffsetEnd)
        {
            offsetEnd = aOffsetEnd;
            return this;
        }

        public Builder withOffsetBegin2(int aOffsetBegin2)
        {
            offsetBegin2 = aOffsetBegin2;
            return this;
        }

        public Builder withOffsetEnd2(int aOffsetEnd2)
        {
            offsetEnd2 = aOffsetEnd2;
            return this;
        }

        public Builder withTokenText(String aTokenText)
        {
            tokenText = aTokenText;
            return this;
        }

        public Builder withAnnotation(String aAnnotation)
        {
            annotation = aAnnotation;
            return this;
        }

        public Builder withUserAction(LearningRecordType aUserAction)
        {
            userAction = aUserAction;
            return this;
        }

        public Builder withUser(String aUser)
        {
            user = aUser;
            return this;
        }

        public Builder withChangeLocation(LearningRecordChangeLocation aChangeLocation)
        {
            changeLocation = aChangeLocation;
            return this;
        }

        public Builder withSuggestionType(SuggestionType aSuggestionType)
        {
            suggestionType = aSuggestionType;
            return this;
        }

        public Builder withActionDate(Date aActionDate)
        {
            actionDate = aActionDate;
            return this;
        }

        public LearningRecord build()
        {
            return new LearningRecord(this);
        }
    }
}
