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

public class SearchQueryRequest
{
    private final Project project;
    private final String username;
    private final String query;

    private final SourceDocument limitedToDocument;

    private final AnnotationLayer annoationLayer;
    private final AnnotationFeature annotationFeature;

    public SearchQueryRequest(Project aProject, String aUsername, String aQuery)
    {
        this(aProject, aUsername, aQuery, null);
    }

    public SearchQueryRequest(Project aProject, String aUsername, String aQuery,
            SourceDocument aLimitedToDocument)
    {
        this(aProject, aUsername, aQuery, aLimitedToDocument, null, null);
    }

    public SearchQueryRequest(Project aProject, String aUsername, String aQuery,
        SourceDocument aLimitedToDocument, AnnotationLayer aAnnotationLayer,
        AnnotationFeature aAnnotationFeature)
    {
        super();
        project = aProject;
        username = aUsername;
        query = aQuery;
        limitedToDocument = aLimitedToDocument;
        annoationLayer = aAnnotationLayer;
        annotationFeature = aAnnotationFeature;
    }

    public Project getProject()
    {
        return project;
    }

    public String getUsername()
    {
        return username;
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
}
