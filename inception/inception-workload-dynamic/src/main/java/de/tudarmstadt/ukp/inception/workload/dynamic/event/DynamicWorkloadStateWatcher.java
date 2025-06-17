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
package de.tudarmstadt.ukp.inception.workload.dynamic.event;

import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.inception.documents.event.AnnotationStateChangeEvent;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.workload.dynamic.config.DynamicWorkloadManagerAutoConfiguration;

/**
 * Watches the state of the annotations and documents in dynamic workload projects.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DynamicWorkloadManagerAutoConfiguration#dynamicWorkloadStateWatcher}
 * </p>
 */
public class DynamicWorkloadStateWatcher
{
    private final SchedulingService schedulingService;

    public DynamicWorkloadStateWatcher(SchedulingService aSchedulingService)
    {
        schedulingService = aSchedulingService;
    }

    @EventListener
    public void onAnnotationStateChangeEvent(AnnotationStateChangeEvent aEvent)
    {
        schedulingService.enqueue(DynamicWorkloadUpdateDocumentStateTask.builder() //
                .withDocument(aEvent.getDocument()) //
                .withTrigger(getClass().getSimpleName()) //
                .build());
    }
}
