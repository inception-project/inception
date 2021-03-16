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
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class AjaxProjectImportedEvent
{
    final private AjaxRequestTarget target;
    final private List<Project> project;

    public AjaxProjectImportedEvent(AjaxRequestTarget aTarget, Project... aProjects)
    {
        this.target = aTarget;
        this.project = asList(aProjects);
    }

    public AjaxProjectImportedEvent(AjaxRequestTarget aTarget, List<Project> aProjects)
    {
        this.target = aTarget;
        this.project = aProjects;
    }

    public AjaxRequestTarget getTarget()
    {
        return target;
    }

    public List<Project> getProjects()
    {
        return project;
    }
}
