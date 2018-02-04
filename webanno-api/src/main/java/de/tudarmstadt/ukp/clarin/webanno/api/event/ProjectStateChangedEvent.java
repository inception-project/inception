/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.event;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;

public class ProjectStateChangedEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = -8212153885477218226L;

    private Project project;
    private ProjectState previousState;
    private ProjectState newState;

    public ProjectStateChangedEvent(Object aSource, Project aProject, ProjectState aPreviousState)
    {
        super(aSource);
        project = aProject;
        previousState = aPreviousState;
        newState = aProject.getState();
    }

    public Project getProject()
    {
        return project;
    }

    public ProjectState getPreviousState()
    {
        return previousState;
    }

    public ProjectState getNewState()
    {
        return newState;
    }
}
