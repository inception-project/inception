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

import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.search.model.AnnotationSearchState;

public class StatisticRequest
{
    private final Project project;
    private final User user;
    private final AnnotationSearchState prefs;

    private final int minTokenPerDoc;
    private final int maxTokenPerDoc;

    private Set<AnnotationFeature> features;
    private final String query;

    public StatisticRequest(Project aProject, User aUser, int aMinTokenPerDoc, int aMaxTokenPerDoc,
            Set<AnnotationFeature> aFeatures, String aQuery, AnnotationSearchState aPrefs)
    {
        project = aProject;
        user = aUser;

        minTokenPerDoc = aMinTokenPerDoc;
        maxTokenPerDoc = aMaxTokenPerDoc;
        query = aQuery;
        features = aFeatures;
        prefs = aPrefs;
    }

    public Project getProject()
    {
        return project;
    }

    public User getUser()
    {
        return user;
    }

    public int getMinTokenPerDoc()
    {
        return minTokenPerDoc;
    }

    public int getMaxTokenPerDoc()
    {
        return maxTokenPerDoc;
    }

    public Set<AnnotationFeature> getFeatures()
    {
        return features;
    }

    public void addFeature(AnnotationFeature aFeature)
    {
        features.add(aFeature);
    }

    public String getQuery()
    {
        return query;
    }

    public AnnotationSearchState getSearchSettings()
    {
        return prefs;
    }
}
