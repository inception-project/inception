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

import de.tudarmstadt.ukp.clarin.webanno.api.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectPermissionsChangedEvent;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.workload.event.RecalculateProjectStateTask;
import de.tudarmstadt.ukp.inception.workload.matrix.config.MatrixWorkloadManagerAutoConfiguration;

/**
 * Watches the state of the annotations and documents in matrix projects.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link MatrixWorkloadManagerAutoConfiguration#matrixWorkloadStateWatcher}
 * </p>
 */
public class MatrixWorkloadStateWatcher
{
    private final SchedulingService schedulingService;

    public MatrixWorkloadStateWatcher(SchedulingService aSchedulingService)
    {
        schedulingService = aSchedulingService;
    }

    @EventListener
    public void onProjectPermissionsChangedEvent(ProjectPermissionsChangedEvent aEvent)
    {
        schedulingService.enqueue(new RecalculateProjectStateTask(aEvent.getProject(),
                "onProjectPermissionsChangedEvent"));
    }

    @EventListener
    public void onAnnotationStateChangeEvent(AnnotationStateChangeEvent aEvent)
    {
        schedulingService.enqueue(new MatrixWorkloadUpdateDocumentStateTask(aEvent.getDocument(),
                getClass().getSimpleName()));
    }
}
