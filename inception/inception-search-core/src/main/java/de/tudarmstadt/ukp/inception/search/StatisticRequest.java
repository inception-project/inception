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

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class StatisticRequest
{
    private final Project project;
    private final User user;
    private final String statistic;

    private final Double lowerDocumentSize;
    private final Double upperDocumentSize;

    public StatisticRequest(Project aProject, User aUser, String aStatistic)
    {
        this(aProject, aUser, aStatistic, null, null);
    }

    public StatisticRequest(Project aProject, User aUser, String aStatistic,
            Double aLowerDocumentSize, Double aUpperDocumentSize)
    {
        project = aProject;
        user = aUser;
        statistic = aStatistic;

        lowerDocumentSize = aLowerDocumentSize;
        upperDocumentSize = aUpperDocumentSize;
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

    public Double getLowerDocumentSize()
    {
        return lowerDocumentSize;
    }

    public Double getUpperDocumentSize()
    {
        return upperDocumentSize;
    }

}
