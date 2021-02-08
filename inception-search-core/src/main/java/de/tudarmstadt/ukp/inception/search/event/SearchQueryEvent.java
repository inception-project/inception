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
package de.tudarmstadt.ukp.inception.search.event;

import java.util.Optional;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class SearchQueryEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = 5672246678417774137L;

    private final Project project;
    private final String user;
    private final String query;
    private final SourceDocument sourceDocument;

    public SearchQueryEvent(Object aSource, Project aProject, String aUser, String aQuery)
    {
        this(aSource, aProject, aUser, aQuery, null);
    }

    /**
     * @param aSource
     *            event source
     * @param aProject
     *            related project
     * @param aUser
     *            user executing the query
     * @param aQuery
     *            the query
     * @param aSourceDocument
     *            to which to limit the query
     */
    public SearchQueryEvent(Object aSource, Project aProject, String aUser, String aQuery,
            SourceDocument aSourceDocument)
    {
        super(aSource);

        project = aProject;
        user = aUser;
        query = aQuery;
        sourceDocument = aSourceDocument;
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

    /**
     * Query is limited to the given document.
     * 
     * @return the source document if it exists or empty optional
     */
    public Optional<SourceDocument> getSourceDocument()
    {
        return Optional.ofNullable(sourceDocument);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("RecommenderEvaluationResultEvent [project=");
        builder.append(project);
        builder.append(", user=");
        builder.append(user);
        builder.append(", query=");
        builder.append(query);
        builder.append("]");
        return builder.toString();
    }
}
