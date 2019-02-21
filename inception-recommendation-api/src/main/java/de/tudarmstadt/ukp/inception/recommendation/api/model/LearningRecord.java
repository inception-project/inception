/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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

    private int offsetTokenBegin;
    private int offsetTokenEnd;
    private int offsetCharacterBegin;
    private int offsetCharacterEnd;

    private String tokenText;
    private String annotation;
    
    @Type(type = "de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserActionType")
    private LearningRecordType userAction;
    
    private String user;
    
    @Type(type = "de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocationType")
    private LearningRecordChangeLocation changeLocation;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SourceDocument getSourceDocument() {
        return sourceDocument;
    }

    public void setSourceDocument(SourceDocument sourceDocument) {
        this.sourceDocument = sourceDocument;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Deprecated
    public int getOffsetTokenBegin() {
        return offsetTokenBegin;
    }

    @Deprecated
    public void setOffsetTokenBegin(int offsetTokenBegin) {
        this.offsetTokenBegin = offsetTokenBegin;
    }

    @Deprecated
    public int getOffsetTokenEnd() {
        return offsetTokenEnd;
    }

    @Deprecated
    public void setOffsetTokenEnd(int offsetTokenEnd) {
        this.offsetTokenEnd = offsetTokenEnd;
    }

    public int getOffsetCharacterBegin() {
        return offsetCharacterBegin;
    }

    public void setOffsetCharacterBegin(int offsetCharacterBegin) {
        this.offsetCharacterBegin = offsetCharacterBegin;
    }

    public int getOffsetCharacterEnd() {
        return offsetCharacterEnd;
    }

    public void setOffsetCharacterEnd(int offsetCharacterEnd) {
        this.offsetCharacterEnd = offsetCharacterEnd;
    }

    public String getTokenText() {
        return tokenText;
    }

    public void setTokenText(String tokenText) {
        // Truncate the token text if it is too long
        int targetLength = Math.min(tokenText.length(), TOKEN_TEXT_LENGTH);
        this.tokenText = tokenText.substring(0, targetLength);
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public LearningRecordType getUserAction() {
        return userAction;
    }

    public void setUserAction(LearningRecordType userAction) {
        this.userAction = userAction;
    }

    public Date getActionDate() {
        return actionDate;
    }

    public void setActionDate(Date actionDate) {
        this.actionDate = actionDate;
    }

    public AnnotationLayer getLayer() {
        return layer;
    }

    public void setLayer(AnnotationLayer layer) {
        this.layer = layer;
    }

    public LearningRecordChangeLocation getChangeLocation() {
        return changeLocation;
    }

    public void setChangeLocation(LearningRecordChangeLocation changeLocation) {
        this.changeLocation = changeLocation;
    }

    public AnnotationFeature getAnnotationFeature()
    {
        return annotationFeature;
    }

    public void setAnnotationFeature(AnnotationFeature anAnnotationFeature)
    {
        annotationFeature = anAnnotationFeature;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date actionDate = new Date();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LearningRecord that = (LearningRecord) o;

        if (offsetCharacterBegin != that.offsetCharacterBegin) {
            return false;
        }
        if (offsetCharacterEnd != that.offsetCharacterEnd) {
            return false;
        }
        if (sourceDocument != null ? !sourceDocument.equals(that.sourceDocument) : that
            .sourceDocument != null) {
            return false;
        }
        if (layer != null ? !layer.equals(that.layer) : that.layer != null) {
            return false;
        }
        if (annotation != null ? !annotation.equals(that.annotation) : that.annotation != null) {
            return false;
        }
        if (annotationFeature != null ? !annotationFeature.equals(that.annotationFeature) :
            that.annotationFeature != null) {
            return false;
        }
        return user != null ? user.equals(that.user) : that.user == null;
    }

    @Override
    public int hashCode() {
        int result = sourceDocument != null ? sourceDocument.hashCode() : 0;
        result = 31 * result + (layer != null ? layer.hashCode() : 0);
        result = 31 * result + (annotationFeature != null ? annotationFeature.hashCode() : 0);
        result = 31 * result + offsetCharacterBegin;
        result = 31 * result + offsetCharacterEnd;
        result = 31 * result + (annotation != null ? annotation.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }
}
