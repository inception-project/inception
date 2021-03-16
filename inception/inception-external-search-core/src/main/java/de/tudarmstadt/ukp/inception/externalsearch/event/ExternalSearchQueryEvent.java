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
package de.tudarmstadt.ukp.inception.externalsearch.event;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class ExternalSearchQueryEvent
    extends ApplicationEvent
{

    private static final long serialVersionUID = 2911869258080097719L;

    private final Project project;
    private final String user;
    private final String query;

    public ExternalSearchQueryEvent(Object aSource, Project aProject, String aUser, String aQuery)
    {
        super(aSource);

        project = aProject;
        user = aUser;
        query = aQuery;
    }

    public String getUser()
    {
        return user;
    }

    public Project getProject()
    {
        return project;
    }

    public String getQuery()
    {
        return query;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("ExternalSearchQueryEvent [project=");
        builder.append(project);
        builder.append(", user=");
        builder.append(user);
        builder.append(", query=");
        builder.append(query);
        builder.append("]");
        return builder.toString();
    }
}
