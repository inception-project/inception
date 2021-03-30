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
package de.tudarmstadt.ukp.inception.workload.matrix.event;

import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeDocumentRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.DocumentStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;

/**
 * Watches the state of the annotations and documents.
 */
public class MatrixWorkloadStateWatcher
{
    private final SchedulingService schedulingService;

    public MatrixWorkloadStateWatcher(SchedulingService aSchedulingService)
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
        schedulingService
                .enqueue(new MatrixUpdateProjectStateTask(aProject, getClass().getSimpleName()));
    }

    @EventListener
    public void onAnnotationStateChangeEvent(AnnotationStateChangeEvent aEvent)
    {
        recalculateDocumentState(aEvent.getAnnotationDocument());
    }

    private void recalculateDocumentState(AnnotationDocument aAnnotationDocument)
    {
        schedulingService.enqueue(new MatrixUpdateDocumentStateTask(
                aAnnotationDocument.getDocument(), getClass().getSimpleName()));
    }
}
