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
package de.tudarmstadt.ukp.clarin.webanno.api.export;

import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

public class ProjectExportRequest
    implements Serializable
{
    private static final long serialVersionUID = -4486934192675904995L;

    public static final String FORMAT_AUTO = "AUTO";

    public int progress = 0;
    
    private Project project;
    private String format;
    private boolean includeInProgress;
    
    private final Queue<LogMessage> messages = new ConcurrentLinkedQueue<>();

    public ProjectExportRequest()
    {
        // Nothing to do;
    }
    
    /**
     * Create a new project export request. Use this constructor if the project is not known yet
     * or may change. Make sure to set the project via the setter before starting the export.
     */
    public ProjectExportRequest(String aFormat, boolean aIncludeInProgress)
    {
        format = aFormat;
        project = null;
        includeInProgress = aIncludeInProgress;
    }
    
    public ProjectExportRequest(Project aProject, String aFormat, boolean aIncludeInProgress)
    {
        format = aFormat;
        project = aProject;
        includeInProgress = aIncludeInProgress;
    }
    
    public void setProject(Project aProject)
    {
        project = aProject;
    }
    
    public Project getProject()
    {
        return project;
    }
    
    /**
     * Set the ID of the export format.
     * 
     * @see FormatSupport#getId()
     */
    public void setFormat(String aFormat)
    {
        format = aFormat;
    }
    
    /**
     * Get the ID of the export format.
     * 
     * @see FormatSupport#getId()
     */
    public String getFormat()
    {
        return format;
    }

    public void setIncludeInProgress(boolean aIncludeInProgress)
    {
        includeInProgress = aIncludeInProgress;
    }
    
    public boolean isIncludeInProgress()
    {
        return includeInProgress;
    }

    public void addMessage(LogMessage aMessage)
    {
        // Avoid repeating the same message over for different users
        if (!messages.contains(aMessage)) {
            messages.add(aMessage);
        }
    }
    
    public Queue<LogMessage> getMessages()
    {
        return messages;
    }
}
