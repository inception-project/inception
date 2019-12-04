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
package de.tudarmstadt.ukp.inception.ui.kb.log;

import java.util.StringJoiner;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class EntityLinkingSearchQueryEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = 5672246678417774137L;
    
    private final Project project;
    private final String user;
    private final String query;
    private final SourceDocument sourceDocument;
    private final int begin;
    private final int end;
    private final int numberOfCandidates;

    public EntityLinkingSearchQueryEvent(Object aSource, Project aProject, String aUser,
            String aQuery, SourceDocument aSourceDocument, int aBegin, int aEnd,
            int aNumberOfCandidates)
    {
        super(aSource);

        project = aProject;
        user = aUser;
        query = aQuery;
        sourceDocument = aSourceDocument;
        begin = aBegin;
        end = aEnd;
        numberOfCandidates = aNumberOfCandidates;
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
     */
    public SourceDocument getSourceDocument()
    {
        return sourceDocument;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public int getNumberOfCandidates() {
        return numberOfCandidates;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EntityLinkingSearchQueryEvent.class.getSimpleName() + "[", "]")
                .add("project=" + project)
                .add("user='" + user + "'")
                .add("query='" + query + "'")
                .add("sourceDocument=" + sourceDocument)
                .add("begin=" + begin)
                .add("end=" + end)
                .toString();
    }
}
