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
package de.tudarmstadt.ukp.inception.project.export.task;

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskHandle;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.project.export.model.MProjectExportStateUpdate;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class NotifyingProjectExportTaskMonitor
    extends ProjectExportTaskMonitor
{
    private final SimpMessagingTemplate msgTemplate;

    private MProjectExportStateUpdate lastUpdate;

    public NotifyingProjectExportTaskMonitor(Project aProject, ProjectExportTaskHandle aHandle,
            String aTitle, String aFilenamePrefix, SimpMessagingTemplate aMsgTemplate)
    {
        super(aProject, aHandle, aTitle, aFilenamePrefix);
        msgTemplate = aMsgTemplate;
    }

    @Override
    public synchronized void setProgress(int aProgress)
    {
        super.setProgress(aProgress);
        sendNotification();
    }

    @Override
    public void addMessage(LogMessage aMessage)
    {
        super.addMessage(aMessage);
        sendNotification();
    }

    @Override
    public synchronized void setState(ProjectExportTaskState aState)
    {
        super.setState(aState);
        sendNotification();
    }

    @Override
    public synchronized void setStateAndProgress(ProjectExportTaskState aState, int aProgress)
    {
        super.setState(aState);
        super.setProgress(aProgress);
        sendNotification();
    }

    @Override
    protected void onDestroy()
    {
        MProjectExportStateUpdate msg = new MProjectExportStateUpdate(this, true);
        msgTemplate.convertAndSend("/topic" + NS_PROJECT + "/" + getProjectId() + "/exports", msg);
    }

    private void sendNotification()
    {
        if (isDestroyed()) {
            return;
        }

        MProjectExportStateUpdate msg = new MProjectExportStateUpdate(this);
        if (lastUpdate == null || !lastUpdate.equals(msg)) {
            msgTemplate.convertAndSend("/topic" + NS_PROJECT + "/" + getProjectId() + "/exports",
                    msg);
            lastUpdate = msg;
        }
    }
}
