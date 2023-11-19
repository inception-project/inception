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
package de.tudarmstadt.ukp.inception.project.api.event;

import java.util.zip.ZipFile;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Deprecated
public class ProjectImportEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = 5604222911753768415L;

    private final ZipFile zip;
    private final ExportedProject exportedProject;
    private final Project project;

    public ProjectImportEvent(Object aSource, ZipFile aZip, ExportedProject aExportedProject,
            Project aProject)
    {
        super(aSource);
        zip = aZip;
        exportedProject = aExportedProject;
        project = aProject;
    }

    public Project getProject()
    {
        return project;
    }

    public ZipFile getZip()
    {
        return zip;
    }

    public de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject getExportedProject()
    {
        return exportedProject;
    }
}
