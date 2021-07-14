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
package de.tudarmstadt.ukp.inception.search;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

import java.util.Optional;

public class StatisticRequest
{
    private final Project project;
    private final User user;
    private final String statistic;

    private final SourceDocument limitedToDocument;

    private final AnnotationLayer annotationLayer;
    private final AnnotationFeature annotationFeature;

    public StatisticRequest(Project aProject, User aUser, String aStatistic)
    {
        this(aProject, aUser, aStatistic, null);
    }

    public StatisticRequest(Project aProject, User aUser, String aStatistic,
            SourceDocument aLimitedToDocument)
    {
        this(aProject, aUser, aStatistic, aLimitedToDocument, null, null);
    }

    public StatisticRequest(Project aProject, User aUser, String aStatistic,
            SourceDocument aLimitedToDocument, AnnotationLayer aAnnotationLayer,
            AnnotationFeature aAnnotationFeature)
    {
        super();
        project = aProject;
        user = aUser;
        statistic = aStatistic;
        limitedToDocument = aLimitedToDocument;
        annotationLayer = aAnnotationLayer;
        annotationFeature = aAnnotationFeature;
    }

    public Project getProject()
    {
        return project;
    }

    public User getUser()
    {
        return user;
    }

    public String getStatistic()
    {
        return statistic;
    }

    public Optional<SourceDocument> getLimitedToDocument()
    {
        return Optional.ofNullable(limitedToDocument);
    }

    public AnnotationLayer getAnnotationLayer()
    {
        return annotationLayer;
    }

    public AnnotationFeature getAnnotationFeature()
    {
        return annotationFeature;
    }

}
