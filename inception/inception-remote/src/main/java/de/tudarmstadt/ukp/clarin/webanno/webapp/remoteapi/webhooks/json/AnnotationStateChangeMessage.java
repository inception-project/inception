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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json;

import static de.tudarmstadt.ukp.inception.remoteapi.AnnotationDocumentStateUtils.annotationDocumentStateToString;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import de.tudarmstadt.ukp.inception.documents.event.AnnotationStateChangeEvent;

public class AnnotationStateChangeMessage
{
    private long timestamp;

    private long projectId;
    private String projectName;

    private long documentId;
    private String documentName;

    private String user;
    private String annotationUser;
    private String annotatorComment;

    private String annotationPreviousState;
    private String annotationState;
    private String annotatorAnnotationState;

    public AnnotationStateChangeMessage()
    {
        // Nothing to do
    }

    public AnnotationStateChangeMessage(AnnotationStateChangeEvent aEvent)
    {
        timestamp = aEvent.getTimestamp();

        projectId = aEvent.getDocument().getProject().getId();
        projectName = aEvent.getDocument().getProject().getName();

        documentId = aEvent.getDocument().getId();
        documentName = aEvent.getDocument().getName();

        user = aEvent.getUser();
        annotationUser = aEvent.getAnnotationDocument().getUser();
        annotatorComment = aEvent.getAnnotationDocument().getAnnotatorComment();

        annotationState = annotationDocumentStateToString(aEvent.getNewState());
        annotationPreviousState = annotationDocumentStateToString(aEvent.getPreviousState());
        annotatorAnnotationState = annotationDocumentStateToString(
                aEvent.getAnnotationDocument().getAnnotatorState());
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(long aTimestamp)
    {
        timestamp = aTimestamp;
    }

    public long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(long aProjectId)
    {
        projectId = aProjectId;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String aProjectName)
    {
        projectName = aProjectName;
    }

    public long getDocumentId()
    {
        return documentId;
    }

    public void setDocumentId(long aDocumentId)
    {
        documentId = aDocumentId;
    }

    public String getDocumentName()
    {
        return documentName;
    }

    public void setDocumentName(String aDocumentName)
    {
        documentName = aDocumentName;
    }

    public String getAnnotationPreviousState()
    {
        return annotationPreviousState;
    }

    public void setAnnotationPreviousState(String aAnnotationPreviousState)
    {
        annotationPreviousState = aAnnotationPreviousState;
    }

    public String getAnnotationState()
    {
        return annotationState;
    }

    public void setAnnotationState(String aAnnotationState)
    {
        annotationState = aAnnotationState;
    }

    public String getAnnotationUser()
    {
        return annotationUser;
    }

    public void setAnnotationUser(String aAnnotationUser)
    {
        annotationUser = aAnnotationUser;
    }

    public void setAnnotatorComment(String aAnnotatorComment)
    {
        annotatorComment = aAnnotatorComment;
    }

    public String getAnnotatorComment()
    {
        return annotatorComment;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String aUser)
    {
        user = aUser;
    }

    public String getAnnotatorAnnotationState()
    {
        return annotatorAnnotationState;
    }

    public void setAnnotatorAnnotationState(String aAnnotatorAnnotationState)
    {
        annotatorAnnotationState = aAnnotatorAnnotationState;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.JSON_STYLE) //
                .append("timestamp", timestamp) //
                .append("projectId", projectId) //
                .append("projectName", projectName) //
                .append("documentId", documentId) //
                .append("documentName", documentName) //
                .append("user", user) //
                .append("annotationUser", annotationUser) //
                .append("annotationPreviousState", annotationPreviousState) //
                .append("annotationState", annotationState) //
                .append("annotatorAnnotationState", annotatorAnnotationState) //
                .append("annotatorComment", annotatorComment) //
                .toString();
    }
}
