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
package de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CasKey
{
    private final long projectId;
    private final long documentId;
    private final AnnotationSet set;

    // These are just for information and are not included when checking for equality or building a
    // hash. Mind that the name of a project (possibly the name of a document) might change during
    // the lifetime of the key.
    private String projectName;
    private String documentName;

    public CasKey(SourceDocument aDocument, AnnotationSet aSet)
    {
        projectName = aDocument.getProject().getName();
        projectId = aDocument.getProject().getId();
        documentName = aDocument.getName();
        documentId = aDocument.getId();
        set = aSet;
    }

    public CasKey(long aProjectId, long aDocumentId, AnnotationSet aSet)
    {
        projectId = aProjectId;
        documentId = aDocumentId;
        set = aSet;
    }

    public long getProjectId()
    {
        return projectId;
    }

    public long getDocumentId()
    {
        return documentId;
    }

    public AnnotationSet getSet()
    {
        return set;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String aProjectName)
    {
        projectName = aProjectName;
    }

    public String getDocumentName()
    {
        return documentName;
    }

    public void setDocumentName(String aDocumentName)
    {
        documentName = aDocumentName;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof CasKey)) {
            return false;
        }
        CasKey castOther = (CasKey) other;
        return new EqualsBuilder() //
                .append(projectId, castOther.projectId) //
                .append(documentId, castOther.documentId) //
                .append(set, castOther.set) //
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder() //
                .append(projectId) //
                .append(documentId) //
                .append(set) //
                .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE) //
                .append("p", projectId) //
                .append("d", documentId) //
                .append("s", set) //
                .toString();
    }

    public static CasKey matchingAllFromProject(Project aProject)
    {
        return new CasKey(aProject.getId(), -1, null)
        {
            @Override
            public boolean equals(final Object other)
            {
                if (!(other instanceof CasKey)) {
                    return false;
                }
                CasKey castOther = (CasKey) other;
                return new EqualsBuilder().append(getProjectId(), castOther.getProjectId())
                        .isEquals();
            }

            @Override
            public int hashCode()
            {
                return new HashCodeBuilder().append(getProjectId()).toHashCode();
            }
        };
    }
}
