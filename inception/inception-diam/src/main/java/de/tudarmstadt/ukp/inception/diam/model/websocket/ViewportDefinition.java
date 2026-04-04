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

import static java.lang.String.join;
import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.lang3.builder.ToStringStyle.NO_CLASS_NAME_STYLE;

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.uima.cas.text.AnnotationPredicates;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.diam.service.DiamWebsocketController;
import de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants;

public class ViewportDefinition
{
    private final long projectId;
    private final long documentId;
    private final String dataOwner;

    private final int begin;
    private final int end;

    private final Set<String> enabledExtensions;
    private final String format;

    private ViewportDefinition(Builder builder)
    {
        projectId = builder.projectId;
        documentId = builder.documentId;
        dataOwner = builder.dataOwner;
        begin = builder.begin;
        end = builder.end;
        format = builder.format;
        enabledExtensions = unmodifiableSet(new HashSet<>(builder.enabledExtensions));
    }

    public boolean matches(long aDocumentId, AnnotationSet aSet, int aBegin, int aEnd)
    {
        if (aDocumentId != documentId || !dataOwner.equals(aSet.id())) {
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
        return dataOwner;
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

    public Set<String> getEnabledExtensions()
    {
        return enabledExtensions;
    }

    public String getTopic()
    {
        var properties = new Properties();
        properties.setProperty(WebSocketConstants.PARAM_PROJECT, String.valueOf(projectId));
        properties.setProperty(WebSocketConstants.PARAM_DOCUMENT, String.valueOf(documentId));
        properties.setProperty(WebSocketConstants.PARAM_USER, dataOwner);
        properties.setProperty(DiamWebsocketController.PARAM_FROM, String.valueOf(begin));
        properties.setProperty(DiamWebsocketController.PARAM_TO, String.valueOf(end));
        return DiamWebsocketController.PLACEHOLDER_RESOLVER.replacePlaceholders(
                DiamWebsocketController.DOCUMENT_VIEWPORT_TOPIC_TEMPLATE, properties);
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ViewportDefinition)) {
            return false;
        }
        var castOther = (ViewportDefinition) other;
        return new EqualsBuilder() //
                .append(documentId, castOther.documentId) //
                .append(dataOwner, castOther.dataOwner) //
                .append(begin, castOther.begin) //
                .append(end, castOther.end) //
                .append(format, castOther.format) //
                .append(enabledExtensions, castOther.enabledExtensions) //
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder() //
                .append(documentId) //
                .append(dataOwner) //
                .append(begin) //
                .append(end) //
                .append(format) //
                .append(enabledExtensions) //
                .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, NO_CLASS_NAME_STYLE) //
                .append("documentId", documentId) //
                .append("user", dataOwner) //
                .append("begin", begin) //
                .append("end", end) //
                .append("format", format) //
                .append("extensions", "[" + join(",", enabledExtensions) + "]") //
                .toString();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private long projectId;
        private long documentId;
        private String dataOwner;
        private int begin = 0;
        private int end = Integer.MAX_VALUE;
        private String format;
        private Set<String> enabledExtensions = new HashSet<>();

        private Builder()
        {
        }

        public Builder withProjectId(long aProjectId)
        {
            projectId = aProjectId;
            return this;
        }

        public Builder withDocumentId(long aDocumentId)
        {
            documentId = aDocumentId;
            return this;
        }

        public Builder withDocument(SourceDocument aDocument)
        {
            projectId = aDocument.getProject().getId();
            documentId = aDocument.getId();
            return this;
        }

        public Builder withDocument(AnnotationDocument aDocument)
        {
            withDocument(aDocument.getDocument());
            dataOwner = aDocument.getUser();
            return this;
        }

        public Builder withDataOwner(String aDataOwner)
        {
            dataOwner = aDataOwner;
            return this;
        }

        public Builder withBegin(int aBegin)
        {
            begin = aBegin;
            return this;
        }

        public Builder withEnd(int aEnd)
        {
            end = aEnd;
            return this;
        }

        public Builder withRange(int aBegin, int aEnd)
        {
            begin = aBegin;
            end = aEnd;
            return this;
        }

        public Builder withFormat(String aFormat)
        {
            format = aFormat;
            return this;
        }

        public Builder enabledExtensions(Collection<String> aExtensions)
        {
            enabledExtensions.clear();
            enabledExtensions.addAll(aExtensions);
            return this;
        }

        public ViewportDefinition build()
        {
            return new ViewportDefinition(this);
        }
    }
}
