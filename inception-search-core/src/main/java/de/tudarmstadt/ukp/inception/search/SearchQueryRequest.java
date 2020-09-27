/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.search;

import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class SearchQueryRequest
{
    private final Project project;
    private final User user;
    private final String query;

    private final SourceDocument limitedToDocument;

    private final AnnotationLayer annoationLayer;
    private final AnnotationFeature annotationFeature;

    private final long offset;
    private final long count;

    public SearchQueryRequest(Project aProject, User aUser, String aQuery)
    {
        this(aProject, aUser, aQuery, null);
    }

    public SearchQueryRequest(Project aProject, User aUser, String aQuery,
        SourceDocument aLimitedToDocument)
    {
        this(aProject, aUser, aQuery, aLimitedToDocument, null, null, 0, Integer.MAX_VALUE);
    }

    public SearchQueryRequest(Project aProject, User aUser, String aQuery,
        SourceDocument aLimitedToDocument, AnnotationLayer aAnnotationLayer,
        AnnotationFeature aAnnotationFeature, long aOffset, long aCount)
    {
        super();
        project = aProject;
        user = aUser;
        query = aQuery;
        limitedToDocument = aLimitedToDocument;
        annoationLayer = aAnnotationLayer;
        annotationFeature = aAnnotationFeature;
        offset = aOffset;
        count = aCount;
    }

    public Project getProject()
    {
        return project;
    }

    public User getUser()
    {
        return user;
    }

    public String getQuery()
    {
        return query;
    }

    public Optional<SourceDocument> getLimitedToDocument()
    {
        return Optional.ofNullable(limitedToDocument);
    }

    public AnnotationLayer getAnnoationLayer()
    {
        return annoationLayer;
    }

    public AnnotationFeature getAnnotationFeature()
    {
        return annotationFeature;
    }

    public long getOffset()
    {
        return offset;
    }

    public long getCount()
    {
        return count;
    }
}
