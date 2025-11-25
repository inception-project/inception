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

class ExtendedId
    implements Serializable
{
    private static final long serialVersionUID = -5214683455382881005L;

    private final int suggestionId;
    private final long recommenderId;
    private final long layerId;
    private final long documentId;
    private final Position position;
    private final int hash;

    public ExtendedId(AnnotationSuggestion aSuggestion)
    {
        documentId = aSuggestion.getDocumentId();
        layerId = aSuggestion.getLayerId();
        suggestionId = aSuggestion.getId();
        recommenderId = aSuggestion.getRecommenderId();
        position = aSuggestion.getPosition();
        hash = Objects.hash(suggestionId, documentId, layerId, position, recommenderId);
    }

    public long getDocumentId()
    {
        return documentId;
    }

    public long getLayerId()
    {
        return layerId;
    }

    public Position getPosition()
    {
        return position;
    }

    public int getSuggestionId()
    {
        return suggestionId;
    }

    public long getRecommenderId()
    {
        return recommenderId;
    }

    @Override
    public int hashCode()
    {
        return hash;
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
        return Objects.equals(position, other.position) //
                && suggestionId == other.suggestionId //
                && documentId == other.documentId //
                && layerId == other.layerId //
                && recommenderId == other.recommenderId;
    }
}
