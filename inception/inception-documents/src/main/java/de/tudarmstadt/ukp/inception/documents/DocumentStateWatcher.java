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
package de.tudarmstadt.ukp.inception.documents;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.documents.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.inception.documents.event.BeforeDocumentRemovedEvent;
import de.tudarmstadt.ukp.inception.documents.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.workload.task.UpdateProjectStateTask;

/**
 * Watches the document state in of projects using matrix workload.
 */
@Component
public class DocumentStateWatcher
{
    private final SchedulingService schedulingService;

    public DocumentStateWatcher(SchedulingService aSchedulingService)
    {
        schedulingService = aSchedulingService;
    }

    @EventListener
    public void onDocumentStateChangeEvent(DocumentStateChangedEvent aEvent)
    {
        recalculateProjectState(aEvent.getDocument().getProject());
    }

    @EventListener
    public void onAfterDocumentCreatedEvent(AfterDocumentCreatedEvent aEvent)
    {
        recalculateProjectState(aEvent.getDocument().getProject());
    }

    @EventListener
    public void onBeforeDocumentRemovedEvent(BeforeDocumentRemovedEvent aEvent)
    {
        recalculateProjectState(aEvent.getDocument().getProject());
    }

    private void recalculateProjectState(Project aProject)
    {
        schedulingService.enqueue(UpdateProjectStateTask.builder() //
                .withProject(aProject) //
                .withTrigger(getClass().getSimpleName()) //
                .build());
    }
}
