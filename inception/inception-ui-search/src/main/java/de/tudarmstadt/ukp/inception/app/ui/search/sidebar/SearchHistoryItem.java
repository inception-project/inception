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
package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchHistoryItem(boolean pinned, String query, boolean limitToDocument,
        String groupingLayer, String groupingFeature, boolean lowLevelPaging)
    implements Serializable
{
    private SearchHistoryItem(Builder builder)
    {
        this(builder.pinned, builder.query, builder.limitToDocument, builder.groupingLayer,
                builder.groupingFeature, builder.lowLevelPaging);
    }

    public SearchHistoryItem togglePin()
    {
        return new SearchHistoryItem(!pinned, query, limitToDocument, groupingLayer,
                groupingFeature, lowLevelPaging);
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof SearchHistoryItem)) {
            return false;
        }
        var castOther = (SearchHistoryItem) other;
        return new EqualsBuilder().append(query, castOther.query)
                .append(limitToDocument, castOther.limitToDocument)
                .append(groupingLayer, castOther.groupingLayer)
                .append(groupingFeature, castOther.groupingFeature)
                .append(lowLevelPaging, castOther.lowLevelPaging).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(query).append(limitToDocument).append(groupingLayer)
                .append(groupingFeature).append(lowLevelPaging).toHashCode();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private boolean pinned;
        private String query;
        private boolean limitToDocument;
        private String groupingLayer;
        private String groupingFeature;
        private boolean lowLevelPaging;

        private Builder()
        {
        }

        public Builder withPinned(boolean aPinned)
        {
            this.pinned = aPinned;
            return this;
        }

        public Builder withQuery(String aQuery)
        {
            this.query = aQuery;
            return this;
        }

        public Builder withLimitToDocument(boolean aLimitToDocument)
        {
            this.limitToDocument = aLimitToDocument;
            return this;
        }

        public Builder withGroupingLayer(AnnotationLayer aGroupingLayer)
        {
            groupingLayer = aGroupingLayer != null ? aGroupingLayer.getName() : null;
            return this;
        }

        public Builder withGroupingFeature(AnnotationFeature aGroupingFeature)
        {
            groupingFeature = aGroupingFeature != null ? aGroupingFeature.getName() : null;
            return this;
        }

        public Builder withLowLevelPaging(boolean aLowLevelPaging)
        {
            this.lowLevelPaging = aLowLevelPaging;
            return this;
        }

        public SearchHistoryItem build()
        {
            return new SearchHistoryItem(this);
        }
    }
}
