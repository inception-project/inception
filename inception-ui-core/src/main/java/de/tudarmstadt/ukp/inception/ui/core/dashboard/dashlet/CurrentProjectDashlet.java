/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.github.rjeschke.txtmark.Processor;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class CurrentProjectDashlet
    extends DashLet_ImplBase
{
    private static final long serialVersionUID = 7732921923832675326L;
    
    private final IModel<Project> projectModel;
    
    public CurrentProjectDashlet(String aId, IModel<Project> aCurrentProject)
    {
        super(aId);
        projectModel = aCurrentProject;
        add(new Label("name", LoadableDetachableModel.of(this::getProjectName)));
        
        add(new Label("description", LoadableDetachableModel.of(this::getProjectDescription))
                .setEscapeModelStrings(false));
    }
    
    private String getProjectName()
    {
        Project project = projectModel.getObject();
        if (project != null) {
            return project.getName();
        }
        else {
            return "No project selected";
        }
    }
    
    private String getProjectDescription()
    {
        Project project = projectModel.getObject();
        if (project != null) {
            if (StringUtils.isBlank(project.getDescription())) {
                return "Project has no description.";
            }
            else {
                return Processor.process(project.getDescription(), true);
            }
        }
        else {
            return "Please select a project from the drop-down list above.";
        }
    }
}
