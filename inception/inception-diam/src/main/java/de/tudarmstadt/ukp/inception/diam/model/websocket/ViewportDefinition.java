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
package de.tudarmstadt.ukp.inception.diam.model.websocket;

import java.util.Properties;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.uima.cas.text.AnnotationPredicates;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.diam.service.DiamWebsocketController;
import de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants;

public class ViewportDefinition
{
    private final long projectId;
    private final long documentId;
    private final String user;
    private final int begin;
    private final int end;
    private final String format;

    public ViewportDefinition(AnnotationDocument aDoc, int aBegin, int aEnd, String aFormat)
    {
        projectId = aDoc.getProject().getId();
        documentId = aDoc.getDocument().getId();
        user = aDoc.getUser();
        begin = aBegin;
        end = aEnd;
        format = aFormat;
    }

    public ViewportDefinition(SourceDocument aDoc, String aUser, int aBegin, int aEnd,
            String aFormat)
    {
        projectId = aDoc.getProject().getId();
        documentId = aDoc.getId();
        user = aUser;
        begin = aBegin;
        end = aEnd;
        format = aFormat;
    }

    public ViewportDefinition(long aProjectId, long aDocumentId, String aUser, int aBegin, int aEnd,
            String aFormat)
    {
        projectId = aProjectId;
        documentId = aDocumentId;
        user = aUser;
        begin = aBegin;
        end = aEnd;
        format = aFormat;
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

    public String getFormat()
    {
        return format;
    }

    public String getTopic()
    {
        Properties properties = new Properties();
        properties.setProperty(WebSocketConstants.PARAM_PROJECT, String.valueOf(projectId));
        properties.setProperty(WebSocketConstants.PARAM_DOCUMENT, String.valueOf(documentId));
        properties.setProperty(WebSocketConstants.PARAM_USER, user);
        properties.setProperty(DiamWebsocketController.PARAM_FROM, String.valueOf(begin));
        properties.setProperty(DiamWebsocketController.PARAM_TO, String.valueOf(end));
        properties.setProperty(DiamWebsocketController.PARAM_FORMAT, String.valueOf(format));
        return DiamWebsocketController.PLACEHOLDER_RESOLVER.replacePlaceholders(
                DiamWebsocketController.DOCUMENT_VIEWPORT_TOPIC_TEMPLATE, properties);
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
                .append(end, castOther.end).append(format, castOther.format).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(documentId).append(user).append(begin).append(end)
                .append(format).toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
                .append("documentId", documentId).append("user", user).append("begin", begin)
                .append("end", end).append("format", format).toString();
    }
}
