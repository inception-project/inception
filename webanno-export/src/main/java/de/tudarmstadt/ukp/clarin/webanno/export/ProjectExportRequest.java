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
package de.tudarmstadt.ukp.clarin.webanno.export;

import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class ProjectExportRequest
    implements Serializable
{
    private static final long serialVersionUID = -4486934192675904995L;
    
    public static final String FORMAT_AUTO = "AUTO";
    
    public String format;
    public Project project;
    public int progress = 0;
    public final Queue<String> messages;
            
    public ProjectExportRequest(Project aProject, String aFormat)
    {
        format = aFormat;
        project = aProject;
        progress = 0;
        messages = new ConcurrentLinkedQueue<>();
    }
}
