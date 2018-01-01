/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks.json;

import de.tudarmstadt.ukp.clarin.webanno.api.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.RemoteApiController2;

public class DocumentStateChangeMessage
{
    private long projectId;
    private String projectName;

    private long documentId;
    private String documentName;
    
    private String documentPreviousState;
    private String documentState;
    
    public DocumentStateChangeMessage()
    {
        // Nothing to do
    }
    
    public DocumentStateChangeMessage(DocumentStateChangedEvent aEvent)
    {
        projectId = aEvent.getDocument().getProject().getId();
        projectName = aEvent.getDocument().getProject().getName();
        
        documentId = aEvent.getDocument().getId();
        documentName = aEvent.getDocument().getName();
        
        documentState = RemoteApiController2.sourceDocumentStateToString(aEvent.getNewState());
        documentPreviousState = RemoteApiController2
                .sourceDocumentStateToString(aEvent.getPreviousState());
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

    public String getDocumentPreviousState()
    {
        return documentPreviousState;
    }

    public void setDocumentPreviousState(String aDocumentPreviousState)
    {
        documentPreviousState = aDocumentPreviousState;
    }

    public String getDocumentState()
    {
        return documentState;
    }

    public void setDocumentState(String aDocumentState)
    {
        documentState = aDocumentState;
    }
}
