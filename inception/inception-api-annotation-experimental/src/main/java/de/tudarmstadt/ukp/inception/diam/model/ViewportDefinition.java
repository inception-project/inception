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
package de.tudarmstadt.ukp.inception.diam.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.uima.cas.text.AnnotationPredicates;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class ViewportDefinition
{
    private final long projectId;
    private final long documentId;
    private final String user;
    private final int begin;
    private final int end;

    public ViewportDefinition(AnnotationDocument aDoc, int aBegin, int aEnd)
    {
        projectId = aDoc.getProject().getId();
        documentId = aDoc.getDocument().getId();
        user = aDoc.getUser();
        begin = aBegin;
        end = aEnd;
    }

    public ViewportDefinition(SourceDocument aDoc, String aUser, int aBegin, int aEnd)
    {
        projectId = aDoc.getProject().getId();
        documentId = aDoc.getId();
        user = aUser;
        begin = aBegin;
        end = aEnd;
    }

    public ViewportDefinition(long aProjectId, long aDocumentId, String aUser, int aBegin, int aEnd)
    {
        projectId = aProjectId;
        documentId = aDocumentId;
        user = aUser;
        begin = aBegin;
        end = aEnd;
    }

    public boolean matches(long aDocumentId, String aUser, int aBegin, int aEnd)
    {
        if (aDocumentId != documentId || !user.equals(aUser)) {
            return false;
        }

        return AnnotationPredicates.overlapping(begin, end, aBegin, aEnd);
    }

    public long getProjectId()
    {
        return projectId;
    }

    public long getDocumentId()
    {
        return documentId;
    }

    public String getUser()
    {
        return user;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    public String getTopic()
    {
        return "/project/" + projectId + "/document/" + documentId + "/user/" + user + "/from/"
                + begin + "/to/" + end;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ViewportDefinition)) {
            return false;
        }
        ViewportDefinition castOther = (ViewportDefinition) other;
        return new EqualsBuilder().append(documentId, castOther.documentId)
                .append(user, castOther.user).append(begin, castOther.begin)
                .append(end, castOther.end).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(documentId).append(user).append(begin).append(end)
                .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
                .append("documentId", documentId).append("user", user).append("begin", begin)
                .append("end", end).toString();
    }
}
