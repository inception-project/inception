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
    private final Position position;
    private final int annotationId;
    private final int sentenceId;
    private final long recommenderId;

    public ExtendedId(String userName, long projectId, String documentName, long layerId,
            Position aPosition, long recommenderId, int annotationId, int sentenceId)
    {
        super();
        this.userName = userName;
        this.projectId = projectId;
        this.documentName = documentName;
        this.layerId = layerId;
        this.annotationId = annotationId;
        this.sentenceId = sentenceId;
        this.recommenderId = recommenderId;
        this.position = aPosition;
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

    public Position getPosition()
    {
        return position;
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

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ExtendedId)) {
            return false;
        }
        ExtendedId castOther = (ExtendedId) other;
        return new EqualsBuilder().append(userName, castOther.userName)
                .append(projectId, castOther.projectId).append(documentName, castOther.documentName)
                .append(layerId, castOther.layerId).append(position, castOther.position)
                .append(annotationId, castOther.annotationId)
                .append(sentenceId, castOther.sentenceId)
                .append(recommenderId, castOther.recommenderId).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(userName).append(projectId).append(documentName)
                .append(layerId).append(position).append(annotationId).append(sentenceId)
                .append(recommenderId).toHashCode();
    }
}
