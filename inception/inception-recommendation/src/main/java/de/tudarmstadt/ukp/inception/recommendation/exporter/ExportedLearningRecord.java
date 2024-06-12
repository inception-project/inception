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
package de.tudarmstadt.ukp.inception.recommendation.exporter;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedLearningRecord
{
    @JsonProperty("document_name")
    private String documentName;

    @JsonProperty("layer_name")
    private String layerName;

    @JsonProperty("feature")
    private String feature;

    @JsonProperty("begin")
    private int offsetBegin;

    @JsonProperty("end")
    private int offsetEnd;

    @JsonProperty("begin2")
    private int offsetBegin2 = -1;

    @JsonProperty("end2")
    private int offsetEnd2 = -1;

    @JsonProperty("text")
    private String tokenText;

    @JsonProperty("label")
    private String annotation;

    @JsonProperty("action")
    private LearningRecordUserAction userAction;

    @JsonProperty("user")
    private String user;

    @JsonProperty("trigger")
    private LearningRecordChangeLocation changeLocation;

    @JsonProperty("type")
    private String suggestionType;

    @JsonProperty("timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    private Date actionDate;

    public String getDocumentName()
    {
        return documentName;
    }

    public void setDocumentName(String aDocumentName)
    {
        documentName = aDocumentName;
    }

    public String getLayerName()
    {
        return layerName;
    }

    public void setLayerName(String aLayerName)
    {
        layerName = aLayerName;
    }

    public String getFeature()
    {
        return feature;
    }

    public void setFeature(String aFeature)
    {
        feature = aFeature;
    }

    public int getOffsetBegin()
    {
        return offsetBegin;
    }

    public void setOffsetBegin(int aOffsetBegin)
    {
        offsetBegin = aOffsetBegin;
    }

    public int getOffsetEnd()
    {
        return offsetEnd;
    }

    public void setOffsetEnd(int aOffsetEnd)
    {
        offsetEnd = aOffsetEnd;
    }

    public int getOffsetBegin2()
    {
        return offsetBegin2;
    }

    public void setOffsetBegin2(int aOffsetBegin2)
    {
        offsetBegin2 = aOffsetBegin2;
    }

    public int getOffsetEnd2()
    {
        return offsetEnd2;
    }

    public void setOffsetEnd2(int aOffsetEnd2)
    {
        offsetEnd2 = aOffsetEnd2;
    }

    public String getTokenText()
    {
        return tokenText;
    }

    public void setTokenText(String aTokenText)
    {
        tokenText = aTokenText;
    }

    public String getAnnotation()
    {
        return annotation;
    }

    public void setAnnotation(String aAnnotation)
    {
        annotation = aAnnotation;
    }

    public LearningRecordUserAction getUserAction()
    {
        return userAction;
    }

    public void setUserAction(LearningRecordUserAction aUserAction)
    {
        userAction = aUserAction;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String aUser)
    {
        user = aUser;
    }

    public LearningRecordChangeLocation getChangeLocation()
    {
        return changeLocation;
    }

    public void setChangeLocation(LearningRecordChangeLocation aChangeLocation)
    {
        changeLocation = aChangeLocation;
    }

    public String getSuggestionType()
    {
        return suggestionType;
    }

    public void setSuggestionType(String aSuggestionType)
    {
        suggestionType = aSuggestionType;
    }

    public Date getActionDate()
    {
        return actionDate;
    }

    public void setActionDate(Date aActionDate)
    {
        actionDate = aActionDate;
    }
}
