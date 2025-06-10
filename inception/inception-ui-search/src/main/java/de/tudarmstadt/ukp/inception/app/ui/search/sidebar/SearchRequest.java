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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public record SearchRequest(User dataOwner, Project project, String query,
        SourceDocument limitToDocument, AnnotationLayer groupingLayer,
        AnnotationFeature groupingFeature, boolean lowLevelPaging)
    implements Serializable
{
    private SearchRequest(Builder builder)
    {
        this(builder.dataOwner, builder.project, builder.query, builder.limitToDocument,
                builder.groupingLayer, builder.groupingFeature, builder.lowLevelPaging);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private User dataOwner;
        private Project project;
        private String query;
        private SourceDocument limitToDocument;
        private AnnotationLayer groupingLayer;
        private AnnotationFeature groupingFeature;
        private boolean lowLevelPaging;

        private Builder()
        {
        }

        public Builder withDataOwner(User aDataOwner)
        {
            this.dataOwner = aDataOwner;
            return this;
        }

        public Builder withProject(Project aProject)
        {
            this.project = aProject;
            return this;
        }

        public Builder withQuery(String aQuery)
        {
            this.query = aQuery;
            return this;
        }

        public Builder withLimitToDocument(SourceDocument aLimitToDocument)
        {
            this.limitToDocument = aLimitToDocument;
            return this;
        }

        public Builder withGroupingLayer(AnnotationLayer aGroupingLayer)
        {
            this.groupingLayer = aGroupingLayer;
            return this;
        }

        public Builder withGroupingFeature(AnnotationFeature aGroupingFeature)
        {
            this.groupingFeature = aGroupingFeature;
            return this;
        }

        public Builder withLowLevelPaging(boolean aLowLevelPaging)
        {
            this.lowLevelPaging = aLowLevelPaging;
            return this;
        }

        public SearchRequest build()
        {
            return new SearchRequest(this);
        }
    }
}
