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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ExtendedId
    implements Serializable
{
    private static final long serialVersionUID = -5214683455382881005L;
    
    private final String userName;
    private final long projectId;
    private final String documentName;
    private final long layerId;
    private final int begin;
    private final int end;
    private final int annotationId;
    private final int sentenceId;
    
    private long recommenderId;

    public ExtendedId(String userName, long projectId, String documentName, long layerId,
            Offset offset, long recommenderId, int annotationId, int sentenceId)
    {
        super();
        this.userName = userName;
        this.projectId = projectId;
        this.documentName = documentName;
        this.layerId = layerId;
        this.annotationId = annotationId;
        this.sentenceId = sentenceId;
        this.recommenderId = recommenderId;
        this.begin = offset.getBeginCharacter();
        this.end = offset.getEndCharacter();
    }

    public String getDocumentName()
    {
        return documentName;
    }

    public long getLayerId()
    {
        return layerId;
    }

    public String getUserName()
    {
        return userName;
    }

    public long getProjectId()
    {
        return projectId;
    }

    @Deprecated
    public Offset getOffset()
    {
        return new Offset(begin, end);
    }

    public int getBegin()
    {
        return begin;
    }
    
    public int getEnd()
    {
        return end;
    }
    
    public int getAnnotationId()
    {
        return annotationId;
    }

    public int getSentenceId()
    {
        return sentenceId;
    }

    public long getRecommenderId()
    {
        return recommenderId;
    }

    public void setRecommenderId(long aRecommenderId)
    {
        recommenderId = aRecommenderId;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ExtendedId)) {
            return false;
        }
        ExtendedId castOther = (ExtendedId) other;
        return new EqualsBuilder().append(userName, castOther.userName)
                .append(projectId, castOther.projectId).append(documentName, castOther.documentName)
                .append(layerId, castOther.layerId).append(begin, castOther.begin)
                .append(end, castOther.end).append(annotationId, castOther.annotationId)
                .append(sentenceId, castOther.sentenceId)
                .append(recommenderId, castOther.recommenderId).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(userName).append(projectId).append(documentName)
                .append(layerId).append(begin).append(end).append(annotationId).append(sentenceId)
                .append(recommenderId).toHashCode();
    }
}
