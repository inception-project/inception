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

public class ExtendedId
    implements Serializable
{
    private static final long serialVersionUID = -5214683455382881005L;
    
    private String userName;
    private long projectId;
    private String documentName;
    private long layerId;
    private Offset offset;
    private int annotationId;
    private int sentenceId;
    private long recommenderId;

    public ExtendedId(String userName, long projectId, String documentName, long layerId,
            Offset offset, long recommenderId, int annotationId, int sentenceId)
    {
        super();
        this.userName = userName;
        this.projectId = projectId;
        this.documentName = documentName;
        this.layerId = layerId;
        this.offset = offset;
        this.annotationId = annotationId;
        this.sentenceId = sentenceId;
        this.recommenderId = recommenderId;
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

    public Offset getOffset()
    {
        return offset;
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

    public void setRecommenderId(long recommenderId)
    {
        this.recommenderId = recommenderId;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + annotationId;
        result = prime * result + ((documentName == null) ? 0 : documentName.hashCode());
        result = prime * result + (int) (layerId ^ (layerId >>> 32));
        result = prime * result + ((offset == null) ? 0 : offset.hashCode());
        result = prime * result + (int) (projectId ^ (projectId >>> 32));
        result = prime * result + (int) (recommenderId ^ (recommenderId >>> 32));
        result = prime * result + sentenceId;
        result = prime * result + ((userName == null) ? 0 : userName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ExtendedId other = (ExtendedId) obj;
        if (annotationId != other.annotationId) {
            return false;
        }
        if (documentName == null) {
            if (other.documentName != null) {
                return false;
            }
        }
        else if (!documentName.equals(other.documentName)) {
            return false;
        }
        if (layerId != other.layerId) {
            return false;
        }
        if (offset == null) {
            if (other.offset != null) {
                return false;
            }
        }
        else if (!offset.equals(other.offset)) {
            return false;
        }
        if (projectId != other.projectId) {
            return false;
        }
        if (recommenderId != other.recommenderId) {
            return false;
        }
        if (sentenceId != other.sentenceId) {
            return false;
        }
        if (userName == null) {
            if (other.userName != null) {
                return false;
            }
        }
        else if (!userName.equals(other.userName)) {
            return false;
        }
        return true;
    }
}
