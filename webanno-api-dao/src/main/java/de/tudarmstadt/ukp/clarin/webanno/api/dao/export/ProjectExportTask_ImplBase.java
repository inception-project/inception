/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.export;

import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState.CANCELLED;
import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState.COMPLETED;
import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState.FAILED;
import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState.RUNNING;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_REPOSITORY_PATH;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_USERNAME;

import java.io.File;
import java.nio.channels.ClosedByInterruptException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskHandle;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

public abstract class ProjectExportTask_ImplBase
    implements Runnable
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    // The task needs to hold on to the handle because it is used in a WeakHashMap in
    // ProjectExportService to allow access to tasks.
    private final ProjectExportTaskHandle handle;
    private final String username;
    private final ProjectExportTaskMonitor monitor;
    private final ProjectExportRequest request;
    
    private @Autowired DocumentService documentService;
    
    public ProjectExportTask_ImplBase(ProjectExportTaskHandle aHandle,
            ProjectExportTaskMonitor aMonitor, ProjectExportRequest aRequest, String aUsername)
    {
        handle = aHandle;
        request = aRequest;
        username = aUsername;
        monitor = aMonitor;
        
        monitor.setCreateTime(System.currentTimeMillis());
    }

    @Override
    public void run()
    {
        try {
            // We are in a new thread. Set up thread-specific MDC
            MDC.put(KEY_USERNAME, username);
            MDC.put(KEY_PROJECT_ID, String.valueOf(request.getProject().getId()));
            MDC.put(KEY_REPOSITORY_PATH, documentService.getDir().toString());
            
            monitor.setState(RUNNING);
            
            File exportedFile = export(request, monitor);
            
            monitor.setExportedFile(exportedFile);
            monitor.setStateAndProgress(COMPLETED, 100);
        }
        catch (ClosedByInterruptException | InterruptedException e) {
            monitor.setStateAndProgress(CANCELLED, 100);
        }
        catch (Throwable e) {
            // This marks the progression as complete and causes ProgressBar#onFinished
            // to be called where we display the messages
            monitor.setStateAndProgress(FAILED, 100);
            monitor.addMessage(LogMessage.error(this, "Unexpected error during project export: %s",
                            ExceptionUtils.getRootCauseMessage(e)));
            log.error("Unexpected error during project export", e);
        }
    }
    
    public abstract File export(ProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor)
        throws Exception;

    public ProjectExportRequest getRequest()
    {
        return request;
    }
    
    public ProjectExportTaskMonitor getMonitor()
    {
        return monitor;
    }
    
    public ProjectExportTaskHandle getHandle()
    {
        return handle;
    }
}
